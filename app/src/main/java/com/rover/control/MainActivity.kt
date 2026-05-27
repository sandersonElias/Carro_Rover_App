package com.rover.control

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
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

        observeBluetoothState()
    }

    private fun observeBluetoothState() {
        lifecycleScope.launch {
            BluetoothService.state.collect { state ->
                when (state) {
                    BluetoothService.State.CONNECTED ->
                        binding.statusIndicator.setImageResource(R.drawable.ic_bt_connected)
                    BluetoothService.State.CONNECTING ->
                        binding.statusIndicator.setImageResource(R.drawable.ic_bt_connecting)
                    BluetoothService.State.ERROR -> {
                        binding.statusIndicator.setImageResource(R.drawable.ic_bt_disconnected)
                        BluetoothService.lastError.value?.let { msg ->
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        }
                    }
                    BluetoothService.State.DISCONNECTED ->
                        binding.statusIndicator.setImageResource(R.drawable.ic_bt_disconnected)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_connect -> {
                startActivity(Intent(this, ConnectActivity::class.java))
                true
            }
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
