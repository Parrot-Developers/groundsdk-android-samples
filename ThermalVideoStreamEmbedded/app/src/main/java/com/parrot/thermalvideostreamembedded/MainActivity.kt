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

package com.parrot.thermalvideostreamembedded

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.BlendedThermalCamera
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.ThermalControl
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.stream.GsdkStreamView

/**
 * GroundSdk Thermal Video Stream Embedded Sample.
 *
 * This activity allows the application to connect to a drone.
 * It displays the connection state and the thermal video stream with blending embedded on the drone.
 * It allows to use different thermal palettes.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ThermalVideoStreamEmbedded"
    }

    /** GroundSdk instance. */
    private lateinit var groundSdk: GroundSdk

    /** Reference to the auto connection facility. */
    private var autoConnectionRef: Ref<AutoConnection>? = null

    // Drone:
    /** Current drone instance. */
    private var drone: Drone? = null
    /** Reference to the current drone state. */
    private var droneStateRef: Ref<DeviceState>? = null
    /** Reference to the current drone stream server Peripheral. */
    private var streamServerRef: Ref<StreamServer>? = null
    /** Reference to the current drone live stream. */
    private var liveStreamRef: Ref<CameraLive>? = null
    /** Reference to the current drone thermal control  peripheral. */
    private var thermalCtrlRef: Ref<ThermalControl>? = null
    /** Reference to the current drone thermal camera peripheral. */
    private var thermalCameraRef: Ref<BlendedThermalCamera>? = null
    /** @code{true} if the drone thermal render is initialized. */
    private var droneThermalRenderInitialized = false

    // User Interface:
    /** Drone state text view. */
    private lateinit var droneStateTxt: TextView
    /** Video thermal stream view. */
    private lateinit var streamView: GsdkStreamView
    /** Palettes selection. */
    private lateinit var palettesSelection: RadioGroup
    /** Relative Palette button. */
    private lateinit var relativeRBT: RadioButton
    /** Spot Palette button. */
    private lateinit var spotRBT: RadioButton

    // Thermal Processing Part:
    /**
     * Relative thermal palette.
     *
     * Palette fully used.
     * The lowest color is associated to the coldest temperature of the scene and
     * the highest color is associated to the hottest temperature of the scene.
     * The temperature association can be locked.
    */
    private val relativePalette = object : ThermalControl.RelativePalette {
        override fun getLowestTemperature(): Double {
            // Not used when the thermal is blending on drone.
            return 0.0
        }

        override fun getHighestTemperature(): Double {
            // Not used when the thermal is blending on drone.
            return 0.0
        }

        override fun getColors(): List<ThermalControl.Palette.Color> {
            // Colors list:
            //     - Blue as color of the lower palette boundary.
            //     - Red as color of the higher palette boundary.
            return listOf(
                    object : ThermalControl.Palette.Color {
                        override fun getRed(): Double {
                            return 0.0
                        }
                        override fun getGreen(): Double {
                            return 0.0
                        }
                        override fun getBlue(): Double {
                            return 1.0
                        }
                        override fun getPosition(): Double {
                            return 0.0
                        }

                    },
                    object : ThermalControl.Palette.Color {
                        override fun getRed(): Double {
                            return 1.0
                        }
                        override fun getGreen(): Double {
                            return 0.0
                        }
                        override fun getBlue(): Double {
                            return 0.0
                        }
                        override fun getPosition(): Double {
                            return 1.0
                        }
                    }
            )
        }

        override fun isLocked(): Boolean {
            return false
            // `return true` can be used to lock the association between colors and temperatures.
            // If relativePalette.isLocked is false, the association between colors and temperatures is update
            // at each render to match with the temperature range of the scene rendered.
        }
    }

    /**
     * Spot thermal palette.
     *
     * Palette to highlight cold spots or hot spots.
     *
     * The palette is fully used:
     *      The lowest color is associated to the coldest temperature of the scene and
     *      the highest color is associated to the hottest temperature of the scene.
     * Only temperature hotter or colder than the threshold are shown.
     */
    private val spotPalette = object : ThermalControl.SpotPalette {
        override fun getThreshold(): Double {
            // Threshold at the 60% of the temperature range of the rendered scene.
            return 0.6
        }

        override fun getType(): ThermalControl.SpotPalette.SpotType {
            // Highlight temperature higher than the threshold.
            return ThermalControl.SpotPalette.SpotType.HOT
            // `return ThermalControl.SpotPalette.SpotType.COLD`
            // to highlight temperature lower than the threshold.
        }

        override fun getColors(): List<ThermalControl.Palette.Color> {
            // Colors list:
            //     - Green as color of the lower palette boundary.
            //     - Orange as color of the higher palette boundary.
            return listOf(
                    object : ThermalControl.Palette.Color {
                        override fun getRed(): Double {
                            return 0.0
                        }
                        override fun getGreen(): Double {
                            return 1.0
                        }
                        override fun getBlue(): Double {
                            return 0.0
                        }
                        override fun getPosition(): Double {
                            return 0.0
                        }

                    },
                    object : ThermalControl.Palette.Color {
                        override fun getRed(): Double {
                            return 1.0
                        }
                        override fun getGreen(): Double {
                            return 0.5
                        }
                        override fun getBlue(): Double {
                            return 0.0
                        }
                        override fun getPosition(): Double {
                            return 1.0
                        }
                    }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(this)
        // All references taken are linked to the activity lifecycle and
        // automatically closed at its destruction.

        // Get user interface instances.
        streamView = findViewById(R.id.streamView)
        droneStateTxt = findViewById(R.id.droneStateTxt)
        palettesSelection = findViewById(R.id.palettesSelection)
        relativeRBT = findViewById(R.id.relativeRBT)
        spotRBT = findViewById(R.id.spotRBT)
        palettesSelection.setOnCheckedChangeListener { _: RadioGroup?, id: Int ->
            thermalCtrlRef?.get()?.let { thermalCtrl ->
                // Send the new thermal palette according to the selection.
                sendThermalPalette(thermalCtrl, id)
            }
        }

        // Initialize user interface
        resetUi()
    }

    override fun onStart() {
        super.onStart()

        // Monitor the auto connection facility.
        autoConnectionRef = groundSdk.getFacility(AutoConnection::class.java) {
            // Called when the auto connection facility is available and when it changes.

            it?.let {
                // Start auto connection.
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }

                // If the drone has changed.
                if (drone?.uid != it.drone?.uid) {
                    if (drone != null) {
                        // Stop monitoring the old drone.
                        stopDroneMonitors()

                        // Reset user interface.
                        resetUi()
                    }

                    // Monitor the new drone.
                    drone = it.drone
                    if (drone != null) {
                        startDroneMonitors()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        // Stop to monitor the auto connection facility.
        autoConnectionRef?.close()
        autoConnectionRef = null
    }

    /**
     * Resets user interface.
     */
    private fun resetUi() {
        // Reset user interface views.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        palettesSelection.check(relativeRBT.id)

        // Stop rendering the stream
        streamView.setStream(null)
    }

    /**
     * Starts drone monitors.
     */
    private fun startDroneMonitors() {
        // Monitor drone state.
        monitorDroneState()

        // To switch from the main camera to the thermal camera:
        //    1) The video stream must be stopped.
        //    2) Set thermal control mode to embedded.
        //    3) Wait for the thermal camera to be active.
        //    4) Start the video stream.

        // Monitor stream server.
        monitorStreamServer()

        // Monitor thermal control peripheral.
        monitorThermalControl()

        // Monitor thermal camera.
        monitorThermalCamera()
    }

    /**
     * Stops drone monitors.
     */
    private fun stopDroneMonitors() {
        // Close all references linked to the current drone to stop their monitoring.

        droneStateRef?.close()
        droneStateRef = null

        streamServerRef?.close()
        streamServerRef = null

        liveStreamRef?.close()
        liveStreamRef = null

        thermalCtrlRef?.close()
        thermalCtrlRef = null

        thermalCameraRef?.close()
        thermalCameraRef = null

        // Reset drone render initialisation state
        droneThermalRenderInitialized = false
    }

    /**
     * Monitor current drone state.
     */
    private fun monitorDroneState() {
        // Prevent monitoring restart
        if (droneStateRef != null) return

        // Monitor current drone state.
        droneStateRef = drone?.getState {
            // Called at each drone state update.

            it?.let {
                // Update drone connection state view.
                droneStateTxt.text = it.connectionState.toString()
            }
        }
    }

    /**
     * Monitor the stream server.
     */
    private fun monitorStreamServer() {
        // Prevent monitoring restart
        if (streamServerRef != null) return

        // Monitor the stream server.
        streamServerRef = drone?.getPeripheral(StreamServer::class.java) { streamServer ->
            // Called when the stream server is available and when it changes.

            streamServer?.let {
                thermalCameraRef?.get()?.let { thermalCamera ->
                    // Enable the stream server only if the thermal camera is active
                    it.enableStreaming(thermalCamera.isActive)
                }

                // Monitor the live stream
                monitorLiveStream(it)
            }
        }
    }

    /**
     * Monitor the live stream.
     *
     * @param streamServer: the stream server.
     */
    private fun monitorLiveStream(streamServer: StreamServer) {
        // Prevent monitoring restart
        if (liveStreamRef != null) return

        // Monitor the live stream.
        liveStreamRef = streamServer.live { liveStream ->
            // Called when the live stream is available and when it changes.

            // Start to play the live stream only if the thermal camera is active.
            liveStream?.let {
                if (thermalCameraRef?.get()?.isActive == true) {
                    startVideoStream(liveStream)
                }
            }

            // Set the live stream as the stream to be render by the stream view.
            streamView.setStream(liveStream)
        }
    }

    /**
     * Monitors the thermal control peripheral.
     */
    private fun monitorThermalControl() {
        // Prevent monitoring restart
        if (thermalCtrlRef != null) return

        // Monitor the thermal control peripheral.
        thermalCtrlRef = drone?.getPeripheral(ThermalControl::class.java) { thermalCtrl ->
            // Called when the thermal control peripheral is available and when it changes.

            thermalCtrl?.let {
                // Active the thermal camera, if not yet done.
                if (it.mode().value != ThermalControl.Mode.EMBEDDED) {
                    it.mode().value = ThermalControl.Mode.EMBEDDED
                }

                // In order to the drone video recording look like the local render,
                // send a thermal render settings.
                if (!droneThermalRenderInitialized) {
                    sendThermalRenderSettings(it)
                    droneThermalRenderInitialized = true
                }
            }
        }
    }

    /**
     * Monitors the thermal camera.
     */
    private fun monitorThermalCamera() {
        // Prevent monitoring restart
        if (thermalCameraRef != null) return

        // Monitor the thermal blended camera.
        thermalCameraRef = drone?.getPeripheral(BlendedThermalCamera::class.java) { thermalCamera ->
            // Called when the thermal camera is available and when it changes.

            // Start the video stream if the thermal camera is active.
            liveStreamRef?.get()?.let { liveStream ->
                if (thermalCamera?.isActive == true ) {
                    startVideoStream(liveStream)
                }
            }
        }
    }

    /**
     * Starts the video stream.
     *
     * @param liveStream: the stream to start
     */
    private fun startVideoStream(liveStream: CameraLive) {
        // Force the stream server enabling.
        streamServerRef?.get()?.enableStreaming(true)

        // Play the live stream.
        liveStream.play()
    }

    /**
     * Sends Thermal palette to the drone according to the selection.
     *
     * @param thermalCtrl: thermal control.
     * @param id: selection palette button checked
     */
    private fun sendThermalPalette(thermalCtrl: ThermalControl, id: Int) {
        when (id) {
            relativeRBT.id -> thermalCtrl.sendPalette(relativePalette)
            spotRBT.id -> thermalCtrl.sendPalette(spotPalette)
            else -> thermalCtrl.sendPalette(relativePalette)
        }
    }

    /**
     * Sends Thermal Render settings to the drone.
     *
     * @param thermalCtrl: thermal control.
     */
    private fun sendThermalRenderSettings(thermalCtrl: ThermalControl) {

        // To optimize, do not send settings that have not changed.

        // Send rendering mode.
        thermalCtrl.sendRendering(object : ThermalControl.Rendering {
            override fun getBlendingRate(): Double {
                // Set the blending rate to 50%.
                return 0.5
            }

            override fun getMode(): ThermalControl.Rendering.Mode {
                // Set the rendering as blended between thermal image and visible image.
                return ThermalControl.Rendering.Mode.BLENDED
                // return ThermalControl.Rendering.Mode..VISIBLE` to render visible images only.
                // return ThermalControl.Rendering.Mode.THERMAL` to render thermal images only.
                // return ThermalControl.Rendering.Mode..MONOCHROME` to render visible images in monochrome only.
            }
        })

        // Send thermal palette.
        sendThermalPalette(thermalCtrl, palettesSelection.checkedRadioButtonId)
    }
}
