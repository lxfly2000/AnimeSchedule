package com.lxfly2000.animeschedule;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import com.obsez.android.lib.filechooser.ChooserDialog;

public class SettingsFragment extends PreferenceFragment {
    SharedPreferences sharedPreferences;
    boolean updated=false;

    ListPreference enumSortMethod,enumSortOrder;
    CheckBoxPreference checkSeperate;
    Preference prefBilibiliSavePath;
    ListPreference enumBilibiliClient,enumBilibiliApi;
    ListPreference enumStarMark;
    EditTextPreference numConnectionTimeOut,numReadTimeOut,numMaxRedirect;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences=Values.GetPreference(getActivity());
        StringizeSettings();
        addPreferencesFromResource(R.xml.pref_application);
        ConvertIntSettings();

        enumSortMethod=(ListPreference)findPreference(getString(R.string.key_sort_method));
        enumSortOrder=(ListPreference)findPreference(getString(R.string.key_sort_order));
        checkSeperate=(CheckBoxPreference)findPreference(getString(R.string.key_sort_separate_abandoned));
        prefBilibiliSavePath=findPreference(getString(R.string.key_bilibili_save_path));
        enumBilibiliClient=(ListPreference)findPreference(getString(R.string.key_bilibili_version_index));
        enumBilibiliApi=(ListPreference)findPreference(getString(R.string.key_api_method));
        enumStarMark=(ListPreference)findPreference(getString(R.string.key_star_mark));
        numConnectionTimeOut=(EditTextPreference)findPreference(getString(R.string.key_test_connection_timeout));
        numReadTimeOut=(EditTextPreference)findPreference(getString(R.string.key_test_read_timeout));
        numMaxRedirect=(EditTextPreference)findPreference(getString(R.string.key_redirect_max_count));

        String[]stringArray=new String[getResources().getStringArray(R.array.list_sort_methods).length];
        for(int i=0;i<stringArray.length;i++)
            stringArray[i]=String.valueOf(i);
        enumSortMethod.setEntryValues(stringArray);
        stringArray=new String[getResources().getStringArray(R.array.list_sort_order).length];
        for(int i=0;i<stringArray.length;i++)
            stringArray[i]=String.valueOf(i);
        enumSortOrder.setEntryValues(stringArray);
        stringArray=new String[getResources().getStringArray(R.array.pkg_name_bilibili_versions).length];
        for(int i=0;i<stringArray.length;i++)
            stringArray[i]=String.valueOf(i);
        enumBilibiliClient.setEntryValues(stringArray);
        stringArray=new String[BilibiliQueryInfo.queryMethodCount];
        for(int i=0;i<stringArray.length;i++)
            stringArray[i]=String.valueOf(i+1);
        enumBilibiliApi.setEntries(stringArray);
        stringArray=new String[BilibiliQueryInfo.queryMethodCount];
        for(int i=0;i<stringArray.length;i++)
            stringArray[i]=String.valueOf(i);
        enumBilibiliApi.setEntryValues(stringArray);
        stringArray=new String[getResources().getStringArray(R.array.star_marks).length];
        for(int i=0;i<stringArray.length;i++)
            stringArray[i]=String.valueOf(i);
        enumStarMark.setEntryValues(stringArray);
        prefBilibiliSavePath.setOnPreferenceClickListener(preference -> {
            //https://github.com/hedzr/android-file-chooser#choose-a-folder
            new ChooserDialog(this)
                    .displayPath(true)
                    .withFilter(true,false)
                    .withStartFile(preference.getSummary().toString())
                    .withResources(R.string.label_bilibili_download_path,android.R.string.ok,android.R.string.cancel)
                    .withChosenListener((s, file) -> {
                        onChangeListener.onPreferenceChange(preference,s);
                    }).build().show();
            return true;
        });
        enumSortMethod.setOnPreferenceChangeListener(onChangeListener);
        enumSortOrder.setOnPreferenceChangeListener(onChangeListener);
        checkSeperate.setOnPreferenceChangeListener(onChangeListener);
        enumBilibiliClient.setOnPreferenceChangeListener(onChangeListener);
        enumBilibiliApi.setOnPreferenceChangeListener(onChangeListener);
        enumStarMark.setOnPreferenceChangeListener(onChangeListener);
        numConnectionTimeOut.setOnPreferenceChangeListener(onChangeListener);
        numReadTimeOut.setOnPreferenceChangeListener(onChangeListener);
        numMaxRedirect.setOnPreferenceChangeListener(onChangeListener);

        PresentSettings();
    }

    Preference.OnPreferenceChangeListener onChangeListener=(preference, o) -> {
        if(preference instanceof CheckBoxPreference) {
            sharedPreferences.edit().putBoolean(preference.getKey(), (boolean) o).apply();
        }else if(preference instanceof EditTextPreference||preference instanceof ListPreference){
            try {
                sharedPreferences.edit().putInt(preference.getKey(), Integer.parseInt((String) o)).apply();
            }catch (NumberFormatException e){
                return false;
            }
        }else{
            sharedPreferences.edit().putString(preference.getKey(),(String)o).apply();
        }
        PresentSettings();
        updated=true;
        return true;
    };

    public boolean isUpdated() {
        return updated;
    }

    public void RestoreSettinngs(){
        sharedPreferences.edit().clear().apply();
        StringizeSettings();
        sharedPreferences.edit()
                .putString(getString(R.string.key_bilibili_save_path),Values.GetvDefaultBilibiliSavePath(getActivity()))
                .putBoolean(getString(R.string.key_sort_separate_abandoned),Values.vDefaultSortSeperateAbandoned)
                .apply();
        ConvertIntSettings();
        PresentSettings();
        updated=true;
    }

    private void PresentSettings(){
        PresentSettings(enumSortMethod,Values.vDefaultSortMethod);
        PresentSettings(enumSortOrder,Values.vDefaultSortOrder);
        PresentSettings(checkSeperate,Values.vDefaultSortSeperateAbandoned);
        PresentSettings(prefBilibiliSavePath,Values.GetvDefaultBilibiliSavePath(getActivity()));
        PresentSettings(enumBilibiliClient,Values.vDefaultBilibiliVersionIndex);
        PresentSettings(enumBilibiliApi,Values.vDefaultApiMethod);
        PresentSettings(enumStarMark,Values.vDefaultStarMark);
        PresentSettings(numConnectionTimeOut,Values.vDefaultTestConnectionTimeout);
        PresentSettings(numReadTimeOut,Values.vDefaultTestReadTimeout);
        PresentSettings(numMaxRedirect,Values.vDefaultRedirectMaxCount);
    }

    private void PresentSettings(Preference p,Object def){
        if(p instanceof CheckBoxPreference){
            ((CheckBoxPreference) p).setChecked(sharedPreferences.getBoolean(p.getKey(),(boolean)def));
        }else if(p instanceof EditTextPreference) {
            ((EditTextPreference) p).setText(String.valueOf(sharedPreferences.getInt(p.getKey(),(int)def)));
            p.setSummary(((EditTextPreference) p).getText());
        }else if(p instanceof ListPreference){
            ((ListPreference) p).setValueIndex(sharedPreferences.getInt(p.getKey(),(int)def));
            p.setSummary(((ListPreference) p).getEntries()[sharedPreferences.getInt(p.getKey(),(int)def)]);
        }else{
            p.setSummary(sharedPreferences.getString(p.getKey(),(String)def));
        }
    }

    int stringizeKeyId[]={
            R.string.key_sort_method,
            R.string.key_sort_order,
            R.string.key_bilibili_version_index,
            R.string.key_api_method,
            R.string.key_star_mark,
            R.string.key_test_connection_timeout,
            R.string.key_test_read_timeout,
            R.string.key_redirect_max_count
    };
    int defValues[]={
            Values.vDefaultSortMethod,
            Values.vDefaultSortOrder,
            Values.vDefaultBilibiliVersionIndex,
            Values.vDefaultApiMethod,
            Values.vDefaultStarMark,
            Values.vDefaultTestConnectionTimeout,
            Values.vDefaultTestReadTimeout,
            Values.vDefaultRedirectMaxCount
    };

    private void StringizeSettings(){
        for (int i=0;i<stringizeKeyId.length;i++) {
            sharedPreferences.edit().putString(getString(stringizeKeyId[i]),String.valueOf(sharedPreferences.getInt(getString(stringizeKeyId[i]),defValues[i]))).apply();
        }
    }

    private void ConvertIntSettings(){
        for(int i=0;i<stringizeKeyId.length;i++){
            sharedPreferences.edit().putInt(getString(stringizeKeyId[i]),Integer.parseInt(sharedPreferences.getString(getString(stringizeKeyId[i]),String.valueOf(defValues[i])))).apply();
        }
    }
}
