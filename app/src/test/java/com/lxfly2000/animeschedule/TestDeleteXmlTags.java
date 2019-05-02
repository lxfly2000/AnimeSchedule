package com.lxfly2000.animeschedule;

import com.lxfly2000.animeschedule.spider.YoukuSpider;
import org.junit.Assert;
import org.junit.Test;

public class TestDeleteXmlTags {
    @Test
    public void TestMain(){
        String src="<li class=\"p-row\">声优：<a href=\"//list.youku.com/star/show/uid_UMTU5NjY1Mg==.html\" title=\"金元寿子\" target=\"_blank\">金元寿子</a><i class=\"p-s\">/</i><a href=\"//list.youku.com/star/show/uid_UMTA0NjEyOA==.html\" title=\"藤村步\" target=\"_blank\">藤村步</a><i class=\"p-s\">/</i><a href=\"//list.youku.com/star/show/uid_UMTAxOTU5Ng==.html\" title=\"田中理惠\" target=\"_blank\">田中理惠</a><i class=\"p-s\">/</i><a href=\"//list.youku.com/star/show/uid_UMTU2MzkyOA==.html\" title=\"伊藤佳奈惠\" target=\"_blank\">伊藤佳奈惠</a></li>";
        String expect="声优：金元寿子/藤村步/田中理惠/伊藤佳奈惠";
        Assert.assertEquals(expect, YoukuSpider.DeleteAllXmlTags(src));
    }
}
