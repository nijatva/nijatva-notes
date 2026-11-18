package in.nijatva.notes;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/** Locks the vault when the last activity stops, so backgrounding the app always
 *  drops the key from memory. No GMS, no services, just lifecycle callbacks. */
public class App extends Application {

    private int started = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity a, Bundle b) {
            }

            public void onActivityStarted(Activity a) {
                started++;
            }

            public void onActivityResumed(Activity a) {
            }

            public void onActivityPaused(Activity a) {
            }

            public void onActivityStopped(Activity a) {
                started--;
                if (started <= 0) {
                    started = 0;
                    Vault.lock();
                }
            }

            public void onActivitySaveInstanceState(Activity a, Bundle b) {
            }

            public void onActivityDestroyed(Activity a) {
            }
        });
    }
}
