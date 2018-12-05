package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.*;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.AndroidUtility;
import com.lxfly2000.utilities.StreamUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class BilibiliDownloadDialog {
    private Context ctx;
    private SharedPreferences preferences;
    BilibiliDownloadDialog(@NonNull Context context){
        ctx=context;
        preferences=Values.GetPreference(ctx);
        checkEpisodes=new ArrayList<>();
    }

    CheckBox checkOpenBilibili;
    ArrayList<CheckBox> checkEpisodes;
    Spinner spinnerVideoQuality;
    Button buttonOk;
    LinearLayout linearLayout;

    JSONObject htmlJson;

    private CompoundButton.OnCheckedChangeListener checkChangeListener=new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            int checkedCount=0;
            for (CheckBox checkBox : checkEpisodes) {
                if (checkBox.isChecked())
                    checkedCount++;
            }
            buttonOk.setEnabled(checkedCount>0);
        }
    };

    private String GetBilibiliDownloadEntryPath(String ssid,String epid){
        return preferences.getString(Values.keyBilibiliSavePath,Values.GetAppDataPathExternal(ctx))
                .concat("/").concat(Values.pkgNameBilibiliVersions[preferences.getInt(Values.keyBilibiliVersionIndex,Values.vDefaultBilibiliVersionIndex)])
                .concat("/download/s_").concat(ssid).concat("/").concat(epid).concat("/entry.json");
    }

    void OpenDownloadDialog(AnimeJson json,int index){
        final String ssid=URLUtility.GetBilibiliSeasonIdString(json.GetWatchUrl(index));
        if(ssid==null){
            Toast.makeText(ctx,R.string.message_bilibili_ssid_not_found,Toast.LENGTH_LONG).show();
            return;
        }
        final AlertDialog dialog=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String pkgName=Values.pkgNameBilibiliVersions[preferences.getInt(Values.keyBilibiliVersionIndex,Values.vDefaultBilibiliVersionIndex)];
                        Toast.makeText(ctx,"TODO:这个功能正在制作中。",Toast.LENGTH_LONG).show();
                        AndroidUtility.KillProcess(ctx,pkgName);
                        if(checkOpenBilibili.isChecked()) {
                            try {
                                AndroidUtility.StartApplication(ctx, pkgName);
                            }catch (NullPointerException e){
                                Toast.makeText(ctx,ctx.getString(R.string.message_app_not_found,pkgName),Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel,null)
                .setView(R.layout.dialog_bilibili_download)
                .show();
        checkOpenBilibili=(CheckBox)dialog.findViewById(R.id.checkOpenBilibili);
        spinnerVideoQuality=(Spinner)dialog.findViewById(R.id.spinnerVideoQuality);
        linearLayout=(LinearLayout)dialog.findViewById(R.id.linearLayoutEpisodes);
        buttonOk=(Button)dialog.findViewById(android.R.id.button1);
        buttonOk.setEnabled(false);
        spinnerVideoQuality.setSelection(4);
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    dialog.dismiss();
                    Toast.makeText(ctx,R.string.message_unable_to_fetch_anime_info,Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    String jsonString = URLUtility.GetBilibiliJsonContainingSSID(StreamUtility.GetStringFromStream(stream), ssid);
                    if (jsonString == null) {
                        Toast.makeText(ctx, R.string.message_bilibili_ssid_code_not_found, Toast.LENGTH_LONG).show();
                        return;
                    }
                    htmlJson = new JSONObject(jsonString);
                    dialog.setTitle(htmlJson.getJSONObject("mediaInfo").getString("title"));
                    JSONArray epArray=htmlJson.getJSONArray("epList");
                    for(int i=0;i<epArray.length();i++){
                        CheckBox checkBox=new CheckBox(dialog.getContext());
                        JSONObject epObject=epArray.getJSONObject(i);
                        checkBox.setText("["+epObject.getString("index")+"] "+epObject.getString("index_title"));
                        checkBox.setOnCheckedChangeListener(checkChangeListener);
                        checkEpisodes.add(checkBox);
                        linearLayout.addView(checkBox);
                    }
                }catch (JSONException e){
                    Toast.makeText(ctx,ctx.getString(R.string.message_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }catch (IOException e){
                    Toast.makeText(ctx,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }
            }
        };
        task.execute("https://www.bilibili.com/bangumi/play/ss"+ssid);
    }
}
