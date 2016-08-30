package ***.utils.location;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import timber.log.Timber;

/**
 * Play services realisation, not always available, check by {link to static check method here}.
 */
public class PlayServicesLocationProvider extends AbstractGeolocationProvider implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static boolean isGoogleServicesAvailable(@NonNull final Context context) {
        final int responseCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        switch (responseCode) {
            case ConnectionResult.SUCCESS:
                Timber.i("Google service is available");
                return true;

            case ConnectionResult.SERVICE_MISSING:
                Timber.w("Google service is missing");
                return false;

            case ConnectionResult.SERVICE_UPDATING:
                // TODO: try again later (or use specific listener)
                Timber.w("Google services is updating");
                return false;

            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Timber.w("Google service requires update");
                return false;

            case ConnectionResult.SERVICE_DISABLED:
                Timber.w("Google service is disabled");
                return false;

            case ConnectionResult.SERVICE_INVALID:
                Timber.i("Google services is invalid");
                return false;

            default:
                Timber.w("Unhandled Google service response");
                return false;
        }
    }

    private static final int REQUEST_CHECK_SETTINGS = 1;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean mIsSettingsChecked;
    private Activity mActivity;

    /* package */ PlayServicesLocationProvider() {
        mLocationRequest = LocationRequest.create()
                .setInterval(UPDATES_INTERVAL_MILLIS)
                .setFastestInterval(THROTTLE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void initialize(@NonNull final Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void subscribe(@NonNull final Activity activity) {
        mActivity = activity;
        mGoogleApiClient.connect();
    }

    @Override
    public void unsubscribe() {
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        if (!mIsSettingsChecked) {
            checkLocationSettings();
            mIsSettingsChecked = true;
        }

        try {
            final Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastLocation != null) {
                mLocation = lastLocation;
            }
            // subscribe to updates
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (final SecurityException exception) {
            Timber.e("Location permission is not granted");
        }
    }

    @Override
    public void onConnectionSuspended(final int i) {
        Timber.i("PlayServices connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Timber.e("PlayServices connection failed");
    }

    @Override
    public void onLocationChanged(final Location location) {
        if (location != null) {
            mLocation = location;
            if (mUpdateListener != null) {
                mUpdateListener.onLocationUpdated(location);
            }
        }
    }

    private void checkLocationSettings() {
        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        final PendingResult<LocationSettingsResult> pendingResult =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        pendingResult.setResultCallback(result -> {
            final Status status = result.getStatus();
            switch (status.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    // All location settings are satisfied. The client can
                    // initialize location requests here.
                    break;

                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // TODO: check the result in onActivityResult().
                        status.startResolutionForResult(
                                mActivity,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {
                        // Ignore the error.
                    }
                    break;

                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    // Location settings are not satisfied. However, we have no way
                    // to fix the settings so we won't show the dialog.
                    break;
            }
        });
    }
}
