package com.songbook.app;

import android.app.Activity;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.SecureRandom;

/**
 * Minimal WebSocket client used by the Songbook page instead of the WebView's own WebSocket,
 * so connections to other devices on the Wi-Fi are made by the app itself (like SongHub's own remote).
 * JS calls: AndroidWs.connect(id, host, port), sendText(id, s), sendBytes(id, "1,2,3"), close(id)
 * Callbacks: window.__kws.open(id), .msg(id, text), .close(id, reason)
 */
public class NativeWs {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset ASCII = Charset.forName("US-ASCII");
    private static final SecureRandom RND = new SecureRandom();

    private final WebView web;
    private final Activity act;
    private volatile Conn cur;
    private volatile int scanId = 0;

    public NativeWs(Activity activity, WebView web) { this.web = web; this.act = activity; }

    /** Android 17 local network permission; harmless on versions that don't have it. */
    public static void askPermission(Activity a) {
        try {
            if (Build.VERSION.SDK_INT >= 36) {
                String p = "android.permission.ACCESS_LOCAL_NETWORK";
                if (a.checkSelfPermission(p) != 0) a.requestPermissions(new String[]{p}, 7);
            }
        } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public void connect(int id, String host, int port) {
        Conn old = cur;
        if (old != null) old.shutdown();
        Conn c = new Conn(id, host, port);
        cur = c;
        Thread t = new Thread(c, "songbook-ws-" + id);
        t.setDaemon(true);
        t.start();
    }

    @JavascriptInterface
    public boolean sendText(int id, String s) {
        Conn c = cur;
        return c != null && c.id == id && c.sendFrame(1, s.getBytes(UTF8));
    }

    @JavascriptInterface
    public boolean sendBytes(int id, String csv) {
        Conn c = cur;
        if (c == null || c.id != id) return false;
        String[] parts = csv.isEmpty() ? new String[0] : csv.split(",");
        byte[] b = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) b[i] = (byte) Integer.parseInt(parts[i].trim());
        return c.sendFrame(2, b);
    }

    @JavascriptInterface
    public void close(int id) {
        Conn c = cur;
        if (c != null && c.id == id) c.shutdown();
    }


    // ---------------- SongHub discovery ----------------
    // SongHub broadcasts "SONGHUB_SERVER:<port>" to UDP 48080 every 1-5 s and greets each
    // WebSocket client with {"type":"welcome","server":"SongHub ...","ip":"..."}.
    // We listen for the beacon and also sweep the local /24 for port 48080, then confirm each with the welcome.
    // JS callbacks: window.__kscan.found(id, host, port, name, ip) and .done(id, count)
    private static final Pattern BEACON = Pattern.compile("SONGHUB_SERVER:(\\d{1,5})");
    private static final Pattern SERVER = Pattern.compile("\"server\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern IP = Pattern.compile("\"ip\"\\s*:\\s*\"([^\"]*)\"");

    @JavascriptInterface
    public void scan(final int id, final int ms) {
        scanId = id;
        Thread t = new Thread(new Runnable() { public void run() { doScan(id, Math.max(2000, Math.min(ms, 15000))); } }, "songbook-scan");
        t.setDaemon(true);
        t.start();
    }

    @JavascriptInterface
    public void stopScan() { scanId = -1; }

    private void doScan(final int id, final int ms) {
        final long deadline = System.currentTimeMillis() + ms;
        final Set<String> seen = new HashSet<String>();
        final int[] count = {0};
        Object lock = null;
        try {
            Object wm = act.getApplicationContext().getSystemService("wifi");
            if (wm instanceof android.net.wifi.WifiManager) {
                android.net.wifi.WifiManager.MulticastLock ml = ((android.net.wifi.WifiManager) wm).createMulticastLock("songbook-scan");
                ml.acquire(); lock = ml;
            }
        } catch (Throwable ignored) { }
        final ExecutorService pool = Executors.newFixedThreadPool(48);
        // 1) listen for SongHub's beacon
        Thread udp = new Thread(new Runnable() { public void run() {
            DatagramSocket ds = null;
            try {
                ds = new DatagramSocket(null);
                ds.setReuseAddress(true);
                ds.bind(new InetSocketAddress(48080));
                ds.setSoTimeout(400);
                byte[] buf = new byte[256];
                while (System.currentTimeMillis() < deadline && scanId == id) {
                    DatagramPacket pk = new DatagramPacket(buf, buf.length);
                    try { ds.receive(pk); } catch (java.net.SocketTimeoutException te) { continue; }
                    Matcher m = BEACON.matcher(new String(pk.getData(), 0, pk.getLength(), ASCII));
                    if (m.find()) {
                        final String host = pk.getAddress().getHostAddress();
                        final int port = Integer.parseInt(m.group(1));
                        try { pool.execute(new Runnable() { public void run() { probe(id, host, port, seen, count, 2500); } }); } catch (Throwable ignored) { }
                    }
                }
            } catch (Throwable ignored) {
            } finally { if (ds != null) ds.close(); }
        } }, "songbook-beacon");
        udp.setDaemon(true);
        udp.start();
        // 2) sweep this phone's own network for port 48080 (plus this phone itself)
        List<String> hosts = new ArrayList<String>();
        hosts.add("127.0.0.1");
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress a = ia.getAddress();
                    if (!(a instanceof Inet4Address) || !a.isSiteLocalAddress()) continue;
                    byte[] b = a.getAddress();
                    String base = (b[0] & 255) + "." + (b[1] & 255) + "." + (b[2] & 255) + ".";
                    for (int i = 1; i < 255; i++) { String h = base + i; if (i != (b[3] & 255) && !hosts.contains(h)) hosts.add(h); }
                }
            }
        } catch (Throwable ignored) { }
        for (final String h : hosts) {
            try { pool.execute(new Runnable() { public void run() {
                if (scanId != id || System.currentTimeMillis() > deadline) return;
                Socket s = new Socket();
                try { s.connect(new InetSocketAddress(h, 48080), 450); s.close(); probe(id, h, 48080, seen, count, 2000); }
                catch (Throwable ignored) { try { s.close(); } catch (IOException ignored2) { } }
            } }); } catch (Throwable ignored) { }
        }
        try { udp.join(Math.max(0, deadline - System.currentTimeMillis()) + 500); } catch (InterruptedException ignored) { }
        pool.shutdown();
        try { pool.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        try { if (lock != null) ((android.net.wifi.WifiManager.MulticastLock) lock).release(); } catch (Throwable ignored) { }
        if (scanId == id) js("window.__kscan&&__kscan.done(" + id + "," + count[0] + ")");
    }

    /** Opens a WebSocket, waits for SongHub's welcome, reports it, closes. */
    private void probe(int id, String host, int port, Set<String> seen, int[] count, int timeout) {
        synchronized (seen) { if (!seen.add(host + ":" + port)) return; }
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setSoTimeout(timeout);
            InputStream in = new BufferedInputStream(s.getInputStream());
            OutputStream out = s.getOutputStream();
            byte[] k = new byte[16]; RND.nextBytes(k);
            out.write(("GET / HTTP/1.1\r\nHost: " + host + ":" + port + "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + b64(k) + "\r\nSec-WebSocket-Version: 13\r\n\r\n").getBytes(ASCII));
            out.flush();
            String status = readLine(in);
            if (status == null || !status.contains(" 101")) return;
            while (true) { String l = readLine(in); if (l == null) return; if (l.isEmpty()) break; }
            String name = "", ip = "";
            for (int f = 0; f < 3; f++) {          // first text frame(s): look for the welcome
                int b0 = rd(in), b1 = rd(in);
                long len = b1 & 0x7f;
                if (len == 126) len = (rd(in) << 8) | rd(in);
                else if (len == 127) { len = 0; for (int i = 0; i < 8; i++) len = (len << 8) | rd(in); }
                if (len > 1024 * 1024) return;
                byte[] p = new byte[(int) len]; readFully(in, p);
                if ((b0 & 0x0f) != 1) continue;
                String txt = new String(p, UTF8);
                if (txt.contains("\"welcome\"")) {
                    Matcher m = SERVER.matcher(txt); if (m.find()) name = m.group(1);
                    m = IP.matcher(txt); if (m.find()) ip = m.group(1);
                    break;
                }
                if (txt.contains("\"state_update\"")) break;   // a SongHub that skipped the welcome
            }
            try { out.write(new byte[]{(byte) 0x88, (byte) 0x80, 1, 2, 3, 4}); out.flush(); } catch (IOException ignored) { }
            synchronized (seen) { count[0]++; }
            if (scanId == id) js("window.__kscan&&__kscan.found(" + id + "," + JSONObject.quote(host) + "," + port + "," + JSONObject.quote(name) + "," + JSONObject.quote(ip) + ")");
        } catch (Throwable ignored) {
        } finally { try { s.close(); } catch (IOException ignored) { } }
    }

    void js(final String code) {
        web.post(new Runnable() {
            public void run() {
                try { web.evaluateJavascript(code, null); } catch (Throwable ignored) { }
            }
        });
    }

    static String b64(byte[] in) {
        final String T = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < in.length; i += 3) {
            int n = (in[i] & 0xff) << 16 | (i + 1 < in.length ? (in[i + 1] & 0xff) << 8 : 0) | (i + 2 < in.length ? (in[i + 2] & 0xff) : 0);
            sb.append(T.charAt(n >> 18 & 63)).append(T.charAt(n >> 12 & 63));
            sb.append(i + 1 < in.length ? T.charAt(n >> 6 & 63) : '=');
            sb.append(i + 2 < in.length ? T.charAt(n & 63) : '=');
        }
        return sb.toString();
    }

    final class Conn implements Runnable {
        final int id; final String host; final int port;
        private Socket sock; private OutputStream out;
        private volatile boolean closed; private boolean notified;

        Conn(int id, String host, int port) { this.id = id; this.host = host; this.port = port; }

        public void run() {
            try {
                Socket s = new Socket();
                synchronized (this) { sock = s; }
                s.connect(new InetSocketAddress(host, port), 6000);
                if (closed) { finish("closed"); return; }
                s.setTcpNoDelay(true);
                InputStream in = new BufferedInputStream(s.getInputStream());
                synchronized (this) { out = s.getOutputStream(); }
                byte[] k = new byte[16]; RND.nextBytes(k);
                String req = "GET / HTTP/1.1\r\nHost: " + host + ":" + port +
                        "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + b64(k) +
                        "\r\nSec-WebSocket-Version: 13\r\n\r\n";
                synchronized (this) { out.write(req.getBytes(ASCII)); out.flush(); }
                String status = readLine(in);
                if (status == null || !status.contains(" 101")) throw new IOException("SongHub refused the connection (" + status + ")");
                while (true) { String l = readLine(in); if (l == null) throw new IOException("Connection closed during handshake"); if (l.isEmpty()) break; }
                js("window.__kws&&__kws.open(" + id + ")");
                ByteArrayOutputStream msg = new ByteArrayOutputStream();
                int msgOp = 0;
                while (!closed) {
                    int b0 = in.read(); if (b0 < 0) break;
                    int b1 = in.read(); if (b1 < 0) break;
                    boolean fin = (b0 & 0x80) != 0; int op = b0 & 0x0f; boolean masked = (b1 & 0x80) != 0;
                    long len = b1 & 0x7f;
                    if (len == 126) len = (rd(in) << 8) | rd(in);
                    else if (len == 127) { len = 0; for (int i = 0; i < 8; i++) len = (len << 8) | rd(in); }
                    byte[] mask = null;
                    if (masked) { mask = new byte[4]; readFully(in, mask); }
                    if (len > 32L * 1024 * 1024) throw new IOException("Message too large");
                    byte[] p = new byte[(int) len]; readFully(in, p);
                    if (mask != null) for (int i = 0; i < p.length; i++) p[i] ^= mask[i & 3];
                    if (op == 8) { sendFrame(8, new byte[0]); break; }
                    if (op == 9) { sendFrame(10, p); continue; }
                    if (op == 10) continue;
                    if (op == 1 || op == 2) { msg.reset(); msgOp = op; }
                    msg.write(p, 0, p.length);
                    if (fin) {
                        if (msgOp == 1) js("window.__kws&&__kws.msg(" + id + "," + JSONObject.quote(new String(msg.toByteArray(), UTF8)) + ")");
                        msg.reset();
                    }
                }
                finish("SongHub closed the connection");
            } catch (Throwable e) {
                finish(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        synchronized boolean sendFrame(int op, byte[] payload) {
            if (out == null || closed && op != 8) return false;
            try {
                ByteArrayOutputStream f = new ByteArrayOutputStream();
                f.write(0x80 | op);
                int n = payload.length;
                if (n < 126) f.write(0x80 | n);
                else if (n < 65536) { f.write(0x80 | 126); f.write(n >> 8); f.write(n & 0xff); }
                else { f.write(0x80 | 127); for (int i = 7; i >= 0; i--) f.write((int) ((long) n >> (8 * i)) & 0xff); }
                byte[] m = new byte[4]; RND.nextBytes(m); f.write(m, 0, 4);
                for (int i = 0; i < n; i++) f.write(payload[i] ^ m[i & 3]);
                out.write(f.toByteArray()); out.flush();
                return true;
            } catch (IOException e) { return false; }
        }

        void shutdown() {
            closed = true;
            sendFrame(8, new byte[0]);
            synchronized (this) { try { if (sock != null) sock.close(); } catch (IOException ignored) { } }
        }

        private synchronized void finish(String reason) {
            if (notified) return;
            notified = true; closed = true;
            try { if (sock != null) sock.close(); } catch (IOException ignored) { }
            js("window.__kws&&__kws.close(" + id + "," + JSONObject.quote(reason == null ? "closed" : reason) + ")");
        }
    }

    static int rd(InputStream in) throws IOException { int b = in.read(); if (b < 0) throw new IOException("Connection closed"); return b; }

    static void readFully(InputStream in, byte[] b) throws IOException {
        int off = 0;
        while (off < b.length) { int r = in.read(b, off, b.length - off); if (r < 0) throw new IOException("Connection closed"); off += r; }
    }

    static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = in.read();
            if (c < 0) return sb.length() == 0 ? null : sb.toString();
            if (c == '\n') break;
            if (c != '\r') sb.append((char) c);
            if (sb.length() > 8192) throw new IOException("Header too long");
        }
        return sb.toString();
    }
}
