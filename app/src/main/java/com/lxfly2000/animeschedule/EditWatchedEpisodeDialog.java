package com.lxfly2000.animeschedule;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.widget.ToggleButton;
import com.google.android.flexbox.FlexboxLayout;

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

    public void Show(int index){
        new TypeFlexbox().Show(index);
    }

    private class TypeFlexbox{
        private FlexboxLayout flexboxDialogWatchedEpisode;
        void Show(final int index){
            AlertDialog dlg=new AlertDialog.Builder(ctx)
                    .setTitle(animeJson.GetTitle(index))
                    .setView(R.layout.dialog_edit_watched_episodes_flexbox)
                    .setNegativeButton(android.R.string.cancel,null)
                    .setNeutralButton(R.string.button_change_layout,null/*TODO:Change another layout.*/)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //读取已观看的集数
                            for(int i_epi=1;i_epi<=animeJson.GetLastUpdateEpisode(index);i_epi++) {
                                boolean i_epi_watched=((ToggleButton)flexboxDialogWatchedEpisode.getChildAt(i_epi-1)).isChecked();
                                if(animeJson.GetEpisodeWatched(index,i_epi)!=i_epi_watched)
                                    animeJson.SetEpisodeWatched(index, i_epi, i_epi_watched);
                            }
                            okListener.onClick(dialogInterface,i);
                        }
                    }).show();
            flexboxDialogWatchedEpisode=dlg.findViewById(R.id.flexboxDialogWatchedEpisodes);
            //显示观看的集数
            ToggleButton toggleEpisode;
            FlexboxLayout.LayoutParams layoutToggleEpisode=new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,FlexboxLayout.LayoutParams.WRAP_CONTENT);
            for(int i=1;i<=animeJson.GetLastUpdateEpisode(index);i++){
                toggleEpisode=new ToggleButton(ctx);
                toggleEpisode.setLayoutParams(layoutToggleEpisode);
                toggleEpisode.setMinWidth(0);
                toggleEpisode.setMinimumWidth(0);
                toggleEpisode.setTextOn(String.valueOf(i));
                toggleEpisode.setTextOff(String.valueOf(i));
                toggleEpisode.setChecked(animeJson.GetEpisodeWatched(index,i));
                flexboxDialogWatchedEpisode.addView(toggleEpisode);
            }
        }
    }
}
