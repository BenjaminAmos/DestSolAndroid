package com.miloshpetrov.sol2.android;

import android.os.Bundle;
import android.content.Context;
import android.util.Log;
import android.support.multidex.MultiDex;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import org.destinationsol.SolApplication;
import org.destinationsol.modules.ModuleManager;
import org.destinationsol.android.AndroidModuleManager;

public class SolAndroid extends AndroidApplication {
    @Override
    protected void attachBaseContext(Context base) {
       super.attachBaseContext(base);
       MultiDex.install(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

        ModuleManager manager = new AndroidModuleManager(this);

        try {
            manager.init();
        } catch (Exception e) {
            Log.e("DESTINATION_SOL_INIT", "Failed to initialise ModuleManager.");
        }

        try {
            initialize(new SolApplication(manager, 60.0f), config);
        } catch (Exception e) {
            Log.e("DESTINATION_SOL", "FATAL ERROR: Forced abort!", e);
        }
    }
}
