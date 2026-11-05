package android.net.wifi;
public class WifiManager { public MulticastLock createMulticastLock(String t){return new MulticastLock();}
  public class MulticastLock { boolean h; public void acquire(){h=true;System.out.println("multicast lock on");} public void release(){h=false;System.out.println("multicast lock off");} public boolean isHeld(){return h;} } }
