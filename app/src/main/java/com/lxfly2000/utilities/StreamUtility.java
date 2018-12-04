package com.lxfly2000.utilities;

import java.io.*;

public class StreamUtility {
    //http://blog.csdn.net/hanqunfeng/article/details/4364583
    public static String GetStringFromStream(InputStream stream,boolean resetStream)throws IOException {
        if(resetStream)
            stream.reset();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder getString = new StringBuilder();
        String newString;
        while (true) {
            newString = reader.readLine();
            if (newString == null)
                break;
            getString.append(newString).append("\n");
        }
        return getString.toString();
    }
    public static String GetStringFromStream(InputStream stream)throws IOException {
        return GetStringFromStream(stream,true);
    }
}
