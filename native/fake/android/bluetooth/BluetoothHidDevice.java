package android.bluetooth;
public final class BluetoothHidDevice implements BluetoothProfile {
  public static final byte SUBCLASS1_KEYBOARD=(byte)0x40;
  public abstract static class Callback {
    public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered){}
    public void onConnectionStateChanged(BluetoothDevice device, int state){}
    public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize){}
    public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data){}
    public void onSetProtocol(BluetoothDevice device, byte protocol){}
    public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data){}
    public void onVirtualCableUnplug(BluetoothDevice device){} }
  Callback cb; java.util.concurrent.Executor ex;
  public boolean registerApp(BluetoothHidDeviceAppSdpSettings s, BluetoothHidDeviceAppQosSettings in, BluetoothHidDeviceAppQosSettings out, java.util.concurrent.Executor e, Callback c){
    cb=c;ex=e;System.out.println("registerApp name="+s.n+" subclass=0x"+Integer.toHexString(s.sub&255)+" descriptor="+s.desc.length+" bytes");ex.execute(()->cb.onAppStatusChanged(null,true));return true;}
  public boolean unregisterApp(){System.out.println("unregisterApp");return true;}
  public boolean sendReport(BluetoothDevice d, int id, byte[] data){StringBuilder b=new StringBuilder();for(byte x:data)b.append(String.format("%02X ",x));System.out.println("  report id="+id+" ["+b.toString().trim()+"]");return true;}
  public boolean replyReport(BluetoothDevice d, byte type, byte id, byte[] data){return true;}
  public boolean connect(BluetoothDevice d){System.out.println("connect "+d.getName());ex.execute(()->{cb.onConnectionStateChanged(d,1);cb.onConnectionStateChanged(d,2);});return true;}
  public boolean disconnect(BluetoothDevice d){System.out.println("disconnect "+d.getName());ex.execute(()->cb.onConnectionStateChanged(d,0));return true;}
  public java.util.List<BluetoothDevice> getConnectedDevices(){return new java.util.ArrayList<>();}
}
