package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.HashUtility;
import org.junit.Assert;
import org.junit.Test;

public class TestHash {
    @Test
    public void TestStringMD5(){
        String digest="130e29f351572e58c49fd4c910d7beb0";
        String src="bilibili";
        Assert.assertEquals(digest, HashUtility.GetStringMD5(src));
    }
    @Test
    public void TestFileMD5(){
        String digest="5329994d18cb66ac70052b694a89bd05";
        String src="114514.txt";
        FileUtility.WriteFile(src,"114514\n1919\n810\n就是这↑里↓了\n哼！哼！啊啊啊啊啊啊啊啊啊啊啊啊啊！！");
        Assert.assertEquals(digest,HashUtility.GetFileMD5(src));
        FileUtility.DeleteFile(src);
    }
}
