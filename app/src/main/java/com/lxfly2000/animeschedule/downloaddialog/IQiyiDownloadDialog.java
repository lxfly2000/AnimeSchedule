//可直接用链接区分集数；可以选择下载画质
//需要文字说明，不需要钩选框，需要列表框

package com.lxfly2000.animeschedule.downloaddialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.animeschedule.spider.IQiyiSpider;
import com.lxfly2000.animeschedule.spider.Spider;

import java.util.ArrayList;

public class IQiyiDownloadDialog extends DownloadDialog {
    private SharedPreferences preferences;
    public IQiyiDownloadDialog(@NonNull Context context) {
        super(context);
        checkEpisodes=new ArrayList<>();
        preferences= Values.GetPreference(context);
    }

    ArrayList<CheckBox>checkEpisodes;
    Button buttonOk;
    LinearLayout linearLayout;

    private CompoundButton.OnCheckedChangeListener checkedChangeListener=(compoundButton, b) -> {
        int checkedCount=0;
        for(CheckBox checkBox:checkEpisodes){
            if(checkBox.isChecked())
                checkedCount++;
        }
        buttonOk.setEnabled(checkedCount>0);
    };

    private boolean episodeTitleOK=false;

    @Override
    public void OpenDownloadDialog(AnimeJson json, int index) {
        AlertDialog dialog=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    AlertDialog dq=new AlertDialog.Builder(ctx)
                            .setTitle(json.GetTitle(index))
                            .setView(R.layout.dialog_anime_download_choose_quality)
                            .setPositiveButton("爱奇艺的下载功能正在制作中，敬请期待！",null)
                            .show();
                })
                .setNegativeButton(android.R.string.cancel,null)
                .setView(R.layout.dialog_anime_download_with_notice)
                .show();
        linearLayout=dialog.findViewById(R.id.linearLayoutEpisodes);
        buttonOk=dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonOk.setEnabled(false);
        IQiyiSpider spider=new IQiyiSpider(ctx);
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
                    checkBox.setOnCheckedChangeListener(checkedChangeListener);
                    checkEpisodes.add(checkBox);
                    linearLayout.addView(checkBox);
                    episodeTitleOK=true;
                }
            }
        });
        spider.Execute(json.GetWatchUrl(index));
        ((TextView)dialog.findViewById(R.id.textViewDownloadNotice)).setText(R.string.label_iqiyi_download_notice);
    }
}
