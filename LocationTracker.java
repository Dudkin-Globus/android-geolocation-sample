package ***;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import ***.content.models.PointModel;
import ***.utils.permission.Permissions;
import ***.utils.permission.PermissionsCompat;

import timber.log.Timber;

public class LocationTracker implements Application.ActivityLifecycleCallbacks {

    private static final int PERMISSION_STATE_NOT_REQUESTED = 0;
    private static final int PERMISSION_STATE_GRANTED = 1;
    private static final int PERMISSION_STATE_DENIED = -1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private AbstractGeolocationProvider mProvider;
    private boolean mInitialized;
    private int mPermissionState = PERMISSION_STATE_NOT_REQUESTED;

    @Override
    public void onActivityCreated(final Activity activity, final Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(final Activity activity) {
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        if (checkLocationPermission(activity)) {
            runGeolocation(activity);
        }
    }

    private boolean checkLocationPermission(@NonNull final Activity activity) {
        switch (mPermissionState) {
            case PERMISSION_STATE_DENIED:
                // user has explicitly forbidden geolocation, no matter to ask again
                return false;

            // user could revoke geolocation permission
            case PERMISSION_STATE_GRANTED:
            case PERMISSION_STATE_NOT_REQUESTED:
                // since API23 dangerous permissions must be requested at runtime
                final Permissions permissions = PermissionsCompat.checkPermissions(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissions.isAllPermissionsGranted()) {
                    mPermissionState = PERMISSION_STATE_GRANTED;
                    return true;

                } else {
                    PermissionsCompat.requestPermissions(activity, permissions, LOCATION_PERMISSION_REQUEST_CODE);
                    return false;
                }

            default:
                throw new IllegalStateException("Unexpected permission state, probably developer's mistake");
        }
    }

    public void requestRuntimePermissions(@NonNull final Activity activity) {
        final Permissions permissions = PermissionsCompat.checkPermissions(activity,
                Manifest.permission.ACCESS_FINE_LOCATION);
        PermissionsCompat.requestPermissions(activity, permissions, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void runGeolocation(@NonNull final Activity activity) {
        if (!mInitialized) {
            if (PlayServicesLocationProvider.isGoogleServicesAvailable(activity)) {
                mProvider = new PlayServicesLocationProvider();
            } else {
                mProvider = new SystemManagerLocationProvider();
            }
            mProvider.initialize(activity);
            mInitialized = true;
        }
        mProvider.subscribe(activity);
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        if (isLocationTrackingAllowed()) {
            mProvider.unsubscribe();
        }
    }

    @Override
    public void onActivityStopped(final Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(final Activity activity, final Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(final Activity activity) {
    }

    @NonNull
    public MetaDataLocation getCurrentLocation() {
        final Location location = (mProvider != null ? mProvider.getCurrentLocation() : null);
        return new MetaDataLocation(location);
    }

    public void setUpdatesListener(@Nullable LocationUpdateListener listener) {
        mProvider.setUpdateListener(listener);
    }

    public boolean onRequestPermissionsResult(final int requestCode,
                                              @NonNull final String[] permissions,
                                              @NonNull final int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            final Permissions response = new Permissions(permissions, grantResults);
            mPermissionState = (response.isAllPermissionsGranted() ?
                    PERMISSION_STATE_GRANTED : PERMISSION_STATE_DENIED);
            return true;

        } else {
            return false;
        }
    }

    public boolean isLocationTrackingAllowed() {
        return mPermissionState == PERMISSION_STATE_GRANTED;
    }

    /**
     * Calculates distance between user last known position and given point.
     *
     * @param point remote place to calculate distance to.
     * @return distance in meters.
     */
    public double getDistanceToPoint(@NonNull final PointModel point) {
        return getDistanceTo(point.latitude, point.longitude);
    }

    public double getDistanceTo(final double eventLatitude, final double eventLongitude) {
        final MetaDataLocation metaDataLocation = getCurrentLocation();
        final Location location = metaDataLocation.getLocation();
        Timber.i("Calculating distance from eventLocation(%.6f;%.6f) to currentLocation(%.6f;%.6f)",
                eventLatitude, eventLongitude, location.getLatitude(), location.getLongitude());

        final Location eventLocation = new Location("event");
        eventLocation.setLatitude(eventLatitude);
        eventLocation.setLongitude(eventLongitude);

        return eventLocation.distanceTo(location);
    }
}
