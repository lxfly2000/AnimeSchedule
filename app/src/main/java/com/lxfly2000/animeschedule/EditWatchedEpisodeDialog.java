package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.android.flexbox.FlexboxLayout;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.StreamUtility;
import com.lxfly2000.utilities.YMDDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class EditWatchedEpisodeDialog {
    private Context ctx;
    private AnimeJson animeJson;
    private SharedPreferences preferences;
    private DialogInterface.OnClickListener okListener;
    public EditWatchedEpisodeDialog(@NonNull Context context){
        ctx=context;
        preferences=Values.GetPreference(ctx);
    }

    public void SetJson(AnimeJson json){
        animeJson=json;
    }

    public void SetOnOkListener(DialogInterface.OnClickListener listener){
        okListener=listener;
    }

    private int paramIndex;
    public void Show(int index){
        paramIndex=index;
        switch (GetLayoutType()){
            case 0:new TypeFlexbox().Show();break;
            case 1:new TypeCheckList().Show();break;
        }
    }

    private void SetLayoutType(int type){
        preferences.edit().putInt(Values.keyEditWatchedEpisodeDialogType,type).apply();
    }

    private int GetLayoutType(){
        return preferences.getInt(Values.keyEditWatchedEpisodeDialogType,Values.vDefaultEditWatchedEpisodeDialogType);
    }

    private DialogInterface.OnClickListener clickChangeLayoutListener=new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            SetLayoutType((GetLayoutType()+1)%2);
            Show(paramIndex);
        }
    };

    private interface DialogType{
        void Show();
    }

    private class TypeFlexbox implements DialogType{
        private FlexboxLayout flexboxDialogWatchedEpisode;
        public void Show(){
            AlertDialog dlg=new AlertDialog.Builder(ctx)
                    .setTitle(animeJson.GetTitle(paramIndex))
                    .setView(R.layout.dialog_edit_watched_episodes_flexbox)
                    .setNegativeButton(android.R.string.cancel,null)
                    .setNeutralButton(R.string.button_change_layout,clickChangeLayoutListener)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        //读取已观看的集数
                        for(int i_epi=1;i_epi<=animeJson.GetLastUpdateEpisode(paramIndex);i_epi++) {
                            boolean i_epi_watched=((ToggleButton)flexboxDialogWatchedEpisode.getChildAt(i_epi-1)).isChecked();
                            if(animeJson.GetEpisodeWatched(paramIndex,i_epi)!=i_epi_watched)
                                animeJson.SetEpisodeWatched(paramIndex, i_epi, i_epi_watched);
                        }
                        okListener.onClick(dialogInterface,i);
                    }).show();
            flexboxDialogWatchedEpisode=dlg.findViewById(R.id.flexboxDialogWatchedEpisodes);
            //显示观看的集数
            ToggleButton toggleEpisode;
            FlexboxLayout.LayoutParams layoutToggleEpisode=new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,FlexboxLayout.LayoutParams.WRAP_CONTENT);
            for(int i=1;i<=animeJson.GetLastUpdateEpisode(paramIndex);i++){
                toggleEpisode=new ToggleButton(ctx);
                toggleEpisode.setLayoutParams(layoutToggleEpisode);
                toggleEpisode.setMinWidth(0);
                toggleEpisode.setMinimumWidth(0);
                toggleEpisode.setTextOn(String.valueOf(i));
                toggleEpisode.setTextOff(String.valueOf(i));
                toggleEpisode.setChecked(animeJson.GetEpisodeWatched(paramIndex,i));
                flexboxDialogWatchedEpisode.addView(toggleEpisode);
            }
        }
    }

    private class TypeCheckList implements DialogType{
        private LinearLayout linearLayout;
        public void Show(){
            final AlertDialog dlg=new AlertDialog.Builder(ctx)
                    .setTitle(animeJson.GetTitle(paramIndex))
                    .setView(R.layout.dialog_edit_watched_episodes_checklist)
                    .setNegativeButton(android.R.string.cancel,null)
                    .setNeutralButton(R.string.button_change_layout,clickChangeLayoutListener)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        for(int i_epi=1;i_epi<=animeJson.GetLastUpdateEpisode(paramIndex);i_epi++) {
                            boolean i_epi_watched=((CheckBox)linearLayout.getChildAt(i_epi-1)).isChecked();
                            if(animeJson.GetEpisodeWatched(paramIndex,i_epi)!=i_epi_watched)
                                animeJson.SetEpisodeWatched(paramIndex, i_epi, i_epi_watched);
                        }
                        okListener.onClick(dialogInterface,i);
                    }).show();
            linearLayout=dlg.findViewById(R.id.linearLayoutEpisodes);
            for(int i=1;i<=animeJson.GetLastUpdateEpisode(paramIndex);i++){
                CheckBox checkBox=new CheckBox(dlg.getContext());
                StringBuilder sb=new StringBuilder();
                sb.append("[").append(i).append("]");
                if(animeJson.GetEpisodeWatched(paramIndex,i)){
                    checkBox.setChecked(true);
                    int iDate=animeJson.GetEpisodeWatchedIntDate(paramIndex,i);
                    YMDDate date=new YMDDate();
                    if(iDate==0&&i==animeJson.GetLastWatchEpisodeForAnime(paramIndex)){
                        date.FromString(animeJson.GetLastWatchDateStringForAnime(paramIndex));
                        iDate=date.To8DigitsInt();
                    }
                    if(iDate!=0){
                        sb.append(" ").append(ctx.getString(R.string.message_last_watch_date)).append(date.From8DigitsInt(iDate).ToLocalizedFormatString());
                    }
                }else {
                    checkBox.setChecked(false);
                }
                checkBox.setText(sb.toString());
                linearLayout.addView(checkBox);
            }
            if(URLUtility.IsBilibiliSeasonLink(animeJson.GetWatchUrl(paramIndex))){
                final String ssid=URLUtility.GetBilibiliSeasonIdString(animeJson.GetWatchUrl(paramIndex));
                if(ssid==null){
                    Toast.makeText(ctx,R.string.message_bilibili_ssid_not_found,Toast.LENGTH_LONG).show();
                    return;
                }
                AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                    @Override
                    public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                        if(!success){
                            Toast.makeText(ctx,R.string.message_unable_to_fetch_anime_info,Toast.LENGTH_LONG).show();
                            return;
                        }
                        try {
                            String jsonString = StreamUtility.GetStringFromStream(stream);
                            if (jsonString == null) {
                                Toast.makeText(ctx, R.string.message_bilibili_ssid_code_not_found, Toast.LENGTH_LONG).show();
                                return;
                            }
                            JSONObject htmlJson = new JSONObject(jsonString).getJSONObject("result");
                            try {
                                dlg.setTitle(htmlJson.getString("title"));
                            }catch (JSONException e){/*Ignore*/}
                            JSONArray epArray=htmlJson.getJSONArray("episodes");
                            for(int i=0;i<epArray.length()&&i<linearLayout.getChildCount();i++){
                                CheckBox checkBox=(CheckBox)linearLayout.getChildAt(i);
                                try {
                                    JSONObject epObject = epArray.getJSONObject(i);
                                    String titleText = "[" + epObject.getString("index") + "] " + epObject.getString("index_title");
                                    String originalString = checkBox.getText().toString();
                                    if (originalString.indexOf("]") + 1 < originalString.length())
                                        titleText += "\n";
                                    checkBox.setText(originalString.replaceFirst("\\[\\d*\\] *", titleText));
                                }catch (JSONException e){/*Ignore*/}
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
    }
}
