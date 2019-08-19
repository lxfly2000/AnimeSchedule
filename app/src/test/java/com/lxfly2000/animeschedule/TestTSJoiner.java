package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.youget.joiner.TSJoiner;
import org.junit.Assert;
import org.junit.Test;

public class TestTSJoiner {
    @Test
    public void TestJoin(){
        String[]inputs={"a.txt","b.txt"};
        String output="c.txt";
        FileUtility.WriteFile(inputs[0],"Abc\n123");
        FileUtility.WriteFile(inputs[1],"Def\n456");
        new TSJoiner().join(inputs,output);
        Assert.assertEquals("Abc\n123Def\n456",FileUtility.ReadFile(output));
    }
}
