package com.lxfly2000.acfunget;

import com.lxfly2000.utilities.AndroidDownloadFileTask;

public class Common {
    public static void SetAcFunHttpHeader(AndroidDownloadFileTask task){
        task.SetUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:64.0) Gecko/20100101 Firefox/64.0");
        task.SetAcceptCharset("UTF-8,*;q=0.5");
        task.SetAcceptEncoding("gzip,deflate,sdch");
        task.SetAccept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        task.SetAcceptLanguage("en-US,en;q=0.8");
    }

    class URLInfoResults{
        public String type,ext;
        public int size;
    }
}
