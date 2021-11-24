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

import android.widget.Button
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MainCamera
import com.parrot.drone.groundsdk.device.peripheral.camera2.Camera
import com.parrot.drone.groundsdk.device.peripheral.camera2.component

/**
 * Sample code to manage a button to start and stop photo capture and video recording, using
 * `MainCamera` and `camera2.MainCamera` peripherals (respectively Camera1 API and Camera2 API).
 */
class StartStop(
    /** Start and stop button. */
    private val startStopBt: Button
) {
    /** Reference to `MainCamera` peripheral. */
    private var mainCameraRef: Ref<MainCamera>? = null
    /** Reference to `camera2.MainCamera` peripheral. */
    private var mainCamera2Ref: Ref<com.parrot.drone.groundsdk.device.peripheral.camera2.MainCamera>? = null
    /** `camera2.MainCamera` peripheral. This is used to know when peripheral appears. */
    private var mainCamera2: Camera? = null

    init {
        // Setup button listener.
        startStopBt.setOnClickListener { onStartStopButtonClicked() }
    }

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
        // `camera2.MainCamera` provides sub-components, we have to register listeners for each
        // sub-component for which we want to be notified of a change.
        mainCamera2Ref = drone.getPeripheral(com.parrot.drone.groundsdk.device.peripheral.camera2.MainCamera::class.java) { camera->
            // Called when the camera changes, on main thread.
            camera?.let {
                updateViewCamera2(camera)

                // Register sub-components listeners, only when camera peripheral appears.
                if (mainCamera2 == null) {
                    // Get notified every time photo capture sub-component changes.
                    camera.component<Camera.PhotoCapture> { updateViewCamera2(camera) }
                    // Get notified every time recording sub-component changes.
                    camera.component<Camera.Recording> { updateViewCamera2(camera) }

                    // Note: sub-component refs are automatically closed by the camera itself becomes unavailable
                }
            } ?: run {
                resetView()
            }

            mainCamera2 = camera
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

        mainCamera2 = null

        resetView()
    }

    /**
     * Resets start and button display.
     */
    private fun resetView() {
        startStopBt.isEnabled = false
        startStopBt.setText(R.string.unavailable)
    }

    /**
     * Updates start and button display with `MainCamera` peripheral (Camera1 API).
     *
     * @param camera camera peripheral
     */
    private fun updateViewCamera1(camera: MainCamera) {
        // Enable button when camera is active.
        startStopBt.isEnabled = camera.isActive

        // Update button text depending on camera state.
        // `MainCamera` provides methods indicating whether photo and recording can be started or stopped.
        startStopBt.setText(when {
            camera.canStartPhotoCapture() -> R.string.start_photo
            camera.canStopPhotoCapture() -> R.string.stop_photo
            camera.canStartRecording() -> R.string.start_recording
            camera.canStopRecording() -> R.string.stop_recording
            else -> R.string.unavailable
        })
    }

    /**
     * Updates start and button display with `camera2.MainCamera` peripheral (Camera2 API).
     *
     * @param camera camera peripheral
     */
    private fun updateViewCamera2(camera: Camera) {
        // Enable start and stop button when camera is active.
        startStopBt.isEnabled = camera.active

        // Get current camera configuration and check camera mode (photo or recording).
        when (camera.config[Camera.Config.MODE].value) {

            Camera.Mode.PHOTO -> {
                // Current camera mode is photo.
                // So, get PhotoCapture sub-component to get photo capture state.
                camera.component<Camera.PhotoCapture>()?.apply {
                    // Update button text based on photo capture state.
                    startStopBt.setText(when (state) {
                        Camera.PhotoCapture.State.Starting,
                        is Camera.PhotoCapture.State.Started ->R.string.stop_photo
                        is Camera.PhotoCapture.State.Stopping,
                        is Camera.PhotoCapture.State.Stopped -> R.string.start_photo
                    })
                } ?: run {
                    // PhotoCapture sub-component is not available, meaning photos cannot be taken.
                    startStopBt.setText(R.string.unavailable)
                    startStopBt.isEnabled = false
                }
            }

            Camera.Mode.RECORDING -> {
                // Current camera mode is recording.
                // So, get Recording sub-component to get video recording state.
                camera.component<Camera.Recording>()?.apply {
                    // Update button text based on video recording state.
                    startStopBt.setText(when (state) {
                        Camera.Recording.State.Starting,
                        is Camera.Recording.State.Started -> R.string.stop_recording
                        is Camera.Recording.State.Stopping,
                        is Camera.Recording.State.Stopped -> R.string.start_recording
                    })
                } ?: run {
                    // Recording sub-component is not available, meaning videos cannot be recorded.
                    startStopBt.setText(R.string.unavailable)
                    startStopBt.isEnabled = false
                }
            }
        }
    }

    /**
     * Starts or stops photo capture or video recording with `MainCamera` peripheral (Camera1 API).
     *
     * @param camera camera peripheral
     */
    private fun startStopCamera1(camera: MainCamera) {
        // `MainCamera`provides methods indicating whether photo and recording can be started or stopped.
        // Use these methods to trigger the desired action.
        when {
            camera.canStartPhotoCapture() -> camera.startPhotoCapture()
            camera.canStopPhotoCapture() -> camera.stopPhotoCapture()
            camera.canStartRecording() -> camera.startRecording()
            camera.canStopRecording() -> camera.stopRecording()
        }
    }

    /**
     * Starts or stops photo capture or video recording with `camera2.MainCamera` peripheral (Camera2 API).
     *
     * @param camera camera peripheral
     */
    private fun startStopCamera2(camera: Camera) {
        // Read current camera configuration and check camera mode (photo or recording).
        when (camera.config[Camera.Config.MODE].value) {

            Camera.Mode.PHOTO -> {
                // Current camera mode is photo.
                // So, get PhotoCapture sub-component to get photo capture state.
                camera.component<Camera.PhotoCapture>()?.run {
                    // Start or stop photo capture depending on photo capture state.
                    when (state) {
                        Camera.PhotoCapture.State.Starting,
                        is Camera.PhotoCapture.State.Started -> stop()
                        is Camera.PhotoCapture.State.Stopping,
                        is Camera.PhotoCapture.State.Stopped -> start()
                    }
                }
            }

            Camera.Mode.RECORDING -> {
                // Current camera mode is recording.
                // So, get Recording sub-component to get video recording state.
                camera.component<Camera.Recording>()?.run {
                    // Start or stop video recording depending on recording state.
                    when (state) {
                        Camera.Recording.State.Starting,
                        is Camera.Recording.State.Started -> stop()
                        is Camera.Recording.State.Stopping,
                        is Camera.Recording.State.Stopped -> start()
                    }
                }
            }
        }
    }

    /**
     * Called when start and stop button is clicked.
     */
    private fun onStartStopButtonClicked() {
        // Get `MainCamera` peripheral from its reference, if available.
        mainCameraRef?.get()?.let { camera ->
            startStopCamera1(camera)
        } ?:

        // Otherwise, get `camera2.MainCamera` peripheral from its reference, if available.
        mainCamera2Ref?.get()?.let { camera ->
            startStopCamera2(camera)
        }
    }
}