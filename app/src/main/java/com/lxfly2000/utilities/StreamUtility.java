package com.lxfly2000.utilities;

import java.io.*;

public class StreamUtility {
    //http://blog.csdn.net/hanqunfeng/article/details/4364583
    public static String GetStringFromStream(ByteArrayInputStream stream)throws IOException {
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
}
