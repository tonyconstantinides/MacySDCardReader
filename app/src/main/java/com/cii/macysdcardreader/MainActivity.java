package com.cii.macysdcardreader;
import android.content.res.Configuration;
import android.os.Bundle;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageHelper;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.LayoutInflater;

import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.cii.macysdcardreader.TaskStatusCallback;

public class MainActivity extends AppCompatActivity
implements TaskStatusCallback, TabLayout.OnTabSelectedListener {
    private static final String TAG = MainActivity.class.getCanonicalName();
    private TaskStatusCallback mStatusCallback;
    private MainActivityFragment mainFragment = null;
    private ShareActionProvider mShareActionProvider;
    private TabLayout tabLayout = null;
    private  MyAdapter mAdapter;
    private ViewPager mPager;
    static final int NUM_ITEMS = 4;
    public enum TaskStatus { NOT_STARTED, STARTED, CANCELLED, INPROGRESS, FINISHED };
    private boolean mScanTaskRunning = false;
    private boolean mScanNeverActivated = true;
    private  ProgressBar mProgressBar = null;
    private TaskStatus status;

    public MyAdapter getAdapter() {
        return mAdapter;
    }


    @Override
    public void onTaskStarted() {
        Log.d(TAG, "onTaskStarted");
        status = TaskStatus.STARTED;
    }

    @Override
    public void onTaskFinished() {
        Log.d(TAG, "onTaskFisnhed");
        status = TaskStatus.FINISHED;
    }

    @Override
    public void onTaskCancelled() {
        Log.d(TAG, "onTaskCancelled");
        status = TaskStatus.CANCELLED;
    }

    @Override
    public void onTaskProgressUpdate(int progress) {
        mProgressBar.setProgress(progress);
        status = TaskStatus.INPROGRESS;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        setupActionBar();
        setupProgressBar();
        setupAdapter();
        setupPager();
        setupTabLayout();
        setupPagerEventListeners();
        status = TaskStatus.NOT_STARTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stop the scan if it going on orientation status
        if (mainFragment != null) {
            mainFragment.stopScan();
        }
    }

    public void setupAdapter() {
        mAdapter = new MyAdapter(getSupportFragmentManager(),  4);
        mAdapter.setContext(this);
    }

    public void setupPager() {
        mPager = (ViewPager)findViewById(R.id.view_pager);
        if (mPager != null) {
            mPager.setAdapter(mAdapter);
        }
    }

    public void setupTabLayout() {
        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        //Adding the tabs using addTab() method
        tabLayout.addOnTabSelectedListener(this);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(mPager);
    }

    public void setupPagerEventListeners() {
        mPager.setOffscreenPageLimit(0);

        mPager.addOnAdapterChangeListener(new ViewPager.OnAdapterChangeListener() {
            @Override
            public void onAdapterChanged(@NonNull ViewPager viewPager,
                                         @Nullable PagerAdapter oldAdapter,
                                         @Nullable PagerAdapter newAdapter) {
               Log.d(TAG, "ViewPager Adapter is changing!");
               newAdapter.notifyDataSetChanged();
            }
        });

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d(TAG, "onPageScrolled Scrolled at position: " + position);
            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected at position: " + position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d(TAG, "onPageScrollStateChanged at state: " + state);
            }
        });
    }

    public void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void setupProgressBar() {
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        if (mProgressBar == null)
            Log.e(TAG, "ProgressBar is null!");
    }

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    @Override
    public void onBackPressed() {
        if (mainFragment != null) {
            mainFragment.stopScan();
        }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "landscape");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
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

    // Handles the tabs
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Log.d(TAG, "onTabSelected" );

        if (status == TaskStatus.NOT_STARTED) {
            TabLayout.Tab scanTab = tabLayout.getTabAt(0);
            scanTab.select();
            return;
        } else  if (status  == TaskStatus.CANCELLED) {
            Toast.makeText(getBaseContext(), "Scan SD card to see results!" , Toast.LENGTH_SHORT).show();
            TabLayout.Tab scanTab = tabLayout.getTabAt(0);
            scanTab.select();
            return;

        } else  if ((status == TaskStatus.STARTED) || (status  == TaskStatus.INPROGRESS)) {
            Toast.makeText(getBaseContext(), "Wait until Scan is finished!" , Toast.LENGTH_SHORT).show();
            TabLayout.Tab scanTab = tabLayout.getTabAt(0);
            scanTab.select();
        } else if (status  == TaskStatus.FINISHED) {
           tab.select();
       }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        Log.d(TAG, "onTabUnselected" );
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        Log.d(TAG, "onTabReselected" );
    }


    public static class ScanResultsFragment extends Fragment {
        private static final String TAB_POSITION = "tab_position";
        public SwipeRefreshLayout mSwipeRefreshLayout;
        public RecyclerView       mRecyclerView;
        public String mTitle;
        public static List<Data> ScanResults =  new ArrayList<Data>(10);
        public static List<Data> AverageFileSize = new ArrayList<Data>(10);
        public static List<Data> CommonFileExtensions = new  ArrayList<Data>(10);
        public ScanResultsFragment() {
        }

        public static ScanResultsFragment  newInstance(int tabPosition) {
            ScanResultsFragment  fragment = new ScanResultsFragment();
            Bundle args = new Bundle();
            args.putInt(TAB_POSITION, tabPosition);
            fragment.setArguments(args);
            return fragment;
        }

        void refreshItems() {
            onItemsLoadComplete();
        }

        void onItemsLoadComplete() {
            // Update the adapter and notify data set changed
            mRecyclerView.getAdapter().notifyDataSetChanged();
            // Stop refresh animation
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);
            View view =  inflater.inflate(R.layout.fragment_list_view, container, false);
            mSwipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.swipeRefreshLayout);
            mRecyclerView       = (RecyclerView)view.findViewById(R.id.recyclerview);

            if (mTitle.equalsIgnoreCase("BiggestFiles")) {
                ResultsRecyclerAdapter adapter = new ResultsRecyclerAdapter(ScanResults, getContext());
                mRecyclerView.setAdapter(adapter);
            } else if (mTitle.equalsIgnoreCase("AverageFileSize")) {
                ResultsRecyclerAdapter adapter = new ResultsRecyclerAdapter(AverageFileSize, getContext());
                mRecyclerView.setAdapter(adapter);
            } else if (mTitle.equalsIgnoreCase("CommonFileExtensions")) {
                ResultsRecyclerAdapter adapter = new ResultsRecyclerAdapter(CommonFileExtensions, getContext());
                mRecyclerView.setAdapter(adapter);
            }

            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.getAdapter().notifyDataSetChanged();
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    // Refresh items
                    refreshItems();
                }
            });
            return view;
        }
    }

    public static class MyAdapter extends FragmentStatePagerAdapter {
        final int PAGE_COUNT = 4;
        int tabCount;
        private String tabTitles[] = new String[] { "ScanSDCard", "Biggest", "FileSize", "Extensions" };
        private Context mContext;
        public static List<String> BiggestfileNames = new ArrayList<>(10);
        public static List<String> BiggestfileSizes = new ArrayList<>(10);
        public static List<String> AverageFileSize = new ArrayList<>(10);
        public static List<String> CommonExtensions = new ArrayList<>(10);

        public MyAdapter(FragmentManager fm, int tabCount) {
            super(fm);
            this.tabCount = tabCount;
        }

        public void setContext(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return tabCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // Generate title based on item position
            return tabTitles[position];
        }

        @Override
        public int getItemPosition(Object item) {
           // this causes the fragement to be recreated which is what I want
           return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            //Returning the current tabs
            switch (position) {
                case 0:
                    MainActivityFragment tab0 = MainActivityFragment.newInstance(0);
                    return tab0;
                case 1:
                    ScanResultsFragment tab1 = ScanResultsFragment.newInstance(1);
                    tab1.mTitle = "BiggestFiles";
                    if (!BiggestfileNames.isEmpty() && !BiggestfileSizes.isEmpty()) {
                        for (int inc = 0; inc < BiggestfileNames.size(); inc++) {
                            ScanResultsFragment.ScanResults.add(
                                    new Data(BiggestfileNames.get(inc),
                                            BiggestfileSizes.get(inc),
                                            R.drawable.ic_insert_drive_file_black_24dp));
                        }
                    }
                    return tab1;
                case 2:
                    ScanResultsFragment tab2 = ScanResultsFragment.newInstance(2);
                    tab2.mTitle = "AverageFileSize";
                    return tab2;
                case 3:
                    ScanResultsFragment tab3 = ScanResultsFragment.newInstance(3);
                    tab3.mTitle = "CommonFileExtensions";
                    return tab3;
                default:
                    return null;
            }

        }

    }


}
