package com.lxfly2000.animeschedule.downloaddialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.lxfly2000.acfunget.AcFunGet;
import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.animeschedule.spider.AcFunSpider;
import com.lxfly2000.animeschedule.spider.Spider;
import org.json.JSONObject;

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

    private boolean episodeTitleOK=false;

    @Override
    public void OpenDownloadDialog(AnimeJson json, int index){
        AlertDialog dialog=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setPositiveButton(android.R.string.ok,(dialogInterface, i) -> {
                    for(int i_check=0;i_check<checkEpisodes.size();i_check++) {
                        if (checkEpisodes.get(i_check).isChecked()){
                            AcFunGet acfunGet=new AcFunGet(ctx);
                            acfunGet.SetOnFinish(new AcFunGet.OnFinishFunction() {
                                @Override
                                public void OnFinish(boolean success, @Nullable String bangumiPath, @Nullable String danmakuPath, @Nullable String msg) {
                                    if(msg!=null)
                                        Toast.makeText(ctx,msg,Toast.LENGTH_LONG).show();
                                    if(!success){
                                        String path=bangumiPath;
                                        if(path==null)
                                            path=danmakuPath;
                                        Toast.makeText(ctx,ctx.getString(R.string.message_download_failed,path),Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    if(bangumiPath!=null)
                                        Toast.makeText(ctx,ctx.getString(R.string.message_download_finish,bangumiPath),Toast.LENGTH_LONG).show();
                                    if(danmakuPath!=null)
                                        Toast.makeText(ctx,ctx.getString(R.string.message_download_finish,danmakuPath),Toast.LENGTH_LONG).show();
                                }
                            });
                            acfunGet.DownloadBangumi(json.GetWatchUrl(index),i_check,preferences.getString(ctx.getString(
                                    R.string.key_acfun_save_path),Values.GetRepositoryPathOnLocal()),checkIncludeDanmaku.isChecked());
                        }
                    }
                    Toast.makeText(ctx,R.string.message_download_task_created,Toast.LENGTH_SHORT).show();
                })
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
