package com.rover.control.ui.arm

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.rover.control.bluetooth.BluetoothService
import com.rover.control.databinding.FragmentArmBinding
import com.rover.control.ui.connect.ConnectActivity
import com.rover.control.ui.excavator.ExcavatorActivity
import kotlinx.coroutines.launch

/**
 * Tela de controle do braço robótico.
 *
 * Sliders A1–A6 controlam cada servo individualmente.
 * Botões: Garra abrir/fechar, Home, Presets, Gravar/Reproduzir.
 */
class ArmFragment : Fragment() {

    private var _binding: FragmentArmBinding? = null
    private val binding get() = _binding!!
    private val vm: ArmViewModel by activityViewModels()

    private var recordingPulseAnimator: ObjectAnimator? = null

    // Listeners individuais para cada slider
    private val seekListeners = Array(6) { index ->
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                vm.positions[index] = progress
                updateAngleLabel(index, progress)
                if (fromUser) BluetoothService.setServo(index + 1, progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreState()
        setupSliders()
        setupButtons()
        setupRecording()
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
                binding.contentScroll.alpha = if (isConnected) 1f else 0.3f
            }
        }
    }

    private fun restoreState() {
        // Restore slider positions from ViewModel
        val seekBars = listOf(
            binding.seekA1, binding.seekA2, binding.seekA3,
            binding.seekA4, binding.seekA5, binding.seekA6
        )
        seekBars.forEachIndexed { i, sb ->
            sb.progress = vm.positions[i]
            updateAngleLabel(i, vm.positions[i])
        }
    }

    // ── Sliders A1–A6 ─────────────────────────────────────────────────────────
    private fun setupSliders() {
        val seekBars = listOf(
            binding.seekA1, binding.seekA2, binding.seekA3,
            binding.seekA4, binding.seekA5, binding.seekA6
        )
        seekBars.forEachIndexed { i, sb ->
            sb.max = 180
            sb.progress = 90
            sb.setOnSeekBarChangeListener(seekListeners[i])
        }
        // Labels iniciais
        for (i in 0..5) updateAngleLabel(i, 90)
    }

    private fun updateAngleLabel(index: Int, angle: Int) {
        val label = when (index) {
            0 -> binding.tvA1
            1 -> binding.tvA2
            2 -> binding.tvA3
            3 -> binding.tvA4
            4 -> binding.tvA5
            5 -> binding.tvA6
            else -> return
        }
        val names = arrayOf("Base", "Ombro", "Cotovelo", "Pulso↕", "Pulso↻", "Garra")
        label.text = "${names[index]}: $angle°"
    }

    // ── Botões de controle ────────────────────────────────────────────────────
    private fun setupButtons() {
        binding.btnOpenGripper.setOnClickListener  { BluetoothService.openGripper() }
        binding.btnCloseGripper.setOnClickListener { BluetoothService.closeGripper() }

        binding.btnHome.setOnClickListener {
            BluetoothService.goHome()
            resetSlidersToCentre()
        }

        binding.btnPreset1.setOnClickListener { BluetoothService.sendButton("D") }
        binding.btnPreset2.setOnClickListener { BluetoothService.sendButton("d") }
        binding.btnPreset3.setOnClickListener { BluetoothService.sendButton("E") }
        binding.btnPreset4.setOnClickListener { BluetoothService.sendButton("e") }

        binding.btnSendAll.setOnClickListener {
            BluetoothService.setAllServos(vm.positions)
        }

        binding.btnExcavatorMode.setOnClickListener {
            startActivity(Intent(requireContext(), ExcavatorActivity::class.java))
        }
    }

    // ── Gravação e Playback ───────────────────────────────────────────────────

    private fun setupRecording() {
        binding.seekDelay.max = 4900          // 100–5000 ms
        binding.seekDelay.progress = vm.delayMs - 100
        updateDelayLabel(vm.delayMs)

        binding.seekDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val ms = progress + 100
                vm.delayMs = ms
                updateDelayLabel(ms)
                if (fromUser) BluetoothService.setDelay(ms)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.seekRepeat.max = 9
        binding.seekRepeat.progress = vm.repeatCount - 1
        updateRepeatLabel(vm.repeatCount)

        binding.seekRepeat.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val rep = progress + 1
                vm.repeatCount = rep
                updateRepeatLabel(rep)
                if (fromUser) BluetoothService.setRepeat(rep)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        updateRecordButtonState()
        updatePlayButtonState()

        binding.btnRecord.setOnClickListener {
            vm.isRecording = !vm.isRecording
            updateRecordButtonState()
            if (vm.isRecording) {
                BluetoothService.recStart()
                startRecordingPulse()
            } else {
                BluetoothService.recStop()
                stopRecordingPulse()
            }
        }

        binding.btnPlay.setOnClickListener {
            vm.isPlaying = !vm.isPlaying
            updatePlayButtonState()
            if (vm.isPlaying) {
                BluetoothService.recPlay()
            } else {
                BluetoothService.recPause()
            }
        }

        binding.btnClearRec.setOnClickListener {
            BluetoothService.recClear()
            vm.isRecording = false
            vm.isPlaying   = false
            updateRecordButtonState()
            updatePlayButtonState()
            stopRecordingPulse()
        }
    }

    private fun updateRecordButtonState() {
        if (vm.isRecording) {
            binding.btnRecord.text = "⏹ Parar"
            binding.tvRecordingStatus.text = "● Gravando..."
            binding.tvRecordingStatus.visibility = View.VISIBLE
        } else {
            binding.btnRecord.text = "⏺ Gravar"
            binding.tvRecordingStatus.visibility = View.GONE
        }
    }

    private fun updatePlayButtonState() {
        binding.btnPlay.text = if (vm.isPlaying) "⏸ Pausar" else "▶ Reproduzir"
    }

    private fun updateDelayLabel(ms: Int) {
        binding.tvDelay.text = "Delay: ${ms}ms"
    }

    private fun updateRepeatLabel(rep: Int) {
        binding.tvRepeat.text = "Repetições: $rep"
    }

    private fun resetSlidersToCentre() {
        val seekBars = listOf(
            binding.seekA1, binding.seekA2, binding.seekA3,
            binding.seekA4, binding.seekA5, binding.seekA6
        )
        seekBars.forEach { it.progress = 90 }
        for (i in 0..5) { vm.positions[i] = 90; updateAngleLabel(i, 90) }
    }

    // ── Recording Pulse Animation ─────────────────────────────────────────────
    private fun startRecordingPulse() {
        recordingPulseAnimator = ObjectAnimator.ofFloat(
            binding.tvRecordingStatus,
            "alpha",
            1f, 0.3f
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
        binding.tvRecordingStatus.alpha = 1f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecordingPulse()
        _binding = null
    }
}
