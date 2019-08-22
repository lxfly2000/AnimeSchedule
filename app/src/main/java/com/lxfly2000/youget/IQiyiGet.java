package com.lxfly2000.youget;

import android.content.Context;
import androidx.annotation.NonNull;

public class IQiyiGet extends YouGet {
    public IQiyiGet(@NonNull Context context) {
        super(context);
    }

    @Override
    public void DownloadBangumi(String url, int episodeToDownload_fromZero, int quality, String saveDirPath) {

    }

    @Override
    public void QueryQualities(String url, int episodeToDownload_fromZero, OnReturnVideoQualityFunction f) {

    }
}
