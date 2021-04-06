package com.myfirst.backgoundtasks;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String SOME_VALUE_KEY = "SOME_VALUE_KEY";
    public static final String PROGRESS_VALUE_KEY = "PROGRESS_VALUE_KEY";
    public static final String SOME_WORK_ID = "SOME_WORK_ID";
    public static final String SOME_PERIOD_WORK_ID = "SOME_PERIOD_WORK_ID";

    public final SimpleDateFormat formatDateTime = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault());

    private App app;

    private void initApp() {
        if (app == null) app = (App) getApplication();
    }

    private AlarmManager alarmManager;
    private Intent receiverIntent;
    private BroadcastReceiver broadcastReceiver;

    private TextView tvProgress;
    private TextView tvDateTime;
    private TextView etRepeatTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initApp();
        receiverIntent = new Intent(this, SomeBroadcastReceiver.class);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        cancelAlarm(null);

        tvProgress = findViewById(R.id.tv_progress);
        tvProgress.setMovementMethod(new ScrollingMovementMethod());

        tvDateTime = findViewById(R.id.tv_date_time);
        tvDateTime.setOnClickListener(this);

        etRepeatTime = findViewById(R.id.et_repeat_time);

        broadcastReceiver = new SomeBroadcastReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        this.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    public void doSomeWork(View view) {

        int value = new Random().nextInt(3);
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

        Log.d(TAG, "start work with value " + value);
        app.workManager.enqueue(someWorkRequest).getState().observe(this, state -> {
            // working before ENQUEUED
            if (state.toString().equals("SUCCESS")) {
                Log.d(TAG, "SUCCESS");
            } else if (state.toString().equals("FAILURE")) {
                Log.d(TAG, "FAILURE");
            } else {
                Log.d(TAG, "IN_PROGRESS");
            }
        });

        app.workManager.getWorkInfoById(someWorkRequest.getId()).addListener((Runnable) () -> {
            // working before ENQUEUED
            Log.d(TAG, "listener");
        }, Executors.newSingleThreadExecutor());

        app.workManager.getWorkInfoByIdLiveData(someWorkRequest.getId())
                .observe(this, this::logProgress);

    }

    public void doPeriodicSomeWork(View view) {
        int value = new Random().nextInt(3);
        Data data = new Data.Builder()
                .putInt(SOME_VALUE_KEY, value) // todo why is value not working???
                .build();

        PeriodicWorkRequest periodicRequest =
                new PeriodicWorkRequest.Builder(SomeWork.class,
                        10, TimeUnit.SECONDS)
                        .setInputData(data)
                        .addTag(SOME_PERIOD_WORK_ID)
                        .build();

        Log.d(TAG, "start periodic work with value " + value);
        app.workManager.enqueue(periodicRequest);

        app.workManager.getWorkInfoByIdLiveData(periodicRequest.getId())
                .observe(this, this::logProgress);
    }

//    todo add new button for this works
//    app.workManager.beginWith().then().then()

    private HashMap<String, Long> mapStartTime = new HashMap<>();

    private void logProgress(WorkInfo workInfo) {
        StringBuilder stringBuilder = new StringBuilder();
        String workId = workInfo.getId().toString();
        stringBuilder.append(workId).append(":\n");

        if (workInfo.getState() == WorkInfo.State.ENQUEUED) {
            mapStartTime.put(workId, System.currentTimeMillis());
        } else {
            double currentTime = (System.currentTimeMillis() - mapStartTime.get(workId));
            currentTime /= 1000;
            stringBuilder.append(" --- ").append(currentTime).append(": ");
        }

        stringBuilder.append(workInfo.getState().name()).append(" ");

        String progress = workInfo.getProgress().getString(PROGRESS_VALUE_KEY);
        if (progress != null) {
            stringBuilder.append(progress);
        }

        stringBuilder.append("\n");

        Log.d(TAG, "progress - " + stringBuilder.toString());
        tvProgress.append(stringBuilder.toString());
    }

    private final Calendar calendar = Calendar.getInstance();

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_date_time:
                showDateTimeDialog(calendar, () -> {
                    Date date = calendar.getTime();
                    tvDateTime.setText(formatDateTime.format(date));
                });
                break;
        }
    }

    interface OnDateTimeCallback {
        void onDateTimeSelected();
    }

    private void showDateTimeDialog(Calendar calendar, OnDateTimeCallback callback) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (dateTimeDialogView, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                callback.onDateTimeSelected();

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    public void setAlarm(View view) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, receiverIntent, 0);

//        all types AlarmManager.ELAPSED_REALTIME, AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.RTC, AlarmManager.RTC_WAKEUP
        int type = AlarmManager.RTC;

        int minutes = 0;
        try {
            minutes = Integer.parseInt(etRepeatTime.getText().toString());
        } catch (NumberFormatException ignored) {
        }
        cancelAlarm(null);
        if (alarmManager != null && minutes > 0) {
            alarmManager.setRepeating(type, calendar.getTimeInMillis(), minutes * 60 * 1000, pendingIntent);
//            use AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_DAY, etc for setInexactRepeating()
//            alarmManager.setInexactRepeating(type, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        } else {
            alarmManager.set(type, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    public void cancelAlarm(View view) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, receiverIntent, PendingIntent.FLAG_NO_CREATE);

        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}