package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.youget.joiner.MP4Joiner;
import org.junit.Assert;
import org.junit.Test;

public class TestMp4Parser {
    @Test
    public void TestMerge(){
        String[]inputs={"D:\\Yueyu\\Anime\\咕噜咕噜魔法阵剧场\\12_ffmpeg.mp4","D:\\Yueyu\\Anime\\咕噜咕噜魔法阵剧场\\13_ffmpeg.mp4"};
        for (String input : inputs) {
            if (!FileUtility.IsFileExists(input))
                return;
        }
        Assert.assertEquals(0,new MP4Joiner().join(inputs,"TestOutput.mp4"));
        FileUtility.DeleteFile("TestOutput.mp4");
    }
}
