package com.diandian.coolco.dragsortlistview;

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
        scrollDownBoundary = getHeight() * 0.25f;
        scrollUpBoundary = getHeight() * 0.75f;

        draggingItemViewBitmapPaint = new Paint();
        draggingItemViewBitmapPaint.setAlpha(0x88);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
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
        if (downPosition == AdapterView.INVALID_POSITION){
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
        if (!dragHandlerHitRect.contains(((int) downX), ((int) downY))){

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

        scrollListView(currY);
//        scrollListBy();
//        smoothScrollBy();

//        reorderListView(ev);

        //reorder ListView
        int currPosition = pointToPosition(((int) ev.getX()), ((int) ev.getY()));
        if (currPosition == AdapterView.INVALID_POSITION || currPosition == srcPosition){
            return;
        }
        ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(currPosition);
        ((CommonDragSortAdapter) getAdapter()).moveItem(srcPosition, currPosition);
        srcPosition = currPosition;
    }

    private void scrollListView(float y) {
        //the distance you want to scroll ListView
        float dy = 0;
        if (y < scrollDownBoundary){
            dy = (scrollDownBoundary-y) / 10;
        } else if (y > scrollUpBoundary){
            dy = (scrollUpBoundary-y) / 10;
        }

        if (dy == 0){
            return;
        }

        //scroll down
        if (dy > 0){
            View firstVisibleItemView = getChildAt(0);
            setSelectionFromTop(getFirstVisiblePosition(), ((int) (firstVisibleItemView.getTop() + dy)));
            return;
        }

        //scroll up
        if (dy < 0){
            View lastVisibleItemView = getChildAt(getLastVisiblePosition()-getFirstVisiblePosition()-1);
            setSelectionFromTop(getLastVisiblePosition(), ((int) (lastVisibleItemView.getTop() + dy)));
            return;
        }
    }


    private void handleUpEvent(MotionEvent ev) {

        // reset
        dragging = false;
        draggingItemViewBitmap = null;
        draggingItemView.setVisibility(VISIBLE);
        draggingItemView = null;

//        reorderListView(ev);
        //reorder ListView
        int currPosition = pointToPosition(((int) ev.getX()), ((int) ev.getY()));
        int dstPosition = currPosition;
        if (currPosition == AdapterView.INVALID_POSITION){
            dstPosition = srcPosition;
        }
        ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(-1);

        //move item even if srcPosition equals with dstPosition, because we want to refresh ListView
        ((CommonDragSortAdapter) getAdapter()).moveItem(srcPosition, dstPosition);
        srcPosition = -1;
    }

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
            //ACTION_UP
            ((CommonDragSortAdapter) getAdapter()).setDragSrcPosition(-1);
            ((CommonDragSortAdapter) getAdapter()).moveItem(srcPosition, currPosition);
            srcPosition = -1;
//            invalidate();
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dragging) {
            canvas.drawBitmap(draggingItemViewBitmap, draggingItemViewRect.left, draggingItemViewRect.top, draggingItemViewBitmapPaint);
        }
    }

    public static Bitmap getBitmapFromView(View view){
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
