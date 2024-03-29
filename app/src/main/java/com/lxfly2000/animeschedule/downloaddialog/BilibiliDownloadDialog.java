package com.lxfly2000.animeschedule.downloaddialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.widget.*;
import com.lxfly2000.animeschedule.*;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.AndroidUtility;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.StreamUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;

public class BilibiliDownloadDialog extends DownloadDialog{
    private SharedPreferences preferences;
    public BilibiliDownloadDialog(@NonNull Context context){
        super(context);
        preferences= Values.GetPreference(ctx);
        checkEpisodes=new ArrayList<>();
    }

    CheckBox checkOpenBilibili;
    ArrayList<CheckBox> checkEpisodes;
    Spinner spinnerVideoQuality;
    Button buttonOk,buttonNeutral;
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
            buttonNeutral.setEnabled(buttonOk.isEnabled());
        }
    };

    @Override
    public void OpenDownloadDialog(AnimeJson json, int index){
        final String ssid= URLUtility.GetBilibiliSeasonIdString(json.GetWatchUrl(index));
        if(ssid==null){
            Toast.makeText(ctx, R.string.message_bilibili_ssid_not_found,Toast.LENGTH_LONG).show();
            return;
        }
        String pkgName=ctx.getResources().getStringArray(R.array.pkg_name_bilibili_versions)[preferences.getInt(ctx.getString(R.string.key_bilibili_version_index),Values.vDefaultBilibiliVersionIndex)];
        final AlertDialog dialog=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    for(int i_check=0;i_check<checkEpisodes.size();i_check++){
                        if(!checkEpisodes.get(i_check).isChecked())
                            continue;
                        try {
                            JSONObject jsonEntry = new JSONObject(BilibiliUtility.jsonRawBilibiliEntry);
                            JSONObject checkedEp=htmlJson.getJSONArray("episodes").getJSONObject(i_check);
                            jsonEntry.put("title",htmlJson.getString("title"));
                            jsonEntry.put("cover",htmlJson.getString("cover"));
                            jsonEntry.put("prefered_video_quality",BilibiliUtility.videoQualities[spinnerVideoQuality.getSelectedItemPosition()].value);
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
                            FileUtility.WriteFile(BilibiliUtility.GetBilibiliDownloadEntryPath(ctx,jsonEntry.getString("season_id"),
                                    String.valueOf(jsonEntry.getJSONObject("ep").getInt("episode_id"))),jsonEntry.toString());
                        }catch (JSONException e){
                            Toast.makeText(ctx,ctx.getString(R.string.message_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    AndroidUtility.KillProcess(ctx,pkgName);
                    if(checkOpenBilibili.isChecked()) {
                        try {
                            AndroidUtility.StartApplication(ctx, pkgName);
                        }catch (NullPointerException e){
                            Toast.makeText(ctx,ctx.getString(R.string.message_app_not_found,pkgName),Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    Toast.makeText(ctx,R.string.message_bilibili_download_task_created,Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(android.R.string.cancel,null)
                .setNeutralButton(R.string.button_bilibili_download_sysdown,(dialogInterface, i) -> {
                    int error=0;
                    for(int i_check=0;i_check<checkEpisodes.size();i_check++) {
                        if (!checkEpisodes.get(i_check).isChecked())
                            continue;
                        //一定要把对象创建放在里面要不然会下串集数的！！
                        BilibiliAnimeEpisodeDownloader downloader=new BilibiliAnimeEpisodeDownloader(ctx);
                        downloader.DownloadEpisode(htmlJson,i_check,BilibiliUtility.videoQualities[spinnerVideoQuality.getSelectedItemPosition()].value,
                                preferences.getInt(ctx.getString(R.string.key_api_method),Values.vDefaultApiMethod));
                        error|=downloader.error;
                    }
                    if(error==0) {
                        AndroidUtility.KillProcess(ctx, pkgName);
                        Toast.makeText(ctx, R.string.message_bilibili_wait_sysdownload, Toast.LENGTH_SHORT).show();
                    }
                })
                .setView(R.layout.dialog_bilibili_download)
                .show();
        checkOpenBilibili= dialog.findViewById(R.id.checkOpenBilibili);
        spinnerVideoQuality= dialog.findViewById(R.id.spinnerVideoQuality);
        linearLayout= dialog.findViewById(R.id.linearLayoutEpisodes);
        buttonOk= dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonOk.setEnabled(false);
        buttonNeutral=dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        buttonNeutral.setEnabled(false);
        spinnerVideoQuality.setSelection(3);
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
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
                        if (animeTitle.contains("僅")) {
                            buttonOk.setText(R.string.message_bilibili_download_region_restricted_warning);
                        }
                    }catch (JSONException e){/*Ignore*/}
                    JSONArray epArray=htmlJson.getJSONArray("episodes");
                    for(int i=0;i<epArray.length();i++){
                        CheckBox checkBox=new CheckBox(dialog.getContext());
                        try {
                            JSONObject epObject = epArray.getJSONObject(i);
                            //2024-3-16：原有链接已失效，字段名已更改
                            //checkBox.setText("[" + epObject.getString("index") + "] " + epObject.getString("index_title"));
                            checkBox.setText("[" + epObject.getString("title") + "] " + epObject.getString("long_title"));
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
        //2024-3-16：原有链接已失效：
        //task.execute("https://bangumi.bilibili.com/view/web_api/season?season_id="+ssid);
        //参考：https://github.com/Nemo2011/bilibili-api/blob/a0474871bbbc0cc1b85dcae6e8fa0c33035ef279/bilibili_api/data/api/bangumi.json#L90
        task.execute("https://api.bilibili.com/pgc/view/web/season?season_id="+ssid);
    }
}
