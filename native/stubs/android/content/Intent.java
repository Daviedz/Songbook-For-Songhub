package android.content;
public class Intent { public Intent(String a){throw new RuntimeException();} public Intent(Context c, Class<?> k){throw new RuntimeException();}
  public Intent putExtra(String k,int v){throw new RuntimeException();} public Intent putExtra(String k,String v){throw new RuntimeException();}
  public String getStringExtra(String k){throw new RuntimeException();} public Intent setFlags(int f){throw new RuntimeException();} }
