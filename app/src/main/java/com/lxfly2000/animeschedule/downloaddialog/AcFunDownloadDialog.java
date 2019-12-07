package com.lxfly2000.animeschedule.downloaddialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.animeschedule.spider.AcFunSpider;
import com.lxfly2000.animeschedule.spider.Spider;
import com.lxfly2000.youget.AcFunGet;
import com.lxfly2000.youget.YouGet;

import java.util.ArrayList;

public class AcFunDownloadDialog extends DownloadDialog {
    private SharedPreferences preferences;
    public AcFunDownloadDialog(@NonNull Context context){
        super(context);
        preferences= Values.GetPreference(ctx);
        checkEpisodes=new ArrayList<>();
    }

    CheckBox checkIncludeDanmaku;
    ArrayList<CheckBox> checkEpisodes;
    Button buttonOk;
    LinearLayout linearLayout;

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

    YouGet.OnFinishFunction onFinishFunction=new YouGet.OnFinishFunction() {
        @Override
        public void OnFinish(boolean success, @Nullable String bangumiPath, @Nullable String danmakuPath, @Nullable String msg) {
            if(msg!=null)
                Toast.makeText(ctx,msg,Toast.LENGTH_LONG).show();
            if(bangumiPath==null)
                bangumiPath=danmakuPath;
            if(!success){
                if(bangumiPath==null)
                    return;
                if(msg==null)
                    Toast.makeText(ctx,ctx.getString(R.string.message_download_failed,bangumiPath),Toast.LENGTH_LONG).show();
                return;
            }
            if(bangumiPath!=null&&msg==null)
                Toast.makeText(ctx,ctx.getString(R.string.message_download_finish,bangumiPath),Toast.LENGTH_LONG).show();
        }
    };

    AlertDialog dq;
    public void OpenVideoQualityDialog(AnimeJson json,int index){
        dq=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setView(R.layout.dialog_anime_download_choose_quality)
                .setPositiveButton(android.R.string.ok,(dialogInterface, i) -> {
                    RadioGroup radioGroup=dq.findViewById(R.id.radiosVideoQuality);
                    for(int i_radio=0;i_radio<radioGroup.getChildCount();i_radio++){
                        RadioButton button=(RadioButton)radioGroup.getChildAt(i_radio);
                        if(button.isChecked()){
                            for(int i_epi=0;i_epi<checkEpisodes.size();i_epi++){
                                if(checkEpisodes.get(i_epi).isChecked()){
                                    AcFunGet acFunGet=new AcFunGet(ctx);
                                    acFunGet.SetOnFinish(onFinishFunction);
                                    acFunGet.DownloadBangumi(animeItem.episodeTitles.get(i_epi).episodeWatchUrl,i_epi,i_radio,preferences.getString(ctx.getString(
                                            R.string.key_acfun_save_path),Values.GetRepositoryPathOnLocal(ctx)),checkIncludeDanmaku.isChecked(),true);
                                }
                            }
                            Toast.makeText(ctx,R.string.message_download_task_created,Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel,null)
                .show();
        dq.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        RadioGroup radioGroup=dq.findViewById(R.id.radiosVideoQuality);
        //查询可用清晰度
        for(int i=0;i<checkEpisodes.size();i++){
            if(checkEpisodes.get(i).isChecked()){
                AcFunGet acFunGet=new AcFunGet(ctx);
                acFunGet.SetOnFinish(onFinishFunction);
                acFunGet.QueryQualities(animeItem.episodeTitles.get(i).episodeWatchUrl, i, new YouGet.OnReturnVideoQualityFunction() {
                    @Override
                    public void OnReturnVideoQuality(boolean success, ArrayList<YouGet.VideoQuality> qualities) {
                        for(int i=0;i<qualities.size();i++) {
                            RadioButton radioButton = new RadioButton(dq.getContext());
                            radioButton.setText(qualities.get(i).qualityName);
                            radioButton.setId(i);
                            radioButton.setLayoutParams(radioGroup.getLayoutParams());
                            radioGroup.addView(radioButton);
                            if(i==0) {
                                radioGroup.check(i);
                                dq.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                            }
                        }
                    }
                },true);
                break;
            }
        }
    }

    private boolean episodeTitleOK=false;
    private AnimeItem animeItem;

    @Override
    public void OpenDownloadDialog(AnimeJson json, int index){
        AlertDialog dialog=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setPositiveButton(android.R.string.ok,(dialogInterface, i) -> OpenVideoQualityDialog(json, index))
                .setNegativeButton(android.R.string.cancel,null)
                .setView(R.layout.dialog_acfun_download)
                .show();
        checkIncludeDanmaku=dialog.findViewById(R.id.checkAcfunIncludeDanmaku);
        linearLayout=dialog.findViewById(R.id.linearLayoutEpisodes);
        buttonOk=dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonOk.setEnabled(false);
        AcFunSpider spider=new AcFunSpider(ctx);
        spider.SetOnReturnDataFunction(new Spider.OnReturnDataFunction() {
            @Override
            public void OnReturnData(AnimeItem data, int status, String resultMessage, int focusId) {
                if(resultMessage!=null)
                    Toast.makeText(ctx,resultMessage,Toast.LENGTH_LONG).show();
                if(status==Spider.STATUS_FAILED)
                    return;
                if(data.title!=null)
                    dialog.setTitle(data.title);
                if(episodeTitleOK)
                    return;
                animeItem=data;
                for(int i=0;i<data.episodeTitles.size();i++){
                    CheckBox checkBox=new CheckBox(dialog.getContext());
                    checkBox.setText("["+data.episodeTitles.get(i).episodeIndex+"] "+data.episodeTitles.get(i).episodeTitle);
                    checkBox.setOnCheckedChangeListener(checkChangeListener);
                    checkEpisodes.add(checkBox);
                    linearLayout.addView(checkBox);
                    episodeTitleOK=true;
                }
            }
        });
        spider.Execute(json.GetWatchUrl(index));
    }
}
