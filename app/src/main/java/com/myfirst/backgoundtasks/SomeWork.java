package com.myfirst.backgoundtasks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SomeWork extends Worker {

    private final static String TAG = SomeWork.class.getSimpleName();

    public SomeWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setProgress("start");
            Thread.sleep(2000);
            setProgress("after 2000 millis");
            int value = getInputData().getInt(MainActivity.SOME_VALUE_KEY, -1);
            if (value != -1) {
                if (value == 0) {
                    setProgress("Result.retry()");
                    return Result.retry();
                } else if (value % 2 == 0) {
                    setProgress("Result.success()");
                    return Result.success();
                } else {
                    setProgress("Result.failure()");
                    return Result.failure();
                }
            } else {
                setProgress("Result.success()");
                return Result.success();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            setProgress("Result.failure(" + e.getMessage() + ")");
            return Result.failure();
        }
    }

    private void setProgress(String progress) {
        Log.d(TAG, progress);
        try {
            Data data = new Data.Builder()
                    .putString(MainActivity.PROGRESS_VALUE_KEY, progress)
                    .build();
            setProgressAsync(data);

            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
