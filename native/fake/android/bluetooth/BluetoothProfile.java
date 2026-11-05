package android.bluetooth;
public interface BluetoothProfile { int STATE_DISCONNECTED=0; int STATE_CONNECTING=1; int STATE_CONNECTED=2; int STATE_DISCONNECTING=3; int HID_DEVICE=19;
  interface ServiceListener { void onServiceConnected(int profile, BluetoothProfile proxy); void onServiceDisconnected(int profile); } }
