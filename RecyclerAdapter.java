package com.watermelon.doodle.RecyclerVw;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.watermelon.doodle.Log;
import com.watermelon.doodle.R;
import com.watermelon.doodle.core.PicassoOps;
import com.watermelon.doodle.tools.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    public static final String TAG = RecyclerAdapter.class.getSimpleName();

    private Activity mActivity;
    private float density;
    private AdapterView.OnItemClickListener onItemClickListener;
    private static List<Map<String, String>> listItems = new ArrayList<Map<String, String>>();

    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public LinearLayout cTopView;
        public ImageView imageOptLeft, imageOptRight;
        public ImageView imageIcon;
        public TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            cTopView = (LinearLayout) itemView.findViewById(R.id.cell_content);
            imageOptLeft = (ImageView) itemView.findViewById(R.id.left_option);
            imageOptRight = (ImageView) itemView.findViewById(R.id.right_option);
            imageIcon = (ImageView) itemView.findViewById(R.id.cell_icon);
            textView = (TextView) itemView.findViewById(R.id.cell_text);

            // === RECYCLER CLICK ===
            imageOptLeft.setOnClickListener(this);
            imageOptRight.setOnClickListener(this);
            imageIcon.setOnClickListener(this);
            textView.setOnClickListener(this);
        }

        // for SWIPE HELPER
        public View getTopView() {
            return cTopView;
        }

        // for SWIPE HELPER
        public View getLeftOpt() {
            return imageOptLeft;
        }

        // for SWIPE HELPER
        public View getRightOpt() {
            return imageOptRight;
        }

        // for SWIPE HELPER
        public View getImage() {
            return imageIcon;
        }

        // === RECYCLER CLICK ===
        @Override
        public void onClick(View v) {
            if (onItemClickListener != null)
                onItemClickListener.onItemClick(null, v, getAdapterPosition(), v.getId());
        }
    }

    public RecyclerAdapter(Activity activity) {
        this.mActivity = activity;
        DisplayMetrics metrics = this.mActivity.getResources().getDisplayMetrics();
        density = metrics.density;
    }

    public RecyclerAdapter(Activity activity, AdapterView.OnItemClickListener onItemClickListener) {
        this.mActivity = activity;
        this.onItemClickListener = onItemClickListener;
        DisplayMetrics metrics = this.mActivity.getResources().getDisplayMetrics();
        density = metrics.density;
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
            Log.w(TAG, "getMap null " + position);
            return;
        }

        Log.d(TAG, "bindView " + position + "," + textItem);

        // === STYLE_LOCK_L | STYLE_LOCK_R | STYLE_LOCK ===
        fixCellTranslationX(holder, position);

        ImageView imageView = holder.imageIcon;
        ImageView imageViewLeft = holder.imageOptLeft;
        ImageView imageViewRight = holder.imageOptRight;
        TextView textView1 = holder.textView;

        PicassoOps.PicassoImage(RecyclerDot.IMAGE_SRC + textItem, imageView,
                (int) (70 * density), (int) (70 * density), null, TAG);
        PicassoOps.PicassoImage(RecyclerDot.IMAGE_SRC + textItem + RecyclerDot.IMAGE_SRC_2, imageViewLeft,
                (int) (70 * density), (int) (70 * density), null, TAG);
        PicassoOps.PicassoImage(RecyclerDot.IMAGE_SRC + textItem + RecyclerDot.IMAGE_SRC_3, imageViewRight,
                (int) (70 * density), (int) (70 * density), null, TAG);
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

    // === STYLE_LOCK_L | STYLE_LOCK_R | STYLE_LOCK ===
    private void fixCellTranslationX(RecyclerAdapter.ViewHolder holder, int position) {
        Object tagL = holder.getLeftOpt().getTag();
        Object tagR = holder.getRightOpt().getTag();
        if (tagL != null || tagR != null) {
            Log.w(TAG, "cell FIXING " + position);
            holder.getLeftOpt().setTag(null);
            holder.getRightOpt().setTag(null);
            holder.getTopView().setTranslationX(0);
        }
    }

}
