package com.lxfly2000.animeschedule;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private EditText editRepositoryUrl,editBranch,editDisplayName,editUserName,editMail,editPassword;
    private boolean modified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.buttonSaveSettings).setOnClickListener(buttonCallbacks);
        preferences=getPreferences(MODE_PRIVATE);
        editRepositoryUrl=(EditText)findViewById(R.id.editRepositoryUrl);
        editRepositoryUrl.addTextChangedListener(editCallback);
        editBranch=(EditText)findViewById(R.id.editRepositoryBranch);
        editBranch.addTextChangedListener(editCallback);
        editDisplayName=(EditText)findViewById(R.id.editDisplayName);
        editDisplayName.addTextChangedListener(editCallback);
        editUserName=(EditText)findViewById(R.id.editUserName);
        editUserName.addTextChangedListener(editCallback);
        editMail=(EditText)findViewById(R.id.editMail);
        editMail.addTextChangedListener(editCallback);
        editPassword=(EditText)findViewById(R.id.editPassword);
        editPassword.addTextChangedListener(editCallback);
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
            }
        }
    };

    private TextWatcher editCallback=new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //Nothing.
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //Nothing.
        }

        @Override
        public void afterTextChanged(Editable editable) {
            modified=true;
        }
    };

    private void LoadSettings(){
        editRepositoryUrl.setText(preferences.getString(Values.keyRepositoryUrl,Values.vdRepositoryUrl));
        editBranch.setText(preferences.getString(Values.keyBranch,Values.vdBranch));
        editMail.setText(preferences.getString(Values.keyMail,Values.vdMail));
        editDisplayName.setText(preferences.getString(Values.keyDisplayName,Values.vdDisplayName));
        editUserName.setText(preferences.getString(Values.keyUserName,Values.vdUserName));
        editPassword.setText(preferences.getString(Values.keyPassword,Values.vdPassword));
        modified=false;
    }

    private boolean OnTestConnection(){
        //TODO
        String msg="TODO：测试可否连接至远程仓库，可否下载，有无anime.json文件。\n本地存储位置："+Values.GetJsonDataFullPath();
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
        return true;
    }

    private boolean OnSaveSettings(boolean finishActivity){
        SharedPreferences.Editor wPreference=preferences.edit();
        wPreference.putString(Values.keyRepositoryUrl,editRepositoryUrl.getText().toString());
        wPreference.putString(Values.keyBranch,editBranch.getText().toString());
        wPreference.putString(Values.keyMail,editMail.getText().toString());
        wPreference.putString(Values.keyDisplayName,editDisplayName.getText().toString());
        wPreference.putString(Values.keyUserName,editUserName.getText().toString());
        wPreference.putString(Values.keyPassword,editPassword.getText().toString());
        wPreference.apply();
        modified=false;
        Toast.makeText(this,R.string.message_settings_saved,Toast.LENGTH_LONG).show();
        if(finishActivity)
            finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:return OnBackButton();
            case R.id.action_save:return OnSaveSettings(false);
            case R.id.action_test_connection:return OnTestConnection();
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
                        ClearSettings();
                        LoadSettings();
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
