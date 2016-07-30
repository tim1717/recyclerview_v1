package com.watermelon.doodle.RecyclerVw;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RecyclerTouchHelper extends ItemTouchHelper.SimpleCallback {
    public static final String TAG = RecyclerTouchHelper.class.getSimpleName();

    private Handler action;
    private int actionId1, actionId2;

    private List<Integer> disabledSwipePos = new ArrayList<Integer>();

    private Paint p = new Paint();
    private int leftColor, rightColor;
    private static final int defLeftColor = Color.parseColor("#00FF00");
    private static final int defRightColor = Color.parseColor("#FF0000");
    private Bitmap leftIcon, rightIcon;
    private float padding = 0;
    private float cols = 1;

    public RecyclerTouchHelper(int dragDirs, int swipeDirs, Handler result, int actionId1, int actionId2) {
        super(dragDirs, swipeDirs);
        this.action = result;
        this.actionId1 = actionId1;
        this.actionId2 = actionId2;
    }

    public void disableSwipePos(int cellPos, boolean addOrNew) {
        if (addOrNew) {
            if (!disabledSwipePos.contains(cellPos))
                disabledSwipePos.add(cellPos);
        } else {
            disabledSwipePos.clear();
            disabledSwipePos.add(cellPos);
        }
    }

    public void disableSwipePos(List<Integer> cellPos, boolean addOrNew) {
        cellPos = new LinkedList<Integer>(cellPos);
        if (addOrNew) {
            Set<Integer> asSet = new LinkedHashSet<>(disabledSwipePos);
            asSet.addAll(cellPos);
            disabledSwipePos = new ArrayList<Integer>(asSet);
        } else {
            disabledSwipePos.clear();
            disabledSwipePos = cellPos;
        }
    }

    public void removeDisabledSwipePos(int cellPos) {
        int pos = disabledSwipePos.indexOf(cellPos);
        if (pos >= 0)
            disabledSwipePos.remove(pos);
    }

    public boolean isSwipePosDisabled(int cellPos) {
        if (disabledSwipePos.contains(cellPos))
            return true;
        else
            return false;
    }

    public List<Integer> getDisabledSwipePos() {
        return disabledSwipePos;
    }

    // can override drag with return 0
    @Override
    public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return super.getDragDirs(recyclerView, viewHolder);
    }

    // can override swipe with return 0
    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int pos = viewHolder.getAdapterPosition();

        if (action == null)
            return 0;
        if (isSwipePosDisabled(pos))
            return 0;

        return super.getSwipeDirs(recyclerView, viewHolder);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();

        if (action != null) {
            Message message = new Message();
            if (direction == ItemTouchHelper.LEFT) {
                message.what = actionId1;
                message.obj = pos;
            } else {
                message.what = actionId2;
                message.obj = pos;
            }

            action.sendMessage(message);
        }
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState,
                            boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;

            swipePhaseView(itemView, dX);

            if (dX > 0) {
                c = swipeRightDrawView(c, itemView, dX);
            } else {
                c = swipeLeftDrawView(c, itemView, dX);
            }

        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void swipePhaseView(View itemView, float dX) {
        final float minAlpha = 0.15f;
        float maxWidth = (float) itemView.getWidth();
        float absPos = Math.abs(dX);

        float alpha = (float) 1 - (absPos / maxWidth);
        if (alpha < minAlpha)
            alpha = minAlpha;

        itemView.setAlpha(alpha);
    }

    public void setSwipeDrawBgView(String leftColorHex, Bitmap leftIcon,
                                   String rightColorHex, Bitmap rightIcon, float padding) {
        this.leftIcon = leftIcon;
        this.rightIcon = rightIcon;

        try {
            if (!TextUtils.isEmpty(leftColorHex)) {
                leftColor = Color.parseColor(leftColorHex);
            } else {
                leftColor = defLeftColor;
            }
        } catch (IllegalArgumentException e) {
            leftColor = defLeftColor;
        }
        try {
            if (!TextUtils.isEmpty(rightColorHex)) {
                rightColor = Color.parseColor(rightColorHex);
            } else {
                rightColor = defRightColor;
            }
        } catch (IllegalArgumentException e) {
            rightColor = defRightColor;
        }

        if (padding >= 0)
            this.padding = padding;
    }

    public void setSwipeDrawBgView(int cols) {
        if (cols > 0)
            this.cols = cols;
    }

    //TODO need to modify relative to gridlayout column position
    private Canvas swipeRightDrawView(Canvas c, View itemView, float dX) {
        float left_bg, right_bg, top_bg, bottom_bg;
        float left_ic, right_ic, top_ic, bottom_ic, height, width_ic;
        p.setColor(leftColor);

        left_bg = (float) itemView.getLeft() + padding;
        right_bg = dX;
        top_bg = (float) itemView.getTop() + padding;
        bottom_bg = (float) itemView.getBottom() - padding;

        RectF background = new RectF(left_bg, top_bg, right_bg, bottom_bg);
        c.drawRect(background, p);

        if (leftIcon != null) {
            height = (float) itemView.getBottom() - (float) itemView.getTop();
            width_ic = height / 3;

            left_ic = (float) itemView.getLeft() + width_ic;
            right_ic = (float) itemView.getLeft() + 2 * width_ic;
            top_ic = (float) itemView.getTop() + width_ic;
            bottom_ic = (float) itemView.getBottom() - width_ic;

            RectF icon_dest = new RectF(left_ic, top_ic, right_ic, bottom_ic);
            c.drawBitmap(leftIcon, null, icon_dest, p);
        }

        return c;
    }

    //TODO need to modify relative to gridlayout column position
    private Canvas swipeLeftDrawView(Canvas c, View itemView, float dX) {
        float left_bg, right_bg, top_bg, bottom_bg;
        float left_ic, right_ic, top_ic, bottom_ic, height, width_ic;
        p.setColor(rightColor);

        left_bg = (float) itemView.getRight() + dX;
        right_bg = (float) itemView.getRight() - padding;
        top_bg = (float) itemView.getTop() + padding;
        bottom_bg = (float) itemView.getBottom() - padding;

        RectF background = new RectF(left_bg, top_bg, right_bg, bottom_bg);
        c.drawRect(background, p);

        if (rightIcon != null) {
            height = (float) itemView.getBottom() - (float) itemView.getTop();
            width_ic = height / 3;

            left_ic = (float) itemView.getRight() - 2 * width_ic;
            right_ic = (float) itemView.getRight() - width_ic;
            top_ic = (float) itemView.getTop() + width_ic;
            bottom_ic = (float) itemView.getBottom() - width_ic;

            RectF icon_dest = new RectF(left_ic, top_ic, right_ic, bottom_ic);
            c.drawBitmap(rightIcon, null, icon_dest, p);
        }

        return c;
    }

}
