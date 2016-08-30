package ***.utils.location;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;

import timber.log.Timber;

public class SystemManagerLocationProvider extends AbstractGeolocationProvider {

    private String mCurrentLocationProvider = LocationManager.NETWORK_PROVIDER;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(final Location location) {
            if (location != null) {
                mLocation = location;
                if (mUpdateListener != null) {
                    mUpdateListener.onLocationUpdated(location);
                }
            }
        }

        @Override
        public void onStatusChanged(final String provider, final int status, final Bundle extras) {

        }

        @Override
        public void onProviderEnabled(final String provider) {
            if (LocationManager.GPS_PROVIDER.equals(provider) &&
                    LocationManager.NETWORK_PROVIDER.equals(mCurrentLocationProvider)) {
                mCurrentLocationProvider = LocationManager.GPS_PROVIDER;
                unsubscribe();
                subscribe(null);
            }
        }

        @Override
        public void onProviderDisabled(final String provider) {
            if (LocationManager.NETWORK_PROVIDER.equals(provider) &&
                    LocationManager.GPS_PROVIDER.equals(mCurrentLocationProvider)) {
                mCurrentLocationProvider = LocationManager.NETWORK_PROVIDER;
                unsubscribe();
                subscribe(null);
            }
        }
    };

    @Override
    void initialize(final Context context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mCurrentLocationProvider = LocationManager.GPS_PROVIDER;
            }
            final Location lastLocation = mLocationManager.getLastKnownLocation(mCurrentLocationProvider);
            if (lastLocation != null) {
                mLocation = lastLocation;
            }
        } catch (final SecurityException exception) {
            Timber.e("Location permission is not granted");
        }
    }

    @Override
    public void subscribe(@Nullable final Activity activity) {
        // subscribe to updates
        try {
            mLocationManager.requestLocationUpdates(mCurrentLocationProvider, UPDATES_INTERVAL_MILLIS, 0, mLocationListener);
        } catch (final SecurityException exception) {
            Timber.e("Location permission is not granted");
        }
    }

    @Override
    void unsubscribe() {
        try {
            // remove the listener
            mLocationManager.removeUpdates(mLocationListener);
        } catch (final SecurityException exception) {
            Timber.e("Location permission is not granted");
        }
    }
}
