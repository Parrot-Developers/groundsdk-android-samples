/*
 *     Copyright (C) 2019 Parrot Drones SAS
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

package com.parrot.hellodrone

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.RemoteControl
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.device.pilotingitf.Activable
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.stream.GsdkStreamView

/**
 * GroundSdk Hello Drone Sample.
 *
 * This activity allows the application to connect to a drone and/or a remote control.
 * It displays the connection state, battery level and video stream.
 * It allows to take off and land by button click.
 */
class MainActivity : AppCompatActivity() {

    /** GroundSdk instance. */
    private lateinit var groundSdk: GroundSdk

    // Drone:
    /** Current drone instance. */
    private var drone: Drone? = null
    /** Reference to the current drone state. */
    private var droneStateRef: Ref<DeviceState>? = null
    /** Reference to the current drone battery info instrument. */
    private var droneBatteryInfoRef: Ref<BatteryInfo>? = null
    /** Reference to a current drone piloting interface. */
    private var pilotingItfRef: Ref<ManualCopterPilotingItf>? = null
    /** Reference to the current drone stream server Peripheral. */
    private var streamServerRef: Ref<StreamServer>? = null
    /** Reference to the current drone live stream. */
    private var liveStreamRef: Ref<CameraLive>? = null
    /** Current drone live stream. */
    private var liveStream: CameraLive? = null

    // Remote control:
    /** Current remote control instance. */
    private var rc: RemoteControl? = null
    /** Reference to the current remote control state. */
    private var rcStateRef: Ref<DeviceState>? = null
    /** Reference to the current remote control battery info instrument. */
    private var rcBatteryInfoRef: Ref<BatteryInfo>? = null

    // User Interface:
    /** Video stream view. */
    private lateinit var streamView: GsdkStreamView
    /** Drone state text view. */
    private lateinit var droneStateTxt: TextView
    /** Drone battery charge level text view. */
    private lateinit var droneBatteryTxt: TextView
    /** Remote state level text view. */
    private lateinit var rcStateTxt: TextView
    /** Remote battery charge level text view. */
    private lateinit var rcBatteryTxt: TextView
    /** Take off / land button. */
    private lateinit var takeOffLandBt: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get user interface instances.
        streamView = findViewById(R.id.stream_view)
        droneStateTxt = findViewById(R.id.droneStateTxt)
        droneBatteryTxt = findViewById(R.id.droneBatteryTxt)
        rcStateTxt = findViewById(R.id.rcStateTxt)
        rcBatteryTxt = findViewById(R.id.rcBatteryTxt)
        takeOffLandBt = findViewById(R.id.takeOffLandBt)
        takeOffLandBt.setOnClickListener {onTakeOffLandClick()}

        // Initialize user interface default values.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()

        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(this)
        // All references taken are linked to the activity lifecycle and
        // automatically closed at its destruction.
    }

    override fun onStart() {
        super.onStart()

        // Monitor the auto connection facility.
        groundSdk.getFacility(AutoConnection::class.java) {
            // Called when the auto connection facility is available and when it changes.

            it?.let{
                // Start auto connection.
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }

                // If the drone has changed.
                if (drone?.uid != it.drone?.uid) {
                    if(drone != null) {
                        // Stop monitoring the old drone.
                        stopDroneMonitors()

                        // Reset user interface drone part.
                        resetDroneUi()
                    }

                    // Monitor the new drone.
                    drone = it.drone
                    if(drone != null) {
                        startDroneMonitors()
                    }
                }

                // If the remote control has changed.
                if (rc?.uid  != it.remoteControl?.uid) {
                    if(rc != null) {
                        // Stop monitoring the old remote.
                        stopRcMonitors()

                        // Reset user interface Remote part.
                        resetRcUi()
                    }

                    // Monitor the new remote.
                    rc = it.remoteControl
                    if(rc != null) {
                        startRcMonitors()
                    }
                }
            }
        }
    }

    /**
     * Resets drone user interface part.
     */
    private fun resetDroneUi() {
        // Reset drone user interface views.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        droneBatteryTxt.text = ""
        takeOffLandBt.isEnabled = false
        // Stop rendering the stream
        streamView.setStream(null)
    }

    /**
     * Starts drone monitors.
     */
    private fun startDroneMonitors() {
        // Monitor drone state.
        monitorDroneState()

        // Monitor drone battery charge level.
        monitorDroneBatteryChargeLevel()

        // Monitor piloting interface.
        monitorPilotingInterface()

        // Start video stream.
        startVideoStream()
    }

    /**
     * Stops drone monitors.
     */
    private fun stopDroneMonitors() {
        // Close all references linked to the current drone to stop their monitoring.

        droneStateRef?.close()
        droneStateRef = null

        droneBatteryInfoRef?.close()
        droneBatteryInfoRef = null

        pilotingItfRef?.close()
        pilotingItfRef = null

        liveStreamRef?.close()
        liveStreamRef = null

        streamServerRef?.close()
        streamServerRef = null

        liveStream = null
    }

    /**
     * Starts the video stream.
     */
    private fun startVideoStream() {
        // Monitor the stream server.
        streamServerRef = drone?.getPeripheral(StreamServer::class.java) { streamServer ->
            // Called when the stream server is available and when it changes.

            if (streamServer != null) {
                // Enable Streaming
                if(!streamServer.streamingEnabled()) {
                    streamServer.enableStreaming(true)
                }

                // Monitor the live stream.
                if (liveStreamRef == null) {
                    liveStreamRef = streamServer.live { liveStream ->
                        // Called when the live stream is available and when it changes.

                        if (liveStream != null) {
                            if (this.liveStream == null) {
                                // It is a new live stream.

                                // Set the live stream as the stream
                                // to be render by the stream view.
                                streamView.setStream(liveStream)
                            }

                            // Play the live stream.
                            if (liveStream.playState() != CameraLive.PlayState.PLAYING) {
                                liveStream.play()
                            }
                        } else {
                            // Stop rendering the stream
                            streamView.setStream(null)
                        }
                        // Keep the live stream to know if it is a new one or not.
                        this.liveStream = liveStream
                    }
                }
            } else {
                // Stop monitoring the live stream
                liveStreamRef?.close()
                liveStreamRef = null
                // Stop rendering the stream
                streamView.setStream(null)
            }
        }
    }

    /**
     * Monitor current drone state.
     */
    private fun monitorDroneState() {
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
     * Monitors current drone battery charge level.
     */
    private fun monitorDroneBatteryChargeLevel() {
        // Monitor the battery info instrument.
        droneBatteryInfoRef = drone?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                droneBatteryTxt.text = getString(R.string.percentage, it.charge)
            }
        }
    }

    /**
     * Monitors current drone piloting interface.
     */
    private fun monitorPilotingInterface() {
        // Monitor a piloting interface.
        pilotingItfRef = drone?.getPilotingItf(ManualCopterPilotingItf::class.java) {
            // Called when the manual copter piloting Interface is available
            // and when it changes.

            // Disable the button if the piloting interface is not available.
            if (it == null) {
                takeOffLandBt.isEnabled = false
            } else {
                managePilotingItfState(it)
            }
        }
    }

    /**
     * Manage piloting interface state.
     *
     * @param itf the piloting interface
     */
    private fun managePilotingItfState(itf: ManualCopterPilotingItf) {
        when(itf.state) {
            Activable.State.UNAVAILABLE -> {
                // Piloting interface is unavailable.
                takeOffLandBt.isEnabled = false
            }

            Activable.State.IDLE -> {
                // Piloting interface is idle.
                takeOffLandBt.isEnabled = false

                // Activate the interface.
                itf.activate()
            }

            Activable.State.ACTIVE -> {
                // Piloting interface is active.

                when {
                    itf.canTakeOff() -> {
                        // Drone can take off.
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = getString(R.string.take_off)
                    }
                    itf.canLand() -> {
                        // Drone can land.
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = getString(R.string.land)
                    }
                    else -> // Disable the button.
                        takeOffLandBt.isEnabled = false
                }
            }
        }
    }

    /**
     * Called on take off/land button click.
     */
    private fun onTakeOffLandClick() {
        // Get the piloting interface from its reference.
        pilotingItfRef?.get()?.let { itf ->
            // Do the action according to the interface capabilities
            if (itf.canTakeOff()) {
                // Take off
                itf.takeOff()
            } else if (itf.canLand()) {
                // Land
                itf.land()
            }
        }
    }

    /**
     * Resets remote user interface part.
     */
    private fun resetRcUi() {
        // Reset remote control user interface views.
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcBatteryTxt.text = ""
    }

    /**
     * Starts remote control monitors.
     */
    private fun startRcMonitors() {
        // Monitor remote state
        monitorRcState()

        // Monitor remote battery charge level
        monitorRcBatteryChargeLevel()
    }

    /**
     * Stops remote control monitors.
     */
    private fun stopRcMonitors() {
        // Close all references linked to the current remote to stop their monitoring.

        rcStateRef?.close()
        rcStateRef = null

        rcBatteryInfoRef?.close()
        rcBatteryInfoRef = null
    }

    /**
     * Monitor current remote control state.
     */
    private fun monitorRcState() {
        // Monitor current drone state.
        rcStateRef = rc?.getState {
            // Called at each remote state update.

            it?.let {
                // Update remote connection state view.
                rcStateTxt.text = it.connectionState.toString()
            }
        }
    }

    /**
     * Monitors current remote control battery charge level.
     */
    private fun monitorRcBatteryChargeLevel() {
        // Monitor the battery info instrument.
        rcBatteryInfoRef = rc?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                rcBatteryTxt.text = getString(R.string.percentage, it.charge)
            }
        }
    }
}
