package android.content;
public class Context { public Context getApplicationContext(){throw new RuntimeException();} public Object getSystemService(String n){throw new RuntimeException();}
  public void startActivity(Intent i){throw new RuntimeException();} public ComponentName startService(Intent i){throw new RuntimeException();} public ComponentName startForegroundService(Intent i){throw new RuntimeException();}
  public boolean stopService(Intent i){throw new RuntimeException();} public android.content.pm.ApplicationInfo getApplicationInfo(){throw new RuntimeException();} }
