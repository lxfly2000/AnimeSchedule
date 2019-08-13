package com.lxfly2000.acfunget;

import com.lxfly2000.utilities.AndroidDownloadFileTask;

import java.io.IOException;
import java.net.URL;

public class Common {
    public static void SetAcFunHttpHeader(AndroidDownloadFileTask task){
        task.SetUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:64.0) Gecko/20100101 Firefox/64.0");
        task.SetAcceptCharset("UTF-8,*;q=0.5");
        task.SetAcceptEncoding("gzip,deflate,sdch");
        task.SetAccept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        task.SetAcceptLanguage("en-US,en;q=0.8");
    }

    public static int GetURLResponseSize(String url){
        try{
            URL jUrl=new URL(url);
            return jUrl.openConnection().getContentLength();
        } catch (IOException e){
            return -1;
        }
    }
}
