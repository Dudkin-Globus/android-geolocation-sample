package ***;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

public abstract class AbstractGeolocationProvider {

    /* package */ static final long UPDATES_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(15);
    /* package */ static final long THROTTLE_INTERVAL = TimeUnit.SECONDS.toMillis(1);

    @Nullable
    /* package */ Location mLocation = null;

    @Nullable
    /* package */ LocationUpdateListener mUpdateListener;

    abstract void initialize(final Context context);

    public abstract void subscribe(final Activity activity);

    abstract void unsubscribe();

    /* package */ void setUpdateListener(@Nullable final LocationUpdateListener listener) {
        mUpdateListener = listener;
    }

    @Nullable
    /* package */ Location getCurrentLocation() {
        return mLocation;
    }

}
