package com.cii.macysdcardreader;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getCanonicalName();
    private ShareActionProvider mShareActionProvider;
    private int mId = 1001;
    private boolean mScanInProgress = false;
    private Thread mScanSDCardThread = null;
    private NotificationCompat.Builder mBuilder = null;
    private NotificationManager        mNotifyManager = null;
    private ArrayList<Long> mFileSizes = new ArrayList<>();
    private HashMap<Long, String> mFilesByFileSizes = new HashMap<>();
    private long mTotalFilesSizes, mAverageFileSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Stop Scan?", Snackbar.LENGTH_INDEFINITE)
                        .setAction("STOP", new View.OnClickListener() {
                         @Override
                        public void onClick(View v) {
                            Log.d(TAG, "Stop Scan clicked");
                            stopScan();
                         }
                        })
                        .show();
                showNotification();
                startScan();
            }
        });

    }

    public void showNotification()
    {
        mBuilder = new android.support.v7.app.NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentTitle("Scanning SD Card")
                        .setContentText("Working");

        Intent notifyIntent = new Intent(getApplicationContext(), ResultActivity.class);
        // Sets the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        // Puts the PendingIntent into the notification builder
        mBuilder.setContentIntent(notifyPendingIntent);
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyManager.notify(mId, mBuilder.build());
    }

    public void startScan() {
        if (mScanInProgress) {
            stopScan();
        }
        mScanInProgress = true;
        // I would use HandlerThread but no time ot set that up
        mScanSDCardThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        final String state = Environment.getExternalStorageState();
                        File[] files = null;
                        if ( Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) ) {  // we can read the External Storage...
                            // Sets an activity indicator for an operation of indeterminate length
                            mBuilder.setProgress(0, 0, true);
                            mNotifyManager.notify(mId, mBuilder.build());
                            // this works on my HTC M8
                            String root_sd = Environment.getExternalStorageDirectory().toString() + "/";
                            File file = new File( root_sd ) ;
                            File list[] = file.listFiles();
                            if (list == null) {
                                Log.e(TAG, "Either phone does not have a SDF card or cannot access it");
                                return;
                            }
                            // walk through array and sort it File size
                            for (File item: list) {
                                if (!item.isDirectory()) {
                                    long fileSize = getFileSize(item);
                                    mTotalFilesSizes += fileSize;
                                    String fileName = item.getName();
                                    String fileType = getFileType(item.getAbsolutePath());
                                    Log.d(TAG, "File Name of file is: " + fileName);
                                    Log.d(TAG, "File Type of file is: " + fileType);
                                    Log.d(TAG, "File Size of file is:"  + fileSize);
                                    if (fileSize > 0) {
                                        mFileSizes.add(fileSize);
                                        // store all files names with file sizes as keys
                                        mFilesByFileSizes.put( fileSize, fileName);
                                    }
                                }
                            }
                            // sort the file sizes
                            Collections.sort(mFileSizes);
                            // now print out the ten largest file sizes and names
                            int sortedArryaSize = mFileSizes.size();
                            if (sortedArryaSize > 10) {
                              for (int inc = (sortedArryaSize - 1); inc >= (sortedArryaSize - 11); inc--){
                                  long keyFileSize = mFileSizes.get(inc);
                                  String myFileName = mFilesByFileSizes.get(keyFileSize);
                                  Log.d(TAG, "Biggest FileName and Size is: "
                                          + myFileName  + "," + String.valueOf(keyFileSize));
                              }
                            }
                            mAverageFileSize = (mTotalFilesSizes / list.length);
                            Log.d(TAG, "Average File size is :" + String.valueOf(mAverageFileSize));
                            mBuilder.setContentText("Scan complete")
                                    // Removes the progress bar
                                    .setProgress(0,0,false);
                            mNotifyManager.notify(mId, mBuilder.build());
                            Log.d(TAG,  "File Scan complete");
                        }
                    }
                });
        mScanSDCardThread.start();
    }

    public void stopScan()  {
        if (!mScanInProgress) return;
        mScanInProgress = false;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mId);
        if (mScanSDCardThread != null) {
            if (mScanSDCardThread.isAlive()) {
                mScanSDCardThread.interrupt();
                mScanSDCardThread = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        stopScan();
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG,  "landscape");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.d(TAG, "portrait");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);
        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        // Return true to display menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static long getFileSize(final File file)
    {
        if(file == null || !file.exists())
            return 0;
        if(!file.isDirectory())
            return file.length();
        final List<File> dirs = new LinkedList<File>();
        dirs.add(file);
        long result=0;
        while(!dirs.isEmpty())
        {
            final File dir = dirs.remove(0);
            if(!dir.exists())
                continue;
            final File[] listFiles = dir.listFiles();
            if(listFiles == null || listFiles.length == 0)
                continue;
            for(final File child : listFiles)
            {
                result += child.length();
                if(child.isDirectory())
                    dirs.add(child);
            }
        }
        return result;
    }

    public static String getFileType(String url)
    {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }
}