package com.diandian.coolco.dragsortlistview;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * @param <T>
 */
public class CommonDragSortAdapter<T> extends BaseAdapter {
    protected static final String TAG = CommonDragSortAdapter.class.getSimpleName();
    ;

    private Context context;
    private int layout;
    private Class<? extends CommonViewHolder<T>> viewHolderClazz;
    private List<T> datas;

    private int dragSrcPosition = -1;


    /**
     * @param context
     * @param layout
     * @param datas
     * @param clazz
     */
    public CommonDragSortAdapter(Context context, int layout, List<T> datas, Class<? extends CommonViewHolder<T>> clazz) {
        super();
        this.context = context;
        this.layout = layout;
        this.datas = datas;
        this.viewHolderClazz = clazz;
    }

    /**
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        CommonViewHolder<T> holder = null;
//        if (convertView == null) {
        convertView = LayoutInflater.from(context).inflate(layout, parent, false);

        if (position == dragSrcPosition) {
            convertView.setVisibility(View.INVISIBLE);
            return convertView;
        }

        try {
            holder = viewHolderClazz.getDeclaredConstructor(View.class).newInstance(convertView);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }

//        convertView.setTag(holder);
//        } else {
//            holder = (CommonViewHolder<T>) convertView.getTag();
//        }
        holder.setItem(datas.get(position));

        convertView.setTag(position);

        return convertView;
    }

    /**
     * @return
     */
    @Override
    public int getCount() {
        return datas.size();
    }

    /**
     * @param position
     * @return
     */
    @Override
    public Object getItem(int position) {
        return null;
    }

    /**
     * @param position
     * @return
     */
    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void moveItem(int srcPosition, int dstPosition) {
        if (srcPosition == dstPosition){
            notifyDataSetChanged();
//            notifyDataSetInvalidated();
            return;
        }
        T movingItem = datas.get(srcPosition);
        datas.remove(srcPosition);
        datas.add(dstPosition, movingItem);
//        notifyDataSetChanged();
    }

    /**
     * @param <T>
     */
    public static abstract class CommonViewHolder<T> {
        /**
         * @param item
         */
        public abstract void setItem(T item);
    }

    public void setDragSrcPosition(int dragSrcPosition) {
        this.dragSrcPosition = dragSrcPosition;
    }
}