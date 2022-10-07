/*
 *     Copyright (C) 2021 Parrot Drones SAS
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions
 *     are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of the Parrot Company nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *     LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *     FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *     PARROT COMPANY BE LIABLE FOR ANY DIRECT, INDIRECT,
 *     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *     BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *     OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *     AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *     OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *     OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *     SUCH DAMAGE.
 *
 */

package com.parrot.camera

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MainCamera
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalance
import com.parrot.drone.groundsdk.device.peripheral.camera2.Camera
import com.parrot.drone.groundsdk.device.peripheral.camera2.MainCamera as MainCamera2

/**
 * Sample code to display and change custom white balance temperature, using `MainCamera` and
 * `camera2.MainCamera` peripherals (respectively Camera1 API and Camera2 API).
 */
class WhiteBalanceTemperature(
    /** Custom white balance temperature spinner. */
    private val whiteBalanceSpinner: Spinner
) {
    /** Reference to `MainCamera` peripheral. */
    private var mainCameraRef: Ref<MainCamera>? = null
    /** Reference to `camera2.MainCamera` peripheral. */
    private var mainCamera2Ref: Ref<MainCamera2>? = null
    /** Index of the currently selected item. */
    private var selectionIndex = AdapterView.INVALID_POSITION

    /**
     * Starts camera peripherals monitoring.
     *
     * @param drone drone to monitor
     */
    fun startMonitoring(drone: Drone) {
        // Drones: ANAFI_4K, ANAFI_THERMAL, ANAFI_USA
        // Monitor `MainCamera` peripheral, for drones supporting Camera1 API.
        // We keep camera reference as a class property, otherwise change notifications would stop.
        mainCameraRef = drone.getPeripheral(MainCamera::class.java) { camera ->
            // Called when the camera changes, on main thread.
            camera?.let {
                updateViewCamera1(camera)
            } ?: run {
                resetView()
            }
        }

        // Drones: ANAFI_2
        // Monitor `camera2.MainCamera` peripheral, for drones supporting Camera2 API.
        // We keep camera reference as a class property, otherwise change notifications would stop.
        mainCamera2Ref = drone.getPeripheral(MainCamera2::class.java) { camera ->
            // Called when the camera changes, on main thread.
            camera?.let {
                updateViewCamera2(camera)
            } ?: run {
                resetView()
            }
        }
    }

    /**
     * Stops camera peripherals monitoring.
     */
    fun stopMonitoring() {
        // Release `MainCamera` peripheral reference.
        mainCameraRef?.close()
        mainCameraRef = null

        // Release `camera2.MainCamera` peripheral reference.
        mainCamera2Ref?.close()
        mainCamera2Ref = null

        resetView()
    }

    /**
     * Resets custom white balance temperature display.
     */
    private fun resetView() {
        whiteBalanceSpinner.isEnabled = false
        whiteBalanceSpinner.adapter = null
        whiteBalanceSpinner.onItemSelectedListener = null
    }

    /**
     * Updates custom white balance temperature display with `MainCamera` peripheral (Camera1 API).
     *
     * @param camera camera peripheral
     */
    private fun updateViewCamera1(camera: MainCamera) {
        // Get the set of supported white balance temperatures.
        val supportedWhiteBalanceTemperatures = camera.whiteBalance().supportedCustomTemperatures()
        // Get the current custom white balance temperature.
        val whiteBalanceTemperature = camera.whiteBalance().customTemperature()
        // Create adapter for white balance temperatures choice.
        val adapter = ArrayAdapter<CameraWhiteBalance.Temperature>(whiteBalanceSpinner.context, android.R.layout.simple_spinner_item)
        // Fill adapter with supported white balance temperatures.
        adapter.addAll(supportedWhiteBalanceTemperatures)
        // Enable spinner if white balance is not currently changing.
        whiteBalanceSpinner.isEnabled = !camera.whiteBalance().isUpdating
        // Set spinner adapter.
        whiteBalanceSpinner.adapter = adapter
        // Tell to spinner the current white balance temperature.
        selectionIndex = adapter.getPosition(whiteBalanceTemperature)
        whiteBalanceSpinner.setSelection(selectionIndex)
        // Register spinner listener.
        whiteBalanceSpinner.onItemSelectedListener = whiteBalanceSpinnerListener
    }

    /**
     * Updates custom white balance temperature display with `camera2.MainCamera` peripheral (Camera2 API).
     *
     * @param camera camera peripheral
     */
    private fun updateViewCamera2(camera: Camera) {
        // Get the current white balance mode.
        val whiteBalanceMode = camera.config[Camera.Config.WHITE_BALANCE_MODE].value
        // Get configuration parameter for white balance temperature.
        val whiteBalanceTemperatureParam = camera.config[Camera.Config.WHITE_BALANCE_TEMPERATURE]
        // Get the set of supported white balance temperatures.
        val supportedWhiteBalanceTemperatures = whiteBalanceTemperatureParam.supportedValues(onlyCurrent = false)
        // Get the current custom white balance temperature.
        val whiteBalanceTemperature = whiteBalanceTemperatureParam.value
        // Create adapter for white balance temperatures choice.
        val adapter = ArrayAdapter<Camera.WhiteBalanceTemperature>(
            whiteBalanceSpinner.context, android.R.layout.simple_spinner_item
        ).apply {
            // Fill adapter with supported white balance temperatures.
            addAll(supportedWhiteBalanceTemperatures)
        }
        whiteBalanceSpinner.apply {
            // Enable spinner if white balance is not currently changing.
            isEnabled = !camera.config.updating
            // Set spinner adapter.
            this.adapter = adapter
            // Tell to spinner the current white balance temperature
            selectionIndex = adapter.getPosition(whiteBalanceTemperature)
            setSelection(selectionIndex)
            if (whiteBalanceMode != Camera.WhiteBalanceMode.CUSTOM) {
                // in case the camera is not in CUSTOM mode, force it
                setWhiteBalanceTemperatureCamera2(camera, whiteBalanceTemperature)
            }
            // Register spinner listener.
            onItemSelectedListener = whiteBalanceSpinnerListener
        }
    }

    /**
     * Sets custom white balance temperature with `MainCamera` peripheral (Camera1 API).
     *
     * @param camera camera peripheral
     * @param temperature new custom white balance temperature
     */
    private fun setWhiteBalanceTemperatureCamera1(camera: MainCamera,
                                                  temperature: CameraWhiteBalance.Temperature) {
        // Get white balance setting and set mode to `custom`, to allow definition of a custom temperature.
        // This will send immediately this mode to the drone, if connected.
        camera.whiteBalance().setMode(CameraWhiteBalance.Mode.CUSTOM)
        // Get white balance setting and set the custom temperature.
        // This will send immediately this value to the drone, if connected.
        camera.whiteBalance().setCustomTemperature(temperature)
    }

    /**
     * Sets custom white balance temperature with `camera2.MainCamera` peripheral (Camera2 API).
     *
     * @param camera camera peripheral
     * @param temperature new custom white balance temperature
     */
    private fun setWhiteBalanceTemperatureCamera2(camera: Camera,
                                                  temperature: Camera.WhiteBalanceTemperature) {
        // To change custom white balance temperature with `camera2.MainCamera` peripheral,
        // we use the configuration editor.
        // Create a configuration editor, starting from current configuration.
        val editor = camera.config.edit(fromScratch = false)
        // Set white balance mode to `custom`, to allow definition of a custom temperature.
        // And set the custom white balance temperature.
        // Note: In case of conflicts with other parameters, the editor may automatically unset the
        // other conflicting parameters, so that the configuration remains consistent.
        editor[Camera.Config.WHITE_BALANCE_MODE].value = Camera.WhiteBalanceMode.CUSTOM
        editor[Camera.Config.WHITE_BALANCE_TEMPERATURE].value = temperature
        // Automatically complete the edited configuration, to ensure that all parameters are set.
        editor.autoComplete()
        // Apply and send the new configuration to the drone, if the drone is connected.
        editor.commit()
    }

    /** Listener for custom white balance spinner. */
    private val whiteBalanceSpinnerListener = object : AdapterView.OnItemSelectedListener {

        override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
            // Skip events that are not user-originated
            if (selectionIndex == pos) return

            // Get `MainCamera` peripheral from its reference, if available.
            mainCameraRef?.get()?.let { camera ->
                // Get white balance temperature selected by user.
                val selectedWhiteBalanceTemperature = parent.getItemAtPosition(pos) as CameraWhiteBalance.Temperature
                // Set white balance temperature.
                setWhiteBalanceTemperatureCamera1(camera, selectedWhiteBalanceTemperature)
            } ?:

            // Otherwise, get `camera2.MainCamera` peripheral from its reference, if available.
            mainCamera2Ref?.get()?.let { camera ->
                // Get white balance temperature selected by user.
                val selectedWhiteBalanceTemperature = parent.getItemAtPosition(pos) as Camera.WhiteBalanceTemperature
                // Set white balance temperature.
                setWhiteBalanceTemperatureCamera2(camera, selectedWhiteBalanceTemperature)
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }
}