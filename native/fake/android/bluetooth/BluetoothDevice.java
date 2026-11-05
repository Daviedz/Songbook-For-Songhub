package android.bluetooth;
public class BluetoothDevice { String n,a; int m; public BluetoothDevice(String n,String a,int m){this.n=n;this.a=a;this.m=m;} public String getName(){return n;} public String getAddress(){return a;} public BluetoothClass getBluetoothClass(){return new BluetoothClass(m);} }
