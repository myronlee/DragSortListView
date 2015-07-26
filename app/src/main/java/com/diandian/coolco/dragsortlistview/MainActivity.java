package com.diandian.coolco.dragsortlistview;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        ListView testListView = (ListView) findViewById(R.id.lv_test);
//        String[] testTitleStrArray = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19"};
//        new ArrayList<String>(Arrays.asList(testTitleStrArray))
        List<String> testTitleStrList = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            testTitleStrList.add(i+"");
        }
        BaseAdapter adapter = new CommonDragSortAdapter<>(this, R.layout.list_item, testTitleStrList, TitleViewHolder.class);
//        BaseAdapter adapter = new DragSortListViewAdapter(this, testTitleStrList);
        testListView.setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    static class TitleViewHolder extends CommonDragSortAdapter.CommonViewHolder<String>{
        private TextView titleTextView;

        public TitleViewHolder(View convertView){
            titleTextView = (TextView) convertView.findViewById(R.id.tv_title);
        }

        @Override
        public void setItem(String item) {
            titleTextView.setText(item);
        }
    }

    class DragSortListViewAdapter extends BaseAdapter{

        private Context context;
        private LayoutInflater inflater;
        private List<String> datas;

        private int invisblePosition = -1;

        public DragSortListViewAdapter(Context context, List<String> datas) {
            this.context = context;
            this.datas = datas;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            return datas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = inflater.inflate(R.layout.list_item, parent, false);

            if(position == invisblePosition){
                itemView.setVisibility(View.INVISIBLE);
                return itemView;
            }

            TextView titleTextView = (TextView) itemView.findViewById(R.id.tv_title);
            titleTextView.setText((CharSequence) getItem(position));
            return itemView;
        }

        public void setInvisblePosition(int invisblePosition) {
            this.invisblePosition = invisblePosition;
        }
    }
}
