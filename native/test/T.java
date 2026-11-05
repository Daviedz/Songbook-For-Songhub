import com.songbook.app.NativeWs;
public class T { public static void main(String[] a) throws Exception {
  NativeWs w=new NativeWs(new android.app.Activity(), new android.webkit.WebView());
  NativeWs.askPermission(new android.app.Activity());
  w.connect(1,"127.0.0.1",48080); Thread.sleep(500);
  System.out.println("sendText melody: "+w.sendText(1,"{\"action\":\"melody\"}"));
  System.out.println("sendBytes reserve: "+w.sendBytes(1,"2,48,50,48,57,48,51"));
  System.out.println("sendBytes key -1: "+w.sendBytes(1,"4,255"));
  System.out.println("sendBytes play/pause: "+w.sendBytes(1,"0"));
  Thread.sleep(500);
  w.connect(2,"127.0.0.1",48080); Thread.sleep(400);   // switching connection closes the old one
  System.out.println("old id send after switch: "+w.sendText(1,"x"));
  w.close(2); Thread.sleep(300);
  w.connect(3,"127.0.0.1",48099); Thread.sleep(800);   // nothing listening there
  w.connect(4,"10.255.255.1",48080); Thread.sleep(7000); // unreachable -> timeout
}}
