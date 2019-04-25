package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.widget.*;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.AndroidUtility;
import com.lxfly2000.utilities.FileUtility;
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
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    String pkgName=Values.pkgNameBilibiliVersions[preferences.getInt(Values.keyBilibiliVersionIndex,Values.vDefaultBilibiliVersionIndex)];
                    for(int i_check=0;i_check<checkEpisodes.size();i_check++){
                        if(!checkEpisodes.get(i_check).isChecked())
                            continue;
                        try {
                            JSONObject jsonEntry = new JSONObject(Values.jsonRawBilibiliEntry);
                            JSONObject checkedEp=htmlJson.getJSONArray("episodes").getJSONObject(i_check);
                            jsonEntry.put("title",htmlJson.getString("title"));
                            jsonEntry.put("cover",htmlJson.getString("cover"));
                            jsonEntry.put("prefered_video_quality",Values.typeBilibiliPreferredVideoQualities[spinnerVideoQuality.getSelectedItemPosition()]);
                            jsonEntry.put("time_create_stamp",System.currentTimeMillis());
                            jsonEntry.put("time_update_stamp",System.currentTimeMillis());
                            jsonEntry.put("season_id",String.valueOf(htmlJson.getInt("season_id")));
                            jsonEntry.getJSONObject("ep").put("av_id",checkedEp.getInt("aid"));
                            jsonEntry.getJSONObject("ep").put("page",checkedEp.getInt("page"));
                            jsonEntry.getJSONObject("ep").put("danmaku",checkedEp.getInt("cid"));
                            jsonEntry.getJSONObject("ep").put("cover",checkedEp.getString("cover"));
                            jsonEntry.getJSONObject("ep").put("episode_id",checkedEp.getInt("ep_id"));
                            jsonEntry.getJSONObject("ep").put("index",checkedEp.getString("index"));
                            jsonEntry.getJSONObject("ep").put("index_title",checkedEp.getString("index_title"));
                            jsonEntry.getJSONObject("ep").put("from",checkedEp.getString("from"));
                            jsonEntry.getJSONObject("ep").put("season_type",htmlJson.getInt("season_type"));
                            FileUtility.WriteFile(GetBilibiliDownloadEntryPath(jsonEntry.getString("season_id"),
                                    String.valueOf(jsonEntry.getJSONObject("ep").getInt("episode_id"))),jsonEntry.toString());
                        }catch (JSONException e){
                            Toast.makeText(ctx,ctx.getString(R.string.message_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    Toast.makeText(ctx,R.string.message_bilibili_download_task_created,Toast.LENGTH_LONG).show();
                    AndroidUtility.KillProcess(ctx,pkgName);
                    if(checkOpenBilibili.isChecked()) {
                        try {
                            AndroidUtility.StartApplication(ctx, pkgName);
                        }catch (NullPointerException e){
                            Toast.makeText(ctx,ctx.getString(R.string.message_app_not_found,pkgName),Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel,null)
                .setNeutralButton(R.string.button_bilibili_download_sysdown,(dialogInterface, i) -> {
                    Toast.makeText(ctx,"TODO: 该功能正在制作中。",Toast.LENGTH_LONG).show();
                })
                .setView(R.layout.dialog_bilibili_download)
                .show();
        checkOpenBilibili= dialog.findViewById(R.id.checkOpenBilibili);
        spinnerVideoQuality= dialog.findViewById(R.id.spinnerVideoQuality);
        linearLayout= dialog.findViewById(R.id.linearLayoutEpisodes);
        buttonOk= dialog.findViewById(android.R.id.button1);
        buttonOk.setEnabled(false);
        spinnerVideoQuality.setSelection(3);
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    dialog.dismiss();
                    Toast.makeText(ctx,R.string.message_unable_to_fetch_anime_info,Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    String jsonString = StreamUtility.GetStringFromStream(stream);
                    if (jsonString == null) {
                        Toast.makeText(ctx, R.string.message_bilibili_ssid_code_not_found, Toast.LENGTH_LONG).show();
                        return;
                    }
                    htmlJson = new JSONObject(jsonString).getJSONObject("result");
                    try {
                        String animeTitle = htmlJson.getString("title");
                        dialog.setTitle(animeTitle);
                        if (animeTitle.contains("僅"))
                            buttonOk.setText(R.string.message_bilibili_download_region_restricted_warning);
                    }catch (JSONException e){/*Ignore*/}
                    JSONArray epArray=htmlJson.getJSONArray("episodes");
                    for(int i=0;i<epArray.length();i++){
                        CheckBox checkBox=new CheckBox(dialog.getContext());
                        try {
                            JSONObject epObject = epArray.getJSONObject(i);
                            checkBox.setText("[" + epObject.getString("index") + "] " + epObject.getString("index_title"));
                        }catch (JSONException e){
                            checkBox.setText("["+(i+1)+"] ("+e.getLocalizedMessage()+")");
                        }
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
        task.execute("https://bangumi.bilibili.com/view/web_api/season?season_id="+ssid);
    }
}
