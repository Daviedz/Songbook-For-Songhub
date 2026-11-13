package android.content;
public class Context { public Context getApplicationContext(){throw new RuntimeException();} public Object getSystemService(String n){throw new RuntimeException();}
  public void startActivity(Intent i){throw new RuntimeException();} public ComponentName startService(Intent i){throw new RuntimeException();} public ComponentName startForegroundService(Intent i){throw new RuntimeException();}
  public boolean stopService(Intent i){throw new RuntimeException();} public android.content.pm.ApplicationInfo getApplicationInfo(){throw new RuntimeException();}
  public java.io.File getCacheDir(){throw new RuntimeException();} public String getPackageName(){throw new RuntimeException();}
  public android.content.pm.PackageManager getPackageManager(){throw new RuntimeException();}
  public Intent registerReceiver(BroadcastReceiver r, IntentFilter f){throw new RuntimeException();}
  public Intent registerReceiver(BroadcastReceiver r, IntentFilter f, int flags){throw new RuntimeException();}
  public void unregisterReceiver(BroadcastReceiver r){throw new RuntimeException();} }
