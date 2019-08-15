package com.lxfly2000.animeschedule;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    static final String keyNeedReload="need_reload";
    SettingsFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragment=new SettingsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,fragment).commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_settings,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:finish();return true;
            case R.id.action_restore_settings:return OnRestoreSettings();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean OnRestoreSettings(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.message_notice_title)
                .setMessage(R.string.message_overwrite_settings)
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    fragment.RestoreSettinngs();
                })
                .setNegativeButton(android.R.string.no,null)
                .show();
        return true;
    }

    @Override
    public void finish(){
        if(fragment.isUpdated()){
            Toast.makeText(this,R.string.message_settings_saved,Toast.LENGTH_LONG).show();
            Intent rIntent=new Intent();
            rIntent.putExtra(keyNeedReload,fragment.isNeedReload());
            setResult(RESULT_OK,rIntent);
        }
        super.finish();
    }
}
