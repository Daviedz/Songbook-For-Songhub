package android.app;
public abstract class Service extends android.content.Context {
  public int onStartCommand(android.content.Intent i, int f, int id){throw new RuntimeException();}
  public abstract android.os.IBinder onBind(android.content.Intent i);
  public final void startForeground(int id, Notification n){throw new RuntimeException();}
  public final void startForeground(int id, Notification n, int type){throw new RuntimeException();}
  public final void stopForeground(boolean r){throw new RuntimeException();}
  public final void stopSelf(){throw new RuntimeException();}
  public void onDestroy(){throw new RuntimeException();} }
