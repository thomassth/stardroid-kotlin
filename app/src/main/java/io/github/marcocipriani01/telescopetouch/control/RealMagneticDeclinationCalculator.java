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

package io.github.marcocipriani01.telescopetouch.control;

import android.hardware.GeomagneticField;
import android.location.Location;

import androidx.annotation.NonNull;

/**
 * Encapsulates the calculation of magnetic declination for the user's location
 * and position.
 *
 * @author John Taylor
 */
public class RealMagneticDeclinationCalculator implements MagneticDeclinationCalculator {

    private GeomagneticField geomagneticField;

    /**
     * {@inheritDoc}
     * Silently returns zero if the time and location have not been set.
     */
    @Override
    public float getDeclination() {
        if (geomagneticField == null) return 0.0f;
        return geomagneticField.getDeclination();
    }

    /**
     * Sets the user's current location and time.
     */
    @Override
    public void setLocationAndTime(Location location, long timeInMillis) {
        geomagneticField = new GeomagneticField((float) location.getLatitude(),
                (float) location.getLongitude(), (float) location.getAltitude(), timeInMillis);
    }

    @NonNull
    @Override
    public String toString() {
        return "Real Magnetic Correction";
    }
}