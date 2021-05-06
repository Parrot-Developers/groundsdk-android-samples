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

package com.parrot.thermalvideostreamstandard

import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.ThermalCamera
import com.parrot.drone.groundsdk.device.peripheral.ThermalControl
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.libtproc.TProc
import com.parrot.libtproc.TProcPaletteFactory
import com.parrot.libtproc.TProcStreamView
import com.parrot.libtproc.TProcVideo

/**
 * GroundSdk Thermal Video Stream Standard Sample.
 *
 * This activity allows the application to connect to a drone.
 * It displays the connection state, thermal video stream and temperature info with thermal blending
 * `standard` make by the application.
 * It allows to use different thermal palettes.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ThermalVideoStreamStandard"
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
    private var thermalCameraRef: Ref<ThermalCamera>? = null
    /** @code{true} if the drone thermal render is initialized. */
    private var droneThermalRenderInitialized = false

    // User Interface:
    /** Drone state text view. */
    private lateinit var droneStateTxt: TextView
    /** Video thermal stream view. */
    private lateinit var streamView: TProcStreamView
    /** Text view to display the current lowest temperature rendered. */
    private lateinit var lowTmpTxt: TextView
    /** Text view to display the current highest temperature rendered. */
    private lateinit var highTmpTxt: TextView
    /** Text view to display the current  temperature rendered at the thermal probe location. */
    private lateinit var probeTmpTxt: TextView
    /** Text view to display the current  thermal probe location X. */
    private lateinit var probeXTxt: TextView
    /** Text view to display the current  thermal probe location Y. */
    private lateinit var probeYTxt: TextView
    /** Palettes selection. */
    private lateinit var palettesSelection: RadioGroup
    /** Relative Palette button. */
    private lateinit var relativeRBT: RadioButton
    /** Absolute Palette button. */
    private lateinit var absoluteRBT: RadioButton
    /** Spot Palette button. */
    private lateinit var spotRBT: RadioButton

    // Local Thermal Processing Part:
    /** Thermal video processing. */
    private lateinit var tproc : TProcVideo
    /** Relative thermal palette. */
    private lateinit var relativePalette: TProcPaletteFactory.RelativePalette
    /** Absolute thermal palette. */
    private lateinit var absolutePalette: TProcPaletteFactory.AbsolutePalette
    /** Spot thermal palette. */
    private lateinit var spotPalette: TProcPaletteFactory.SpotPalette

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(this)
        // All references taken are linked to the activity lifecycle and
        // automatically closed at its destruction.

        // Create a thermal processing video
        tproc = TProc.createTProcVideo { status ->
            // Called for each thermal processing status.

            // Update user interface according to thermal processing status.
            status.min()?.let { min ->
                lowTmpTxt.text = min.temperature().toInt().toString()
            }

            status.min()?.let { max ->
                highTmpTxt.text = max.temperature().toInt().toString()
            }

            status.probe()?.let { probe ->
                probeTmpTxt.text = probe.temperature().toInt().toString()
                probeXTxt.text = "%.2f".format(probe.position().x)
                probeYTxt.text = "%.2f".format(probe.position().y)
            }
        }

        // Initialize local thermal palettes.
        initThermalPalettes()

        // Initialize local thermal processing.
        initThermalProc()

        // Get user interface instances.
        streamView = findViewById(R.id.streamView)
        droneStateTxt = findViewById(R.id.droneStateTxt)
        lowTmpTxt = findViewById(R.id.lowTmpTxt)
        highTmpTxt = findViewById(R.id.highTmpTxt)
        probeTmpTxt = findViewById(R.id.probeTmpTxt)
        probeXTxt = findViewById(R.id.probeXTxt)
        probeYTxt = findViewById(R.id.probeYTxt)
        palettesSelection = findViewById(R.id.palettesSelection)
        relativeRBT = findViewById(R.id.relativeRBT)
        absoluteRBT = findViewById(R.id.absoluteRBT)
        spotRBT = findViewById(R.id.spotRBT)
        palettesSelection.setOnCheckedChangeListener { _: RadioGroup?, id: Int ->
            // Use thermal palette according to the selection.
            when (id) {
                relativeRBT.id -> tproc.palette = relativePalette
                absoluteRBT.id -> tproc.palette = absolutePalette
                spotRBT.id -> tproc.palette = spotPalette
                else -> tproc.palette = relativePalette
            }

            // Send the new thermal settings to use in the drone recording video.
            thermalCtrlRef?.get()?.let { thermalCtrl ->
                sendThermalRenderSettings(thermalCtrl)
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
     * Initialize thermal palettes.
     */
    private fun initThermalPalettes() {
        // Initialize relative thermal palette
        initRelativeThermalPalette()

        // Initialize absolute thermal palette
        initAbsoluteThermalPalette()

        // Initialize spot thermal palette
        initSpotThermalPalette()
    }

    /**
     * Initialize relative thermal palette.
     */
    private fun initRelativeThermalPalette() {
        // Create a Relative thermal palette:
        //
        // Palette fully used.
        // The lowest color is associated to the coldest temperature of the scene and
        // the highest color is associated to the hottest temperature of the scene.
        // The temperature association can be locked.
        relativePalette = TProcPaletteFactory.createRelativePalette(
            // Colors list:
            //     - Blue as color of the lower palette boundary.
            //     - Red as color of the higher palette boundary.
            listOf(
                TProcPaletteFactory.Palette.Color(0.0f,0.0f,1.0f,0.0f),
                TProcPaletteFactory.Palette.Color(1.0f,0.0f,0.0f,1.0f)
            )) {
                // Called when the temperatures associated to the palette boundaries change.

                Log.i(TAG, "Blue is associated to " + relativePalette.lowestTemperature + " kelvin.")
                Log.i(TAG, "Red is associated to " + relativePalette.highestTemperature+ " kelvin.")
            }

        //`relativePalette.isLocked = true` can be used to lock the association between colors and temperatures.
        // If relativePalette.isLocked is false, the association between colors and temperatures is update
        // at each render to match with the temperature range of the scene rendered.
    }

    /**
     * Initialize absolute thermal palette.
     */
    private fun initAbsoluteThermalPalette() {
        // Create a Absolute thermal palette:
        //
        // Palette used between temperature range set.
        // The palette can be limited or extended for out of range temperatures.
        absolutePalette = TProcPaletteFactory.createAbsolutePalette(
            // Colors list:
            //     - Brown as color of the lower palette boundary.
            //     - Purple as the middle color of the palette.
            //     - Yellow as color of the higher palette boundary.
            listOf(
                TProcPaletteFactory.Palette.Color(0.34f,0.16f,0.0f,0.0f),
                TProcPaletteFactory.Palette.Color(0.40f,0.0f,0.60f,0.5f),
                TProcPaletteFactory.Palette.Color(1.0f,1.0f,0.0f,1.0f)
            )
        )

        // Set a range between 300.0 Kelvin and 310.0 Kelvin.
        // Brown will be associated with 300.0 Kelvin.
        // Yellow will be associated with 310.0 Kelvin.
        // Purple will be associated with the middle range therefore 305.0 Kelvin.
        absolutePalette.lowestTemperature = 300.0
        absolutePalette.highestTemperature = 310.0

        // Limit the palette, to render in black color temperatures out of range.
        absolutePalette.isLimited = true
        // If the palette is not limited:
        //    - temperatures lower than `lowestTemperature` are render with the lower palette boundary color.
        //    - temperatures higher than `highestTemperature` are render with the higher palette boundary color.
    }

    /**
     * Initialize spot thermal palette.
     */
    private fun initSpotThermalPalette() {
        // Create a Spot thermal palette:
        //
        // Palette to highlight cold spots or hot spots.
        //
        // The palette is fully used:
        //     The lowest color is associated to the coldest temperature of the scene and
        //     the highest color is associated to the hottest temperature of the scene.
        // Only temperature hotter or colder than the threshold are shown.
        spotPalette = TProcPaletteFactory.createSpotPalette(
            // Colors list:
            //     - Green as color of the lower palette boundary.
            //     - Orange as color of the higher palette boundary.
            listOf(
                TProcPaletteFactory.Palette.Color(0.0f,1.0f,0.0f,0.0f),
                TProcPaletteFactory.Palette.Color(1.0f,0.5f,0.0f,1.0f)
            )) {
                // Called when the temperatures associated to the palette boundaries change.

                Log.i(TAG, "Green is associated to " + spotPalette.lowestTemperature + " kelvin.")
                Log.i(TAG, "Orange is associated to " + spotPalette.highestTemperature + " kelvin.")
            }

        // Highlight temperature higher than the threshold.
        spotPalette.temperatureType = TProcPaletteFactory.SpotPalette.TemperatureType.HOT
        // `spotPalette.temperatureType = TProcPaletteFactory.SpotPalette.TemperatureType.COLD`
        // to highlight temperature lower than the threshold.

        // Set the threshold at the 60% of the temperature range of the rendered scene.
        spotPalette.threshold = 0.6
    }

    /**
     * Initialize local thermal processing.
     */
    private fun initThermalProc() {

        // Set the rendering as blended at 50% between thermal image and visible image.
        tproc.renderingMode = TProc.RenderingMode.BLENDED
        tproc.blendingRate = 0.5
        // `tproc.renderingMode = TProc.RenderingMode.VISIBLE` to render visible images only.
        // `tproc.renderingMode = TProc.RenderingMode.THERMAL` to render thermal images only.
        // `tproc.renderingMode = TProc.RenderingMode.MONOCHROME` to render visible images in monochrome only.

        // Set the thermal probe position at the center of the render.
        //
        // The Origin point [0;0] is at the render top left and the point [1;1] is at the at render bottom right.
        tproc.setProbe(PointF(0.5f, 0.5f))

        // Use the relative palette
        tproc.palette = relativePalette
    }

    /**
     * Resets user interface.
     */
    private fun resetUi() {
        // Reset user interface views.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        lowTmpTxt.text = ""
        highTmpTxt.text = ""
        probeTmpTxt.text = ""
        probeXTxt.text = ""
        probeYTxt.text = ""
        palettesSelection.check(relativeRBT.id)

        // Stop rendering the stream
        streamView.setStream(null)
        streamView.tproc = null
    }

    /**
     * Starts drone monitors.
     */
    private fun startDroneMonitors() {
        // Monitor drone state.
        monitorDroneState()

        // To switch from the main camera to the thermal camera:
        //    1) The video stream must be stopped.
        //    2) Set thermal control mode to standard.
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
                if (it.mode().value != ThermalControl.Mode.STANDARD) {
                    it.mode().value = ThermalControl.Mode.STANDARD
                }

                // Warning: TProc and TProcStreamView should not be used in EMBEDDED mode,
                // In EMBEDDED mode the stream should be displayed directly by a GsdkStreamView.

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

        // Monitor the thermal camera.
        thermalCameraRef = drone?.getPeripheral(ThermalCamera::class.java) { thermalCamera ->
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
     * @param liveStream: the stream to start.
     */
    private fun startVideoStream(liveStream: CameraLive) {
        // Force the stream server enabling.
        streamServerRef?.get()?.enableStreaming(true)

        // Set thermal Camera model to use according to the drone model.
        streamView.thermalCamera = when (drone?.model) {
            Drone.Model.ANAFI_THERMAL -> TProc.ThermalCamera.LEPTON
            Drone.Model.ANAFI_UA -> TProc.ThermalCamera.BOSON
            Drone.Model.ANAFI_USA -> TProc.ThermalCamera.BOSON
            else -> TProc.ThermalCamera.LEPTON
        }

        // Set the thermal processing instance to use to the thermal stream view.
        streamView.tproc = tproc

        // Play the live stream.
        liveStream.play()
    }

    // Thermal processing is local only.
    // If you want that the thermal video recorded on the drone look like the local render,
    // you should send thermal rendering settings to the drone.

    /**
     * Sends Thermal Render settings to the drone.
     *
     * @param thermalCtrl: thermal control.
     */
    private fun sendThermalRenderSettings(thermalCtrl: ThermalControl) {

        // To optimize, do not send settings that have not changed.

        // Send rendering mode.
        thermalCtrl.sendRendering(thermalRenderingModeGsdk())

        // Send emissivity .
        thermalCtrl.sendEmissivity(tproc.emissivity)

        // Send Background Temperature.
        thermalCtrl.sendBackgroundTemperature(tproc.backgroundTemp)

        // Send thermal palette.
        tproc.palette.toGsdk()?.let { paletteGsdk ->
            thermalCtrl.sendPalette(paletteGsdk)
        }
    }

    /**
     * Converts thermal processing colors to GroundSdk thermal colors.
     */
    private fun TProcPaletteFactory.Palette.Color.toGsdk() = object: ThermalControl.Palette.Color {
        override fun getRed() = this@toGsdk.red
        override fun getGreen() =  this@toGsdk.green
        override fun getBlue() =  this@toGsdk.blue
        override fun getPosition() =  this@toGsdk.position
    }

    /**
     * Converts thermal processing relative palette to GroundSdk thermal relative palette.
     */
    private fun TProcPaletteFactory.RelativePalette.toGsdk() = object: ThermalControl.RelativePalette {
        override fun getColors(): Collection<ThermalControl.Palette.Color> {
            val gsdkColors = mutableListOf<ThermalControl.Palette.Color>()
            for (color in tproc.palette.colors) {
                gsdkColors.add(color.toGsdk())
            }
            return gsdkColors
        }

        override fun getLowestTemperature() = this@toGsdk.lowestTemperature
        override fun getHighestTemperature() = this@toGsdk.highestTemperature
        override fun isLocked() = this@toGsdk.isLocked
    }

    /**
     * Converts thermal processing absolute palette to GroundSdk thermal absolute palette.
     */
    private fun TProcPaletteFactory.AbsolutePalette.toGsdk() = object: ThermalControl.AbsolutePalette {
        override fun getColors(): Collection<ThermalControl.Palette.Color> {
            val gsdkColors = mutableListOf<ThermalControl.Palette.Color>()
            for (color in tproc.palette.colors) {
                gsdkColors.add(color.toGsdk())
            }
            return gsdkColors
        }

        override fun getLowestTemperature() = this@toGsdk.lowestTemperature
        override fun getHighestTemperature() = this@toGsdk.highestTemperature
        override fun getColorizationMode() =
            if (this@toGsdk.isLimited) ThermalControl.AbsolutePalette.ColorizationMode.LIMITED else
                                       ThermalControl.AbsolutePalette.ColorizationMode.EXTENDED
    }

    /**
     * Converts thermal processing spot palette to GroundSdk thermal spot palette.
     */
    private fun TProcPaletteFactory.SpotPalette.toGsdk() = object: ThermalControl.SpotPalette {
        override fun getColors(): Collection<ThermalControl.Palette.Color> {
            val gsdkColors = mutableListOf<ThermalControl.Palette.Color>()
            for (color in tproc.palette.colors) {
                gsdkColors.add(color.toGsdk())
            }
            return gsdkColors
        }

        override fun getType() = when (this@toGsdk.temperatureType) {
            TProcPaletteFactory.SpotPalette.TemperatureType.COLD -> ThermalControl.SpotPalette.SpotType.COLD
            TProcPaletteFactory.SpotPalette.TemperatureType.HOT -> ThermalControl.SpotPalette.SpotType.HOT
        }
        override fun getThreshold() = this@toGsdk.threshold
    }

    /**
     * Converts thermal processing palette to GroundSdk thermal palette.
     */
    private fun TProcPaletteFactory.Palette.toGsdk() : ThermalControl.Palette? {
        var gsdkPalette: ThermalControl.Palette? = null

        (tproc.palette as? TProcPaletteFactory.RelativePalette)?.let { relativePalette ->
            gsdkPalette = relativePalette.toGsdk()
        }

        (tproc.palette as? TProcPaletteFactory.AbsolutePalette)?.let { absolutePalette ->
            gsdkPalette = absolutePalette.toGsdk()
        }

        (tproc.palette as? TProcPaletteFactory.SpotPalette)?.let { spotPalette ->
            gsdkPalette = spotPalette.toGsdk()
        }

        return gsdkPalette
    }

    /**
     * Retrieves GroundSdk rendering mode to send to the drone according to the current thermal processing.
     *
     * @return GroundSdk rendering mode according to the current thermal processing.
     */
    private fun thermalRenderingModeGsdk() : ThermalControl.Rendering {
        return object : ThermalControl.Rendering {
            override fun getMode(): ThermalControl.Rendering.Mode {
                return when (tproc.renderingMode) {
                    TProc.RenderingMode.BLENDED -> ThermalControl.Rendering.Mode.BLENDED
                    TProc.RenderingMode.VISIBLE -> ThermalControl.Rendering.Mode.VISIBLE
                    TProc.RenderingMode.THERMAL -> ThermalControl.Rendering.Mode.THERMAL
                    TProc.RenderingMode.MONOCHROME -> ThermalControl.Rendering.Mode.MONOCHROME
                }
            }

            override fun getBlendingRate(): Double {
                return tproc.blendingRate
            }
        }
    }
}
