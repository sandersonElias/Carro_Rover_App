package com.rover.control

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.rover.control.bluetooth.BluetoothService
import com.rover.control.databinding.ActivityMainBinding
import com.rover.control.ui.connect.ConnectActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHost.navController)

        setupConnectButton()
        observeBluetoothState()
    }

    private fun setupConnectButton() {
        binding.btnConnect.setOnClickListener {
            startActivity(Intent(this, ConnectActivity::class.java))
        }
    }

    private fun observeBluetoothState() {
        lifecycleScope.launch {
            BluetoothService.state.collect { state ->
                when (state) {
                    BluetoothService.State.CONNECTED -> {
                        binding.tvStatusChip.text = "Conectado"
                        binding.tvStatusChip.setTextColor(
                            ContextCompat.getColor(this@MainActivity, R.color.colorBtConnected)
                        )
                        updateStatusChipBackground(R.color.colorBtConnected)
                        binding.btnConnect.setImageResource(R.drawable.ic_bt_connected)
                    }
                    BluetoothService.State.CONNECTING -> {
                        binding.tvStatusChip.text = "Conectando..."
                        binding.tvStatusChip.setTextColor(
                            ContextCompat.getColor(this@MainActivity, R.color.colorBtConnecting)
                        )
                        updateStatusChipBackground(R.color.colorBtConnecting)
                        binding.btnConnect.setImageResource(R.drawable.ic_bt_connecting)
                    }
                    BluetoothService.State.ERROR -> {
                        binding.tvStatusChip.text = "Erro"
                        binding.tvStatusChip.setTextColor(
                            ContextCompat.getColor(this@MainActivity, R.color.colorError)
                        )
                        updateStatusChipBackground(R.color.colorError)
                        binding.btnConnect.setImageResource(R.drawable.ic_bt_disconnected)
                        BluetoothService.lastError.value?.let { msg ->
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        }
                    }
                    BluetoothService.State.DISCONNECTED -> {
                        binding.tvStatusChip.text = "Desconectado"
                        binding.tvStatusChip.setTextColor(
                            ContextCompat.getColor(this@MainActivity, R.color.colorBtDisconnected)
                        )
                        updateStatusChipBackground(R.color.colorBtDisconnected)
                        binding.btnConnect.setImageResource(R.drawable.ic_bt_connect)
                    }
                }
            }
        }
    }

    private fun updateStatusChipBackground(colorRes: Int) {
        val bg = binding.tvStatusChip.background
        if (bg is GradientDrawable) {
            bg.setColor(ContextCompat.getColor(this, R.color.colorSurfaceVariant))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_disconnect -> {
                BluetoothService.disconnect()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothService.shutdown()
    }
}
