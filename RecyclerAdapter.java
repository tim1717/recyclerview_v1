package com.watermelon.doodle.RecyclerVw;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;

import com.watermelon.doodle.R;
import com.watermelon.doodle.tools.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    public static final String TAG = RecyclerAdapter.class.getSimpleName();

    private Activity mActivity;
    private AdapterView.OnItemClickListener onItemClickListener;
    private static List<Map<String, String>> listItems = new ArrayList<Map<String, String>>();

    public class ViewHolder extends RecyclerView.ViewHolder
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

        // for swipe helper
        public View getSwipeView() {
            return mImageView;
        }

        // send recycler onclick to activity listener
        @Override
        public void onClick(View v) {
            if (onItemClickListener != null)
                onItemClickListener.onItemClick(null, v, getAdapterPosition(), v.getId());
        }
    }

    public RecyclerAdapter(Activity activity) {
        this.mActivity = activity;
    }

    public RecyclerAdapter(Activity activity, AdapterView.OnItemClickListener onItemClickListener) {
        this.mActivity = activity;
        this.onItemClickListener = onItemClickListener;
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

        // incase of ItemTouchHelper.clearView bug? needed to restore alpha
        if (holder.getSwipeView().getAlpha() < 1f) {
            holder.getSwipeView().setAlpha(1f);
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
