package android.content;
public class Context { public Context getApplicationContext(){return this;} public Object getSystemService(String n){return "wifi".equals(n)?new android.net.wifi.WifiManager():null;}
  public void startActivity(Intent i){System.out.println("startActivity "+i.a+" "+i.x);}
  public ComponentName startService(Intent i){System.out.println("startService "+i.x);return null;}
  public ComponentName startForegroundService(Intent i){System.out.println("SERVICE start: "+i.x);return null;}
  public boolean stopService(Intent i){System.out.println("SERVICE stop");return true;}
  public android.content.pm.ApplicationInfo getApplicationInfo(){return new android.content.pm.ApplicationInfo();} }
