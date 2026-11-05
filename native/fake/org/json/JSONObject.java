package org.json;
public class JSONObject { public static String quote(String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){if(c=='"'||c=='\\')b.append('\\').append(c);else if(c<0x20)b.append(String.format("\\u%04x",(int)c));else b.append(c);}return b.append('"').toString();} }
