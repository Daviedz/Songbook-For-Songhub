package android.content;
public class Intent { public String a=""; public String x=""; public Intent(String a){this.a=a;} public Intent(Context c, Class<?> k){this.a=k.getSimpleName();}
  public Intent putExtra(String k,int v){x+=k+"="+v;return this;} public Intent putExtra(String k,String v){x+=v;return this;}
  public String getStringExtra(String k){return x;} public Intent setFlags(int f){return this;} }
