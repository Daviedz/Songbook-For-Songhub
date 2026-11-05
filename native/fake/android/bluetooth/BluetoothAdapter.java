package android.bluetooth;
import java.util.*;
public class BluetoothAdapter {
  static BluetoothAdapter A=new BluetoothAdapter();
  static Map<String,BluetoothDevice> D=new LinkedHashMap<>();
  static { D.put("AA:BB:CC:00:00:28",new BluetoothDevice("Karaoke Phone","AA:BB:CC:00:00:28",0x200)); D.put("11:22:33:44:55:66",new BluetoothDevice("Galaxy Buds","11:22:33:44:55:66",0x400)); }
  public static BluetoothAdapter getDefaultAdapter(){return A;}
  public boolean isEnabled(){return true;}
  public Set<BluetoothDevice> getBondedDevices(){return new LinkedHashSet<>(D.values());}
  public boolean getProfileProxy(android.content.Context c, BluetoothProfile.ServiceListener l, int p){System.out.println("getProfileProxy "+p);new Thread(()->l.onServiceConnected(p,new BluetoothHidDevice())).start();return true;}
  public void closeProfileProxy(int p, BluetoothProfile proxy){System.out.println("closeProfileProxy");}
  public BluetoothDevice getRemoteDevice(String a){return D.get(a);}
  public String getName(){return "Aldrin Xiaomi 17";}
}
