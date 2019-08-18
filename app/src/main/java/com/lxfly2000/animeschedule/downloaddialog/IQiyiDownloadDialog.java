//可直接用链接区分集数；可以选择下载画质
//需要文字说明，不需要钩选框，需要列表框

package com.lxfly2000.animeschedule.downloaddialog;

import android.content.Context;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.animeschedule.R;

public class IQiyiDownloadDialog extends DownloadDialog {
    public IQiyiDownloadDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public void OpenDownloadDialog(AnimeJson json, int index) {
        AlertDialog dialog=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    AlertDialog dq=new AlertDialog.Builder(ctx)
                            .setTitle(json.GetTitle(index))
                            .setView(R.layout.dialog_anime_download_choose_quality)
                            .setPositiveButton(android.R.string.ok,null)
                            .show();
                })
                .setView(R.layout.dialog_anime_download_with_notice)
                .show();
        ((TextView)dialog.findViewById(R.id.textViewDownloadNotice)).setText(R.string.label_iqiyi_download_notice);
    }
}
