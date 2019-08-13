package com.lxfly2000.animeschedule.downloaddialog;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.animeschedule.spider.AcFunSpider;
import com.lxfly2000.animeschedule.spider.Spider;
import org.json.JSONObject;

import java.util.ArrayList;

public class AcFunDownloadDialog extends DownloadDialog {
    public AcFunDownloadDialog(@NonNull Context context){
        super(context);
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
                    /*int error=0;
                    for(int i_check=0;i_check<checkEpisodes.size();i_check++) {
                        if (!checkEpisodes.get(i_check).isChecked())
                            continue;
                    }
                    if(error==0) {
                        Toast.makeText(ctx, R.string.message_bilibili_wait_sysdownload, Toast.LENGTH_SHORT).show();
                    }*/
                    Toast.makeText(ctx, R.string.message_acfun_download_advise,Toast.LENGTH_LONG).show();
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
