package com.watermelon.doodle.RecyclerVw;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerDecoration extends RecyclerView.ItemDecoration {
    public static final String TAG = RecyclerDecoration.class.getSimpleName();

    private Drawable mDivider;

    public RecyclerDecoration(Drawable divider) {
        mDivider = divider;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        // ignore first cell
        if (parent.getChildAdapterPosition(view) == 0) return;

        // cell's top spacing from divider's height
        outRect.top = mDivider.getIntrinsicHeight();
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        // for all visible cells
        int childCount = parent.getChildCount();

        // draw dividers
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            // draw divider at cell's bottom, over the next cell's top spacing
            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();

            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

}
