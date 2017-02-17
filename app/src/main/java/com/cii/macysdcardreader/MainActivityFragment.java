package com.cii.macysdcardreader;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment
    implements TaskStatusCallback, View.OnClickListener{
    private static final String TAG = MainActivityFragment.class.getCanonicalName();
    private ProgressDialog progressDialog;
    private TaskStatusCallback mStatusCallback;
    private ScanSDCardTask mScanSDCardThread = null;
    private Button mScanButton = null;
    private int mId = 1001;
    private boolean mScanInProgress = false;
    private NotificationCompat.Builder mBuilder = null;
    private NotificationManager mNotifyManager = null;
    private ArrayList<Long> mFileSizes = new ArrayList<>();
    private HashMap<Long, String> mFilesByFileSizes = new HashMap<>();
    private long mTotalFilesSizes, mAverageFileSize;
    public List<String> BiggestfileNames = new ArrayList<>(10);
    public List<String> AverageFileSize = new ArrayList<>(10);
    public List<String> CommonExtensions = new ArrayList<>(10);

    public MainActivityFragment() {
    }

    public static  MainActivityFragment newInstance(int imageResourceId) {
        MainActivityFragment scanSDCardFragment = new MainActivityFragment();
        // Get arguments passed in, if any
        Bundle args = scanSDCardFragment.getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putInt("imageResourceId", imageResourceId);
        scanSDCardFragment.setArguments(args);
        return scanSDCardFragment;
    }

    public ProgressBar getProgressBar() {
        if (((MainActivity)getActivity()) == null)
            return null;
        else
            return ((MainActivity)getActivity()).getProgressBar();
    }

    @Override
    public void onTaskStarted() {
        Log.d(TAG, "onTaskStarted");
        // notify the activity
        if (mStatusCallback != null) {
            mStatusCallback.onTaskStarted();
        }
        mScanInProgress = true;
        progressDialog = ProgressDialog.show(getActivity(), "Loading", "Please wait a moment!");
    }

    @Override
    public void onTaskFinished() {
        Log.d(TAG, "onTaskFinished");
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        mScanInProgress = false;
        if (mStatusCallback != null) {
            mStatusCallback.onTaskFinished();
        }
    }

    @Override
    public void onTaskCancelled() {
        Log.d(TAG, "onTaskCancelled");
        if (mStatusCallback != null) {
            mStatusCallback.onTaskCancelled();
        }
    }

    @Override
    public void onTaskProgressUpdate(int progress) {
        Log.d(TAG, "onTaskProgressUpdate");
        if (mStatusCallback != null) {
            mStatusCallback.onTaskProgressUpdate(progress);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mStatusCallback  = (TaskStatusCallback)getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // If we are returning here from a screen orientation
        // and the AsyncTask is still working, re-create and display the
        // progress dialog.
        if (mScanInProgress) {
            progressDialog = ProgressDialog.show(getActivity(), "Loading", "Please wait a moment!");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_main, container, false);
          if (rootView != null) {
            setupScanButton(rootView);
        }
        rootView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (mScanInProgress) {
            stopScan();
            mScanButton.setText("Start SD Scan");
        } else {
            showNotification();
            startScan();
            mScanButton.setText("Stop SD Scan");
        }
    }

    @Override
    public void onDetach() {
        // All dialogs should be closed before leaving the activity in order to avoid
        // the: Activity has leaked window com.android.internal.policy... exception
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        // kill the callback associated with teh Activity
        mStatusCallback  = null;
        super.onDetach();
    }



    public void setupScanButton(View rootView) {
        mScanButton = (Button) rootView.findViewById(R.id.scan_sd_button);
        if (mScanButton == null) {
            Log.e(TAG, "ScanButton is null!");
        } else {
            mScanButton.setText("Start SD Scan");
            mScanButton.setOnClickListener(this);
        }
    }

    public void showNotification() {
        mBuilder = new android.support.v7.app.NotificationCompat.Builder(
                getContext())
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle("Scanning SD Card")
                .setContentText("Working");

        Intent notifyIntent = new Intent(getContext(), ResultActivity.class);
        // Sets the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        getContext(),
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        // Puts the PendingIntent into the notification builder
        mBuilder.setContentIntent(notifyPendingIntent);
        mBuilder.setProgress(100, 0, false);
        mNotifyManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyManager.notify(mId, mBuilder.build());
    }

    public void startScan() {
        mScanSDCardThread = new ScanSDCardTask();
        mScanSDCardThread.execute();
        mScanInProgress = true;
        if (mStatusCallback != null) {
            mStatusCallback.onTaskStarted();
        }
    }

    public void stopScan() {
        if (!mScanInProgress) return;
        NotificationManager notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mId);
        if (mScanSDCardThread != null) {
            mScanSDCardThread.cancel(true);
            mScanSDCardThread = null;
        }
        mScanInProgress = false;
        if (mStatusCallback != null) {
            mStatusCallback.onTaskCancelled();
        }
    }

    public void refreshAdapters() {
        if ( ((MainActivity)getActivity()) == null)
            return;
        else
            ((MainActivity)getActivity()).getAdapter().notifyDataSetChanged();
    }

    public List<String> getAverageFileSizeList() {
        if ( ((MainActivity)getActivity()) == null)
            return Collections.EMPTY_LIST;
        else
            return ((MainActivity)getActivity()).getAdapter().AverageFileSize;
    }

    public List<String>  getBiggestfileNamesList() {
        if ( ((MainActivity)getActivity()) == null)
            return Collections.EMPTY_LIST;
        else
             return ((MainActivity)getActivity()).getAdapter().BiggestfileNames;
    }

    public List<String>  getBiggestfileSizeList() {
        if ( ((MainActivity)getActivity()) == null)
            return Collections.EMPTY_LIST;
        else
             return ((MainActivity)getActivity()).getAdapter().BiggestfileSizes;
    }

    public List<String>  getAvergaeFileSizeList() {
        if ( ((MainActivity)getActivity()) == null)
            return Collections.EMPTY_LIST;
        else
            return  ((MainActivity)getActivity()).getAdapter().AverageFileSize;
    }

    public List<String>  getCommonExtensions() {
       if ( ((MainActivity)getActivity()) == null)
           return Collections.EMPTY_LIST;
       else
           return ((MainActivity)getActivity()).getAdapter().CommonExtensions;
    }

    public void clearAdapterLists() {
        if ( ((MainActivity)getActivity()) == null)
            return;
        else {
            ((MainActivity) getActivity()).getAdapter().BiggestfileNames.clear();
            ((MainActivity) getActivity()).getAdapter().BiggestfileSizes.clear();
            ((MainActivity) getActivity()).getAdapter().AverageFileSize.clear();
            ((MainActivity) getActivity()).getAdapter().CommonExtensions.clear();
        }
    }

    // Private class to run the scan task
    private  class ScanSDCardTask extends AsyncTask<Void, Integer, Void> {
        private  final String TAG = ScanSDCardTask.class.getCanonicalName();


        public ScanSDCardTask() {
        }

        @Override
        protected void onPreExecute() {
            Log.d( "ScanSDTask", "onPreExecute");
            mScanButton.setText("Stop SD Scan");
            showProgressBar();
            clearAdapterLists();
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d( "ScanSDTask", "onPostExecute");
            if (mStatusCallback != null)
                mStatusCallback.onTaskFinished();
            hideProgressBar();
            mScanButton.setText("Start SD Scan");
            refreshAdapters();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d( "ScanSDTask", "onProgressUpdate");
            if (mStatusCallback != null)
                mStatusCallback.onTaskProgressUpdate(values[0]);
        }

        @Override
        protected void onCancelled(Void result) {
            if (mStatusCallback != null)
                mStatusCallback.onTaskCancelled();
            hideProgressBar();
            clearAdapterLists();
        }

        @Override
        protected Void doInBackground(Void... params) {
            int progress = 0;
            while (progress < 100 && !isCancelled()) {
                progress++;
                final int progressUpdate = progress;
                // Update the progress bar
                updateProgressBar(progressUpdate);
                Log.d(TAG, "Progress Number: " + progress);

                final String state = Environment.getExternalStorageState();
                File[] files = null;
                if (Environment.MEDIA_MOUNTED.equals(state) ||
                        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {  // we can read the External Storage...
                    mBuilder.setProgress(100, progress, false);
                    mNotifyManager.notify(mId, mBuilder.build());
                    String root_sd = Environment.getExternalStorageDirectory().toString() + "/";
                    File file = new File(root_sd);
                    File list[] = file.listFiles();
                    if (list == null) {
                        Log.e(TAG, "Either phone does not have a SD card or cannot access it");
                        cancel(true);
                        progress = 100;
                        Toast.makeText(getContext(), "This Phone has no external SD Card!", Toast.LENGTH_LONG).show();
                        return null;
                    }
                    // walk through array and sort it File size
                    for (File item : list) {
                        if (!item.isDirectory()) {
                            long fileSize = getFileSize(item);
                            mTotalFilesSizes += fileSize;
                            String fileName = item.getName();
                            String fileType = getFileType(item.getAbsolutePath());
                            if (getCommonExtensions() != null)
                              if (!getCommonExtensions().contains(fileType))
                                if (getCommonExtensions() != Collections.EMPTY_LIST)
                                      getCommonExtensions().add(fileType);
                            Log.d(TAG, "File Name of file is: " + fileName);
                            Log.d(TAG, "File Type of file is: " + fileType);
                            Log.d(TAG, "File Size of file is:" + fileSize);
                            if (fileSize > 0) {
                                mFileSizes.add(fileSize);
                                // store all files names with file sizes as keys
                                mFilesByFileSizes.put(fileSize, fileName);
                            }
                        }
                    }

                    // sort the file sizes
                    if (mFileSizes.isEmpty()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "No files found just directories", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    Collections.sort(mFileSizes);
                    // now print out the ten largest file sizes and names
                        int sortedArryaSize = mFileSizes.size();
                        if (sortedArryaSize > 10) {
                            for (int inc = (sortedArryaSize - 1); inc >= (sortedArryaSize - 11); inc--) {
                                long keyFileSize = mFileSizes.get(inc);
                                String myFileName = mFilesByFileSizes.get(keyFileSize);
                                Log.d(TAG, "Biggest FileName and Size is: "
                                        + myFileName + "," + String.valueOf(keyFileSize));
                                if (getBiggestfileNamesList() != null) {
                                    if (!getBiggestfileNamesList().contains(myFileName)) {
                                        if (getCommonExtensions() != Collections.EMPTY_LIST) {
                                            getBiggestfileNamesList().add(myFileName);
                                            getBiggestfileSizeList().add(String.valueOf(keyFileSize) + " bytes");
                                        }
                                    }
                                }
                            }
                        }

                    mAverageFileSize = (mTotalFilesSizes / list.length);
                    Log.d(TAG, "Average File size is :" + String.valueOf(mAverageFileSize));
                    if ( getAverageFileSizeList() != Collections.EMPTY_LIST) {
                        getAverageFileSizeList().add(String.valueOf(mAverageFileSize));
                    }
                    mBuilder.setContentText("Scan complete");
                }
                mNotifyManager.notify(mId, mBuilder.build());
            }
            // Removes the progress bar
            mBuilder.setProgress(0, 0, false);
            Log.d(TAG, "File Scan complete");
            return null;
        }
    }

    public void showProgressBar() {
        if (getProgressBar() != null) {
            getProgressBar().setVisibility(View.VISIBLE);
        }
    }

    public void updateProgressBar(final int progressUpdate) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getProgressBar().setProgress(progressUpdate);
                }
            });
        }
    }

    public void hideProgressBar() {
        if (getProgressBar() != null) {
            getProgressBar().setVisibility(View.INVISIBLE);
        }
    }

    public static long getFileSize(final File file) {
        if (file == null || !file.exists())
            return 0;
        if (!file.isDirectory())
            return file.length();
        final List<File> dirs = new LinkedList<File>();
        dirs.add(file);
        long result = 0;
        while (!dirs.isEmpty()) {
            final File dir = dirs.remove(0);
            if (!dir.exists())
                continue;
            final File[] listFiles = dir.listFiles();
            if (listFiles == null || listFiles.length == 0)
                continue;
            for (final File child : listFiles) {
                result += child.length();
                if (child.isDirectory())
                    dirs.add(child);
            }
        }
        return result;
    }

    public static String getFileType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

}
