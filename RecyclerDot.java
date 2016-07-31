package com.watermelon.doodle.RecyclerVw;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.watermelon.doodle.Log;
import com.watermelon.doodle.R;
import com.watermelon.doodle.tools.Tool;

import java.util.List;
import java.util.Map;

public class RecyclerDot extends AppCompatActivity {
    public static final String TAG = RecyclerDot.class.getSimpleName();

    private DisplayMetrics metrics;
    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SwipeRefreshLayout.OnRefreshListener mRefreshListener;
    private RecyclerTouchHelper myRecyclerTouchHelper;
    private ItemTouchHelper itemTouchHelper;
    private AdapterView.OnItemClickListener onItemClickListener;

    private List<Map<String, String>> listItems;
    private boolean asListView = true;
    private Bitmap leftIcon, rightIcon;

    public static final int ACTION1 = 1;
    public static final int ACTION2 = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.activity_recycler);
        overridePendingTransition(R.anim.push_left_in, R.anim.zoom_exit);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        initList();

        if (listItems == null || listItems.isEmpty()) {
            loadData(true, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        if (actionHandler != null) {
            actionHandler.removeCallbacksAndMessages(null);
            actionHandler = null;
        }
        super.onDestroy();
    }

    /**
     * <pre>
     * handle actions from outside
     * includes: recycler swipe helper
     * </pre>
     */
    private Handler actionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what)
            {
                case ACTION1:
                    int value1 = (int) msg.obj;
                    mAdapter.removeListItem(value1);
                    break;
                case ACTION2:
                    int value2 = (int) msg.obj;
                    String item = mAdapter.getItem(value2);
                    Tool.toast(RecyclerDot.this, item + "-right");
                    mAdapter.notifyItemChanged(value2);
                    break;
                default:
                    break;
            }
        }
    };

    private void initViews() {
        metrics = getResources().getDisplayMetrics();

        ActionBar actionBar = getSupportActionBar();
        try {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            Log.e(TAG, "!@#$ no action bar support");
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.test, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.test1) {
            // recycler switch layout
            Tool.toast(RecyclerDot.this, "switch layout");
            if (asListView) {
                mLayoutManager = new GridLayoutManager(RecyclerDot.this, 2);
                myRecyclerTouchHelper.setSwipeDrawBgView(2);
            } else {
                mLayoutManager = new LinearLayoutManager(RecyclerDot.this);
                myRecyclerTouchHelper.setSwipeDrawBgView(1);
            }
            mRecyclerView.setLayoutManager(mLayoutManager);
            asListView = !asListView;
            return true;
        } else if (id == R.id.test2) {
            Tool.toast(RecyclerDot.this, "nada");
            return true;
        } else if (id == R.id.test3) {
            Tool.toast(RecyclerDot.this, "nada");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initList() {
        // recycler layout manager
        if (mLayoutManager == null) {
            mLayoutManager = new LinearLayoutManager(RecyclerDot.this);
            mRecyclerView.setLayoutManager(mLayoutManager);
            asListView = true;
        }

        // recycler click listener
        if (onItemClickListener == null)
            onItemClickListener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String item1 = position + "." + mAdapter.getItem(position);

                    if (id == R.id.cell_icon) {
                        item1 = item1 + "-img";
                    } else if (id == R.id.cell_text) {
                        item1 = item1 + "-txt";
                    }

                    Log.d(TAG, item1);
                    Tool.toast(RecyclerDot.this, item1);
                }
            };

        // recycler adapter with listener
        if (mAdapter == null) {
            mAdapter = new RecyclerAdapter(RecyclerDot.this, onItemClickListener);
            mRecyclerView.setAdapter(mAdapter);
        }

        // recycler pulldown refresh
        if (mRefreshListener == null) {
            mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    loadData(false, false);
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            };
            mSwipeRefreshLayout.setOnRefreshListener(mRefreshListener);
        }

        // recycler swipe helper
        if (myRecyclerTouchHelper == null) {
            myRecyclerTouchHelper = new RecyclerTouchHelper(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                    metrics, actionHandler, ACTION1, ACTION2);

            leftIcon = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_info_details);
            rightIcon = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_delete);
            myRecyclerTouchHelper.setSwipeDrawBgView(null, leftIcon, null, rightIcon);
            myRecyclerTouchHelper.setSwipeDrawBgView(1);
        }

        // attach swipe help to recycler
        if (itemTouchHelper == null) {
            itemTouchHelper = new ItemTouchHelper(myRecyclerTouchHelper);
            itemTouchHelper.attachToRecyclerView(mRecyclerView);
        }
    }

    /**
     * loading sample data
     */
    private void loadData(boolean defValues, boolean additonalData) {
        if (myRecyclerTouchHelper != null)
            myRecyclerTouchHelper.disableSwipePos(0, false);

        if (defValues) {
            listItems = Tool.getSampleData(15);
        } else {
            listItems = Tool.getSampleData(Tool.randomInt(3, 10), Tool.randomInt(13, 31));
        }

        if (mAdapter != null) {
            if (additonalData) {
                mAdapter.loadMoreListItems(listItems);
            } else {
                mAdapter.setListItems(listItems);
            }
        }
    }
}
