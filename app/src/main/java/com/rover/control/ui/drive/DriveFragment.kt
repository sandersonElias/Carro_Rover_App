package com.rover.control.ui.drive

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rover.control.bluetooth.BluetoothService
import com.rover.control.databinding.FragmentDriveBinding
import com.rover.control.ui.connect.ConnectActivity
import kotlinx.coroutines.launch

/**
 * Tela de controle de movimento do rover.
 *
 * Joystick principal controla direção (frente/trás/esquerda/direita).
 * D-Pad para movimentos digitais.
 * Velocidade constante (sem PWM).
 */
class DriveFragment : Fragment() {

    private var _binding: FragmentDriveBinding? = null
    private val binding get() = _binding!!

    private var lastJoystickTime = 0L
    private val joystickThrottleMs = 40L  // ~25Hz max

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
        setupEmergencyStop()
        setupConnectionOverlay()
        observeBluetoothState()
    }

    // ── Connection Overlay ──────────────────────────────────────────────────
    private fun setupConnectionOverlay() {
        binding.btnConnectOverlay.setOnClickListener {
            startActivity(Intent(requireContext(), ConnectActivity::class.java))
        }
    }

    private fun observeBluetoothState() {
        viewLifecycleOwner.lifecycleScope.launch {
            BluetoothService.state.collect { state ->
                val isConnected = state == BluetoothService.State.CONNECTED
                binding.overlayConnection.isVisible = !isConnected
                binding.contentLayout.alpha = if (isConnected) 1f else 0.3f
            }
        }
    }

    // ── Joystick analógico ────────────────────────────────────────────────────
    private fun setupJoystick() {
        binding.joystick.onMove = { x, y ->
            if (BluetoothService.isConnected) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastJoystickTime < joystickThrottleMs) {
                    // Throttle: ignora movimentos muito rápidos
                } else {
                    lastJoystickTime = now

                    // Converte joystick para tanque (tank steering)
                    // left  = y - x   (motor esquerdo)
                    // right = y + x   (motor direito)
                    val forward = -y
                    val turn    = x

                    val leftInt  = ((forward - turn) * 255).toInt().coerceIn(-255, 255)
                    val rightInt = ((forward + turn) * 255).toInt().coerceIn(-255, 255)

                    BluetoothService.joystick(leftInt, rightInt)

                    // Update direction indicator
                    updateDirectionIndicator(forward, turn)
                }
            }
        }
    }

    private fun updateDirectionIndicator(forward: Float, turn: Float) {
        val direction = when {
            forward > 0.1f && turn.abs() < 0.3f -> "Frente"
            forward < -0.1f && turn.abs() < 0.3f -> "Ré"
            turn > 0.3f && forward.abs() < 0.3f -> "Direita"
            turn < -0.3f && forward.abs() < 0.3f -> "Esquerda"
            forward > 0.1f && turn > 0.1f -> "Frente-Direita"
            forward > 0.1f && turn < -0.1f -> "Frente-Esquerda"
            forward < -0.1f && turn > 0.1f -> "Ré-Direita"
            forward < -0.1f && turn < -0.1f -> "Ré-Esquerda"
            else -> "Parado"
        }
        binding.tvDirection.text = direction
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

    // ── Stop de emergência ────────────────────────────────────────────────────
    private fun setupEmergencyStop() {
        binding.btnEmergencyStop.setOnClickListener {
            BluetoothService.stop()
            binding.joystick.onMove?.invoke(0f, 0f)
            binding.tvDirection.text = "Parado"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BluetoothService.stop()
        _binding = null
    }
}

// Extension function for Float.abs()
private fun Float.abs(): Float = kotlin.math.abs(this)
