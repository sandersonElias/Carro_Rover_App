package com.rover.control.ui.drive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rover.control.bluetooth.BluetoothService
import com.rover.control.databinding.FragmentDriveBinding
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tela de controle de movimento do rover.
 *
 * Layout superior: Joystick principal (controla frente/trás + virar)
 * Layout inferior: Botões de ajuste de velocidade + STOP de emergência
 *
 * Conversão joystick → tanque (tank steering):
 *   left  = y - x   (motor esquerdo)
 *   right = y + x   (motor direito)
 */
class DriveFragment : Fragment() {

    private var _binding: FragmentDriveBinding? = null
    private val binding get() = _binding!!

    private var currentSpeed = 200  // 0–255

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDriveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupJoystick()
        setupDpad()
        setupSpeedControls()
        setupEmergencyStop()
    }

    // ── Joystick analógico ────────────────────────────────────────────────────
    private fun setupJoystick() {
        binding.joystick.onMove = { x, y ->
            if (BluetoothService.isConnected) {
                val forward = -y
                val turn    = x

                val leftRaw  = (forward - turn) * currentSpeed
                val rightRaw = (forward + turn) * currentSpeed

                val maxVal   = maxOf(abs(leftRaw), abs(rightRaw), currentSpeed.toFloat())
                val scale    = if (maxVal > currentSpeed) currentSpeed / maxVal else 1f

                val leftInt  = (leftRaw  * scale).roundToInt().coerceIn(-255, 255)
                val rightInt = (rightRaw * scale).roundToInt().coerceIn(-255, 255)

                BluetoothService.joystick(leftInt, rightInt)
            }
        }
    }

    // ── D-Pad (botões de direção) ──────────────────────────────────────────────
    private fun setupDpad() {
        fun hold(view: View, onPress: () -> Unit) {
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> onPress()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> BluetoothService.stop()
                }
                true
            }
        }

        hold(binding.btnUp)    { BluetoothService.moveForward() }
        hold(binding.btnDown)  { BluetoothService.moveBackward() }
        hold(binding.btnLeft)  { BluetoothService.turnLeft() }
        hold(binding.btnRight) { BluetoothService.turnRight() }
    }

    // ── Controle de velocidade ────────────────────────────────────────────────
    private fun setupSpeedControls() {
        binding.seekSpeed.max = 255
        binding.seekSpeed.progress = currentSpeed
        updateSpeedLabel()

        binding.seekSpeed.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar, progress: Int, fromUser: Boolean) {
                // Velocidade mínima de 60 para os motores arrancarem
                currentSpeed = maxOf(progress, 60)
                updateSpeedLabel()
                if (fromUser) BluetoothService.setSpeed(currentSpeed)
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar) {}
        })
    }

    private fun updateSpeedLabel() {
        val pct = (currentSpeed / 255f * 100).roundToInt()
        binding.tvSpeed.text = "Velocidade: $pct%"
    }

    // ── Stop de emergência ────────────────────────────────────────────────────
    private fun setupEmergencyStop() {
        binding.btnStop.setOnClickListener {
            BluetoothService.stop()
            // Retorna joystick para centro visualmente (evento sintético)
            binding.joystick.onMove?.invoke(0f, 0f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BluetoothService.stop()
        _binding = null
    }
}
