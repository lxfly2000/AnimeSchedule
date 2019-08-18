//可直接用链接区分集数；仅支持最高画质下载；部分番剧需要登录且付费才可下载
//需要文字说明，不需要钩选框、列表框

package com.lxfly2000.animeschedule.downloaddialog;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.animeschedule.R;

public class QQDownloadDialog extends DownloadDialog {
    public QQDownloadDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public void OpenDownloadDialog(AnimeJson json, int index) {
        AlertDialog dialog=new AlertDialog.Builder(ctx)
                .setTitle(json.GetTitle(index))
                .setPositiveButton(android.R.string.ok,null)
                .setView(R.layout.dialog_anime_download_with_notice)
                .show();
        ((TextView)dialog.findViewById(R.id.textViewDownloadNotice)).setText(R.string.label_qq_download_notice);
    }
}
