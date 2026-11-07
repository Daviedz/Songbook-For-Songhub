package com.songbook.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Makes the phone act as a Bluetooth keyboard (HID device) that SongHub's player reacts to.
 * SongHub keys (from its PlayerActivity.dispatchKeyEvent):
 *   0-9 type the song number, Enter reserves it, Backspace deletes a digit,
 *   Media Play/Pause, Media Stop, Media Next, F2 melody, F3/F4 key -/+, F5/F6 tempo -/+, F7 background (BGV).
 *   Back/Esc is never sent: SongHub uses it to exit the player.
 * JS: AndroidBt.available(), start(addr), devices(), connect(addr), disconnect(), stop(),
 *     discoverable(), send("1,2,enter"), phoneName().  Callback: window.__kbt.update(jsonString)
 */
public class BtKeypad {
    private static final String P_CONNECT = "android.permission.BLUETOOTH_CONNECT";
    private static final String P_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE";

    // Report 1: boot-style keyboard (modifier, reserved, 6 keys). Report 2: consumer control (one 16-bit usage).
    private static final byte[] DESCRIPTOR = new byte[]{
            0x05, 0x01, 0x09, 0x06, (byte) 0xA1, 0x01, (byte) 0x85, 0x01,
            0x05, 0x07, 0x19, (byte) 0xE0, 0x29, (byte) 0xE7, 0x15, 0x00, 0x25, 0x01, 0x75, 0x01, (byte) 0x95, 0x08, (byte) 0x81, 0x02,
            (byte) 0x95, 0x01, 0x75, 0x08, (byte) 0x81, 0x01,
            (byte) 0x95, 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65, 0x05, 0x07, 0x19, 0x00, 0x29, 0x65, (byte) 0x81, 0x00,
            (byte) 0xC0,
            0x05, 0x0C, 0x09, 0x01, (byte) 0xA1, 0x01, (byte) 0x85, 0x02,
            0x15, 0x00, 0x26, (byte) 0xFF, 0x03, 0x19, 0x00, 0x2A, (byte) 0xFF, 0x03, 0x75, 0x10, (byte) 0x95, 0x01, (byte) 0x81, 0x00,
            (byte) 0xC0
    };

    private final Activity act;
    private final WebView web;
    private final ExecutorService cbExec = Executors.newSingleThreadExecutor();
    private final ExecutorService keyExec = Executors.newSingleThreadExecutor();

    private BluetoothAdapter adapter;
    private volatile BluetoothHidDevice hid;
    private volatile boolean registered;
    private volatile BluetoothDevice host;
    private volatile String wantAddr;
    private volatile String state = "off";
    private volatile String error = "";
    private volatile boolean starting;
    private Object callback;   // created lazily so older Android never loads the HID classes

    public BtKeypad(Activity activity, WebView web) { this.act = activity; this.web = web; }

    @JavascriptInterface
    public boolean available() {
        try { return Build.VERSION.SDK_INT >= 28 && BluetoothAdapter.getDefaultAdapter() != null; }
        catch (Throwable t) { return false; }
    }

    @JavascriptInterface
    public String phoneName() {
        try { BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter(); return a == null ? "" : String.valueOf(a.getName()); }
        catch (Throwable t) { return ""; }
    }

    /** Registers the keyboard; connects to addr (if given) once ready. Safe to call repeatedly. */
    @JavascriptInterface
    public void start(final String addr) {
        if (addr != null && !addr.isEmpty()) wantAddr = addr;
        if (starting) return;
        starting = true;
        new Thread(new Runnable() { public void run() {
            try { doStart(); } finally { starting = false; }
        } }, "songbook-bt-start").start();
    }

    private void doStart() {
        try {
            if (Build.VERSION.SDK_INT < 28) { fail("Bluetooth keypad needs Android 9 or newer"); return; }
            adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) { fail("This phone has no Bluetooth"); return; }
            if (Build.VERSION.SDK_INT >= 31 && act.checkSelfPermission(P_CONNECT) != 0) {
                set("permission", "");
                try { act.requestPermissions(Build.VERSION.SDK_INT >= 33 ? new String[]{P_CONNECT, P_ADVERTISE, "android.permission.POST_NOTIFICATIONS"} : new String[]{P_CONNECT, P_ADVERTISE}, 21); } catch (Throwable ignored) { }
                for (int i = 0; i < 120 && act.checkSelfPermission(P_CONNECT) != 0; i++) sleep(500);
                if (act.checkSelfPermission(P_CONNECT) != 0) { fail("Bluetooth permission was not allowed. Allow \"Nearby devices\" for Songbook in Settings."); return; }
            }
            if (!adapter.isEnabled()) {
                set("bt_off", "");
                try { act.startActivity(new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE")); } catch (Throwable ignored) { }
                for (int i = 0; i < 60 && !adapter.isEnabled(); i++) sleep(500);
                if (!adapter.isEnabled()) { fail("Bluetooth is off. Turn it on and try again."); return; }
            }
            if (hid == null) {
                set("registering", "");
                boolean ok = adapter.getProfileProxy(act.getApplicationContext(), new BluetoothProfile.ServiceListener() {
                    public void onServiceConnected(int profile, BluetoothProfile proxy) {
                        if (profile != BluetoothProfile.HID_DEVICE) return;
                        hid = (BluetoothHidDevice) proxy;
                        register();
                    }
                    public void onServiceDisconnected(int profile) {
                        if (profile != BluetoothProfile.HID_DEVICE) return;
                        hid = null; registered = false; host = null; set("off", "Bluetooth keypad service stopped");
                    }
                }, BluetoothProfile.HID_DEVICE);
                if (!ok) fail("This phone does not support acting as a Bluetooth keyboard");
            } else if (!registered) {
                register();
            } else {
                if (host == null && wantAddr != null) connect(wantAddr); else push();
            }
        } catch (Throwable t) {
            fail("Bluetooth error: " + t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : ""));
        }
    }

    private void register() {
        try {
            BluetoothHidDevice h = hid;
            if (h == null) return;
            if (callback == null) callback = new BluetoothHidDevice.Callback() {
                @Override public void onAppStatusChanged(BluetoothDevice plugged, boolean reg) {
                    registered = reg;
                    if (!reg) { host = null; set("off", ""); KeypadService.end(act.getApplicationContext()); return; }
                    if (plugged != null) host = plugged;
                    set(host != null ? "connected" : "ready", "");
                    String w = wantAddr;
                    if (host == null && w != null) connect(w);
                }
                @Override public void onConnectionStateChanged(BluetoothDevice d, int st) {
                    if (st == BluetoothProfile.STATE_CONNECTED) {
                        host = d; wantAddr = d.getAddress(); set("connected", "");
                        String nm = ""; try { nm = String.valueOf(d.getName()); } catch (Throwable ignored) { }
                        KeypadService.begin(act.getApplicationContext(), "Connected to " + nm);
                    }
                    else if (st == BluetoothProfile.STATE_CONNECTING) { set("connecting", ""); }
                    else if (st == BluetoothProfile.STATE_DISCONNECTED) {
                        if (host == null || d == null || same(d, host)) host = null;
                        if (host == null) { set(registered ? "ready" : "off", ""); if (registered) KeypadService.begin(act.getApplicationContext(), "Bluetooth keypad is on, not connected"); }
                    }
                }
                @Override public void onGetReport(BluetoothDevice d, byte type, byte id, int size) {
                    try { BluetoothHidDevice x = hid; if (x != null) x.replyReport(d, type, id, new byte[id == 2 ? 2 : 8]); } catch (Throwable ignored) { }
                }
                @Override public void onVirtualCableUnplug(BluetoothDevice d) { host = null; set(registered ? "ready" : "off", ""); }
            };
            BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                    "Songbook Keypad", "Songbook remote keypad for SongHub", "Songbook",
                    BluetoothHidDevice.SUBCLASS1_KEYBOARD, DESCRIPTOR);
            set("registering", "");
            KeypadService.begin(act.getApplicationContext(), "Bluetooth keypad is on");
            if (!h.registerApp(sdp, null, null, cbExec, (BluetoothHidDevice.Callback) callback)) {
                KeypadService.end(act.getApplicationContext());
                fail("Could not start the Bluetooth keypad. Close other keyboard/remote apps and try again.");
            }
        } catch (Throwable t) {
            fail("Bluetooth error: " + t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : ""));
        }
    }

    @JavascriptInterface
    public String devices() {
        StringBuilder sb = new StringBuilder("[");
        try {
            BluetoothAdapter a = adapter != null ? adapter : BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> set = a == null ? null : a.getBondedDevices();
            if (set != null) {
                List<BluetoothDevice> list = new ArrayList<BluetoothDevice>(set);
                boolean first = true;
                for (BluetoothDevice d : list) {
                    int major = -1;
                    try { BluetoothClass c = d.getBluetoothClass(); if (c != null) major = c.getMajorDeviceClass(); } catch (Throwable ignored) { }
                    if (!first) sb.append(',');
                    first = false;
                    sb.append("{\"name\":").append(JSONObject.quote(String.valueOf(d.getName())))
                      .append(",\"addr\":").append(JSONObject.quote(d.getAddress()))
                      .append(",\"major\":").append(major).append('}');
                }
            }
        } catch (Throwable ignored) { }
        return sb.append(']').toString();
    }

    @JavascriptInterface
    public void connect(String addr) {
        try {
            wantAddr = addr;
            BluetoothHidDevice h = hid;
            if (h == null || !registered) { start(addr); return; }
            BluetoothDevice d = adapter.getRemoteDevice(addr);
            BluetoothDevice cur = host;
            if (cur != null && !same(cur, d)) h.disconnect(cur);
            set("connecting", "");
            if (!h.connect(d)) fail("Could not connect. Make sure the karaoke device is paired and its Bluetooth is on.");
        } catch (Throwable t) {
            fail("Bluetooth error: " + t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : ""));
        }
    }

    @JavascriptInterface
    public void disconnect() {
        wantAddr = null;
        try { BluetoothHidDevice h = hid; BluetoothDevice d = host; if (h != null && d != null) h.disconnect(d); } catch (Throwable ignored) { }
        host = null;
        set(registered ? "ready" : "off", "");
    }

    /** Leaves Bluetooth keypad mode completely. */
    @JavascriptInterface
    public void stop() {
        wantAddr = null;
        try {
            BluetoothHidDevice h = hid; BluetoothDevice d = host;
            if (h != null && d != null) h.disconnect(d);
            if (h != null) h.unregisterApp();
            if (h != null && adapter != null) adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, h);
        } catch (Throwable ignored) { }
        hid = null; registered = false; host = null;
        KeypadService.end(act.getApplicationContext());
        set("off", "");
    }

    @JavascriptInterface
    public void discoverable() {
        try {
            final Intent i = new Intent("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE")
                    .putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 300);
            act.runOnUiThread(new Runnable() { public void run() { try { act.startActivity(i); } catch (Throwable ignored) { } } });
        } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public String status() { return statusJson(); }

    /** Sends a comma-separated list of key names in order, e.g. "bksp,0,2,5,9,0,5,enter". */
    @JavascriptInterface
    public boolean send(String csv) {
        final BluetoothDevice d = host;
        final BluetoothHidDevice h = hid;
        if (d == null || h == null || csv == null) return false;
        final String[] keys = csv.split(",");
        for (String k : keys) if (usage(k.trim()) == null) return false;
        keyExec.execute(new Runnable() { public void run() {
            for (String k : keys) {
                int[] u = usage(k.trim());
                try {
                    if (u[0] == 1) {
                        h.sendReport(d, 1, new byte[]{0, 0, (byte) u[1], 0, 0, 0, 0, 0}); sleep(25);
                        h.sendReport(d, 1, new byte[8]); sleep(30);
                    } else {
                        h.sendReport(d, 2, new byte[]{(byte) (u[1] & 0xff), (byte) (u[1] >> 8)}); sleep(30);
                        h.sendReport(d, 2, new byte[2]); sleep(50);
                    }
                } catch (Throwable ignored) { }
            }
        } });
        return true;
    }

    /** {report, usage}: report 1 = keyboard usage, report 2 = consumer usage. */
    static int[] usage(String k) {
        if (k.length() == 1 && k.charAt(0) >= '1' && k.charAt(0) <= '9') return new int[]{1, 0x1E + (k.charAt(0) - '1')};
        if (k.equals("0")) return new int[]{1, 0x27};
        if (k.equals("enter")) return new int[]{1, 0x28};
        if (k.equals("bksp")) return new int[]{1, 0x2A};
        if (k.equals("f2")) return new int[]{1, 0x3B};
        if (k.equals("f3")) return new int[]{1, 0x3C};
        if (k.equals("f4")) return new int[]{1, 0x3D};
        if (k.equals("f5")) return new int[]{1, 0x3E};
        if (k.equals("f6")) return new int[]{1, 0x3F};
        if (k.equals("f7")) return new int[]{1, 0x40};   // SongHub: change background (BGV)
        if (k.equals("playpause")) return new int[]{2, 0xCD};
        if (k.equals("stop")) return new int[]{2, 0xB7};
        if (k.equals("next")) return new int[]{2, 0xB5};
        return null;
    }

    private static boolean same(BluetoothDevice a, BluetoothDevice b) {
        try { return a.getAddress().equals(b.getAddress()); } catch (Throwable t) { return false; }
    }

    private void set(String s, String err) { state = s; error = err == null ? "" : err; push(); }
    private void fail(String msg) { state = "error"; error = msg; push(); }

    private String statusJson() {
        BluetoothDevice d = host;
        String name = "", addr = "";
        try { if (d != null) { name = String.valueOf(d.getName()); addr = d.getAddress(); } } catch (Throwable ignored) { }
        return "{\"state\":" + JSONObject.quote(state) + ",\"error\":" + JSONObject.quote(error) +
                ",\"registered\":" + registered + ",\"name\":" + JSONObject.quote(name) + ",\"addr\":" + JSONObject.quote(addr) + "}";
    }

    private void push() {
        final String code = "window.__kbt&&__kbt.update(" + JSONObject.quote(statusJson()) + ")";
        web.post(new Runnable() { public void run() { try { web.evaluateJavascript(code, null); } catch (Throwable ignored) { } } });
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { } }
}
