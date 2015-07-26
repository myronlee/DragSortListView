package com.diandian.coolco.dragsortlistview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DragSortListView extends ListView {

    /**
     * the mask image that moves as user finger moves
     */
    private Bitmap draggingItemViewBitmap;
    /**
     * the region that the {@link #draggingItemViewBitmap} will be drawn
     */
    private RectF draggingItemViewRect;
    /**
     * the paint used in drawing {@link #draggingItemViewBitmap}, new it in onDraw isn't recommended
     */
    private Paint draggingItemViewBitmapPaint;

    /**
     * the y coordinate of the beginning <code>ACTION_DOWN</code>, also the beginning of the whole gesture
     */
    private float downY;
    /**
     * the y coordinate of the last MotionEvent
     */
    private float lastY;
    /**
     * the original position of the draggingItemView
     */
    private int srcPosition;
    /**
     * the flag that indicate whether we are dragging some item view
     */
    private boolean dragging;
    /**
     * store it, so we can set it to be visible when dragging ends
     */
    private View draggingItemView;

    /**
     * when user's finger is up to this boundary, scroll down the ListView
     */
    private float scrollDownBoundary;
    /**
     * when user's finger is below to this boundary, scroll up the ListView
     */
    private float scrollUpBoundary;
    private int itemDeltaY;
    private ValueAnimator itemAnimator;
    private long duration = 300;
    private List<View> needMoveItems;
    private Map<View, Float> needMoveItemsOriginalTranslationY;


    public DragSortListView(Context context) {
        super(context);
    }

    public DragSortListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ScrollView
    }

    public DragSortListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        init();
    }

    private void init() {
        scrollDownBoundary = getHeight() * 0.25f;
        scrollUpBoundary = getHeight() * 0.75f;

        draggingItemViewBitmapPaint = new Paint();
        draggingItemViewBitmapPaint.setAlpha(0x88);

        setLayerType(View.LAYER_TYPE_HARDWARE, null);//硬件加速

        needMoveItems = new ArrayList<View>();
        needMoveItemsOriginalTranslationY = new HashMap<View, Float>();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (handleDownEvent(ev)) {
                    //return true means i am interested in this gesture
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    handleMoveEvent(ev);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    handleUpEvent(ev);
                    return true;
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private boolean handleDownEvent(MotionEvent ev) {
        float downX = ev.getX();
        float downY = ev.getY();
        int downPosition = pointToPosition(((int) downX), ((int) downY));
        if (downPosition == AdapterView.INVALID_POSITION) {
            return false;
        }

        // get the item view under user's finger
        View underFingerItemView = getChildAt(downPosition - getFirstVisiblePosition());

        // check whether user's finger pressed on the drag handler
        View dragHandler = underFingerItemView.findViewById(R.id.tv_drag_handler);
        if (dragHandler == null) {

            // if the under item view don't have a drag handler, don't start drag
            return false;
        }
        Rect dragHandlerHitRect = new Rect();
        dragHandler.getHitRect(dragHandlerHitRect);

        //important! change the coordinate system from underFingerItemView to this listview.
        dragHandlerHitRect.offset(((int) underFingerItemView.getLeft()), ((int) underFingerItemView.getTop()));
        if (!dragHandlerHitRect.contains(((int) downX), ((int) downY))) {

            // if user didn't pressed on the drag handler, don't start drag
            return false;
        }

        // now we can start drag
        dragging = true;
        draggingItemView = underFingerItemView;

//        this.downY = downY;
        lastY = downY;

        draggingItemViewBitmap = getBitmapFromView(draggingItemView);
//        underFingerItemView.getHitRect(draggingItemViewRect);
//        underFingerItemView.getDrawingRect();
//        Rect underFingerItemViewDrawingRect = new Rect();
//        underFingerItemView.getDrawingRect(underFingerItemViewDrawingRect);
//        underFingerItemView.getHitRect(underFingerItemViewDrawingRect); the same as up one, the same coordinate , the same size
        draggingItemViewRect = new RectF();
        draggingItemViewRect.set(draggingItemView.getLeft(), draggingItemView.getTop(), draggingItemView.getRight(), draggingItemView.getBottom());


        srcPosition = downPosition;
        ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(srcPosition);

        draggingItemView.setVisibility(INVISIBLE);

        //92
        itemDeltaY = draggingItemView.getMeasuredHeight();

        invalidate();

        return true;
    }

    private void handleMoveEvent(MotionEvent ev) {

        // update the position where the mask image will be drawn
        float currY = ev.getY();
        float dy = currY - lastY;
        lastY = currY;
        draggingItemViewRect.offset(0, dy);

        // redraw
        invalidate();

        // wait item moving ends
        if (itemAnimator != null) {
            Log.e("", "itemAnimator != null");
            return;
        }

        if (scrollListViewIfNeeded(currY)) {//don't move item when the ListView is scrolling
            Log.e("", "scrollListView");
            return;
        }
//        scrollListViewIfNeeded(currY);
//        scrollListBy();
//        smoothScrollBy();

//        reorderListView(ev);

        //reorder ListView
        final int currPosition = pointToPosition(((int) ev.getX()), ((int) ev.getY()));

        if (currPosition == AdapterView.INVALID_POSITION || currPosition == srcPosition) {
            Log.e("", "currPosition == AdapterView.INVALID_POSITION || currPosition == srcPosition  " + currPosition + " "+ srcPosition);
            for (int i = 0; i < getChildCount(); i++) {
                Rect outRect = new Rect();
                getChildAt(i).getHitRect(outRect);
                Log.e("", i + "  " + outRect.toString());
            }
            return;
        }

        moveItem(currPosition, false);
    }

    private void initNeedMoveItems(int srcPosition, int currPosition) {
        needMoveItems.clear();
        needMoveItemsOriginalTranslationY.clear();

        Log.e("", "srcPosition : " + srcPosition + "    currentPostion : " + currPosition);
        if (currPosition > srcPosition) {
            for (int i = srcPosition + 1; i <= currPosition; i++) {
                addNeedMoveItemInfo(i);
            }
        } else {
            for (int i = currPosition; i < srcPosition; i++) {
                addNeedMoveItemInfo(i);
            }
        }

/*

        int beg, end;//为了遍历方便，但不好理解了。
        if (srcPosition < currPosition){
            beg = srcPosition + 1;
            end = currPosition + 1;
        } else {
            beg = currPosition;
            end = srcPosition;
        }

        for (int i = beg; i < end; i++) {
            addNeedMoveItemInfo(i);
        }

*/
    }

    private void addNeedMoveItemInfo(int i) {
        View needMoveItem = findViewWithTag(i);
        if (needMoveItem != null) {
            Log.e("", "find item at position" + i);
            needMoveItems.add(needMoveItem);
            needMoveItemsOriginalTranslationY.put(needMoveItem, needMoveItem.getTranslationY());
        } else {
            Log.e("", "cann't find item at position" + i);
        }
    }

    private void moveItem(final int currPosition, final boolean reset) {
        Log.e("", "moveItem");
        if (currPosition == srcPosition) {

            Log.e("", "currPosition == srcPosition");
            itemMovementEnd(currPosition, reset);

            return;
        }

        initNeedMoveItems(srcPosition, currPosition);

        itemAnimator = currPosition > srcPosition ? ValueAnimator.ofFloat(0, -itemDeltaY) : ValueAnimator.ofFloat(0, itemDeltaY);//currPosition > srcPosition则向上移动itemView
        itemAnimator.setDuration(duration);
        itemAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //我们已经确定好了哪些itemView需要移动，循环移动它们
                for (View needMoveItem : needMoveItems) {
                    float originalTranslationY = needMoveItemsOriginalTranslationY.get(needMoveItem);
                    needMoveItem.setTranslationY(originalTranslationY + ((Float) valueAnimator.getAnimatedValue()));
                }
/*

                if (currPosition < srcPosition) {//move item(s) down
                    for (int i = currPosition; i < srcPosition; i++) {
                        View itemView = getChildAt(i - getFirstVisiblePosition());
                        View itemAtIPos = findViewWithTag(i);
                        if (itemAtIPos != null){

                        } else {
                            Log.e("", "itemAtIPos == null");
                        }
                        if (itemView != null) {
                            itemView.setTranslationY((Float) valueAnimator.getAnimatedValue());
                        }
                    }
                } else {//move item(s) up
                    for (int i = srcPosition + 1; i <= currPosition; i++) {
                        View itemView = getChildAt(i - getFirstVisiblePosition());
                        if (itemView != null) {
                            itemView.setTranslationY(-(Float) valueAnimator.getAnimatedValue());
                        }
                    }
                }

*/

            }
        });
        itemAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                itemMovementEnd(currPosition, reset);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                itemMovementEnd(currPosition, reset);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        itemAnimator.start();
    }

    private void itemMovementEnd(int currPosition, final boolean reset) {
        itemAnimator = null;

        updateNeedMoveItemTagIfNeeded(currPosition);

        if (reset) {
            ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(-1);
            //move item even if srcPosition equals with dstPosition, because we want to refresh ListView
            ((CommonDragSortAdapter) getAdapter()).moveItem(srcPosition, currPosition);
            ((CommonDragSortAdapter) getAdapter()).notifyDataSetChanged();

            srcPosition = -1;
            dragging = false;
            draggingItemView = null;
            if (draggingItemViewBitmap != null) {
                draggingItemViewBitmap.recycle();
                draggingItemViewBitmap = null;
            }

        } else {
            ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(currPosition);
            ((CommonDragSortAdapter) getAdapter()).moveItem(srcPosition, currPosition);
            srcPosition = currPosition;
        }
    }

    private void updateNeedMoveItemTagIfNeeded(int currPosition) {
        if (currPosition != srcPosition) {
            int postionDelta = currPosition > srcPosition ? -1 : 1;
            for (View needMoveItem : needMoveItems) {
                needMoveItem.setTag(((Integer) needMoveItem.getTag()) + postionDelta);
            }
        }
        //release reference
        needMoveItems.clear();
        needMoveItemsOriginalTranslationY.clear();
    }

    private boolean scrollListViewIfNeeded(float y) {

        //the distance you want to scroll ListView
        float dy = 0;
        if (y < scrollDownBoundary && listViewCanScrollDown()) {
            dy = (scrollDownBoundary - y) / 10;
        } else if (y > scrollUpBoundary && listViewCanScrollUp()) {
            dy = (scrollUpBoundary - y) / 10;
        }

        if (dy == 0) {
            return false;
        }

        //scroll down
        if (dy > 0) {
            View firstVisibleItemView = getChildAt(0);
            setSelectionFromTop(getFirstVisiblePosition(), ((int) (firstVisibleItemView.getTop() + dy)));
            return true;
        }

        //scroll up
        if (dy < 0) {
            View lastVisibleItemView = getChildAt(getLastVisiblePosition() - getFirstVisiblePosition() - 1);
            setSelectionFromTop(getLastVisiblePosition(), ((int) (lastVisibleItemView.getTop() + dy)));
            return true;
        }

        return false;
    }

    private boolean listViewCanScrollDown() {
        if (getFirstVisiblePosition() == 0 && getChildAt(0).getTop() >= 0) {
            return false;
        }
        return true;
    }

    private boolean listViewCanScrollUp() {

        if (getLastVisiblePosition() == getAdapter().getCount() - 1 && getChildAt(getCount() - 1).getBottom() <= getHeight()) {
            return false;
        }
        return true;
    }


    private void handleUpEvent(MotionEvent ev) {

        // reset
        dragging = false;
        draggingItemViewBitmap = null;
        draggingItemView.setVisibility(VISIBLE);
        draggingItemView = null;

        if (itemAnimator != null) {
            itemAnimator.end();
        }

        //reorder ListView
        int currPosition = pointToPosition(((int) ev.getX()), ((int) ev.getY()));
        int dstPosition = currPosition;
        if (currPosition == AdapterView.INVALID_POSITION) {
            dstPosition = srcPosition;
        }

        moveItem(dstPosition, true);
    }
/*

    private void reorderListView(MotionEvent ev) {
        // reorder
        int currPosition = pointToPosition(((int) ev.getX()), ((int) ev.getY()));
        if (currPosition == AdapterView.INVALID_POSITION){
//            dstPosition = srcPosition;
            return;
        }
//        int dstPosition = currPosition;
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (currPosition == srcPosition){
                return;
            }
            ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(currPosition);
            ((CommonDragSortAdapter) getAdapter()).moveItem(srcPosition, currPosition);
            srcPosition = currPosition;
        } else {
            //ACTION_UP ACTION_CANCEL
            ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(-1);
            ((CommonDragSortAdapter) getAdapter()).moveItem(srcPosition, currPosition);
            srcPosition = -1;
//            invalidate();
        }
    }

*/

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dragging) {
            canvas.drawBitmap(draggingItemViewBitmap, draggingItemViewRect.left, draggingItemViewRect.top, draggingItemViewBitmapPaint);
        }
    }

    public static Bitmap getBitmapFromView(View view) {
        view.setDrawingCacheEnabled(true);

        // this is the important code :)
        // Without it the view will have a dimension of 0,0 and the bitmap
        // will be null
//        view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
//                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
//        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
//        view.buildDrawingCache(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());

        // clear drawing cache
//        view.setDrawingCacheEnabled(false);
        return bitmap;
    }
}
