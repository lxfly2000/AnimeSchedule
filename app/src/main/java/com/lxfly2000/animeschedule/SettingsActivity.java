package com.lxfly2000.animeschedule;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private boolean modified;
    private Spinner spinnerSortMethods,spinnerSortOrder,spinnerBilibiliVersions;
    private EditText editBilibiliSavePath;
    private CheckBox checkSeperateAbandoned;
    static final String keyNeedReload="need_reload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.buttonSaveSettings).setOnClickListener(buttonCallbacks);
        preferences=Values.GetPreference(this);
        spinnerSortMethods=(Spinner)findViewById(R.id.spinnerSortMethod);
        spinnerSortOrder=(Spinner)findViewById(R.id.spinnerSortOrder);
        spinnerBilibiliVersions=(Spinner)findViewById(R.id.spinnerBilibiliVersions);
        checkSeperateAbandoned=(CheckBox)findViewById(R.id.checkBoxSeperateAbandoned);
        editBilibiliSavePath=(EditText)findViewById(R.id.editBilibiliDownloadPath);

        spinnerSortMethods.setOnItemSelectedListener(spinnerSelectListener);
        spinnerSortOrder.setOnItemSelectedListener(spinnerSelectListener);
        spinnerBilibiliVersions.setOnItemSelectedListener(spinnerSelectListener);
        checkSeperateAbandoned.setOnClickListener(buttonCallbacks);
        editBilibiliSavePath.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Nothing.
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                modified=true;
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //Nothing.
            }
        });
        LoadSettings();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_settings,menu);
        return true;
    }

    private View.OnClickListener buttonCallbacks=new View.OnClickListener() {
        @Override
        public void onClick(View view){
            switch (view.getId()){
                case R.id.buttonSaveSettings:OnSaveSettings(true);break;
                case R.id.checkBoxSeperateAbandoned:modified=true;break;
            }
        }
    };

    private int originalMethod=-1,originalOrder=-1,originalBilibiliVersion=-1;
    private AdapterView.OnItemSelectedListener spinnerSelectListener=new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (((Spinner)parent).getId()){
                case R.id.spinnerSortMethod:
                    if(originalMethod!=-1&&originalMethod!=position)
                        modified=true;
                    originalMethod=position;
                    break;
                case R.id.spinnerSortOrder:
                    if(originalOrder!=-1&&originalOrder!=position)
                        modified=true;
                    originalOrder=position;
                    break;
                case R.id.spinnerBilibiliVersions:
                    if(originalBilibiliVersion!=-1&&originalBilibiliVersion!=position)
                        modified=true;
                    originalBilibiliVersion=position;
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //Nothing.
        }
    };

    private void LoadSettings(){
        spinnerSortOrder.setSelection(preferences.getInt(Values.keySortOrder,Values.vDefaultSortOrder));
        spinnerSortMethods.setSelection(preferences.getInt(Values.keySortMethod,Values.vDefaultSortMethod));
        checkSeperateAbandoned.setChecked(preferences.getBoolean(Values.keySortSeperateAbandoned,Values.vDefaultSortSeperateAbandoned));
        spinnerBilibiliVersions.setSelection(preferences.getInt(Values.keyBilibiliVersionIndex,Values.vDefaultBilibiliVersion));
        editBilibiliSavePath.setText(preferences.getString(Values.keyBilibiliSavePath,Values.GetvDefaultBilibiliSavePath(this)));
        modified=false;
    }

    private boolean OnSaveSettings(boolean finishActivity){
        SharedPreferences.Editor wPreference=preferences.edit();
        wPreference.putInt(Values.keySortOrder,spinnerSortOrder.getSelectedItemPosition());
        wPreference.putInt(Values.keySortMethod,spinnerSortMethods.getSelectedItemPosition());
        wPreference.putBoolean(Values.keySortSeperateAbandoned,checkSeperateAbandoned.isChecked());
        wPreference.putInt(Values.keyBilibiliVersionIndex,spinnerBilibiliVersions.getSelectedItemPosition());
        wPreference.putString(Values.keyBilibiliSavePath,editBilibiliSavePath.getText().toString());
        wPreference.apply();
        modified=false;
        Toast.makeText(this,R.string.message_settings_saved,Toast.LENGTH_LONG).show();
        Intent rIntent=new Intent();
        rIntent.putExtra(keyNeedReload,true);
        setResult(RESULT_OK,rIntent);
        if(finishActivity)
            finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:return OnBackButton();
            case R.id.action_save:return OnSaveSettings(false);
            case R.id.action_restore_settings:return OnRestoreSettings();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean OnRestoreSettings(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.message_notice_title)
                .setMessage(R.string.message_overwrite_settings)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        originalMethod=originalOrder=-1;
                        ClearSettings();
                        LoadSettings();
                        OnSaveSettings(false);
                    }
                })
                .setNegativeButton(android.R.string.no,null)
                .show();
        return true;
    }

    private void ClearSettings(){
        preferences.edit().clear().apply();
        Toast.makeText(this,R.string.message_settings_cleared,Toast.LENGTH_LONG).show();
    }

    private boolean OnBackButton(){
        finish();
        return true;
    }

    @Override
    public void finish(){
        if(modified){
            new AlertDialog.Builder(this)
                    .setTitle(R.string.message_notice_title)
                    .setMessage(R.string.message_lose_changes)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            modified=false;
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.no,null)
                    .show();
        }else {
            super.finish();
        }
    }
}
