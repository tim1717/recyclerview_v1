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

public class RecyclerTouchHelper extends ItemTouchHelper.SimpleCallback {
    public static final String TAG = RecyclerTouchHelper.class.getSimpleName();

    private Handler action;
    private int actionId1, actionId2;

    private int disablePos = -1;

    private Paint p = new Paint();
    private int leftColor, rightColor;
    private String defLeftColorHex = "#0000FF";
    private String defRightColorHex = "#FF0000";
    private Bitmap leftIcon, rightIcon;
    private float padding = 0;
    private float cols = 1;

    public RecyclerTouchHelper(int dragDirs, int swipeDirs, Handler result, int actionId1, int actionId2) {
        super(dragDirs, swipeDirs);
        this.action = result;
        this.actionId1 = actionId1;
        this.actionId2 = actionId2;
    }

    public void disableSwipePos(int pos) {
        this.disablePos = pos;
    }

    public void setSwipeContent(String leftColorHex, Bitmap leftIcon,
                                String rightColorHex, Bitmap rightIcon, float padding) {
        this.leftIcon = leftIcon;
        this.rightIcon = rightIcon;
        this.padding = padding;
        try {
            if (TextUtils.isEmpty(leftColorHex))
                leftColorHex = this.defLeftColorHex;
            leftColor = Color.parseColor(leftColorHex);
        } catch (IllegalArgumentException e) {
            leftColor = Color.parseColor(this.defLeftColorHex);
        }
        try {
            if (TextUtils.isEmpty(rightColorHex))
                rightColorHex = this.defRightColorHex;
            rightColor = Color.parseColor(rightColorHex);
        } catch (IllegalArgumentException e) {
            rightColor = Color.parseColor(this.defRightColorHex);
        }
    }

    public void setSwipeContent(int cols) {
        if (cols < 1) cols = 1;
        this.cols = cols;
    }

    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int pos = viewHolder.getAdapterPosition();

        if (action == null)
            return 0;
        if (disablePos >= 0 && disablePos == pos)
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

            float maxWidth = (float) itemView.getWidth();
            float absPos = Math.abs(dX);

            itemView.setAlpha((float) 1.0 - (absPos / maxWidth));

            float height = (float) itemView.getBottom() - (float) itemView.getTop();
            float width = height / 3;

            //TODO need to modify relative to gridlayout column position
            float leftbg, rightbg, topbg, bottombg;
            if (dX > 0) {
                p.setColor(leftColor);
                leftbg = (float) itemView.getLeft() + padding;
                rightbg = dX;
                topbg = (float) itemView.getTop() + padding;
                bottombg = (float) itemView.getBottom() - padding;
                RectF background = new RectF(leftbg, topbg, rightbg, bottombg);
                c.drawRect(background, p);

                if (leftIcon != null) {
                    RectF icon_dest = new RectF((float) itemView.getLeft() + width,
                            (float) itemView.getTop() + width,
                            (float) itemView.getLeft()+ 2 * width,
                            (float)itemView.getBottom() - width);
                    c.drawBitmap(leftIcon, null, icon_dest, p);
                }
            } else {
                p.setColor(rightColor);
                leftbg = (float) itemView.getRight() + dX;
                rightbg = (float) itemView.getRight() - padding;
                topbg = (float) itemView.getTop() + padding;
                bottombg = (float) itemView.getBottom() - padding;
                RectF background = new RectF(leftbg, topbg, rightbg, bottombg);
                c.drawRect(background, p);

                if (leftIcon != null) {
                    RectF icon_dest = new RectF((float) itemView.getRight() - 2 * width,
                            (float) itemView.getTop() + width,
                            (float) itemView.getRight() - width,
                            (float)itemView.getBottom() - width);
                    c.drawBitmap(rightIcon, null, icon_dest, p);
                }
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
