package com.rover.control.ui.arm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.rover.control.bluetooth.BluetoothService
import com.rover.control.databinding.FragmentArmBinding

/**
 * Tela de controle do braço robótico.
 *
 * Sliders A1–A6 controlam cada servo individualmente.
 * Botões: Garra abrir/fechar, Home, Presets, Gravar/Reproduzir.
 */
class ArmFragment : Fragment() {

    private var _binding: FragmentArmBinding? = null
    private val binding get() = _binding!!

    // Posição atual de cada servo (índice 0–5)
    private val positions = IntArray(6) { 90 }

    // Listeners individuais para cada slider
    private val seekListeners = Array(6) { index ->
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                positions[index] = progress
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
        setupSliders()
        setupButtons()
        setupRecording()
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
            BluetoothService.setAllServos(positions)
        }
    }

    // ── Gravação e Playback ───────────────────────────────────────────────────
    private var isRecording = false
    private var isPlaying   = false

    private fun setupRecording() {
        binding.seekDelay.max = 4900          // 100–5000 ms
        binding.seekDelay.progress = 400      // default 500 ms
        updateDelayLabel(500)

        binding.seekDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val ms = progress + 100
                updateDelayLabel(ms)
                if (fromUser) BluetoothService.setDelay(ms)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.seekRepeat.max = 9
        binding.seekRepeat.progress = 0
        updateRepeatLabel(1)

        binding.seekRepeat.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val rep = progress + 1
                updateRepeatLabel(rep)
                if (fromUser) BluetoothService.setRepeat(rep)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.btnRecord.setOnClickListener {
            isRecording = !isRecording
            if (isRecording) {
                BluetoothService.recStart()
                binding.btnRecord.text = "⏹ Parar Gravação"
                binding.btnRecord.setBackgroundColor(android.graphics.Color.parseColor("#D32F2F"))
            } else {
                BluetoothService.recStop()
                binding.btnRecord.text = "⏺ Gravar"
                binding.btnRecord.setBackgroundColor(android.graphics.Color.parseColor("#00C853"))
            }
        }

        binding.btnPlay.setOnClickListener {
            isPlaying = !isPlaying
            if (isPlaying) {
                BluetoothService.recPlay()
                binding.btnPlay.text = "⏸ Pausar"
            } else {
                BluetoothService.recPause()
                binding.btnPlay.text = "▶ Reproduzir"
            }
        }

        binding.btnClearRec.setOnClickListener {
            BluetoothService.recClear()
            isRecording = false
            isPlaying   = false
            binding.btnRecord.text = "⏺ Gravar"
            binding.btnPlay.text   = "▶ Reproduzir"
        }
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
        for (i in 0..5) { positions[i] = 90; updateAngleLabel(i, 90) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
