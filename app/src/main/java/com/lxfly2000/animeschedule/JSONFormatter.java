package com.lxfly2000.animeschedule;

public class JSONFormatter {
    private static void AppendNewLine(StringBuilder builder,int tabCount){
        builder.append('\n');
        for(int i=0;i<tabCount;i++)
            builder.append('\t');
    }

    //并不具有通用性
    public static String Format(String srcJsonString){
        StringBuilder jsonStringBuilder=new StringBuilder();
        int tabCount=0,readerPos=0;
        int containLeftSquareBracketCount=0;
        while(readerPos<srcJsonString.length()){
            switch (srcJsonString.charAt(readerPos)){
                case '{':
                    jsonStringBuilder.append(srcJsonString.charAt(readerPos));
                    tabCount++;
                    AppendNewLine(jsonStringBuilder,tabCount);
                    containLeftSquareBracketCount=0;
                    break;
                case '}':
                    tabCount--;
                    AppendNewLine(jsonStringBuilder,tabCount);
                    jsonStringBuilder.append(srcJsonString.charAt(readerPos));
                    containLeftSquareBracketCount=0;
                    break;
                case '[':
                    containLeftSquareBracketCount++;
                    jsonStringBuilder.append(srcJsonString.charAt(readerPos));
                    break;
                case ']':
                    jsonStringBuilder.append(srcJsonString.charAt(readerPos));
                    containLeftSquareBracketCount--;
                    break;
                case ',':
                    jsonStringBuilder.append(srcJsonString.charAt(readerPos));
                    if(srcJsonString.charAt(readerPos+1)=='\"'&&containLeftSquareBracketCount==0){
                        AppendNewLine(jsonStringBuilder,tabCount);
                        containLeftSquareBracketCount=0;
                    }
                    break;
                default:
                    jsonStringBuilder.append(srcJsonString.charAt(readerPos));
                    break;
            }
            readerPos++;
        }
        return jsonStringBuilder.toString();
    }
}
