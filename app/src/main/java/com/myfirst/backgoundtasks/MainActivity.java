package com.myfirst.backgoundtasks;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkRequest;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String SOME_VALUE_KEY = "SOME_VALUE_KEY";
    public static final String PROGRESS_VALUE_KEY = "PROGRESS_VALUE_KEY";
    public static final String SOME_WORK_ID = "SOME_WORK_ID";
    public static final String SOME_PERIOD_WORK_ID = "SOME_PERIOD_WORK_ID";

    private App app;

    private void initApp() {
        if (app == null) app = (App) getApplication();
    }

    private TextView tvProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initApp();
        tvProgress = findViewById(R.id.tv_progress);
        tvProgress.setMovementMethod(new ScrollingMovementMethod());
    }

    public void doSomeWork(View view) {

        int value = new Random().nextInt(3);
        Log.d(TAG, "start work with value " + value);
        Data data = new Data.Builder()
                .putInt(SOME_VALUE_KEY, value)
                .build();

        @SuppressLint("IdleBatteryChargingConstraints")
        Constraints constraints = new Constraints.Builder()
//                .setRequiresBatteryNotLow(true)
//                .setRequiredNetworkType(NetworkType.CONNECTED)
//                .setRequiresCharging(true)
//                .setRequiresStorageNotLow(true)
//                .setRequiresDeviceIdle(true)
                .build();

//        WorkRequest someWorkRequest = OneTimeWorkRequest.from(SomeWork.class);
        WorkRequest someWorkRequest =
                new OneTimeWorkRequest.Builder(SomeWork.class)
                        .addTag(SOME_WORK_ID)
                        .setInitialDelay(1, TimeUnit.SECONDS)
                        .setInputData(data)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS, // set custom value
                                TimeUnit.MILLISECONDS)
                        .build();

        // working before ENQUEUED
        app.workManager.enqueue(someWorkRequest).getState().observe(this, state -> {
            if (state.toString().equals("SUCCESS")) {
                Log.d(TAG, "SUCCESS");
            } else if (state.toString().equals("FAILURE")) {
                Log.d(TAG, "FAILURE");
            } else {
                Log.d(TAG, "IN_PROGRESS");
            }
        });

        // working before ENQUEUED
        app.workManager.getWorkInfoById(someWorkRequest.getId()).addListener((Runnable) () -> {
            Log.d(TAG, "listener");
        }, Executors.newSingleThreadExecutor());

        app.workManager.getWorkInfoByIdLiveData(someWorkRequest.getId())
                .observe(this, this::logProgress);

    }

    public void doPeriodicSomeWork(View view) {
        int value = new Random().nextInt(3);
        Log.d(TAG, "start work with value " + value);
        Data data = new Data.Builder()
                .putInt(SOME_VALUE_KEY, value) // todo why is value not working???
                .build();

        PeriodicWorkRequest periodicRequest =
                new PeriodicWorkRequest.Builder(SomeWork.class,
                        10, TimeUnit.SECONDS)
                        .setInputData(data)
                        .addTag(SOME_PERIOD_WORK_ID)
                        .build();

        app.workManager.enqueue(periodicRequest);

        app.workManager.getWorkInfoByIdLiveData(periodicRequest.getId())
                .observe(this, this::logProgress);
    }

//    todo add new button for this works
//    app.workManager.beginWith().then().then()

    HashMap<String, Long> mapStartTime = new HashMap<>();

    private void logProgress(WorkInfo workInfo) {
        String workId = workInfo.getId().toString();
        tvProgress.append(workId + ":\n");

        if (workInfo.getState() == WorkInfo.State.ENQUEUED) {
            mapStartTime.put(workId, System.currentTimeMillis());
        } else {
            double currentTime = (System.currentTimeMillis() - mapStartTime.get(workId));
            currentTime /= 1000;
            tvProgress.append(" --- " + currentTime + ": ");
        }

        tvProgress.append(workInfo.getState().name() + " ");

        String progress = workInfo.getProgress().getString(PROGRESS_VALUE_KEY);
        if (progress != null) {
            tvProgress.append(progress);
        }

        tvProgress.append("\n");
    }
}