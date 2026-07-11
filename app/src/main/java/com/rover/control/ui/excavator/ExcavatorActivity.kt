package com.rover.control.ui.excavator

import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import com.rover.control.bluetooth.BluetoothService
import com.rover.control.databinding.ActivityExcavatorBinding

/**
 * Modo Escavadeira - Controle do braço robótico com dois joysticks.
 *
 * Joystick Esquerdo:
 *   - Eixo X: Base (servo 1) - 0° a 180°
 *   - Eixo Y: Ombro (servo 2) - 30° a 150°
 *
 * Joystick Direito:
 *   - Eixo X: Cotovelo (servo 3) - 20° a 160°
 *   - Eixo Y: Pulso Roll (servo 5) - 0° a 180°
 *
 * Botões:
 *   - Garra Abrir/Fechar (servo 6)
 *   - Home (todos em 90°)
 *   - Voltar (fecha Activity)
 */
class ExcavatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExcavatorBinding

    // Ângulos atuais dos servos
    private var baseAngle = 90
    private var ombroAngle = 90
    private var cotoveloAngle = 90
    private var pulsoAngle = 90

    // Throttle para cada joystick
    private var lastLeftTime = 0L
    private var lastRightTime = 0L
    private val throttleMs = 40L  // ~25Hz

    // Limites dos servos
    private companion object {
        const val BASE_MIN = 0
        const val BASE_MAX = 180
        const val OMBRO_MIN = 30
        const val OMBRO_MAX = 150
        const val COTOVELO_MIN = 20
        const val COTOVELO_MAX = 160
        const val PULSO_MIN = 0
        const val PULSO_MAX = 180
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExcavatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupJoysticks()
        setupButtons()
    }

    private fun setupJoysticks() {
        // Joystick Esquerdo: Base (X) + Ombro (Y)
        binding.joystickLeft.onMove = { x, y ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastLeftTime >= throttleMs) {
                lastLeftTime = now

                // Converte joystick para ângulos
                baseAngle = joystickToAngle(x, BASE_MIN, BASE_MAX)
                ombroAngle = joystickToAngle(-y, OMBRO_MIN, OMBRO_MAX)

                // Envia comandos
                BluetoothService.setServo(1, baseAngle)
                BluetoothService.setServo(2, ombroAngle)

                // Atualiza labels
                binding.tvBaseAngle.text = "Base: ${baseAngle}°"
                binding.tvOmbroAngle.text = "Ombro: ${ombroAngle}°"
            }
        }

        // Joystick Direito: Cotovelo (X) + Pulso Roll (Y)
        binding.joystickRight.onMove = { x, y ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastRightTime >= throttleMs) {
                lastRightTime = now

                // Converte joystick para ângulos
                cotoveloAngle = joystickToAngle(x, COTOVELO_MIN, COTOVELO_MAX)
                pulsoAngle = joystickToAngle(-y, PULSO_MIN, PULSO_MAX)

                // Envia comandos
                BluetoothService.setServo(3, cotoveloAngle)
                BluetoothService.setServo(5, pulsoAngle)

                // Atualiza labels
                binding.tvCotoveloAngle.text = "Cotovelo: ${cotoveloAngle}°"
                binding.tvPulsoAngle.text = "Pulso: ${pulsoAngle}°"
            }
        }
    }

    private fun setupButtons() {
        binding.btnGripperOpen.setOnClickListener {
            BluetoothService.openGripper()
        }

        binding.btnGripperClose.setOnClickListener {
            BluetoothService.closeGripper()
        }

        binding.btnHome.setOnClickListener {
            BluetoothService.goHome()
            // Reseta ângulos locais
            baseAngle = 90
            ombroAngle = 90
            cotoveloAngle = 90
            pulsoAngle = 90
            binding.tvBaseAngle.text = "Base: 90°"
            binding.tvOmbroAngle.text = "Ombro: 90°"
            binding.tvCotoveloAngle.text = "Cotovelo: 90°"
            binding.tvPulsoAngle.text = "Pulso: 90°"
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    /**
     * Converte valor do joystick (-1.0 a 1.0) para ângulo do servo.
     */
    private fun joystickToAngle(value: Float, min: Int, max: Int): Int {
        // value: -1.0 a 1.0
        // Converte para 0.0 a 1.0
        val normalized = (value + 1f) / 2f
        // Converte para ângulo
        val angle = (normalized * (max - min) + min).toInt()
        return angle.coerceIn(min, max)
    }

    override fun onPause() {
        super.onPause()
        // Para motores ao sair
        BluetoothService.stop()
    }
}
