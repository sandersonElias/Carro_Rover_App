package com.rover.control.ui.connect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rover.control.databinding.ItemDeviceBinding

class DeviceAdapter(
    private val onSelect: (BluetoothDevice) -> Unit
) : ListAdapter<BluetoothDevice, DeviceAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemDeviceBinding) :
        RecyclerView.ViewHolder(b.root) {

        @SuppressLint("MissingPermission")
        fun bind(device: BluetoothDevice) {
            b.tvDeviceName.text = device.name ?: "Dispositivo desconhecido"
            b.tvDeviceAddress.text = device.address
            b.root.setOnClickListener { onSelect(device) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BluetoothDevice>() {
            override fun areItemsTheSame(a: BluetoothDevice, b: BluetoothDevice) =
                a.address == b.address
            override fun areContentsTheSame(a: BluetoothDevice, b: BluetoothDevice) =
                a.address == b.address
        }
    }
}
