package android.bluetooth;
public class BluetoothAdapter {
  public static BluetoothAdapter getDefaultAdapter(){throw new RuntimeException();}
  public boolean isEnabled(){throw new RuntimeException();}
  public java.util.Set<BluetoothDevice> getBondedDevices(){throw new RuntimeException();}
  public boolean getProfileProxy(android.content.Context c, BluetoothProfile.ServiceListener l, int p){throw new RuntimeException();}
  public void closeProfileProxy(int p, BluetoothProfile proxy){throw new RuntimeException();}
  public BluetoothDevice getRemoteDevice(String a){throw new RuntimeException();}
  public String getName(){throw new RuntimeException();}
}
