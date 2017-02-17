package com.cii.macysdcardreader;


public  interface TaskStatusCallback {
    void onTaskStarted();
    void onTaskProgressUpdate(int progress);
    void onTaskFinished();
    void onTaskCancelled();
}
