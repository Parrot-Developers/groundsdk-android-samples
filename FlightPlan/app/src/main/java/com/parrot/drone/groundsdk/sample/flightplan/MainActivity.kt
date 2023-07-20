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

package com.parrot.drone.groundsdk.sample.flightplan

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.RemoteControl
import com.parrot.drone.groundsdk.device.pilotingitf.Activable
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf
import com.parrot.drone.groundsdk.facility.AutoConnection
import java.io.File

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
    /** Reference to drone Flight Plan piloting interface. */
    private var pilotingItfRef: Ref<FlightPlanPilotingItf>? = null

    // Remote control:
    /** Current remote control instance. */
    private var rc: RemoteControl? = null
    /** Reference to the current remote control state. */
    private var rcStateRef: Ref<DeviceState>? = null

    // User Interface:
    /** Drone state text view. */
    private lateinit var droneStateTxt: TextView
    /** RC state text view. */
    private lateinit var rcStateTxt: TextView
    /** Flight Plan latest uplaod state text view. */
    private lateinit var uploadStateTxt: TextView
    /** Flight Plan unavailability reasons list. */
    private lateinit var unavailabilityReasonsTxt: TextView
    /** Upload Flight Plan button. */
    private lateinit var uploadBtn: Button
    /** Activate Flight Plan button. */
    private lateinit var activateBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get user interface instances.
        droneStateTxt = findViewById(R.id.droneStateTxt)
        rcStateTxt = findViewById(R.id.rcStateTxt)
        uploadStateTxt = findViewById(R.id.uploadStateTxt)
        unavailabilityReasonsTxt = findViewById(R.id.unavailabilityReasonsTxt)

        activateBtn = findViewById<Button>(R.id.activateBtn).apply {
            setOnClickListener { onActivateClick() }
        }
        uploadBtn = findViewById<Button>(R.id.uploadPlanBtn).apply {
            setOnClickListener { onUploadClick() }
        }

        // Initialize user interface default values.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        uploadStateTxt.text = FlightPlanPilotingItf.UploadState.NONE.toString()

        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(this)
        // All references taken are linked to the activity lifecycle and
        // automatically closed at its destruction.

        // Monitor the auto connection facility.
        groundSdk.getFacility(AutoConnection::class.java) { autoConnection ->
            // Called when the auto connection facility is available and when it changes.
            autoConnection ?: return@getFacility // if it is not available, we have nothing to do

            // Start auto connection if necessary.
            if (autoConnection.status != AutoConnection.Status.STARTED) autoConnection.start()

            // If the drone has changed.
            if (drone?.uid != autoConnection.drone?.uid) {
                // Stop monitoring the previous drone.
                if (drone != null) stopDroneMonitors()
                // Monitor the new drone.
                drone = autoConnection.drone
                if (drone != null) startDroneMonitors()
            }

            // If the remote control has changed.
            if (rc?.uid  != autoConnection.remoteControl?.uid) {
                // Stop monitoring the old remote.
                if (rc != null) stopRcMonitors()
                // Monitor the new remote.
                rc = autoConnection.remoteControl
                if(rc != null) startRcMonitors()
            }
        }
    }

    /**
     * Starts drone monitors.
     */
    private fun startDroneMonitors() {
        // Monitor current drone state.
        droneStateRef = drone?.getState {droneState ->
            // Called at each drone state update.
            droneState ?: return@getState

            // Update drone connection state view.
            droneStateTxt.text = droneState.connectionState.toString()
        }


        // Monitor piloting interface.
        pilotingItfRef = drone?.getPilotingItf(
            FlightPlanPilotingItf::class.java, ::managePilotingItfState)
    }

    /**
     * Stops drone monitors.
     */
    private fun stopDroneMonitors() {
        // Close all references linked to the current drone to stop their monitoring.

        droneStateRef?.close()
        droneStateRef = null

        pilotingItfRef?.close()
        pilotingItfRef = null

        // Reset drone user interface views.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
    }

    /**
     * Manage piloting interface state.
     *
     * @param itf the piloting interface
     */
    private fun managePilotingItfState(itf: FlightPlanPilotingItf?) {
        uploadStateTxt.text = itf?.latestUploadState?.toString() ?: "N/A"

        unavailabilityReasonsTxt.text = itf?.unavailabilityReasons?.joinToString(separator = "\n")

        uploadBtn.isEnabled =
            itf?.latestUploadState !in setOf(null, FlightPlanPilotingItf.UploadState.UPLOADING)

        val state = itf?.state ?: Activable.State.UNAVAILABLE

        activateBtn.apply {
            isEnabled = state != Activable.State.UNAVAILABLE
            text = when (state) {
                Activable.State.ACTIVE -> "stop"
                else                   -> "start"
            }
        }
    }

    /**
     * Called on upload button click.
     */
    private fun onUploadClick() {
        val pilotingItf = pilotingItfRef?.get() ?: return
        // Install the flight plan file somewhere on the device FS.
        // NOTE: this is done so to keep things simple for the example, but this should not be done
        //       this way. Asset -> FS copy should be offloaded to an I/O thread in order not to
        //       block the main thread.
        val flightPlanFile = runCatching {
            assets.open("flightplan.mavlink").use { input ->
                File.createTempFile("flightplan", ".mavlink", cacheDir).also {
                    it.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }.getOrNull() ?: return

        pilotingItf.uploadFlightPlan(flightPlanFile)
    }

    /**
     * Called on activate button click.
     */
    private fun onActivateClick() {
        val pilotingItf = pilotingItfRef?.get() ?: return

        when (pilotingItf.state) {
            Activable.State.ACTIVE -> pilotingItf.stop()
            Activable.State.IDLE   -> pilotingItf.activate(true)
            else -> {}
        }
    }

    /**
     * Starts remote control monitors.
     */
    private fun startRcMonitors() {
        // Monitor current RC state.
        rcStateRef = rc?.getState { rcState ->
            // Called at each remote state update.
            rcState ?: return@getState

            // Update remote connection state view.
            rcStateTxt.text = rcState.connectionState.toString()
        }
    }

    /**
     * Stops remote control monitors.
     */
    private fun stopRcMonitors() {
        // Close all references linked to the current remote to stop their monitoring.
        rcStateRef?.close()
        rcStateRef = null

        // Reset remote control user interface views.
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
    }
}