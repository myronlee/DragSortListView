package com.diandian.coolco.dragsortlistview;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        ListView testListView = (ListView) findViewById(R.id.lv_test);
        String[] testTitleStrArray = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19"};
        BaseAdapter adapter = new CommonDragSortAdapter<>(this, R.layout.list_item, new ArrayList<String>(Arrays.asList(testTitleStrArray)), TitleViewHolder.class);
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
}
