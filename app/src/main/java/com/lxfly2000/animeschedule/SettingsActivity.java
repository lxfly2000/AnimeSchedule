package com.lxfly2000.animeschedule;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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
    private SharedPreferences preferences;
    private boolean modified;
    private Spinner spinnerSortMethods,spinnerSortOrder,spinnerBilibiliVersions;
    private TextView textBilibiliSavePath;
    private CheckBox checkSeperateAbandoned;
    private RadioGroup radiosStarMark,radioBilbiliDownloadApis;
    private final List<Integer> radiosId=Arrays.asList(R.id.radioStarMarkStar,R.id.radioStarMarkBall);
    static final String keyNeedReload="need_reload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.buttonSaveSettings).setOnClickListener(buttonCallbacks);
        preferences=Values.GetPreference(this);
        spinnerSortMethods= findViewById(R.id.spinnerSortMethod);
        spinnerSortOrder= findViewById(R.id.spinnerSortOrder);
        spinnerBilibiliVersions= findViewById(R.id.spinnerBilibiliVersions);
        checkSeperateAbandoned= findViewById(R.id.checkBoxSeperateAbandoned);
        textBilibiliSavePath= findViewById(R.id.textBilibiliSavePath);
        radiosStarMark= findViewById(R.id.radiosStarMark);
        radioBilbiliDownloadApis=findViewById(R.id.radioBilibiliDownloadApis);
        RadioGroup.LayoutParams radioLayout=new RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        radioLayout.weight=1.0f;
        for(int i=0;i<BilibiliQueryInfo.queryMethodCount;i++){
            RadioButton radio=new RadioButton(this);
            radio.setText(String.valueOf(i+1));
            radioBilbiliDownloadApis.addView(radio,i,radioLayout);
        }
        for(int i=0;i<radiosId.size();i++)
            ((RadioButton)findViewById(radiosId.get(i))).setText(Values.starMarks[i]);

        spinnerSortMethods.setOnItemSelectedListener(spinnerSelectListener);
        spinnerSortOrder.setOnItemSelectedListener(spinnerSelectListener);
        spinnerBilibiliVersions.setOnItemSelectedListener(spinnerSelectListener);
        checkSeperateAbandoned.setOnClickListener(buttonCallbacks);
        radiosStarMark.setOnCheckedChangeListener((radioGroup, i) -> modified=true);
        radioBilbiliDownloadApis.setOnCheckedChangeListener(((radioGroup, i) -> modified=true));
        LoadSettings();
    }

    private void SetBilibiliSavePathText(String text){
        //https://www.jianshu.com/p/29a379512a13
        SpannableString spanString=new SpannableString(text);
        spanString.setSpan(new UnderlineSpan(),0,text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new ForegroundColorSpan(Color.RED),0,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                OpenBrowseDialog();
            }
        },0,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textBilibiliSavePath.setText(spanString);
        textBilibiliSavePath.setMovementMethod(LinkMovementMethod.getInstance());
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

    private void OpenBrowseDialog(){
        //https://github.com/hedzr/android-file-chooser#choose-a-folder
        new ChooserDialog(this)
                .displayPath(true)
                .withFilter(true,false)
                .withStartFile(textBilibiliSavePath.getText().toString())
                .withResources(R.string.label_bilibili_download_path,android.R.string.ok,android.R.string.cancel)
                .withChosenListener((s, file) -> {
                    SetBilibiliSavePathText(s);
                    modified=true;
                }).build().show();
    }

    private int originalMethod=-1,originalOrder=-1,originalBilibiliVersion=-1;
    private AdapterView.OnItemSelectedListener spinnerSelectListener=new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (parent.getId()){
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
        spinnerBilibiliVersions.setSelection(preferences.getInt(Values.keyBilibiliVersionIndex,Values.vDefaultBilibiliVersionIndex));
        SetBilibiliSavePathText(preferences.getString(Values.keyBilibiliSavePath,Values.GetvDefaultBilibiliSavePath(this)));
        radiosStarMark.check(radiosId.get(preferences.getInt(Values.keyStarMark,Values.vDefaultStarMark)));
        ((RadioButton)radioBilbiliDownloadApis.getChildAt(preferences.getInt(Values.keyApiMethod,Values.vDefaultApiMethod))).toggle();
        modified=false;
    }

    private boolean OnSaveSettings(boolean finishActivity){
        SharedPreferences.Editor wPreference=preferences.edit();
        wPreference.putInt(Values.keySortOrder,spinnerSortOrder.getSelectedItemPosition());
        wPreference.putInt(Values.keySortMethod,spinnerSortMethods.getSelectedItemPosition());
        wPreference.putBoolean(Values.keySortSeperateAbandoned,checkSeperateAbandoned.isChecked());
        wPreference.putInt(Values.keyBilibiliVersionIndex,spinnerBilibiliVersions.getSelectedItemPosition());
        wPreference.putString(Values.keyBilibiliSavePath,textBilibiliSavePath.getText().toString());
        wPreference.putInt(Values.keyStarMark, radiosId.indexOf(radiosStarMark.getCheckedRadioButtonId()));
        for(int i=0;i<radioBilbiliDownloadApis.getChildCount();i++){
            if(((RadioButton)radioBilbiliDownloadApis.getChildAt(i)).isChecked()){
                wPreference.putInt(Values.keyApiMethod,i);
                break;
            }
        }
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
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    originalMethod=originalOrder=-1;
                    ClearSettings();
                    LoadSettings();
                    OnSaveSettings(false);
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
                    .setIcon(R.drawable.ic_warning_black_24dp)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        modified=false;
                        finish();
                    })
                    .setNegativeButton(android.R.string.no,null)
                    .show();
        }else {
            super.finish();
        }
    }
}
