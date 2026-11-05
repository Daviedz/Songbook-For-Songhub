import com.songbook.app.NativeWs;
public class S { public static void main(String[] a) throws Exception {
  NativeWs w=new NativeWs(new android.app.Activity(), new android.webkit.WebView());
  long t=System.currentTimeMillis(); w.scan(1,6000); Thread.sleep(9500);
  System.out.println("elapsed ~"+(System.currentTimeMillis()-t)/1000+"s");
}}
