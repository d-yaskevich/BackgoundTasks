package com.myfirst.backgoundtasks;

import android.app.Application;

import androidx.work.WorkManager;

public class App extends Application {

    WorkManager workManager;

    private void initWorkManager() {
        if (workManager == null) {
            workManager = WorkManager.getInstance(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initWorkManager();
    }
}
