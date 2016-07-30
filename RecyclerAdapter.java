package com.watermelon.doodle.RecyclerVw;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.watermelon.doodle.Log;
import com.watermelon.doodle.R;
import com.watermelon.doodle.tools.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by TML13 on 7/28/2016.
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    public static final String TAG = RecyclerAdapter.class.getSimpleName();

    private static Activity mActivity;
    private static List<Map<String, String>> listItems = new ArrayList<Map<String, String>>();

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public ImageView mImageView;
        public AppCompatTextView mTextView1;

        public ViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.cell_icon);
            mTextView1 = (AppCompatTextView) itemView.findViewById(R.id.cell_text);

            mImageView.setOnClickListener(this);
            mTextView1.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            String item1 = getMap(getAdapterPosition()).get(Tool.keyITEM1);
            if (v == mImageView) {
                item1 = item1 + "-img";
            } else if (v == mTextView1) {
                item1 = item1 + "-txt";
            }

            Log.d(TAG, item1);
            Tool.toast(mActivity, item1);
        }
    }

    public RecyclerAdapter(Activity activity) {
        this.mActivity = activity;
    }

    public void setListItems(List<Map<String, String>> listItems) {
        this.listItems.clear();
        this.listItems = listItems;
        notifyDataSetChanged();
    }

    public RecyclerAdapter(Activity activity, List<Map<String, String>> listItems) {
        this.mActivity = activity;
        this.listItems = listItems;
    }

    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View contactView = inflater.inflate(R.layout.cell_default, parent, false);
        ViewHolder viewHolder = new ViewHolder(contactView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerAdapter.ViewHolder holder, int position) {
        Map<String, String> map = null;
        String textItem = "";

        try {
            map = getMap(position);
            textItem = map.get(Tool.keyITEM1);
        } catch (Exception e) {
        }

        AppCompatTextView textView1 = holder.mTextView1;

        textView1.setText(textItem);
    }

    @Override
    public int getItemCount() {
        return (this.listItems != null) ? this.listItems.size() : 0;
    }

    public String getItem(int position) {
        String item1 = getMap(position).get(Tool.keyITEM1);
        return item1;
    }

    private static Map<String, String> getMap(int position) {
        return listItems.get(position);
    }

    public void removeListItem(int position) {
        this.listItems.remove(position);
        notifyItemRemoved(position);
    }

    public void loadMoreListItems(List<Map<String, String>> newlistItems) {
        this.listItems.addAll(newlistItems);
        notifyDataSetChanged();
    }
}
