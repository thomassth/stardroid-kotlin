// Copyright 2010 Google Inc.
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

package com.google.android.stardroid.util.smoothers;

import android.content.SharedPreferences;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.android.stardroid.ApplicationConstants;
import com.google.android.stardroid.control.AstronomerModel;
import com.google.android.stardroid.math.Vector3;
import com.google.android.stardroid.util.MiscUtil;

import javax.inject.Inject;

/**
 * Adapts sensor output for use with the astronomer model.
 *
 * @author John Taylor
 */
public class PlainSmootherModelAdaptor implements SensorListener {
  private static final String TAG = MiscUtil.getTag(PlainSmootherModelAdaptor.class);
  private Vector3 magneticValues = ApplicationConstants.INITIAL_SOUTH.copyForJ();
  private Vector3 acceleration = ApplicationConstants.INITIAL_DOWN.copyForJ();
  private AstronomerModel model;
  private boolean reverseMagneticZaxis;

  @Inject
  PlainSmootherModelAdaptor(AstronomerModel model, SharedPreferences sharedPreferences) {
    this.model = model;
    reverseMagneticZaxis = sharedPreferences.getBoolean(
        ApplicationConstants.REVERSE_MAGNETIC_Z_PREFKEY, false);
  }

  @Override
  public void onSensorChanged(int sensor, float[] values) {
    if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
      acceleration.x = values[0];
      acceleration.y = values[1];
      acceleration.z = values[2];
    } else if (sensor == SensorManager.SENSOR_MAGNETIC_FIELD) {
      magneticValues.x = values[0];
      magneticValues.y = values[1];
      // The z direction for the mag magneticField sensor is in the opposite
      // direction to that for accelerometer, except on some phones that are doing it wrong.
      // Yes that's right, the right thing to do is to invert it.  So if we reverse that,
      // we don't invert it.  Got it?
      // TODO(johntaylor): this might not be the best place to do this.
      magneticValues.z = reverseMagneticZaxis ? values[2] : -values[2];
    } else {
      Log.e(TAG, "Pump is receiving values that aren't accel or magnetic");
    }
    model.setPhoneSensorValues(acceleration, magneticValues);
  }

  @Override
  public void onAccuracyChanged(int sensor, int accuracy) {
    // Do nothing, at present.
  }
}
