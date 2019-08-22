package com.lxfly2000.youget;

import android.content.Context;
import androidx.annotation.NonNull;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.utilities.FileUtility;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

public class YoukuGet extends YouGet {
    public YoukuGet(@NonNull Context context) {
        super(context);
    }

    @Override
    public void DownloadBangumi(String url, int episodeToDownload_fromZero, int quality, String saveDirPath) {

    }

    @Override
    public void QueryQualities(String url, int episodeToDownload_fromZero, OnReturnVideoQualityFunction f) {

    }

    String QuoteCNA(String val){
        if(val.contains("%"))
            return val;
        try {
            return URLEncoder.encode(val, "UTF-8");
        }catch (UnsupportedEncodingException e){
            return val;
        }
    }

    String cookiePath= Values.GetRepositoryPathOnLocal()+"/cookie_youku.txt";

    String FetchCNA(){
        String cookies= FileUtility.ReadFile(cookiePath);
        if(cookies!=null){
            HashMap<String,String>mapCookie=new HashMap<>();
            String[]sp=cookies.split(";");
            for (String c : sp) {
                String[] sp2 = c.split("=");
                if (sp2.length > 1)
                    mapCookie.put(sp2[0], sp2[1]);
            }
            String domain=mapCookie.get("domain");
            if(domain==null)
                domain=mapCookie.get("DOMAIN");
            String cna=mapCookie.get("cna");
            if(cna==null)
                cna=mapCookie.get("CNA");
            if(cna!=null&&domain!=null){
                if(domain.equals(".youku.com")){
                    //找到CNA信息
                    return QuoteCNA(cna);
                }
            }
        }
        return "TODO";
    }
}
