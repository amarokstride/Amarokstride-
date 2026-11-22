package com.example.data.integrations

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class BleWatchDevice(
    val address: String,
    val name: String,
    val rssi: Int
)

data class BleHeartRateUiState(
    val isBluetoothAvailable: Boolean = false,
    val isScanning: Boolean = false,
    val discoveredWatches: List<BleWatchDevice> = emptyList(),
    val connectedAddress: String? = null,
    val connectedName: String? = null,
    val liveHeartRateBpm: Int? = null,
    val statusMessage: String = "Broadcast Heart Rate on your Garmin or COROS watch, then tap Scan."
)

/**
 * Native Android Bluetooth Low Energy (BLE) Heart Rate Monitor client for Garmin, COROS,
 * and standard Bluetooth SIG Heart Rate broadcasting watches/straps.
 *
 * Subscribes to:
 * - Heart Rate Service: 0000180D-0000-1000-8000-00805f9b34fb
 * - Heart Rate Measurement Characteristic: 00002A37-0000-1000-8000-00805f9b34fb
 * - Client Characteristic Configuration Descriptor (CCCD): 00002902-0000-1000-8000-00805f9b34fb
 */
class WatchBleHeartRateManager(
    private val context: Context
) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val _state = MutableStateFlow(
        BleHeartRateUiState(isBluetoothAvailable = bluetoothAdapter?.isEnabled == true)
    )
    val state: StateFlow<BleHeartRateUiState> = _state.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var activeScanCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    fun startScanning() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _state.update {
                it.copy(
                    isBluetoothAvailable = false,
                    isScanning = false,
                    statusMessage = "Enable Bluetooth on your phone to scan for Garmin or COROS HR broadcast."
                )
            }
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _state.update {
                it.copy(
                    isScanning = false,
                    statusMessage = "Bluetooth LE scanner unavailable on this device."
                )
            }
            return
        }

        stopScanning()

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val addr = device.address ?: return
                val name = runCatching { device.name }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
                    ?: "HR Watch ($addr)"
                val entry = BleWatchDevice(address = addr, name = name, rssi = result.rssi)
                _state.update { curr ->
                    val updated = (curr.discoveredWatches.filterNot { it.address == addr } + entry)
                        .sortedByDescending { it.rssi }
                        .take(12)
                    curr.copy(
                        discoveredWatches = updated,
                        statusMessage = "Found ${updated.size} heart-rate sensor(s). Tap a watch to connect."
                    )
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.w(TAG, "BLE scan failed with code=$errorCode")
                _state.update {
                    it.copy(
                        isScanning = false,
                        statusMessage = "BLE scan error ($errorCode). Ensure Bluetooth is enabled."
                    )
                }
            }
        }

        activeScanCallback = callback
        try {
            scanner.startScan(listOf(filter), settings, callback)
            _state.update {
                it.copy(
                    isBluetoothAvailable = true,
                    isScanning = true,
                    statusMessage = "Scanning for Garmin / COROS Bluetooth Heart Rate broadcast..."
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing BLE scan permission", e)
            _state.update {
                it.copy(
                    isScanning = false,
                    statusMessage = "Grant Bluetooth permission to scan for your watch."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "BLE scan start failed", e)
            _state.update {
                it.copy(
                    isScanning = false,
                    statusMessage = "Unable to start BLE scan: ${e.localizedMessage ?: "unknown error"}"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        val cb = activeScanCallback ?: return
        activeScanCallback = null
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(cb)
        } catch (_: Exception) {
        }
        _state.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    fun connectToWatch(address: String, displayName: String) {
        stopScanning()
        disconnect()
        val adapter = bluetoothAdapter ?: return
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(address)
            _state.update {
                it.copy(
                    connectedAddress = address,
                    connectedName = displayName,
                    statusMessage = "Connecting to $displayName..."
                )
            }
            activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(appContext, false, gattCallback)
            }
        } catch (e: SecurityException) {
            _state.update { it.copy(statusMessage = "Bluetooth Connect permission required.") }
        } catch (e: Exception) {
            _state.update { it.copy(statusMessage = "Connection error: ${e.localizedMessage}") }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {
        }
        activeGatt = null
        _state.update {
            it.copy(
                connectedAddress = null,
                connectedName = null,
                liveHeartRateBpm = null,
                statusMessage = "Watch BLE stream disconnected."
            )
        }
    }

    fun release() {
        stopScanning()
        disconnect()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _state.update {
                        it.copy(statusMessage = "Connected to ${it.connectedName ?: "Watch"} · Discovering HR stream...")
                    }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _state.update {
                        it.copy(
                            connectedAddress = null,
                            connectedName = null,
                            liveHeartRateBpm = null,
                            statusMessage = "Watch disconnected."
                        )
                    }
                    try {
                        gatt.close()
                    } catch (_: Exception) {
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val hrService = gatt.getService(HEART_RATE_SERVICE_UUID) ?: run {
                _state.update { it.copy(statusMessage = "Heart Rate service (0x180D) not found on device.") }
                return
            }
            val hrChar = hrService.getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID) ?: return
            gatt.setCharacteristicNotification(hrChar, true)
            val cccd = hrChar.getDescriptor(CCCD_UUID) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(cccd)
            }
            _state.update {
                it.copy(statusMessage = "Live Heart Rate stream active on ${it.connectedName ?: "Watch"}!")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_CHAR_UUID) {
                parseAndEmitHeartRate(value)
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_CHAR_UUID) {
                val bytes = characteristic.value ?: return
                parseAndEmitHeartRate(bytes)
            }
        }
    }

    private fun parseAndEmitHeartRate(data: ByteArray) {
        if (data.size < 2) return
        val flags = data[0].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0
        val bpm = if (!is16Bit) {
            data[1].toInt() and 0xFF
        } else if (data.size >= 3) {
            ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        } else {
            return
        }
        if (bpm in 35..235) {
            _state.update { it.copy(liveHeartRateBpm = bpm) }
        }
    }

    companion object {
        private const val TAG = "WatchBleHRManager"
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_CHAR_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
