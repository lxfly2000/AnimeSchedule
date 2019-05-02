package com.lxfly2000.utilities;

public class StringUtility {
    /**
     * 从指定位置查找一个完整的以括号为对象的代码段
     * @param source 要查找的字符串
     * @param start 开始查找的位置
     * @param charCodeLeftBracket 左括号的字符
     * @param charCodeRightBracket 右括号的字符
     * @return 表示对象的代码
     */
    public static String ParseBracketObject(String source,int start,int charCodeLeftBracket,int charCodeRightBracket){
        int bracketCount=0;
        int foundCount=0;
        int pos=start;
        while(pos<source.length()){
            int ch=source.charAt(pos);
            if(ch==charCodeLeftBracket) {
                if(foundCount==0&&bracketCount==0)
                    start=pos;
                bracketCount++;
            }else if(ch==charCodeRightBracket){
                bracketCount--;
                if(bracketCount==0)
                    foundCount++;
            }
            pos++;
            if(foundCount>0)
                break;
        }
        return source.substring(start,pos);
    }
    public static String ArrayStringToString(String[]sa,String divStr){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<sa.length;i++){
            if(sb.length()>0)
                sb.append(divStr);
            sb.append(sa[i]);
        }
        return sb.toString();
    }
}
