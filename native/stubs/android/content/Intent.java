package android.content;
public class Intent implements android.os.Parcelable { public static final String EXTRA_INTENT = "android.intent.extra.INTENT";
  public Intent(String a){throw new RuntimeException();} public Intent(Context c, Class<?> k){throw new RuntimeException();}
  public Intent putExtra(String k,int v){throw new RuntimeException();} public Intent putExtra(String k,String v){throw new RuntimeException();}
  public String getStringExtra(String k){throw new RuntimeException();} public Intent setFlags(int f){throw new RuntimeException();}
  public int getIntExtra(String k,int d){throw new RuntimeException();} public android.os.Parcelable getParcelableExtra(String k){throw new RuntimeException();}
  public Intent addFlags(int f){throw new RuntimeException();} public Intent setPackage(String p){throw new RuntimeException();} }
