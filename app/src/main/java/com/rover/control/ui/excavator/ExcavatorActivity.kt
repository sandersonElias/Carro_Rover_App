package com.rover.control.ui.excavator

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rover.control.bluetooth.BluetoothService
import com.rover.control.databinding.ActivityExcavatorBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modo Escavadeira - Controle do braço robótico com dois joysticks.
 *
 * Usa suavização por delta máximo para evitar movimentos bruscos.
 * Suporta gravação de movimentos no Android e reprodução com o mesmo timing.
 *
 * Joystick Esquerdo:
 *   - Eixo X: Base (servo 1) - 0° a 180°
 *   - Eixo Y: Ombro (servo 2) - 30° a 150°
 *
 * Joystick Direito:
 *   - Eixo X: Cotovelo (servo 3) - 20° a 160°
 *   - Eixo Y: Pulso Roll (servo 5) - 0° a 180°
 */
class ExcavatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExcavatorBinding

    // ── Servo angles ─────────────────────────────────────────────────────────
    private var baseTarget = 90;    private var baseCurrent = 90
    private var ombroTarget = 90;   private var ombroCurrent = 90
    private var cotoveloTarget = 90; private var cotoveloCurrent = 90
    private var pulsoTarget = 90;   private var pulsoCurrent = 90

    // ── Throttle ─────────────────────────────────────────────────────────────
    private var lastLeftTime = 0L
    private var lastRightTime = 0L
    private val throttleMs = 40L
    private val maxAngleDelta = 4

    // ── Gravação ─────────────────────────────────────────────────────────────
    private data class Frame(
        val base: Int,
        val ombro: Int,
        val cotovelo: Int,
        val pulso: Int,
        val timestamp: Long
    )

    private var isRecording = false
    private var isPlaying = false
    private val recordedFrames = mutableListOf<Frame>()
    private var recordingStartTime = 0L
    private var playbackJob: Job? = null
    private var recordingPulseAnimator: ObjectAnimator? = null

    // ── Limites dos servos ───────────────────────────────────────────────────
    private companion object {
        const val BASE_MIN = 0;      const val BASE_MAX = 180
        const val OMBRO_MIN = 30;    const val OMBRO_MAX = 150
        const val COTOVELO_MIN = 20; const val COTOVELO_MAX = 160
        const val PULSO_MIN = 0;     const val PULSO_MAX = 180
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExcavatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupJoysticks()
        setupButtons()
        setupRecording()
    }

    // ── Joysticks ────────────────────────────────────────────────────────────

    private fun setupJoysticks() {
        binding.joystickLeft.onMove = { x, y ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastLeftTime >= throttleMs) {
                lastLeftTime = now

                baseTarget = joystickToAngle(x, BASE_MIN, BASE_MAX)
                ombroTarget = joystickToAngle(-y, OMBRO_MIN, OMBRO_MAX)

                baseCurrent = moveToward(baseCurrent, baseTarget)
                ombroCurrent = moveToward(ombroCurrent, ombroTarget)

                BluetoothService.setServo(1, baseCurrent)
                BluetoothService.setServo(2, ombroCurrent)

                binding.tvBaseAngle.text = "Base: ${baseCurrent}°"
                binding.tvOmbroAngle.text = "Ombro: ${ombroCurrent}°"

                if (isRecording) recordFrame()
            }
        }

        binding.joystickRight.onMove = { x, y ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastRightTime >= throttleMs) {
                lastRightTime = now

                cotoveloTarget = joystickToAngle(x, COTOVELO_MIN, COTOVELO_MAX)
                pulsoTarget = joystickToAngle(-y, PULSO_MIN, PULSO_MAX)

                cotoveloCurrent = moveToward(cotoveloCurrent, cotoveloTarget)
                pulsoCurrent = moveToward(pulsoCurrent, pulsoTarget)

                BluetoothService.setServo(3, cotoveloCurrent)
                BluetoothService.setServo(5, pulsoCurrent)

                binding.tvCotoveloAngle.text = "Cotovelo: ${cotoveloCurrent}°"
                binding.tvPulsoAngle.text = "Pulso: ${pulsoCurrent}°"

                if (isRecording) recordFrame()
            }
        }
    }

    // ── Buttons ──────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnGripperOpen.setOnClickListener { BluetoothService.openGripper() }
        binding.btnGripperClose.setOnClickListener { BluetoothService.closeGripper() }

        binding.btnHome.setOnClickListener {
            BluetoothService.goHome()
            baseTarget = 90; baseCurrent = 90
            ombroTarget = 90; ombroCurrent = 90
            cotoveloTarget = 90; cotoveloCurrent = 90
            pulsoTarget = 90; pulsoCurrent = 90
            binding.tvBaseAngle.text = "Base: 90°"
            binding.tvOmbroAngle.text = "Ombro: 90°"
            binding.tvCotoveloAngle.text = "Cotovelo: 90°"
            binding.tvPulsoAngle.text = "Pulso: 90°"
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    // ── Gravação ─────────────────────────────────────────────────────────────

    private fun setupRecording() {
        updateRecordButtonState()
        updatePlayButtonState()

        binding.btnRecordExc.setOnClickListener {
            if (isPlaying) return@setOnClickListener

            isRecording = !isRecording
            updateRecordButtonState()

            if (isRecording) {
                recordedFrames.clear()
                recordingStartTime = SystemClock.elapsedRealtime()
                startRecordingPulse()
            } else {
                stopRecordingPulse()
                binding.tvRecStatusExc.text = "${recordedFrames.size} frames gravados"
            }
        }

        binding.btnPlayExc.setOnClickListener {
            if (isRecording) return@setOnClickListener
            if (recordedFrames.isEmpty()) return@setOnClickListener

            isPlaying = !isPlaying
            updatePlayButtonState()

            if (isPlaying) {
                startPlayback()
            } else {
                playbackJob?.cancel()
                stopRecordingPulse()
            }
        }

        binding.btnClearExc.setOnClickListener {
            playbackJob?.cancel()
            isRecording = false
            isPlaying = false
            recordedFrames.clear()
            updateRecordButtonState()
            updatePlayButtonState()
            stopRecordingPulse()
            binding.tvRecStatusExc.text = ""
        }
    }

    private fun recordFrame() {
        val now = SystemClock.elapsedRealtime()
        recordedFrames.add(
            Frame(
                base = baseCurrent,
                ombro = ombroCurrent,
                cotovelo = cotoveloCurrent,
                pulso = pulsoCurrent,
                timestamp = now - recordingStartTime
            )
        )
        binding.tvRecStatusExc.text = "● ${recordedFrames.size} frames"
    }

    private fun startPlayback() {
        startRecordingPulse()
        binding.tvRecStatusExc.text = "Reproduzindo..."

        playbackJob = lifecycleScope.launch {
            val frames = recordedFrames.toList()
            for (i in frames.indices) {
                if (!isPlaying) break

                val frame = frames[i]

                // Espera o timing correto entre frames
                if (i > 0) {
                    val delayMs = frame.timestamp - frames[i - 1].timestamp
                    if (delayMs > 0) delay(delayMs)
                }

                if (!isPlaying) break

                // Envia ângulos diretamente (sem suavização no playback para fidelidade)
                BluetoothService.setServo(1, frame.base)
                BluetoothService.setServo(2, frame.ombro)
                BluetoothService.setServo(3, frame.cotovelo)
                BluetoothService.setServo(5, frame.pulso)

                // Atualiza UI
                baseCurrent = frame.base;   ombroCurrent = frame.ombro
                cotoveloCurrent = frame.cotovelo; pulsoCurrent = frame.pulso
                runOnUiThread {
                    binding.tvBaseAngle.text = "Base: ${frame.base}°"
                    binding.tvOmbroAngle.text = "Ombro: ${frame.ombro}°"
                    binding.tvCotoveloAngle.text = "Cotovelo: ${frame.cotovelo}°"
                    binding.tvPulsoAngle.text = "Pulso: ${frame.pulso}°"
                }
            }

            // Playback terminou
            isPlaying = false
            stopRecordingPulse()
            runOnUiThread {
                updatePlayButtonState()
                binding.tvRecStatusExc.text = "Reprodução concluída"
            }
        }
    }

    private fun updateRecordButtonState() {
        binding.btnRecordExc.text = if (isRecording) "⏹ Parar" else "⏺ Gravar"
        binding.btnPlayExc.isEnabled = !isRecording && recordedFrames.isNotEmpty()
    }

    private fun updatePlayButtonState() {
        binding.btnPlayExc.text = if (isPlaying) "⏸ Pausar" else "▶ Reproduzir"
        binding.btnPlayExc.isEnabled = !isPlaying && recordedFrames.isNotEmpty()
    }

    // ── Recording Pulse Animation ────────────────────────────────────────────

    private fun startRecordingPulse() {
        recordingPulseAnimator = ObjectAnimator.ofFloat(
            binding.tvRecStatusExc, "alpha", 1f, 0.3f
        ).apply {
            duration = 800
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopRecordingPulse() {
        recordingPulseAnimator?.cancel()
        recordingPulseAnimator = null
        binding.tvRecStatusExc.alpha = 1f
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun moveToward(current: Int, target: Int): Int {
        val diff = target - current
        return when {
            diff > maxAngleDelta -> current + maxAngleDelta
            diff < -maxAngleDelta -> current - maxAngleDelta
            else -> target
        }
    }

    private fun joystickToAngle(value: Float, min: Int, max: Int): Int {
        val normalized = (value + 1f) / 2f
        val angle = (normalized * (max - min) + min).toInt()
        return angle.coerceIn(min, max)
    }

    override fun onPause() {
        super.onPause()
        playbackJob?.cancel()
        isRecording = false
        isPlaying = false
        BluetoothService.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingPulse()
    }
}
