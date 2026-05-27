package com.rover.control.ui.connect

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rover.control.bluetooth.BluetoothService
import com.rover.control.databinding.ActivityConnectBinding
import kotlinx.coroutines.launch

class ConnectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectBinding
    private lateinit var adapter: DeviceAdapter
    private var btAdapter: BluetoothAdapter? = null

    // ── Permissões ────────────────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) loadPairedDevices()
        else Snackbar.make(binding.root, "Permissão Bluetooth necessária", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val btManager = getSystemService(BluetoothManager::class.java)
        btAdapter = btManager.adapter

        if (btAdapter == null) {
            Snackbar.make(binding.root, "Dispositivo não suporta Bluetooth", Snackbar.LENGTH_INDEFINITE).show()
            return
        }

        adapter = DeviceAdapter { device -> connectTo(device) }
        binding.recyclerDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerDevices.adapter = adapter

        binding.btnRefresh.setOnClickListener { requestPermissionsAndLoad() }

        observeConnectionState()
        requestPermissionsAndLoad()
    }

    private fun requestPermissionsAndLoad() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) loadPairedDevices()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun loadPairedDevices() {
        val paired: Set<BluetoothDevice> = btAdapter?.bondedDevices ?: emptySet()
        adapter.submitList(paired.toList())
        binding.tvEmpty.visibility = if (paired.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun connectTo(device: BluetoothDevice) {
        binding.progressBar.visibility = View.VISIBLE
        BluetoothService.connect(device)
    }

    private fun observeConnectionState() {
        lifecycleScope.launch {
            BluetoothService.state.collect { state ->
                when (state) {
                    BluetoothService.State.CONNECTED -> {
                        binding.progressBar.visibility = View.GONE
                        Snackbar.make(binding.root, "Conectado!", Snackbar.LENGTH_SHORT).show()
                        finish()
                    }
                    BluetoothService.State.ERROR -> {
                        binding.progressBar.visibility = View.GONE
                        Snackbar.make(binding.root,
                            BluetoothService.lastError.value ?: "Erro de conexão",
                            Snackbar.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
