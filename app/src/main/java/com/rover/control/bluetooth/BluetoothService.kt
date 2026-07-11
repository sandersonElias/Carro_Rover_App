package com.rover.control.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
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
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val socketMutex = Mutex()

    private val _incoming = MutableStateFlow<String?>(null)
    val incoming: SharedFlow<String?> = _incoming.asSharedFlow()

    // ── Conectar ao dispositivo ───────────────────────────────────────────────
    fun connect(device: BluetoothDevice) {
        connectJob?.cancel()
        connectJob = scope.launch {
            _state.value = State.CONNECTING
            _lastError.value = null
            socketMutex.withLock {
                try {
                    socket?.close()
                    val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    withTimeout(10_000L) { s.connect() }
                    socket = s
                    _state.value = State.CONNECTED
                    startReading()
                } catch (e: IOException) {
                    _state.value = State.ERROR
                    _lastError.value = "Falha ao conectar: ${e.message}"
                    socket = null
                } catch (e: TimeoutCancellationException) {
                    _state.value = State.ERROR
                    _lastError.value = "Timeout na conexão (10s)"
                    socket = null
                }
            }
        }
    }

    // ── Leitura contínua do InputStream ──────────────────────────────────────
    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                val s = socket ?: return@launch
                val reader = BufferedReader(InputStreamReader(s.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    _incoming.value = line
                }
            } catch (_: IOException) {
                // Conexão perdida
            } finally {
                if (_state.value == State.CONNECTED) {
                    _state.value = State.DISCONNECTED
                }
            }
        }
    }

    // ── Desconectar ───────────────────────────────────────────────────────────
    fun disconnect() {
        connectJob?.cancel()
        readJob?.cancel()
        scope.launch {
            socketMutex.withLock {
                try { socket?.close() } catch (_: IOException) {}
                socket = null
            }
            _state.value = State.DISCONNECTED
        }
    }

    // ── Enviar comando (string + '\n') ────────────────────────────────────────
    suspend fun send(command: String) {
        socketMutex.withLock {
            val s = socket ?: return
            try {
                s.outputStream.write("$command\n".toByteArray())
            } catch (e: IOException) {
                _state.value = State.ERROR
                _lastError.value = "Erro ao enviar: ${e.message}"
            }
        }
    }

    // Wrapper não-suspend para callbacks que não podem ser suspend
    fun sendAsync(command: String) {
        scope.launch { send(command) }
    }

    val isConnected: Boolean get() = _state.value == State.CONNECTED

    fun shutdown() {
        readJob?.cancel()
        scope.launch {
            socketMutex.withLock {
                try { socket?.close() } catch (_: IOException) {}
                socket = null
            }
            _state.value = State.DISCONNECTED
            scope.cancel()
        }
    }

    // ── Comandos de movimento ─────────────────────────────────────────────────
    fun moveForward()       = sendAsync("MOV:F")
    fun moveBackward()      = sendAsync("MOV:B")
    fun turnLeft()          = sendAsync("MOV:L")
    fun turnRight()         = sendAsync("MOV:R")
    fun curveForwardLeft()  = sendAsync("MOV:FL")
    fun curveForwardRight() = sendAsync("MOV:FR")
    fun stop()              = sendAsync("MOV:S")

    // Joystick analógico: left e right em -255..255
    fun joystick(left: Int, right: Int) = sendAsync("MOV:JOY:$left:$right")

    // ── Comandos de servo ─────────────────────────────────────────────────────
    fun setServo(index: Int, angle: Int) = sendAsync("SRV:$index:$angle")
    fun setAllServos(angles: IntArray) {
        require(angles.size == 6)
        sendAsync("ALL:${angles.joinToString(",")}")
    }

    // ── Botões ────────────────────────────────────────────────────────────────
    fun sendButton(btn: String) = sendAsync("BTN:$btn")
    fun openGripper()    = sendButton("B")
    fun closeGripper()   = sendButton("b")
    fun goHome()         = sendButton("C")

    // ── Gravação / Playback ───────────────────────────────────────────────────
    fun recStart()       = sendAsync("REC:START")
    fun recStop()        = sendAsync("REC:STOP")
    fun recPlay()        = sendAsync("REC:PLAY")
    fun recPause()       = sendAsync("REC:PAUSE")
    fun recClear()       = sendAsync("REC:CLEAR")
    fun setDelay(ms: Int)    = sendAsync("SET:DELAY:$ms")
    fun setRepeat(count: Int) = sendAsync("SET:REPEAT:$count")
}
