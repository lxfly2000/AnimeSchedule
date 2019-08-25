package com.lxfly2000.animeschedule;

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
        String digest="e0fe9b2a2b884e95c6e8557654bbde03";
        String src="src/main/res/drawable/ic_animeschedule.xml";
        Assert.assertEquals(digest,HashUtility.GetFileMD5(src));
    }
}
