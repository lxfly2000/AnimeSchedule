package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.*;
import com.lxfly2000.utilities.AndroidUtility;

public class BilibiliDownloadDialog {
    private Context ctx;
    BilibiliDownloadDialog(@NonNull Context context){
        ctx=context;
    }

    CheckBox checkOpenBilibili;
    Spinner spinnerVideoQuality;
    Button buttonOk;

    void OpenDownloadDialog(AnimeJson json,int index){
        String ssid=URLUtility.GetBilibiliSeasonIdString(json.GetWatchUrl(index));
        if(ssid==null){
            Toast.makeText(ctx,R.string.message_bilibili_ssid_not_found,Toast.LENGTH_LONG).show();
            return;
        }
        AlertDialog dialog=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SharedPreferences preferences=Values.GetPreference(ctx);
                        String pkgName=Values.pkgNameBilibiliVersions[preferences.getInt(Values.keyBilibiliVersionIndex,Values.vDefaultBilibiliVersion)];
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
        buttonOk=(Button)dialog.findViewById(android.R.id.button1);
        buttonOk.setEnabled(false);
        spinnerVideoQuality.setSelection(4);
        LinearLayout linearLayout=(LinearLayout)dialog.findViewById(R.id.linearLayoutEpisodes);
        for(int i=0;i<5;i++) {
            CheckBox checkBoxTest=new CheckBox(dialog.getContext());
            checkBoxTest.setText(String.format("[%d] TODO:这个功能正在制作中。这个功能正在制作中。",i+1));
            linearLayout.addView(checkBoxTest);
        }
        Toast.makeText(ctx,"TODO:这个功能正在制作中。",Toast.LENGTH_LONG).show();
    }
}
