// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.android.stardroid.layers

import com.google.android.stardroid.math.getGeocentricCoords
import com.google.android.stardroid.math.Vector3.assign
import com.google.android.stardroid.space.Universe.solarSystemObjectFor
import com.google.android.stardroid.space.CelestialObject.getRaDec
import android.content.res.AssetManager
import com.google.android.stardroid.layers.AbstractSourceLayer
import com.google.android.stardroid.source.AstronomicalSource
import com.google.android.stardroid.layers.AbstractFileBasedLayer
import com.google.android.stardroid.source.proto.ProtobufAstronomicalSource
import com.google.android.stardroid.renderer.RendererObjectManager.UpdateType
import com.google.android.stardroid.util.MiscUtil
import com.google.android.stardroid.renderer.RendererControllerBase.RenderManager
import com.google.android.stardroid.renderer.RendererController
import com.google.android.stardroid.layers.AbstractLayer
import com.google.android.stardroid.renderer.RendererController.AtomicSection
import com.google.android.stardroid.renderer.util.UpdateClosure
import com.google.android.stardroid.source.TextPrimitive
import com.google.android.stardroid.source.PointPrimitive
import com.google.android.stardroid.source.LinePrimitive
import com.google.android.stardroid.source.ImagePrimitive
import com.google.android.stardroid.renderer.RendererControllerBase
import com.google.android.stardroid.search.PrefixStore
import com.google.android.stardroid.layers.AbstractSourceLayer.SourceUpdateClosure
import com.google.android.stardroid.source.Sources
import com.google.android.stardroid.math.Vector3
import com.google.android.stardroid.renderer.util.AbstractUpdateClosure
import com.google.android.stardroid.layers.EclipticLayer.EclipticSource
import com.google.android.stardroid.R
import com.google.android.stardroid.source.AbstractAstronomicalSource
import com.google.android.stardroid.layers.GridLayer.GridSource
import com.google.android.stardroid.math.RaDec
import com.google.android.stardroid.control.AstronomerModel
import com.google.android.stardroid.layers.HorizonLayer.HorizonSource
import com.google.android.stardroid.base.TimeConstants
import com.google.android.stardroid.layers.IssLayer.IssSource
import com.google.android.stardroid.layers.IssLayer.OrbitalElementsGrabber
import kotlin.Throws
import com.google.android.stardroid.ephemeris.OrbitalElements
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Resources
import android.util.Log
import com.google.android.stardroid.layers.LayerManager
import com.google.android.stardroid.search.SearchTermsProvider.SearchTerm
import com.google.android.stardroid.layers.MeteorShowerLayer.Shower
import com.google.android.stardroid.layers.MeteorShowerLayer
import com.google.android.stardroid.layers.MeteorShowerLayer.MeteorRadiantSource
import com.google.android.stardroid.ephemeris.Planet
import com.google.android.stardroid.ephemeris.PlanetSource
import com.google.android.stardroid.layers.SkyGradientLayer
import com.google.android.stardroid.layers.StarOfBethlehemLayer.StarOfBethlehemSource
import com.google.android.stardroid.layers.StarOfBethlehemLayer
import com.google.android.stardroid.search.SearchResult
import com.google.android.stardroid.space.Universe
import java.util.concurrent.locks.ReentrantLock

/**
 * If enabled, keeps the sky gradient up to date.
 *
 * @author John Taylor
 * @author Brent Bryan
 */
class SkyGradientLayer(private val model: AstronomerModel, private val resources: Resources) :
    Layer {
    private val rendererLock = ReentrantLock()
    private var renderer: RendererController? = null
    private var lastUpdateTimeMs = 0L
    override fun initialize() {}
    override fun registerWithRenderer(controller: RendererController?) {
        renderer = controller
        redraw()
    }

    override fun setVisible(visible: Boolean) {
        Log.d(TAG, "Setting showSkyGradient $visible")
        if (visible) {
            redraw()
        } else {
            rendererLock.lock()
            try {
                renderer!!.queueDisableSkyGradient()
            } finally {
                rendererLock.unlock()
            }
        }
    }

    /** Redraws the sky shading gradient using the model's current time.  */
    protected fun redraw() {
        val modelTime = model.time
        if (Math.abs(modelTime.time - lastUpdateTimeMs) > UPDATE_FREQUENCY_MS) {
            lastUpdateTimeMs = modelTime.time
            val sunPosition = universe.solarSystemObjectFor(Planet.Sun).getRaDec(modelTime)
            // Log.d(TAG, "Enabling sky gradient with sun position " + sunPosition);
            rendererLock.lock()
            try {
                renderer!!.queueEnableSkyGradient(getGeocentricCoords(sunPosition))
            } finally {
                rendererLock.unlock()
            }
        }
    }

    override val layerDepthOrder: Int
        get() = -10
    override val preferenceId: String
        get() = "source_provider.$layerNameId"
    override val layerName: String
        get() = resources.getString(layerNameId)
    private val layerNameId: Int
        private get() = R.string.show_sky_gradient

    override fun searchByObjectName(name: String): List<SearchResult?>? {
        return emptyList()
    }

    override fun getObjectNamesMatchingPrefix(prefix: String): Set<String> {
        return emptySet()
    }

    companion object {
        private val TAG = MiscUtil.getTag(SkyGradientLayer::class.java)
        private const val UPDATE_FREQUENCY_MS = 5L * TimeConstants.MILLISECONDS_PER_MINUTE
        val universe = Universe()
    }
}