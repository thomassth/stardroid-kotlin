/*
 * Copyright 2020 Marco Cipriani (@marcocipriani01) and the Sky Map Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.marcocipriani01.telescopetouch.layers;

import android.content.SharedPreferences;
import android.content.res.Resources;

import java.util.ArrayList;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.control.AstronomerModel;
import io.github.marcocipriani01.telescopetouch.ephemeris.Planet;
import io.github.marcocipriani01.telescopetouch.ephemeris.PlanetSource;
import io.github.marcocipriani01.telescopetouch.source.AstronomicalSource;

/**
 * An implementation of the {@link Layer} interface for displaying planets in
 * the Renderer.
 *
 * @author John Taylor
 * @author Brent Bryan
 */
public class PlanetsLayer extends AbstractLayer {

    private final SharedPreferences preferences;
    private final AstronomerModel model;

    public PlanetsLayer(AstronomerModel model, Resources resources, SharedPreferences preferences) {
        super(resources, true);
        this.preferences = preferences;
        this.model = model;
    }

    @Override
    protected void initializeAstroSources(ArrayList<AstronomicalSource> sources) {
        for (Planet planet : Planet.values()) {
            sources.add(new PlanetSource(planet, getResources(), model, preferences));
        }
    }

    // If the preference Id is needed. There is no super method and no need
    // to override.
    public String getPreferenceId() {
        return "source_provider.3";
    }

    @Override
    public int getLayerDepthOrder() {
        // TODO(brent): refactor these to a common location.
        return 60;
    }

    @Override
    protected int getLayerNameId() {
        return R.string.solar_system;
    }
}