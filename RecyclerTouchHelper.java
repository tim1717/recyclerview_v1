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
import android.util.DisplayMetrics;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RecyclerTouchHelper extends ItemTouchHelper.SimpleCallback {
    public static final String TAG = RecyclerTouchHelper.class.getSimpleName();

    private Handler action;
    private int leftActionId, rightActionId;

    private List<Integer> disabledSwipePos = new ArrayList<Integer>();

    private DisplayMetrics metrics;
    private Paint p = new Paint();
    private int leftColor, rightColor;
    private static final int defLeftColor = Color.parseColor("#00FF00");
    private static final int defRightColor = Color.parseColor("#FF0000");
    private static final int padding = 5;
    private Bitmap leftIcon, rightIcon;
    private float cols = 1;

    public RecyclerTouchHelper(int dragDirs, int swipeDirs, DisplayMetrics metrics,
                               Handler result, int leftActionId, int rightActionId) {
        super(dragDirs, swipeDirs);
        this.metrics = metrics;
        this.action = result;
        this.leftActionId = leftActionId;
        this.rightActionId = rightActionId;
    }

    /**
     * disable swipe for 1 n-th cell
     */
    public void disableSwipePos(int cellPos, boolean addOrNew) {
        if (addOrNew) {
            if (!disabledSwipePos.contains(cellPos))
                disabledSwipePos.add(cellPos);
        } else {
            disabledSwipePos.clear();
            disabledSwipePos.add(cellPos);
        }
    }

    /**
     * disable swipe for x n-th cells
     */
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

    /**
     * reenable swipe for 1 n-th cell
     */
    public void removeDisabledSwipePos(int cellPos) {
        int pos = disabledSwipePos.indexOf(cellPos);
        if (pos >= 0)
            disabledSwipePos.remove(pos);
    }

    /**
     * check swipe disabled status for n-th cell
     */
    public boolean isSwipePosDisabled(int cellPos) {
        if (disabledSwipePos.contains(cellPos))
            return true;
        else
            return false;
    }

    /**
     * return all swipe disabled cells positions
     */
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
        // reusing isSwipePosDisabled to indicate which cells have special swipe
        // instead of being disabled
//        if (isSwipePosDisabled(pos))
//            return 0;

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

        // send swipe action to activity handler
        if (action != null) {
            Message message = new Message();
            if (direction == ItemTouchHelper.LEFT) {
                message.what = leftActionId;
                message.obj = pos;
            } else {
                message.what = rightActionId;
                message.obj = pos;
            }

            action.sendMessage(message);
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        View itemView = viewHolder.itemView;
        int pos = viewHolder.getAdapterPosition();

        getDefaultUIUtil().clearView(((RecyclerAdapter.ViewHolder) viewHolder).getSwipeView());

        // bug? also need super or else swipeview status might affect other cells
        super.clearView(recyclerView, viewHolder);

        // bug? needed to restore alpha or might affect other cells
        if (((RecyclerAdapter.ViewHolder) viewHolder).getSwipeView().getAlpha() < 1f)
            ((RecyclerAdapter.ViewHolder) viewHolder).getSwipeView().setAlpha(1f);
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {
            View itemView = viewHolder.itemView;
            int pos = viewHolder.getAdapterPosition();

            if (isSwipePosDisabled(pos)) {
                getDefaultUIUtil().onSelected(((RecyclerAdapter.ViewHolder) viewHolder).getSwipeView());
                return;
            }

            super.onSelectedChanged(viewHolder, actionState);
        }
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState,
                            boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int pos = viewHolder.getAdapterPosition();

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE)
        {
            if (isSwipePosDisabled(pos))
            {
                swipePhaseView(((RecyclerAdapter.ViewHolder) viewHolder).getSwipeView(), dX);

                getDefaultUIUtil().onDraw(c, recyclerView,
                        ((RecyclerAdapter.ViewHolder) viewHolder).getSwipeView(),
                        dX, dY, actionState, isCurrentlyActive);
                return;
            }
            else
            {
                swipePhaseView(itemView, dX);

                if (dX > 0) {
                    c = swipeRightDrawView(c, itemView, dX);
                } else {
                    c = swipeLeftDrawView(c, itemView, dX);
                }
            }

        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState,
                                boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int pos = viewHolder.getAdapterPosition();

        if (isSwipePosDisabled(pos)) {
            getDefaultUIUtil().onDrawOver(c, recyclerView,
                    ((RecyclerAdapter.ViewHolder) viewHolder).getSwipeView(),
                    dX, dY, actionState, isCurrentlyActive);
            return;
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    /**
     * phase visibility proportional to swipe distance
     */
    private void swipePhaseView(View itemView, float dX) {
        final float minAlpha = 0.15f;
        float maxWidth = (float) itemView.getWidth();
        float absPos = Math.abs(dX);

        float alpha = (float) 1 - (absPos / maxWidth);
        if (alpha < minAlpha)
            alpha = minAlpha;

        itemView.setAlpha(alpha);
    }

    /**
     * init background view attributes
     */
    public void setSwipeDrawBgView(String leftColorHex, Bitmap leftIcon,
                                   String rightColorHex, Bitmap rightIcon) {
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
    }

    /**
     * set #column for background draw offsets
     */
    public void setSwipeDrawBgView(int cols) {
        if (cols > 0)
            this.cols = cols;
    }

    /**
     * draw background for swipe right, underneath left area
     * TODO need to modify draw offsets due to gridlayout columns
     */
    private Canvas swipeRightDrawView(Canvas c, View itemView, float dX) {
        float left_bg, right_bg, top_bg, bottom_bg;
        float left_ic, right_ic, top_ic, bottom_ic, height, width_ic;
        float border = padding * metrics.density;
        p.setColor(leftColor);

        left_bg = (float) itemView.getLeft() + border;
        right_bg = dX;
        top_bg = (float) itemView.getTop() + border;
        bottom_bg = (float) itemView.getBottom() - border;

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

    /**
     * draw background for swipe left, underneath right area
     * TODO need to modify draw offsets due to gridlayout columns
     */
    private Canvas swipeLeftDrawView(Canvas c, View itemView, float dX) {
        float left_bg, right_bg, top_bg, bottom_bg;
        float left_ic, right_ic, top_ic, bottom_ic, height, width_ic;
        float border = padding * metrics.density;
        p.setColor(rightColor);

        left_bg = (float) itemView.getRight() + dX;
        right_bg = (float) itemView.getRight() - border;
        top_bg = (float) itemView.getTop() + border;
        bottom_bg = (float) itemView.getBottom() - border;

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
