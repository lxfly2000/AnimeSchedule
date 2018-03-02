package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.JSONFormatter;
import org.junit.Assert;
import org.junit.Test;

//测试JSON格式化类
public class TestJSONFormatter {
    @Test
    public void TestFormat() {
        String srcJSON="{" +
"\"_comment\":\"测试用\"," +
"\"last_watch_index\":0," +
"\"last_watch_episode\":18," +
"\"last_watch_date\":\"2018-2-24\"," +
"\"anime\":[{" +
"\"cover\":\"https://i0.hdslb.com/bfs/bangumi/0f9ab51ef5033eadad06628e98ee70d06a115fec.jpg\"," +
"\"title\":\"魔法使的新娘\"," +
"\"description\":\"羽鸟智世是一个生活缺乏希望的十五岁少女，某一天她被魔法师艾利亚斯买下并被收为“徒弟”，从此她的生命轨迹有了新的变化…\"," +
"\"start_date\":\"2017-10-8\"," +
"\"update_period\":7," +
"\"update_period_unit\":\"day\"," +
"\"episode_count\":27," +
"\"watch_url\":\"http://bangumi.bilibili.com/anime/6438\"," +
"\"absense_count\":1," +
"\"watched_episode\":[true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,false,false,false,false,false,false,false,false]," +
"\"abandoned\":false," +
"\"rank\":4," +
"\"color\":\"Crimson\"," +
"\"category\":[\"恋爱\",\"奇幻\",\"魔法\",\"漫改\"]" +
"},{" +
"\"cover\":\"http://i0.hdslb.com/bfs/bangumi/146b436c329e57a9f9d4cb5ccd42c156af413327.jpg\"," +
"\"title\":\"寒蝉鸣泣之时\"," +
"\"description\":\"Bili:昭和58年的夏天，白天蝉的喧闹与傍晚茅蜩的合唱仿佛在欢迎今年早夏来临的6月，回荡在雏见泽。 雏见泽是远离都会的深山中的一个穷乡僻壤。 人口不足2千人的村子里，最近来了个从都会搬迁过来的少年前原圭一。性格开朗善于交际的圭一在学校里很快融入了周围的同学中。关系最好的喜欢照顾人的礼奈、具有领导才能的委员长魅音、盘球高手的低年级学生沙都子、古手神社千金且带来不可思议的气氛的梨花… 他与伙伴们过着微不足道的日常生活，并以为会永远持续下去。 每年6月举行的祭奠---绵流，没错，那一天也应该是快乐的一天的。 然而在知道了一个关于雏见泽的谜之后，一切都改变了… 在祭奠当日，惨剧再次发生。每年据说都会有一个人死亡，一个人行踪不明。自数年之前开始的连续怪死事件，其真相是？犯人是？圭一由于好奇心的驱使，步入了村子的黑暗之中。从那天起，圭一的周围发生了微妙但确实显而易见的变化。没错，所有一切都… 只有茅蜩的鸣叫声一点未变，在雏见泽预告着早夏的来临。\"," +
"\"start_date\":\"2006-4-4\"," +
"\"update_period\":7," +
"\"update_period_unit\":\"day\"," +
"\"episode_count\":26," +
"\"watch_url\":\"http://bangumi.bilibili.com/anime/3554\"," +
"\"absense_count\":0," +
"\"watched_episode\":[]," +
"\"abandoned\":false," +
"\"rank\":0," +
"\"color\":\"rgb(242,105,71)\"," +
"\"category\":[\"致郁\",\"猎奇\"]" +
"}]" +
"}";
        String formattedJSON="{\n" +
"\t\"_comment\":\"测试用\",\n" +
"\t\"last_watch_index\":0,\n" +
"\t\"last_watch_episode\":18,\n" +
"\t\"last_watch_date\":\"2018-2-24\",\n" +
"\t\"anime\":[{\n" +
"\t\t\"cover\":\"https://i0.hdslb.com/bfs/bangumi/0f9ab51ef5033eadad06628e98ee70d06a115fec.jpg\",\n" +
"\t\t\"title\":\"魔法使的新娘\",\n" +
"\t\t\"description\":\"羽鸟智世是一个生活缺乏希望的十五岁少女，某一天她被魔法师艾利亚斯买下并被收为“徒弟”，从此她的生命轨迹有了新的变化…\",\n" +
"\t\t\"start_date\":\"2017-10-8\",\n" +
"\t\t\"update_period\":7,\n" +
"\t\t\"update_period_unit\":\"day\",\n" +
"\t\t\"episode_count\":27,\n" +
"\t\t\"watch_url\":\"http://bangumi.bilibili.com/anime/6438\",\n" +
"\t\t\"absense_count\":1,\n" +
"\t\t\"watched_episode\":[true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,false,false,false,false,false,false,false,false],\n" +
"\t\t\"abandoned\":false,\n" +
"\t\t\"rank\":4,\n" +
"\t\t\"color\":\"Crimson\",\n" +
"\t\t\"category\":[\"恋爱\",\"奇幻\",\"魔法\",\"漫改\"]\n" +
"\t},{\n" +
"\t\t\"cover\":\"http://i0.hdslb.com/bfs/bangumi/146b436c329e57a9f9d4cb5ccd42c156af413327.jpg\",\n" +
"\t\t\"title\":\"寒蝉鸣泣之时\",\n" +
"\t\t\"description\":\"Bili:昭和58年的夏天，白天蝉的喧闹与傍晚茅蜩的合唱仿佛在欢迎今年早夏来临的6月，回荡在雏见泽。 雏见泽是远离都会的深山中的一个穷乡僻壤。 人口不足2千人的村子里，最近来了个从都会搬迁过来的少年前原圭一。性格开朗善于交际的圭一在学校里很快融入了周围的同学中。关系最好的喜欢照顾人的礼奈、具有领导才能的委员长魅音、盘球高手的低年级学生沙都子、古手神社千金且带来不可思议的气氛的梨花… 他与伙伴们过着微不足道的日常生活，并以为会永远持续下去。 每年6月举行的祭奠---绵流，没错，那一天也应该是快乐的一天的。 然而在知道了一个关于雏见泽的谜之后，一切都改变了… 在祭奠当日，惨剧再次发生。每年据说都会有一个人死亡，一个人行踪不明。自数年之前开始的连续怪死事件，其真相是？犯人是？圭一由于好奇心的驱使，步入了村子的黑暗之中。从那天起，圭一的周围发生了微妙但确实显而易见的变化。没错，所有一切都… 只有茅蜩的鸣叫声一点未变，在雏见泽预告着早夏的来临。\",\n" +
"\t\t\"start_date\":\"2006-4-4\",\n" +
"\t\t\"update_period\":7,\n" +
"\t\t\"update_period_unit\":\"day\",\n" +
"\t\t\"episode_count\":26,\n" +
"\t\t\"watch_url\":\"http://bangumi.bilibili.com/anime/3554\",\n" +
"\t\t\"absense_count\":0,\n" +
"\t\t\"watched_episode\":[],\n" +
"\t\t\"abandoned\":false,\n" +
"\t\t\"rank\":0,\n" +
"\t\t\"color\":\"rgb(242,105,71)\",\n" +
"\t\t\"category\":[\"致郁\",\"猎奇\"]\n" +
"\t}]\n" +
"}";
        String testStr= JSONFormatter.Format(srcJSON);
        Assert.assertEquals(formattedJSON,testStr);
    }
}