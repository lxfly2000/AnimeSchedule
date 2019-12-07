//可直接用链接区分集数；仅支持最高画质下载；部分番剧需要登录且付费才可下载
//需要文字说明，不需要钩选框、列表框

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
import com.lxfly2000.animeschedule.spider.QQVideoSpider;
import com.lxfly2000.animeschedule.spider.Spider;
import com.lxfly2000.youget.QQGet;
import com.lxfly2000.youget.YouGet;

import java.util.ArrayList;

public class QQDownloadDialog extends DownloadDialog {
    private SharedPreferences preferences;
    public QQDownloadDialog(@NonNull Context context) {
        super(context);
        checkEpisodes=new ArrayList<>();
        preferences=Values.GetPreference(context);
    }

    ArrayList<CheckBox> checkEpisodes;
    Button buttonOk;
    LinearLayout linearLayout;

    private CompoundButton.OnCheckedChangeListener checkedChangeListener= (compoundButton, b) -> {
        int checkedCount=0;
        for(CheckBox checkBox:checkEpisodes){
            if(checkBox.isChecked())
                checkedCount++;
        }
        buttonOk.setEnabled(checkedCount>0);
    };

    private boolean episodeTitleOK=false;
    private AnimeItem animeItem;

    @Override
    public void OpenDownloadDialog(AnimeJson json, int index) {
        AlertDialog dialog=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setPositiveButton(android.R.string.ok,(dialogInterface, i) -> {
                    for(int i_check=0;i_check<checkEpisodes.size();i_check++){
                        if(checkEpisodes.get(i_check).isChecked()){
                            QQGet qqGet=new QQGet(ctx);
                            qqGet.SetOnFinish(new YouGet.OnFinishFunction() {
                                @Override
                                public void OnFinish(boolean success, @Nullable String bangumiPath, @Nullable String danmakuPath, @Nullable String msg) {
                                    if(msg!=null)
                                        Toast.makeText(ctx,msg,Toast.LENGTH_LONG).show();
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
                            });
                            qqGet.DownloadBangumi(animeItem.episodeTitles.get(i_check).episodeWatchUrl,i_check,0,preferences.getString(ctx.getString(
                                    R.string.key_acfun_save_path), Values.GetRepositoryPathOnLocal(ctx)));
                        }
                    }
                    Toast.makeText(ctx,R.string.message_download_task_created,Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel,null)
                .setView(R.layout.dialog_anime_download_with_notice)
                .show();
        linearLayout=dialog.findViewById(R.id.linearLayoutEpisodes);
        buttonOk=dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonOk.setEnabled(false);
        QQVideoSpider spider=new QQVideoSpider(ctx);
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
                TextView textView=dialog.findViewById(R.id.textViewDownloadNotice);
                textView.setText(String.format(textView.getText().toString(),QQGet.GetCookiePath(ctx).substring(1+QQGet.GetCookiePath(ctx).lastIndexOf("/"))));
                animeItem=data;
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
        ((TextView)dialog.findViewById(R.id.textViewDownloadNotice)).setText(R.string.label_qq_download_notice);
    }
}
