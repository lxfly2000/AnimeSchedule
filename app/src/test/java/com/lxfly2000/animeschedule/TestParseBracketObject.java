package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.StringUtility;
import org.junit.Assert;
import org.junit.Test;

public class TestParseBracketObject {
    @Test
    public void TestParseJSON(){
        String src="setData({\"name\":\"Li Hua\",\"Age\":15,\"Books\":[{\"Title\":\"Math\",\"ID\":1},{\"Title\":\"English\",\"ID\":2}]})";
        String expect="{\"name\":\"Li Hua\",\"Age\":15,\"Books\":[{\"Title\":\"Math\",\"ID\":1},{\"Title\":\"English\",\"ID\":2}]}";
        Assert.assertEquals(StringUtility.ParseBracketObject(src,6,'{','}'),expect);
    }
}
