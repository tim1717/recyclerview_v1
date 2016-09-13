package com.watermelon.doodle.RecyclerVw;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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

    public static final String IMAGE_SRC = "https://robohash.org/";
    public static final String IMAGE_SRC_2 = "?set=set2";
    public static final String IMAGE_SRC_3 = "?set=set3";
    public static final String TEXT_SRC = "http://jsonplaceholder.typicode.com/todos";

    private DisplayMetrics metrics;
    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private RecyclerDecoration mRecyclerDecoration;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SwipeRefreshLayout.OnRefreshListener mRefreshListener;
    private RecyclerTouchHelper myRecyclerTouchHelper;
    private ItemTouchHelper itemTouchHelper;
    private AdapterView.OnItemClickListener onItemClickListener;

    private List<Map<String, String>> listItems;
    private Drawable divider;
    private Bitmap leftIcon, rightIcon;

    public static final int LEFTACTION = 1;
    public static final int RIGHTACTION = 2;

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

        loadData(true, false);
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
     * handle actions
     * <p>includes: recycler swipe action
     */
    private Handler actionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                // === SWIPE_ACTION ===
                case LEFTACTION: {
                    int value1 = (int) msg.obj;
                    String item1 = mAdapter.getItem(value1);
                    mAdapter.removeListItem(value1);
                    // reset state of new cell occupant
                    if (value1 < mAdapter.getItemCount()) {
                        mAdapter.notifyItemChanged(value1);
                    }
                    Tool.toast(RecyclerDot.this, "del! [" + value1 + "] " + item1);
                    break;
                }
                // === SWIPE_ACTION ===
                case RIGHTACTION: {
                    int value2 = (int) msg.obj;
                    String item2 = mAdapter.getItem(value2);
                    mAdapter.notifyItemChanged(value2);
                    Tool.toast(RecyclerDot.this, "right! [" + value2 + "] " + item2);
                    break;
                }
                default:
                    break;
            }
        }
    };

    private void initViews() {
        Log.v(TAG, "initViews");
        metrics = getResources().getDisplayMetrics();

        ActionBar actionBar = getSupportActionBar();
        try {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            Log.e(TAG, "!@#$ no action bar support");
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // === DIVIDERS ===
        if (mRecyclerDecoration == null) {
            divider = ContextCompat.getDrawable(RecyclerDot.this, R.drawable.divider_hrz_blue);
            mRecyclerDecoration = new RecyclerDecoration(divider);
            mRecyclerView.addItemDecoration(mRecyclerDecoration);
        }

        // recycler pulldown refresh
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
            Tool.toast(RecyclerDot.this, "nada");
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
        Log.v(TAG, "initList");
        // recycler layout manager
        if (mLayoutManager == null) {
            mLayoutManager = new LinearLayoutManager(RecyclerDot.this);
            mRecyclerView.setLayoutManager(mLayoutManager);
        }

        // === RECYCLER CLICK ===
        if (onItemClickListener == null) {
            onItemClickListener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String item1 = "[" + position + "] " + mAdapter.getItem(position);

                    if (id == R.id.cell_icon) {
                        item1 = "ic " + item1;
                    } else if (id == R.id.cell_text) {
                        item1 = "txt " + item1;
                    } else if (id == R.id.left_option) {
                        item1 = "right_ " + item1;
                        mAdapter.notifyItemChanged(position);
                    } else if (id == R.id.right_option) {
                        item1 = "del_ " + item1;
                        mAdapter.removeListItem(position);
                        // === STYLE_LOCK_L | STYLE_LOCK_R | STYLE_LOCK ===
                        // reset state of new cell occupant
                        if (position < mAdapter.getItemCount()) {
                            mAdapter.notifyItemChanged(position);
                        }
                    }

                    Log.d(TAG, item1);
                    Tool.toast(RecyclerDot.this, item1);
                }
            };
        }

        // recycler adapter with click listener
        if (mAdapter == null) {
            mAdapter = new RecyclerAdapter(RecyclerDot.this, onItemClickListener);
            mRecyclerView.setHasFixedSize(false);
            mRecyclerView.setAdapter(mAdapter);
        }

        // recycler pulldown refresh
        if (mRefreshListener == null) {
            mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    Tool.toast(RecyclerDot.this, "refresh with new sample");
                    loadData(false, false);
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            };
            mSwipeRefreshLayout.setOnRefreshListener(mRefreshListener);
        }

        // recycler SWIPE HELPER
        if (myRecyclerTouchHelper == null) {
            myRecyclerTouchHelper = new RecyclerTouchHelper(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                    metrics, actionHandler, LEFTACTION, RIGHTACTION);

            // === STYLE_LOCK_L | STYLE_LOCK_R | STYLE_LOCK ===
            // optional, will default to behind-L/R-image's width positions
            myRecyclerTouchHelper.setCustomLeftThreshold((int) (70 * metrics.density));
            myRecyclerTouchHelper.setCustomRightThreshold((int) (70 * metrics.density));

            // === STYLE_DRAW_HALF | STYLE_DRAW ===
            leftIcon = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_info_details);
            rightIcon = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_delete);
            myRecyclerTouchHelper.setSwipeDrawBgView(null, leftIcon, null, rightIcon);

            // === STYLE_DISABLED ===
            myRecyclerTouchHelper.disableSwipePos(0, false);
        }

        // attach swipe helper to recycler
        if (itemTouchHelper == null) {
            itemTouchHelper = new ItemTouchHelper(myRecyclerTouchHelper);
            itemTouchHelper.attachToRecyclerView(mRecyclerView);
        }
    }

    /**
     * loading sample data
     */
    private void loadData(boolean defValues, boolean additonalData) {
        if (listItems != null && listItems.isEmpty()) return;
        Log.v(TAG, "loadData " + defValues + additonalData);

        if (defValues) {
            listItems = Tool.getSampleData(20);
        } else {
            listItems = Tool.getSampleData(Tool.randomInt(8, 20), Tool.randomInt(13, 31));
        }

        if (mAdapter != null) {
            if (additonalData) {
                mAdapter.loadMoreListItems(listItems);
            } else {
                mAdapter.setListItems(listItems);
            }
        } else {
            initList();
            loadData(defValues, additonalData);
        }
    }
}
