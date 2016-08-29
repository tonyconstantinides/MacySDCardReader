package com.cii.macysdcardreader;
import android.os.HandlerThread;
import android.os.Handler;

public class ScanSDCardThread extends HandlerThread {
    private Handler mWorkerHandler;


    public ScanSDCardThread(String name) {
        super(name);
    }

    @Override
    protected void onLooperPrepared() {
        mWorkerHandler = new Handler(getLooper());
    }

    public void postTask(Runnable task){
        mWorkerHandler.post(task);
    }
}
