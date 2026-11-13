package com.songbook.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SongHub 5.x "Bluetooth remote": the phone connects to SongHub's Bluetooth server (an RFCOMM serial link) and speaks
 * the same messages as the Wi-Fi remote, so the queue, now playing and every control work over Bluetooth too.
 * Frames in both directions: [type:1][length:4, big endian][payload]; type 0 = text (JSON), type 1 = binary opcodes.
 * It looks like a WebSocket to the page, so the page reuses window.__kws: open(id), msg(id, text), close(id, reason).
 * JS calls: AndroidBr.available(), connect(id, address), sendText(id, s), sendBytes(id, "1,2,3"), close(id)
 */
public class BtRemote {
    private static final UUID SVC = UUID.fromString("5f0d7b6a-3c1e-4a8f-9b52-0c6e8d4a1f37");
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String P_CONNECT = "android.permission.BLUETOOTH_CONNECT";

    private final Activity act;
    private final WebView web;
    private volatile Conn cur;

    private static class Conn {
        final int id;
        volatile BluetoothSocket sock;
        volatile OutputStream out;
        volatile boolean open, closed;
        final ExecutorService writer = Executors.newSingleThreadExecutor();
        Conn(int id) { this.id = id; }
    }

    public BtRemote(Activity activity, WebView web) { this.act = activity; this.web = web; }

    private void js(final String code) {
        try { web.post(new Runnable() { public void run() { web.evaluateJavascript(code, null); } }); } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public boolean available() {
        try { return BluetoothAdapter.getDefaultAdapter() != null; } catch (Throwable t) { return false; }
    }

    @JavascriptInterface
    public void connect(final int id, final String addr) {
        closeCur();
        final Conn c = new Conn(id);
        cur = c;
        new Thread(new Runnable() {
            public void run() {
                String err = null;
                try {
                    if (Build.VERSION.SDK_INT >= 31 && act.checkSelfPermission(P_CONNECT) != 0) {
                        try { act.requestPermissions(new String[]{P_CONNECT}, 22); } catch (Throwable ignored) { }
                        for (int i = 0; i < 120 && act.checkSelfPermission(P_CONNECT) != 0 && !c.closed; i++) Thread.sleep(500);
                        if (act.checkSelfPermission(P_CONNECT) != 0) throw new Exception("Allow \"Nearby devices\" for Songbook, then try again.");
                    }
                    BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
                    if (a == null || !a.isEnabled()) throw new Exception("Turn on Bluetooth first.");
                    BluetoothDevice d = a.getRemoteDevice(addr);
                    try { a.cancelDiscovery(); } catch (Throwable ignored) { }
                    BluetoothSocket s = d.createRfcommSocketToServiceRecord(SVC);
                    c.sock = s;
                    s.connect();
                    if (c.closed) { try { s.close(); } catch (Throwable ignored) { } return; }
                    c.out = s.getOutputStream();
                    c.open = true;
                    js("window.__kws&&window.__kws.open(" + id + ")");
                    DataInputStream in = new DataInputStream(s.getInputStream());
                    while (!c.closed) {
                        int type = in.readUnsignedByte();
                        int len = in.readInt();
                        if (len < 0 || len > 33554432) throw new Exception("Bad frame length " + len);
                        byte[] buf = new byte[len];
                        in.readFully(buf);
                        if (type == 0) js("window.__kws&&window.__kws.msg(" + id + "," + JSONObject.quote(new String(buf, UTF8)) + ")");
                    }
                } catch (Throwable e) {
                    String m = e.getMessage();
                    if (m == null || m.length() == 0) m = e.getClass().getSimpleName();
                    err = m + " (is SongHub's Bluetooth remote turned on, and is the device paired?)";
                }
                if (!c.closed) {
                    c.closed = true;
                    closeQuiet(c);
                    js("window.__kws&&window.__kws.close(" + id + "," + JSONObject.quote(err == null ? "" : err) + ")");
                }
            }
        }).start();
    }

    private boolean send(final int id, final int type, final byte[] payload) {
        final Conn c = cur;
        if (c == null || c.id != id || !c.open || c.closed) return false;
        c.writer.execute(new Runnable() {
            public void run() {
                try {
                    OutputStream o = c.out;
                    byte[] f = new byte[payload.length + 5];
                    f[0] = (byte) type;
                    f[1] = (byte) (payload.length >>> 24);
                    f[2] = (byte) (payload.length >>> 16);
                    f[3] = (byte) (payload.length >>> 8);
                    f[4] = (byte) payload.length;
                    System.arraycopy(payload, 0, f, 5, payload.length);
                    o.write(f);
                    o.flush();
                } catch (Throwable e) {
                    closeQuiet(c);                     // the reader thread notices and reports the close to the page
                }
            }
        });
        return true;
    }

    @JavascriptInterface
    public boolean sendText(int id, String s) { return send(id, 0, s.getBytes(UTF8)); }

    @JavascriptInterface
    public boolean sendBytes(int id, String csv) {
        try {
            String[] p = csv.length() == 0 ? new String[0] : csv.split(",");
            byte[] b = new byte[p.length];
            for (int i = 0; i < p.length; i++) b[i] = (byte) Integer.parseInt(p[i].trim());
            return send(id, 1, b);
        } catch (Throwable t) { return false; }
    }

    @JavascriptInterface
    public void close(int id) {
        Conn c = cur;
        if (c != null && c.id == id) closeCur();
    }

    private void closeCur() {
        Conn c = cur;
        cur = null;
        if (c != null) { c.closed = true; closeQuiet(c); }
    }

    private static void closeQuiet(Conn c) {
        try { if (c.sock != null) c.sock.close(); } catch (Throwable ignored) { }
        try { c.writer.shutdownNow(); } catch (Throwable ignored) { }
    }
}
