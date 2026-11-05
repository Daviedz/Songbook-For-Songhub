package android.bluetooth;
public final class BluetoothHidDevice implements BluetoothProfile {
  public static final byte SUBCLASS1_KEYBOARD = (byte) 0x40;
  public abstract static class Callback {
    public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered){}
    public void onConnectionStateChanged(BluetoothDevice device, int state){}
    public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize){}
    public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data){}
    public void onSetProtocol(BluetoothDevice device, byte protocol){}
    public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data){}
    public void onVirtualCableUnplug(BluetoothDevice device){}
  }
  public boolean registerApp(BluetoothHidDeviceAppSdpSettings s, BluetoothHidDeviceAppQosSettings in, BluetoothHidDeviceAppQosSettings out, java.util.concurrent.Executor e, Callback cb){throw new RuntimeException();}
  public boolean unregisterApp(){throw new RuntimeException();}
  public boolean sendReport(BluetoothDevice d, int id, byte[] data){throw new RuntimeException();}
  public boolean replyReport(BluetoothDevice d, byte type, byte id, byte[] data){throw new RuntimeException();}
  public boolean connect(BluetoothDevice d){throw new RuntimeException();}
  public boolean disconnect(BluetoothDevice d){throw new RuntimeException();}
  public java.util.List<BluetoothDevice> getConnectedDevices(){throw new RuntimeException();}
}
