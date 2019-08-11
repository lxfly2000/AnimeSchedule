package com.lxfly2000.acfunget;

import com.lxfly2000.utilities.AndroidDownloadFileTask;

import java.io.ByteArrayInputStream;

public class AcFunGet {
    public void GetBangumiDownloadLink(String url){
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, Object additionalReturned) {
                //TODO
                if(onReturnLinkFunction!=null){
                    onReturnLinkFunction.OnReturnLink(false,"TODO");
                }
            }
        };
        task.SetUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:64.0) Gecko/20100101 Firefox/64.0");
        task.SetAcceptCharset("UTF-8,*;q=0.5");
        task.SetAcceptEncoding("gzip,deflate,sdch");
        task.SetAccept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        task.SetAcceptLanguage("en-US,en;q=0.8");
        task.execute(url);
    }

    public static abstract class OnReturnLinkFunction{
        public abstract void OnReturnLink(boolean success,String link);
    }

    private OnReturnLinkFunction onReturnLinkFunction;

    public void SetOnReturnLink(OnReturnLinkFunction f){
        onReturnLinkFunction=f;
    }
}
