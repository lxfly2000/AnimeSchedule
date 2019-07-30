package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.flexbox.FlexboxLayout;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.animeschedule.spider.*;
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

    private RatingBar.OnRatingBarChangeListener rbListener=(ratingBar, v, b) ->
            ((TextView)ratingBar.getRootView().findViewById(R.id.textDialogRank))
                    .setText(ctx.getString(R.string.label_anime_ranking)+" ("+(int)ratingBar.getRating()+")");

    private class TypeFlexbox implements DialogType{
        private FlexboxLayout flexboxDialogWatchedEpisode;
        private RatingBar ratingBarRank;
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
                        animeJson.SetRank(paramIndex,(int)ratingBarRank.getRating());
                        okListener.onClick(dialogInterface,i);
                    }).show();
            flexboxDialogWatchedEpisode=dlg.findViewById(R.id.flexboxDialogWatchedEpisodes);
            ratingBarRank=dlg.findViewById(R.id.ratingDialogRank);
            ratingBarRank.setOnRatingBarChangeListener(rbListener);
            ratingBarRank.setRating(animeJson.GetRank(paramIndex));
            rbListener.onRatingChanged(ratingBarRank,ratingBarRank.getRating(),false);
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
        private RatingBar ratingBarRank;
        AlertDialog dlg;
        public void Show(){
            dlg=new AlertDialog.Builder(ctx)
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
                        animeJson.SetRank(paramIndex,(int)ratingBarRank.getRating());
                        okListener.onClick(dialogInterface,i);
                    }).show();
            linearLayout=dlg.findViewById(R.id.linearLayoutEpisodes);
            ratingBarRank=dlg.findViewById(R.id.ratingDialogRank);
            ratingBarRank.setOnRatingBarChangeListener(rbListener);
            ratingBarRank.setRating(animeJson.GetRank(paramIndex));
            rbListener.onRatingChanged(ratingBarRank,ratingBarRank.getRating(),false);
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
            Spider spider=null;
            if(URLUtility.IsBilibiliSeasonBangumiLink(animeJson.GetWatchUrl(paramIndex))){
                spider=new BilibiliSpider(ctx);
                ((BilibiliSpider)spider).SetTitleUseSeriesTitle(false);
            }else if(URLUtility.IsAcFunLink(animeJson.GetWatchUrl(paramIndex))){
                spider=new AcFunSpider(ctx);
            }else if(URLUtility.IsIQiyiLink(animeJson.GetWatchUrl(paramIndex))){
                spider=new IQiyiSpider(ctx);
            }else if(URLUtility.IsYoukuLink(animeJson.GetWatchUrl(paramIndex))){
                spider=new YoukuSpider(ctx);
            }/*else if(URLUtility.IsQQVideoLink(animeJson.GetWatchUrl(paramIndex))){//腾讯视频目前不提供各集标题，暂时无解
                spider=new QQVideoSpider(ctx);
            }*/
            if(spider!=null){
                spider.SetOnReturnDataFunction(onReturnDataFunction);
                spider.Execute(animeJson.GetWatchUrl(paramIndex));
            }
        }

        private boolean titleOK=false;

        private Spider.OnReturnDataFunction onReturnDataFunction=new Spider.OnReturnDataFunction() {
            @Override
            public void OnReturnData(AnimeItem data, int status, String resultMessage, int focusId) {
                if(resultMessage!=null)
                    Toast.makeText(ctx,resultMessage,Toast.LENGTH_LONG).show();
                if(status==Spider.STATUS_FAILED)
                    return;
                if(data.title!=null)
                    dlg.setTitle(data.title);
                if(titleOK)
                    return;
                for(int i=0;i<Math.min(data.episodeTitles.size(),linearLayout.getChildCount());i++) {
                    CheckBox checkBox=(CheckBox)linearLayout.getChildAt(i);
                    String titleText = "[" + data.episodeTitles.get(i).episodeIndex + "] " + data.episodeTitles.get(i).episodeTitle;
                    String originalString = checkBox.getText().toString();
                    if (originalString.indexOf("]") + 1 < originalString.length())
                        titleText += "\n";
                    checkBox.setText(originalString.replaceFirst("\\[\\d*\\] *", titleText));
                    titleOK=true;
                }
            }
        };
    }
}
