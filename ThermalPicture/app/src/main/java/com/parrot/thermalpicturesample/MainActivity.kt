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

package com.parrot.thermalpicturesample

import android.graphics.PointF
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.libtproc.TProc
import com.parrot.libtproc.TProcPaletteFactory
import com.parrot.libtproc.TProcPicture
import com.parrot.libtproc.TProcPictureData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GroundSdk Thermal Picture Sample.
 *
 * This activity allows to display a thermal picture and temperature info.
 * It allows to use different thermal palettes.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ThermalPicture"

        const val pictureName = "ThermalPicture.jpg"
    }

    /** GroundSdk instance. */
    private lateinit var groundSdk: GroundSdk

    // User Interface:
    /** Picture thermal view. */
    private lateinit var pictureView: GLSurfaceView
    /** Picture view width. */
    private var pictureViewWidth = 0
    /** Picture view height. */
    private var pictureViewHeight = 0
    /** Text view to display the current lowest temperature rendered. */
    private lateinit var lowTmpTxt: TextView
    /** Text view to display the current highest temperature rendered. */
    private lateinit var hightTmpTxt: TextView
    /** Text view to display the current temperature rendered at the thermal probe location. */
    private lateinit var probeTmpTxt: TextView
    /** Text view to display the current thermal probe location X. */
    private lateinit var probeXTxt: TextView
    /** Text view to display the current thermal probe location Y. */
    private lateinit var probeYTxt: TextView
    /** Palettes selection. */
    private lateinit var palettesSelection: RadioGroup
    /** Relative Palette button. */
    private lateinit var relativeRBT: RadioButton
    /** Absolute Palette button. */
    private lateinit var absoluteRBT: RadioButton
    /** Spot Palette button. */
    private lateinit var spotRBT: RadioButton

    // Thermal Processing Part:
    /** Thermal picture processing. */
    private lateinit var tproc : TProcPicture
    /** Thermal picture data. */
    private var pictureData: TProcPictureData? = null
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
        tproc = TProc.createTProcPicture { status ->
            // Called for each thermal processing status.

            // Update user interface according to thermal processing status.
            status.min()?.let { min ->
                lowTmpTxt.text = min.temperature().toInt().toString()
            }

            status.max()?.let { max ->
                hightTmpTxt.text = max.temperature().toInt().toString()
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
        pictureView = findViewById(R.id.pictureView)
        pictureView.setEGLContextClientVersion(3)
        pictureView.setRenderer(object : GLSurfaceView.Renderer {

            override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
                Log.i(TAG, "onSurfaceCreated")
            }

            override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
                Log.i(TAG, "onSurfaceChanged")
                pictureViewWidth = width
                pictureViewHeight = height
            }

            override fun onDrawFrame(unused: GL10) {
                Log.i(TAG, "onDrawFrame")

                pictureData?.let{
                    if (!tproc.rendererIsStarted()) {
                        Log.i(TAG, "tproc.startRenderer")

                        // Set thermal Camera model to use according to the drone model.
                        val thermalCamera = if (it.droneModelId() == Drone.Model.ANAFI_UA.id())
                            TProc.ThermalCamera.BOSON else TProc.ThermalCamera.LEPTON

                        // keep visible picture aspect ratio
                        pictureViewWidth = (pictureViewHeight * (it.visibleWidth().toFloat() / it.visibleHeight())).toInt()
                        tproc.startRenderer(thermalCamera, pictureViewWidth, pictureViewHeight)
                    }

                    Log.i(TAG, "tproc.render")
                    tproc.render(pictureViewWidth, pictureViewHeight, it)
                } ?: run {
                    Log.e(TAG, "NO pictureData")
                }
            }
        })
        pictureView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        lowTmpTxt = findViewById(R.id.lowTmpTxt)
        hightTmpTxt = findViewById(R.id.hightTmpTxt)
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
            pictureView.requestRender()
        }

        // Initialize user interface
        resetUi()
    }

    override fun onStart() {
        super.onStart()

        val pictureFile = copyPictureOnInternalStorage()
        readPictureData(pictureFile)
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
     * Initialize thermal processing.
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
        lowTmpTxt.text = ""
        hightTmpTxt.text = ""
        probeTmpTxt.text = ""
        probeXTxt.text = ""
        probeYTxt.text = ""
        palettesSelection.check(relativeRBT.id)

        pictureView.requestRender()
    }

    /**
     * Reads picture data.
     *
     * @param pictureFile thermal picture file.
     */
    private fun readPictureData(pictureFile: File) {
        if (pictureFile.exists()) {
            if (pictureData == null) {
                // Read thermal picture data.
                pictureData = TProcPictureData.createFromFile(pictureFile)
            }

            // Redraw the thermal picture view.
            pictureView.requestRender()
        } else {
            Log.e(TAG, "${pictureFile.path} not exist !")
        }
    }

    /**
     * Copies sample thermal picture from resources to the internal storage.
     *
     * @return internal storage thermal picture file.
     */
    private fun copyPictureOnInternalStorage(): File {
        val internalStoragePictureFile = File(applicationContext.filesDir, pictureName)

        if (!internalStoragePictureFile.exists()) {
            try {
                resources.openRawResource(R.drawable.thermal_picture).use { inputStream ->
                    FileOutputStream(internalStoragePictureFile).use { outputStream ->
                        // Copy picture data
                        inputStream.copyTo(outputStream)
                        outputStream.flush()
                    }
                }
            } catch (e: IOException) {
                    e.printStackTrace()
            }
        }

        return internalStoragePictureFile
    }
}
