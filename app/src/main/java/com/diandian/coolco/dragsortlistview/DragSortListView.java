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
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

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
    private int emptyPosition;
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
    private float topBoundary;
    /**
     * when user's finger is below to this boundary, scroll up the ListView
     */
    private float bottomBoundary;
    private int draggingItemHeight;
    private ValueAnimator itemAnimator;
    private long duration = 300;
    private List<View> needMoveItems;


    public DragSortListView(Context context) {
        super(context);
    }

    public DragSortListView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        topBoundary = getHeight() * 0.25f;
        bottomBoundary = getHeight() * 0.75f;

        draggingItemViewBitmapPaint = new Paint();
        draggingItemViewBitmapPaint.setAlpha(0x88);

        setLayerType(View.LAYER_TYPE_HARDWARE, null); //hardware acceleration

        needMoveItems = new ArrayList<View>();
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


        emptyPosition = downPosition;
        ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(emptyPosition);

        draggingItemView.setVisibility(INVISIBLE);

        draggingItemHeight = draggingItemView.getMeasuredHeight();

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
            return;
        }

        if (scrollListViewIfNeeded(currY)) {//don't move item when the ListView is scrolling
            return;
        }

        //reorder ListView
        final int curPosition = pointToPosition(((int) ev.getX()), ((int) ev.getY()));

        if (curPosition == AdapterView.INVALID_POSITION || curPosition == emptyPosition) {
            return;
        }

        moveItems(emptyPosition, curPosition, false);
    }

    /**
     * figure which item need move when empty position change from old to new
     *
     * @param oldEmptyPosition
     * @param newEmptyPosition
     */
    private void initNeedMoveItems(int oldEmptyPosition, int newEmptyPosition) {
        needMoveItems.clear();

        if (newEmptyPosition > oldEmptyPosition) {
            for (int i = oldEmptyPosition + 1; i <= newEmptyPosition; i++) {
                addItemIfNotNull(i);
            }
        } else {
            for (int i = newEmptyPosition; i < oldEmptyPosition; i++) {
                addItemIfNotNull(i);
            }
        }

    }

    private void addItemIfNotNull(int i) {
        View item = getChildAt(i - getFirstVisiblePosition());
        if (item != null) {
            needMoveItems.add(item);
        }
    }

    /**
     * move items to make empty position change from old to new, reset will be true if this method get called by handleUpEvent
     *
     * @param oldEmptyPosition
     * @param newEmptyPosition
     * @param reset
     */
    private void moveItems(final int oldEmptyPosition, final int newEmptyPosition, final boolean reset) {

        if (newEmptyPosition == oldEmptyPosition) {
            itemMovementEnd(oldEmptyPosition, newEmptyPosition, reset);
            return;
        }

        initNeedMoveItems(oldEmptyPosition, newEmptyPosition);

        itemAnimator = newEmptyPosition > oldEmptyPosition 
            ? ValueAnimator.ofFloat(0, -draggingItemHeight - getDividerHeight()) 
            : ValueAnimator.ofFloat(0, draggingItemHeight + getDividerHeight()); //when currPosition > srcPosition, move up itemView
        itemAnimator.setDuration(duration);
        itemAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                for (View needMoveItem : needMoveItems) {
                    needMoveItem.setTranslationY((Float) valueAnimator.getAnimatedValue());
                }
            }
        });
        itemAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                itemMovementEnd(oldEmptyPosition, newEmptyPosition, reset);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                itemMovementEnd(oldEmptyPosition, newEmptyPosition, reset);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        itemAnimator.start();
    }

    private void itemMovementEnd(final int oldEmptyPosition, final int newEmptyPosition, final boolean reset) {
        itemAnimator = null;

        if (reset) {
            ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(-1);
            //move item even if emptyPosition equals with dstPosition, because we want to refresh ListView
            ((CommonDragSortAdapter) getAdapter()).moveItem(oldEmptyPosition, newEmptyPosition);
            ((CommonDragSortAdapter) getAdapter()).notifyDataSetChanged();

            emptyPosition = -1;
            dragging = false;
            draggingItemView = null;
            if (draggingItemViewBitmap != null) {
                draggingItemViewBitmap.recycle();
                draggingItemViewBitmap = null;
            }
        } else {
            ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(newEmptyPosition);
            ((CommonDragSortAdapter) getAdapter()).moveItem(oldEmptyPosition, newEmptyPosition);
            ((CommonDragSortAdapter) getAdapter()).notifyDataSetChanged();

            emptyPosition = newEmptyPosition;
        }
    }

    private boolean scrollListViewIfNeeded(float y) {

        //the distance you want to scroll ListView
        int dy = 0;
        if (y < topBoundary && !reachTop()) {
            dy = (int) ((topBoundary - y) / 10);
        } else if (y > bottomBoundary && !reachBottom()) {
            dy = (int) ((bottomBoundary - y) / 10);
        }

        if (dy == 0) {
            //tell the event handler, i am not scrolling the ListView , you can move items if you want
            return false;
        } else {
            //tell the event handler, i am scrolling the ListView , do not move items
            setSelectionFromTop(getFirstVisiblePosition(), getChildAt(0).getTop()+dy);
            return true;
        }
    }

    private boolean reachTop() {
        if (getFirstVisiblePosition() == 0 && getChildAt(0).getTop() >= 0) {
            return true;
        }
        return false;
    }

    private boolean reachBottom() {
        if (getLastVisiblePosition() == getAdapter().getCount() - 1 && getChildAt(getChildCount() - 1).getBottom() <= getHeight()) {
            return true;
        }
        return false;
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
        int curPosition = pointToPosition(((int) ev.getX()), ((int) ev.getY()));
        int dstPosition = curPosition;
        if (curPosition == AdapterView.INVALID_POSITION) {
            dstPosition = emptyPosition;
        }

        moveItems(emptyPosition, dstPosition, true);
    }

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
