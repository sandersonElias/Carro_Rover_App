package com.rover.control.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.UUID

/**
 * Singleton que gerencia a conexão RFCOMM com o HC-05.
 * Usa coroutines para não bloquear a UI thread.
 */
object BluetoothService {

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    enum class State { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state: StateFlow<State> = _state

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private var socket: BluetoothSocket? = null
    private var connectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Conectar ao dispositivo ───────────────────────────────────────────────
    fun connect(device: BluetoothDevice) {
        connectJob?.cancel()
        connectJob = scope.launch {
            _state.value = State.CONNECTING
            _lastError.value = null
            try {
                socket?.close()
                val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()
                socket = s
                _state.value = State.CONNECTED
            } catch (e: IOException) {
                _state.value = State.ERROR
                _lastError.value = "Falha ao conectar: ${e.message}"
                socket = null
            }
        }
    }

    // ── Desconectar ───────────────────────────────────────────────────────────
    fun disconnect() {
        connectJob?.cancel()
        scope.launch {
            try { socket?.close() } catch (_: IOException) {}
            socket = null
            _state.value = State.DISCONNECTED
        }
    }

    // ── Enviar comando (string + '\n') ────────────────────────────────────────
    fun send(command: String) {
        val s = socket ?: return
        scope.launch {
            try {
                s.outputStream.write("$command\n".toByteArray())
            } catch (e: IOException) {
                _state.value = State.ERROR
                _lastError.value = "Erro ao enviar: ${e.message}"
            }
        }
    }

    val isConnected: Boolean get() = _state.value == State.CONNECTED

    fun shutdown() {
        disconnect()
        scope.cancel()
    }

    // ── Comandos de movimento ─────────────────────────────────────────────────
    fun moveForward()       = send("MOV:F")
    fun moveBackward()      = send("MOV:B")
    fun turnLeft()          = send("MOV:L")
    fun turnRight()         = send("MOV:R")
    fun curveForwardLeft()  = send("MOV:FL")
    fun curveForwardRight() = send("MOV:FR")
    fun stop()              = send("MOV:S")
    fun setSpeed(value: Int) = send("SPD:$value")

    // Joystick analógico: left e right em -255..255
    fun joystick(left: Int, right: Int) = send("MOV:JOY:$left:$right")

    // ── Comandos de servo ─────────────────────────────────────────────────────
    fun setServo(index: Int, angle: Int) = send("SRV:$index:$angle")
    fun setAllServos(angles: IntArray) {
        require(angles.size == 6)
        send("ALL:${angles.joinToString(",")}")
    }

    // ── Botões ────────────────────────────────────────────────────────────────
    fun sendButton(btn: String) = send("BTN:$btn")
    fun openGripper()    = sendButton("B")
    fun closeGripper()   = sendButton("b")
    fun goHome()         = sendButton("C")

    // ── Gravação / Playback ───────────────────────────────────────────────────
    fun recStart()       = send("REC:START")
    fun recStop()        = send("REC:STOP")
    fun recPlay()        = send("REC:PLAY")
    fun recPause()       = send("REC:PAUSE")
    fun recClear()       = send("REC:CLEAR")
    fun setDelay(ms: Int)    = send("SET:DELAY:$ms")
    fun setRepeat(count: Int) = send("SET:REPEAT:$count")
}
