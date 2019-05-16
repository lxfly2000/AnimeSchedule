package com.lxfly2000.animeschedule;

import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class TestAvailabilityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_availability);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_test_availability,menu);
        menu.findItem(R.id.action_retest).getIcon().setTint(Color.WHITE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_show_bilibili:
                return true;
            case R.id.action_show_iqiyi:
                return true;
            case R.id.action_show_qqvideo:
                return true;
            case R.id.action_show_youku:
                return true;
            case R.id.action_show_etc:
                return true;
            case R.id.action_show_available:
                return true;
            case R.id.action_show_unavailable:
                return true;
            case R.id.action_retest:
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
