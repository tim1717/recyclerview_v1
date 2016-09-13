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

import com.watermelon.doodle.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RecyclerTouchHelper extends ItemTouchHelper.SimpleCallback {
    public static final String TAG = RecyclerTouchHelper.class.getSimpleName();

    /**
     * style NAMES - TAGS
     */
    private static final int VIEW_CLEARED = -1;     // clearView: for all styles, when itemRemoved
    private static final int STYLE_DISABLED = 0;    // swipe disabled
    private static final int STYLE_LOCK_L = 1;      // locked to behind-left view
    private static final int STYLE_LOCK_R = 2;      // locked to behind-right view
    private static final int STYLE_LOCK = 3;        // locked to either behind views
    private static final int STYLE_DRAW_HALF = 4;   // draw bg & locked halfway
    private static final int STYLE_DRAW = 5;        // draw bg & draw icon, full swipe + phase
    private static final int STYLE_PHASE = 6;       // phase + standard swipe
                                                    // default: standard swipe

    private DisplayMetrics metrics;

    // === SWIPE_ACTION ===
    private Handler action;
    private int leftActionId, rightActionId;

    // === STYLE_DISABLED ===
    private List<Integer> disabledSwipePos = new ArrayList<Integer>();

    // === STYLE_DRAW_HALF | STYLE_DRAW ===
    private Paint p = new Paint();
    private int leftColor, rightColor;
    private static final int defLeftColor = Color.parseColor("#00FF00");
    private static final int defRightColor = Color.parseColor("#FF0000");
    private static final int padding = 0;
    private Bitmap leftIcon, rightIcon;

    // === STYLE_LOCK_L | STYLE_LOCK_R | STYLE_LOCK ===
    private int customLeftThreshold = -1;
    private int customRightThreshold = -1;
    private static final boolean SET_TAG = false;
    public static final String ANCHOR = "ANCHOR";
    private int SWIPEDIR = (ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT);
    private boolean RESTORING = false;

    // for verbose debugging
    public static String kNameAction(int value) {
        String[] names = new String[] {"IDLE", "SWIPE", "DRAG"};
        int[] values = new int[] {0, 1, 2};
        return Log.getKName(names, values, value, TAG);
    }

    // for verbose debugging
    public static String kNameDir(int value) {
        String[] names = new String[] {"NONE", "DOWN", "END", "LEFT", "RIGHT", "START", "UP", "R&L"};
        int[] values = new int[] {0, 2, 32, 4, 8, 16, 1, (ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT)};
        return Log.getKName(names, values, value, TAG);
    }

    public RecyclerTouchHelper(int dragDirs, int swipeDirs, DisplayMetrics metrics,
                               Handler result, int leftActionId, int RightActionId) {
        super(dragDirs, swipeDirs);
        this.metrics = metrics;
        this.action = result;
        this.leftActionId = leftActionId;
        this.rightActionId = RightActionId;
    }

    // can override drag with return 0
    @Override
    public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return super.getDragDirs(recyclerView, viewHolder);
    }

    // can override swipe with return 0
    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        View itemView = viewHolder.itemView;
        RecyclerAdapter.ViewHolder vHolder = (RecyclerAdapter.ViewHolder) viewHolder;
        int pos = viewHolder.getAdapterPosition();
        Log.i(TAG, "getSwipeDirs " + pos);

        // === SWIPE_ACTION === if not assigned
        if (action == null) return 0;

        // === STYLE_DISABLED ===
        if (isSwipePosDisabled(pos)) return 0;

        // *** PER SWIPE STYLE ***
        switch (pos) {
            case STYLE_LOCK_L: {
                Object tag = vHolder.getLeftOpt().getTag();
                float tX = vHolder.getTopView().getTranslationX();
                int swipedir;

                if (isAnchored(tag, pos)) {
                    swipedir = ItemTouchHelper.LEFT;
                } else {
                    swipedir = ItemTouchHelper.RIGHT;
                }
                Log.d(TAG, "getSwipeDirs " + tX + "," + kNameDir(swipedir) + ","
                        + (tag == null ? "null" : (String) tag) + "," + isAnchored(tag, pos));
                return swipedir;
            }
            case STYLE_LOCK_R: {
                Object tag = vHolder.getRightOpt().getTag();
                float tX = vHolder.getTopView().getTranslationX();
                int swipedir;

                if (isAnchored(tag, pos)) {  // tX also <= threshold, else 0
                    swipedir = ItemTouchHelper.RIGHT;
                } else {
                    swipedir = ItemTouchHelper.LEFT;
                }
                Log.d(TAG, "getSwipeDirs " + tX + "," + kNameDir(swipedir) + ","
                        + (tag == null ? "null" : (String) tag) + "," + isAnchored(tag, pos));
                return swipedir;
            }
            case STYLE_LOCK: {
                Object tagR = vHolder.getRightOpt().getTag();
                Object tagL = vHolder.getLeftOpt().getTag();
                float tX = vHolder.getTopView().getTranslationX();

                // while anchored, only enable swipe dir of restore
                if (isAnchored(tagR, pos) && isAnchored(tagL, pos)) {
                    // should never happen
                    Log.w(TAG, "getSwipeDirs !@#$ DOUBLE ANCHOR");
                    SWIPEDIR = 0;
                    RESTORING = false;
                    setAnchorTag(vHolder.getRightOpt(), pos, !SET_TAG);
                    setAnchorTag(vHolder.getLeftOpt(), pos, !SET_TAG);
                } else if (isAnchored(tagR, pos)) {
                    SWIPEDIR = ItemTouchHelper.RIGHT;
                } else if (isAnchored(tagL, pos)) {
                    SWIPEDIR = ItemTouchHelper.LEFT;
                } else {
                    if (RESTORING) {
                        // while restoring from an anchored position, disable swipe
                        SWIPEDIR = 0;
                    } else {
                        SWIPEDIR = ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT;
                    }
                }

                Log.d(TAG, "getSwipeDirs L " + tX + "," + kNameDir(SWIPEDIR) + ","
                        + (tagL == null ? "null" : (String) tagL) + "," + isAnchored(tagL, pos));
                Log.d(TAG, "getSwipeDirs R " + tX + "," + kNameDir(SWIPEDIR) + ","
                        + (tagR == null ? "null" : (String) tagR) + "," + isAnchored(tagR, pos));
                return SWIPEDIR;
            }
            case STYLE_DRAW_HALF: {
                // none
                break;
            }
            case STYLE_DRAW: {
                // none
                break;
            }
            case STYLE_PHASE: {
                // none
                break;
            }
            default: {
                // base case
            }
        }

        return super.getSwipeDirs(recyclerView, viewHolder);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        View itemView = viewHolder.itemView;
        RecyclerAdapter.ViewHolder vHolder = (RecyclerAdapter.ViewHolder) viewHolder;
        int pos = viewHolder.getAdapterPosition();
        Log.d(TAG, "onSwiped " + pos + "," + kNameDir(direction));

        // *** PER SWIPE STYLE ***
        switch (pos) {
            case STYLE_LOCK_L: {
                // return, lock style disables triggered swipe
                return;
            }
            case STYLE_LOCK_R: {
                // return, lock style disables triggered swipe
                return;
            }
            case STYLE_LOCK: {
                // return, lock style disables triggered swipe
                return;
            }
            case STYLE_DRAW_HALF: {
                // none
                break;
            }
            case STYLE_DRAW: {
                // none
                break;
            }
            case STYLE_PHASE: {
                // none
                break;
            }
            default: {
                // base case
            }
        }

        // === SWIPE_ACTION ===
        swipeAction(direction, pos);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        View itemView = viewHolder.itemView;
        RecyclerAdapter.ViewHolder vHolder = (RecyclerAdapter.ViewHolder) viewHolder;
        int pos = viewHolder.getAdapterPosition();
        Log.i(TAG, "clearView " + pos);

        // *** PER SWIPE STYLE ***
        switch (pos) {
            //*** FOR ALL STYLES, REQUIRED *** itemRemoved pos becomes -1
            case VIEW_CLEARED: {
                // for STYLE_PHASE
                resetPhaseView(vHolder.getTopView());
                resetPhaseView(itemView);
                // for ALL
                getDefaultUIUtil().clearView(vHolder.getTopView());
                getDefaultUIUtil().clearView(itemView);
                break;
            }
            case STYLE_LOCK_L: {
                getDefaultUIUtil().clearView(vHolder.getTopView());
                getDefaultUIUtil().clearView(itemView);
                break;
            }
            case STYLE_LOCK_R: {
                getDefaultUIUtil().clearView(vHolder.getTopView());
                getDefaultUIUtil().clearView(itemView);
                break;
            }
            case STYLE_LOCK: {
                getDefaultUIUtil().clearView(vHolder.getTopView());
                getDefaultUIUtil().clearView(itemView);
                if (RESTORING) {
                    // restore from anchored position complete
                    RESTORING = false;
                    Log.d(TAG, "RESTORE complete");
                }
                break;
            }
            case STYLE_DRAW_HALF: {
                getDefaultUIUtil().clearView(vHolder.getTopView());
                getDefaultUIUtil().clearView(itemView);
                break;
            }
            case STYLE_DRAW: {
                resetPhaseView(vHolder.getTopView());
                resetPhaseView(itemView);
                break;
            }
            case STYLE_PHASE: {
                resetPhaseView(vHolder.getTopView());
                resetPhaseView(itemView);
                break;
            }
            default: {
                // base case
            }
        }

        super.clearView(recyclerView, viewHolder);
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {
            View itemView = viewHolder.itemView;
            RecyclerAdapter.ViewHolder vHolder = (RecyclerAdapter.ViewHolder) viewHolder;
            int pos = viewHolder.getAdapterPosition();
            Log.d(TAG, "onSelectedChanged " + pos + "," + kNameAction(actionState));

            // === STYLE_DISABLED ===
            if (isSwipePosDisabled(pos)) return;

            // *** PER SWIPE STYLE ***
            switch (pos) {
                case STYLE_LOCK_L: {
                    getDefaultUIUtil().onSelected(vHolder.getTopView());
                    getDefaultUIUtil().onSelected(itemView);
                    return;
                }
                case STYLE_LOCK_R: {
                    getDefaultUIUtil().onSelected(vHolder.getTopView());
                    getDefaultUIUtil().onSelected(itemView);
                    return;
                }
                case STYLE_LOCK: {
                    getDefaultUIUtil().onSelected(vHolder.getTopView());
                    getDefaultUIUtil().onSelected(itemView);
                    return;
                }
                case STYLE_DRAW_HALF: {
                    // none
                    break;
                }
                case STYLE_DRAW: {
                    // none
                    break;
                }
                case STYLE_PHASE: {
                    // none
                    break;
                }
                default: {
                    // base case
                }
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
        RecyclerAdapter.ViewHolder vHolder = (RecyclerAdapter.ViewHolder) viewHolder;
        int pos = viewHolder.getAdapterPosition();

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE)
        {
            // *** PER SWIPE STYLE ***
            switch (pos) {
                case STYLE_LOCK_L: {
                    float translationX;
                    float threshold = getLeftThreshold(vHolder);
                    Object tag = vHolder.getLeftOpt().getTag();

                    if (dX >= 0f) {
                        // draw until anchor threshold
                        if (!isAnchored(tag, pos)) {
                            translationX = Math.min(dX, threshold);
                        } else {
                            translationX = threshold;
                        }

                        // mark anchored using tag
                        if (dX >= threshold) {
                            if (!isAnchored(tag, pos)) {
                                tag = setAnchorTag(vHolder.getLeftOpt(), pos, SET_TAG);
                            }
                        }
                    } else {
                        // restoring, snap to original state at 0
                        translationX = 0f;

                        // clear anchor tag
                        if (isAnchored(tag, pos)) {
                            tag = setAnchorTag(vHolder.getLeftOpt(), pos, !SET_TAG);
                        }
                    }

                    Log.d(TAG, "onChildDraw " + dX + "," + translationX + "," + kNameAction(actionState)
                            + "," + isCurrentlyActive + "," + isAnchored(tag, pos));
                    vHolder.getTopView().setTranslationX(translationX);
                    // swipe setTranslationX, no super
                    return;
                }
                case STYLE_LOCK_R: {
                    float translationX;
                    float threshold = getRightThreshold(vHolder);
                    Object tag = vHolder.getRightOpt().getTag();

                    if (dX <= 0f) {
                        // draw until anchor threshold
                        if (!isAnchored(tag, pos)) {
                            translationX = Math.max(dX, threshold);
                        } else {
                            translationX = threshold;
                        }

                        // mark anchored using tag
                        if (dX <= threshold) {
                            if (!isAnchored(tag, pos)) {
                                tag = setAnchorTag(vHolder.getRightOpt(), pos, SET_TAG);
                            }
                        }
                    } else {
                        // restoring, snap to original state at 0
                        translationX = 0f;

                        // clear anchor tag
                        if (isAnchored(tag, pos)) {
                            tag = setAnchorTag(vHolder.getRightOpt(), pos, !SET_TAG);
                        }
                    }

                    Log.d(TAG, "onChildDraw " + dX + "," + translationX + "," + kNameAction(actionState)
                            + "," + isCurrentlyActive + "," + isAnchored(tag, pos));
                    vHolder.getTopView().setTranslationX(translationX);
                    // swipe setTranslationX, no super
                    return;
                }
                case STYLE_LOCK: {
                    float translationX = 0f;
                    float thresholdR = getRightThreshold(vHolder);
                    Object tagR = vHolder.getRightOpt().getTag();
                    float thresholdL = getLeftThreshold(vHolder);
                    Object tagL = vHolder.getLeftOpt().getTag();

                    if (dX > 0f) {
                        if (isAnchored(tagL, pos)) {
                            // anchored to LEFT, lock draw
                            translationX = thresholdL;
                        } else if (isAnchored(tagR, pos)) {
                            // restoring to RIGHT, snap to original state at 0
                            translationX = 0f;

                            RESTORING = true;
                            tagR = setAnchorTag(vHolder.getRightOpt(), pos, !SET_TAG);
                        } else {
                            // while restoring to RIGHT, do not trigger draw for LEFT
                            if (!RESTORING) {
                                if (!isAnchored(tagL, pos)) {
                                    // draw until LEFT anchor threshold
                                    translationX = Math.min(dX, thresholdL);

                                    // mark anchored using tag
                                    if (dX >= thresholdL) {
                                        if (!isAnchored(tagL, pos)) {
                                            tagL = setAnchorTag(vHolder.getLeftOpt(), pos, SET_TAG);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dX < 0f) {
                        if (isAnchored(tagL, pos)) {
                            // restoring to LEFT, snap to original state at 0
                            translationX = 0f;

                            RESTORING = true;
                            tagL = setAnchorTag(vHolder.getLeftOpt(), pos, !SET_TAG);
                        } else if (isAnchored(tagR, pos)) {
                            // anchored to RIGHT, lock draw
                            translationX = thresholdR;
                        } else {
                            // while restoring to LEFT, do not trigger draw for RIGHT
                            if (!RESTORING) {
                                // draw until RIGHT anchor threshold
                                if (!isAnchored(tagR, pos)) {
                                    translationX = Math.max(dX, thresholdR);

                                    // mark anchored using tag
                                    if (dX <= thresholdR) {
                                        if (!isAnchored(tagR, pos)) {
                                            tagR = setAnchorTag(vHolder.getRightOpt(), pos, SET_TAG);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // while anchored keep locked at threshold, else keep at rest 0
                        if (isAnchored(tagL, pos)) {
                            translationX = thresholdL;
                        }
                        if (isAnchored(tagR, pos)) {
                            translationX = thresholdR;
                        }

                        Log.i(TAG, "onChildDraw DX0 " + translationX);
                    }

                    if (Math.abs(dX) == metrics.widthPixels) {
                        // should never happen when !isCurrentlyActive
                        Log.w(TAG, "!@#$ CRAZY " + !isCurrentlyActive);
                    }

                    Log.d(TAG, "onChildDraw " + dX + "," + translationX + "," + kNameAction(actionState)
                            + "," + isCurrentlyActive + "," + isAnchored(tagL, pos) + "," + isAnchored(tagR, pos));
                    vHolder.getTopView().setTranslationX(translationX);
                    // swipe setTranslationX, no super
                    return;

                }
                case STYLE_DRAW_HALF: {
                    float width = itemView.getWidth();
                    float threshold = (float) Math.round(width / 2);  // half, right on trigger point
                    float translationX;

                    // draw until thresholds
                    if (dX > 0) {
                        swipeRightDrawView(c, itemView, dX, null);

                        translationX = Math.min(dX, threshold);
                    } else {
                        swipeLeftDrawView(c, itemView, dX, null);

                        translationX = -Math.min(-dX, threshold);
                    }

                    Log.d(TAG, "onChildDraw " + dX + "," + translationX + "," + kNameAction(actionState)
                            + "," + isCurrentlyActive);
                    vHolder.getTopView().setTranslationX(translationX);
                    // swipe setTranslationX, no super
                    return;
                }
                case STYLE_DRAW: {
                    swipePhaseView(itemView, dX);

                    if (dX > 0) {
                        c = swipeRightDrawView(c, itemView, dX, leftIcon);
                    } else {
                        c = swipeLeftDrawView(c, itemView, dX, rightIcon);
                    }
                    break;
                }
                case STYLE_PHASE: {
                    swipePhaseView(itemView, dX);
                    break;
                }
                default: {
                    // base case
                }
            }
        } else {
            Log.d(TAG, "onChildDraw " + dX + "," + kNameAction(actionState) + "," + isCurrentlyActive);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    // </ ========== SWIPE_ACTION ==================================================================
    /**
     * swipe action : handler / callback
     */
    private void swipeAction(int direction, int position) {
        if (action == null) return;

        Message message = new Message();
        if (direction == ItemTouchHelper.LEFT) {
            message.what = leftActionId;
            message.obj = position;
        } else if (direction == ItemTouchHelper.RIGHT) {
            message.what = rightActionId;
            message.obj = position;
        } else {
            return;
        }

        action.sendMessage(message);
    }
    // ==================== SWIPE_ACTION ========================================================= />

    // </ ========== STYLE_PHASE ===================================================================
    /**
     * phase visibility proportional to swipe distance
     */
    private void swipePhaseView(View itemView, float dX) {
        final float minAlpha = 0.10f;
        float maxWidth = (float) itemView.getWidth();
        float absPos = Math.abs(dX);

        // alpha based on difference from rest 0
        float alpha = (float) 1 - (absPos / maxWidth);
        if (alpha < minAlpha)
            alpha = minAlpha;

        itemView.setAlpha(alpha);
    }

    /**
     * reset phase
     */
    private void resetPhaseView(View view) {
        if (view.getAlpha() < 1f)
            view.setAlpha(1f);
    }
    // ==================== STYLE_PHASE ========================================================= />

    // </ ========== STYLE_LOCK_L | STYLE_LOCK_R | STYLE_LOCK ======================================
    /**
     * is anchored
     */
    private boolean isAnchored(Object tag, int pos) {
        Object expected = ANCHOR + Integer.toString(pos);
        return (expected.equals(tag));
    }

    /**
     * set anchor tag
     */
    private String setAnchorTag(View view, int pos, boolean clear) {
        Object tag = null;
        if (clear) {
            String temp = (view.getTag() == null ? "null" : (String) view.getTag());
            Log.i(TAG, "@" + temp + "->CLEAR");
            view.setTag(tag);
        } else {
            tag = ANCHOR + Integer.toString(pos);
            view.setTag(tag);
            Log.i(TAG, "@" + tag + "->SET");
        }
        return (tag == null ? null : (String) tag);
    }

    /**
     * set custom left threshold, include metrics.density
     */
    public void setCustomLeftThreshold(int threshold) {
        this.customLeftThreshold = threshold;
    }

    /**
     * set custom right threshold, include metrics.density
     */
    public void setCustomRightThreshold(int threshold) {
        this.customRightThreshold = threshold;
    }

    /**
     * get left threshold
     */
    private float getLeftThreshold(RecyclerAdapter.ViewHolder vHolder) {
        float threshold;
        if (customLeftThreshold > 0) {
            threshold = customLeftThreshold;

            // just under default threshold to avoid onSwiped trigger
            int max_half = (int) Math.floor(vHolder.itemView.getWidth() / 2);
            if (threshold > max_half) threshold = max_half - 1;
        } else {
            // default to behind-image's border
            threshold = vHolder.getLeftOpt().getRight();
        }
        return threshold;
    }

    /**
     * get right threshold
     */
    private float getRightThreshold(RecyclerAdapter.ViewHolder vHolder) {
        float threshold;
        if (customRightThreshold > 0) {
            threshold = customRightThreshold;

            // just under default threshold to avoid onSwiped trigger
            int max_half = (int) Math.floor(vHolder.itemView.getWidth() / 2);
            if (threshold > max_half) threshold = max_half + 1;
        } else {
            // default to behind-image's border
            threshold = vHolder.getRightOpt().getLeft();
        }
        return -threshold;
    }
    // ==================== STYLE_LOCK_L | STYLE_LOCK_R | STYLE_LOCK ============================ />

    // </ ========== STYLE_DRAW_HALF | STYLE_DRAW ==================================================
    /**
     * init background view attributes, icon and color
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
     * draw background for swipe right, underneath left area
     */
    private Canvas swipeRightDrawView(Canvas c, View itemView, float dX, Bitmap icon) {
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

        if (icon != null) {
            height = (float) itemView.getBottom() - (float) itemView.getTop();
            width_ic = height / 3;

            left_ic = (float) itemView.getLeft() + width_ic;
            right_ic = (float) itemView.getLeft() + 2 * width_ic;
            top_ic = (float) itemView.getTop() + width_ic;
            bottom_ic = (float) itemView.getBottom() - width_ic;

            RectF icon_dest = new RectF(left_ic, top_ic, right_ic, bottom_ic);
            c.drawBitmap(icon, null, icon_dest, p);
        }

        return c;
    }

    /**
     * draw background for swipe left, underneath right area
     */
    private Canvas swipeLeftDrawView(Canvas c, View itemView, float dX, Bitmap icon) {
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

        if (icon != null) {
            height = (float) itemView.getBottom() - (float) itemView.getTop();
            width_ic = height / 3;

            left_ic = (float) itemView.getRight() - 2 * width_ic;
            right_ic = (float) itemView.getRight() - width_ic;
            top_ic = (float) itemView.getTop() + width_ic;
            bottom_ic = (float) itemView.getBottom() - width_ic;

            RectF icon_dest = new RectF(left_ic, top_ic, right_ic, bottom_ic);
            c.drawBitmap(icon, null, icon_dest, p);
        }

        return c;
    }
    // ==================== STYLE_DRAW_HALF | STYLE_DRAW ======================================== />

    // </ ========== STYLE_DISABLED ================================================================
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
            disabledSwipePos.remove(disabledSwipePos.indexOf(cellPos));
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
    // ==================== STYLE_DISABLED ====================================================== />

}
