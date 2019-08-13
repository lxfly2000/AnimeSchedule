package com.lxfly2000.animeschedule.downloaddialog;

import android.content.Context;
import androidx.annotation.NonNull;
import com.lxfly2000.animeschedule.AnimeJson;

public abstract class DownloadDialog {
    protected Context ctx;
    public DownloadDialog(@NonNull Context context){
        ctx=context;
    }

    public abstract void OpenDownloadDialog(AnimeJson json,int index);
}
