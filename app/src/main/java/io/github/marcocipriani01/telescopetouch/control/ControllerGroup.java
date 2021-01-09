package io.github.marcocipriani01.telescopetouch.control;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

import javax.inject.Inject;

import io.github.marcocipriani01.telescopetouch.base.VisibleForTesting;
import io.github.marcocipriani01.telescopetouch.units.GeocentricCoordinates;
import io.github.marcocipriani01.telescopetouch.util.MiscUtil;

/**
 * Manages all the different controllers that affect the model of the observer.
 * Is both a factory and acts as a facade to the underlying controllers.
 *
 * @author John Taylor
 */
public class ControllerGroup implements Controller {

    private final static String TAG = MiscUtil.getTag(ControllerGroup.class);
    private final ArrayList<Controller> controllers = new ArrayList<>();
    private final ZoomController zoomController;
    private final ManualOrientationController manualDirectionController;
    private final SensorOrientationController sensorOrientationController;
    private final TimeTravelClock timeTravelClock = new TimeTravelClock();
    private final TransitioningCompositeClock transitioningClock = new TransitioningCompositeClock(
            timeTravelClock, new RealClock());
    private final TeleportingController teleportingController;
    private boolean usingAutoMode = true;
    private AstronomerModel model;

    // TODO(jontayler): inject everything else.
    @Inject
    ControllerGroup(SensorOrientationController sensorOrientationController, LocationController locationController) {
        addController(locationController);
        this.sensorOrientationController = sensorOrientationController;
        addController(sensorOrientationController);
        manualDirectionController = new ManualOrientationController();
        addController(manualDirectionController);
        zoomController = new ZoomController();
        addController(zoomController);
        teleportingController = new TeleportingController();
        addController(teleportingController);
        setAutoMode(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        Log.i(TAG, "Enabling all controllers");
        for (Controller controller : controllers) {
            controller.setEnabled(enabled);
        }
    }

    @Override
    public void setModel(AstronomerModel model) {
        Log.i(TAG, "Setting model");
        for (Controller controller : controllers) {
            controller.setModel(model);
        }
        this.model = model;
        model.setAutoUpdatePointing(usingAutoMode);
        model.setClock(transitioningClock);
    }

    /**
     * Switches to time-travel model and start with the supplied time.
     * See {@link #useRealTime()}.
     */
    public void goTimeTravel(Date d) {
        transitioningClock.goTimeTravel(d);
    }

    /**
     * Gets the id of the string used to display the current speed of time travel.
     */
    public int getCurrentSpeedTag() {
        return timeTravelClock.getCurrentSpeedTag();
    }

    /**
     * Sets the model back to using real time.
     * See {@link #goTimeTravel(Date)}.
     */
    public void useRealTime() {
        transitioningClock.returnToRealTime();
    }

    /**
     * Increases the rate of time travel into the future (or decreases the rate of
     * time travel into the past) if in time travel mode.
     */
    public void accelerateTimeTravel() {
        timeTravelClock.accelerateTimeTravel();
    }

    /**
     * Decreases the rate of time travel into the future (or increases the rate of
     * time travel into the past) if in time travel mode.
     */
    public void decelerateTimeTravel() {
        timeTravelClock.decelerateTimeTravel();
    }

    /**
     * Pauses time, if in time travel mode.
     */
    public void pauseTime() {
        timeTravelClock.pauseTime();
    }

    /**
     * Are we in auto mode (aka sensor mode) or manual?
     */
    public boolean isAutoMode() {
        return usingAutoMode;
    }

    /**
     * Sets auto mode (true) or manual mode (false).
     */
    public void setAutoMode(boolean enabled) {
        manualDirectionController.setEnabled(!enabled);
        sensorOrientationController.setEnabled(enabled);
        if (model != null) {
            model.setAutoUpdatePointing(enabled);
        }
        usingAutoMode = enabled;
    }

    @Override
    public void start() {
        Log.i(TAG, "Starting controllers");
        for (Controller controller : controllers) {
            controller.start();
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping controllers");
        for (Controller controller : controllers) {
            controller.stop();
        }
    }

    /**
     * Moves the pointing right and left.
     *
     * @param radians the angular change in the pointing in radians (only
     *                accurate in the limit as radians tends to 0.)
     */
    public void changeRightLeft(float radians) {
        manualDirectionController.changeRightLeft(radians);
    }

    /**
     * Moves the pointing up and down.
     *
     * @param radians the angular change in the pointing in radians (only
     *                accurate in the limit as radians tends to 0.)
     */
    public void changeUpDown(float radians) {
        manualDirectionController.changeUpDown(radians);
    }

    /**
     * Rotates the view about the current center point.
     */
    public void rotate(float degrees) {
        manualDirectionController.rotate(degrees);
    }

    /**
     * Sends the astronomer's pointing to the new target.
     *
     * @param target the destination
     */
    public void teleport(GeocentricCoordinates target) {
        teleportingController.teleport(target);
    }

    /**
     * Adds a new controller to this
     */
    @VisibleForTesting
    public void addController(Controller controller) {
        controllers.add(controller);
    }

    public void zoomBy(float ratio) {
        zoomController.zoomBy(ratio);
    }
}
