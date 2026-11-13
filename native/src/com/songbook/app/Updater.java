package com.songbook.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * In-app updater: the page asks GitHub for the latest release, then this class downloads the APK and hands it to
 * Android's package installer (the system shows its own "Update this app?" confirmation).
 * JS calls:  AndroidUp.get(id, url)      -> window.__upd.got(id, ok, text)        (plain HTTPS GET, text body)
 *            AndroidUp.download(url)     -> window.__upd.progress(pct) / .state(text) / .fail(message)
 */
public class Updater {
    private static final String ACTION = "com.songbook.app.UPDATE_RESULT";
    private final Activity act;
    private final WebView web;
    private volatile boolean busy = false;
    private BroadcastReceiver receiver;

    public Updater(Activity activity, WebView web) { this.act = activity; this.web = web; }

    private void js(final String code) {
        try { web.post(new Runnable() { public void run() { web.evaluateJavascript(code, null); } }); } catch (Throwable ignored) { }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Songbook-Android-Updater");
        c.setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream, */*");
        return c;
    }

    @JavascriptInterface
    public void get(final String id, final String url) {
        new Thread(new Runnable() {
            public void run() {
                String out; boolean ok = false;
                try {
                    HttpURLConnection c = open(url);
                    int code = c.getResponseCode();
                    InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192]; int n;
                    while (in != null && (n = in.read(buf)) > 0 && b.size() < 400000) b.write(buf, 0, n);
                    out = b.toString("UTF-8");
                    ok = code == 200;
                    if (!ok) out = "HTTP " + code;
                } catch (Throwable e) { out = String.valueOf(e.getMessage()); }
                js("window.__upd&&window.__upd.got(" + JSONObject.quote(id) + "," + ok + "," + JSONObject.quote(out) + ")");
            }
        }).start();
    }

    @JavascriptInterface
    public void download(final String url) {
        if (busy) return;
        busy = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    File f = new File(act.getCacheDir(), "update.apk");
                    HttpURLConnection c = open(url);
                    if (c.getResponseCode() != 200) throw new Exception("Download failed (HTTP " + c.getResponseCode() + ")");
                    long total = c.getContentLength();
                    InputStream in = c.getInputStream();
                    OutputStream os = new FileOutputStream(f);
                    byte[] buf = new byte[32768]; int n; long got = 0; int last = -1;
                    while ((n = in.read(buf)) > 0) {
                        os.write(buf, 0, n); got += n;
                        if (total > 0) {
                            int pct = (int) (got * 100 / total);
                            if (pct != last) { last = pct; js("window.__upd&&window.__upd.progress(" + pct + ")"); }
                        }
                    }
                    os.close(); in.close();
                    if (got < 100000) throw new Exception("The downloaded file is too small");
                    js("window.__upd&&window.__upd.state('Installing...')");
                    install(f, got);
                } catch (Throwable e) {
                    busy = false;
                    js("window.__upd&&window.__upd.fail(" + JSONObject.quote(String.valueOf(e.getMessage())) + ")");
                }
            }
        }).start();
    }

    private void install(File f, long size) throws Exception {
        final Context ctx = act.getApplicationContext();
        if (receiver != null) { try { ctx.unregisterReceiver(receiver); } catch (Throwable ignored) { } }
        receiver = new BroadcastReceiver() {
            public void onReceive(Context c, Intent i) {
                int status = i.getIntExtra("android.content.pm.extra.STATUS", -999);
                if (status == -1) {                                  // STATUS_PENDING_USER_ACTION: Android asks the user to confirm
                    Intent confirm = (Intent) i.getParcelableExtra(Intent.EXTRA_INTENT);
                    if (confirm != null) { confirm.addFlags(0x10000000); c.startActivity(confirm); }
                } else if (status == 0) {
                    busy = false;                                    // success: the app is replaced and restarts
                } else {
                    busy = false;
                    String m = i.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");
                    js("window.__upd&&window.__upd.fail(" + JSONObject.quote(m == null ? "Install failed" : m) + ")");
                }
            }
        };
        IntentFilter fl = new IntentFilter(ACTION);
        if (Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receiver, fl, 4);   // RECEIVER_NOT_EXPORTED
        else ctx.registerReceiver(receiver, fl);

        PackageInstaller pi = act.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams p = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        int id = pi.createSession(p);
        PackageInstaller.Session s = pi.openSession(id);
        OutputStream os = s.openWrite("songbook", 0, size);
        InputStream in = new FileInputStream(f);
        byte[] buf = new byte[65536]; int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        in.close();
        s.fsync(os);
        os.close();
        Intent cb = new Intent(ACTION);
        cb.setPackage(act.getPackageName());
        int flags = 0x08000000 | (Build.VERSION.SDK_INT >= 31 ? 0x02000000 : 0);    // UPDATE_CURRENT | MUTABLE (the installer adds extras)
        PendingIntent pend = PendingIntent.getBroadcast(ctx, id, cb, flags);
        s.commit(pend.getIntentSender());
        s.close();
    }
}
