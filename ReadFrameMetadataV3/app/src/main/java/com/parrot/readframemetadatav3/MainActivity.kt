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

package com.parrot.readframemetadatav3

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.stream.GsdkStreamView

/**
 * GroundSdk Read Frame metadataV3 Sample.
 *
 * This activity allows the application to connect to a drone.
 * It displays the connection state, the video stream and
 * reads the drone quaternion from frame metadataV3 received from the overlayer.
 */
class ReadFrameMetadataV3 : AppCompatActivity() {

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

    // User Interface:
    /** Drone state text view. */
    private lateinit var droneStateTxt: TextView
    /** Drone quaternion text view. */
    private lateinit var droneQuatTxt: TextView
    /** Video stream view. */
    private lateinit var streamView: GsdkStreamView

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
        droneQuatTxt = findViewById(R.id.droneQuatTxt)

        // Set stream view overlayer.
        streamView.setOverlayer2 { overlayContext ->
            // Called at each frame rendering.

            // Read drone quaternion from metadata native pointer.
            val droneQuaternion = FloatArray(4)
            readFrameMetadataDroneQuat(overlayContext.frameMetadataHandle(), droneQuaternion)

            // Display quaternion values.
            runOnUiThread { droneQuatTxt.text = "x: ${"%.2f".format(droneQuaternion[0])}" +
                    " y: ${"%.2f".format(droneQuaternion[1])}"+
                    " z: ${"%.2f".format(droneQuaternion[2])}" +
                    " w: ${"%.2f".format(droneQuaternion[3])}" }
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
        droneQuatTxt.text = ""

        // Stop rendering the stream
        streamView.setStream(null)

    }

    /**
     * Starts drone monitors.
     */
    private fun startDroneMonitors() {
        // Monitor drone state.
        monitorDroneState()

        // Monitor stream server.
        monitorStreamServer()
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

            // Start to play the live stream.
            liveStream?.let {
                startVideoStream(liveStream)

                // Set the live stream as the stream to be render by the stream view.
                streamView.setStream(liveStream)
            }
        }
    }

    /**
     * Starts the video stream.
     *
     * @param liveStream: the stream to start.
     */
    private fun startVideoStream(liveStream: CameraLive) {
        // Prevent stream restart
        if (liveStream.playState() == CameraLive.PlayState.PLAYING) return

        // Force the stream server enabling.
        streamServerRef?.get()?.enableStreaming(true)

        // Play the live stream.
        liveStream.play()
    }

    /**
     * Reads drone quaternion.
     *
     * @param frameMetadataHandle frame metadata native pointer.
     * @param quaternion array of 4 floats to store quaternion values.
     *
     * @note Native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private external fun readFrameMetadataDroneQuat(frameMetadataHandle: Long,
                                                    quaternion: FloatArray)

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}