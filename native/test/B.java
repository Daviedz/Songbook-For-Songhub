import com.songbook.app.BtKeypad;
public class B { public static void main(String[] a) throws Exception {
  BtKeypad k=new BtKeypad(new android.app.Activity(), new android.webkit.WebView());
  System.out.println("available="+k.available()+" phone="+k.phoneName());
  System.out.println("devices="+k.devices());
  k.start("AA:BB:CC:00:00:28"); Thread.sleep(600);
  System.out.println("status="+k.status());
  System.out.println("== reserve 025905:"); k.send("bksp,0,2,5,9,0,5,enter"); Thread.sleep(1500);
  System.out.println("== play/pause, stop, next:"); k.send("playpause,stop,next"); Thread.sleep(700);
  System.out.println("== key+, tempo-, melody:"); k.send("f4,f5,f2"); Thread.sleep(600);
  System.out.println("unknown key rejected: "+!k.send("esc"));
  k.disconnect(); Thread.sleep(200); k.stop(); Thread.sleep(200);
  System.exit(0);
}}
