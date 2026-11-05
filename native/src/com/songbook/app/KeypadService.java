package com.songbook.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/**
 * Keeps the Bluetooth keypad alive while Songbook is in the background.
 * Android drops a phone-as-keyboard (HID device) registration when its app is no longer in the
 * foreground, unless the app runs a foreground service. This shows an ongoing notification.
 */
public class KeypadService extends Service {
    private static final String CHANNEL = "songbook_keypad";
    private static final int FGS_TYPE_CONNECTED_DEVICE = 0x10;

    public static void begin(Context c, String text) {
        try {
            Intent i = new Intent(c, KeypadService.class).putExtra("text", text);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
        } catch (Throwable ignored) { }
    }

    public static void end(Context c) {
        try { c.stopService(new Intent(c, KeypadService.class)); } catch (Throwable ignored) { }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String text = null;
        try { if (intent != null) text = intent.getStringExtra("text"); } catch (Throwable ignored) { }
        if (text == null) text = "Bluetooth keypad is on";
        try {
            Notification n = build(text);
            if (Build.VERSION.SDK_INT >= 29) startForeground(7, n, FGS_TYPE_CONNECTED_DEVICE); else startForeground(7, n);
        } catch (Throwable t) {
            try { stopSelf(); } catch (Throwable ignored) { }
        }
        return 2; // START_NOT_STICKY
    }

    private Notification build(String text) {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                NotificationManager nm = (NotificationManager) getSystemService("notification");
                if (nm != null) nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Bluetooth keypad", 2));
            } catch (Throwable ignored) { }
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        Intent open = new Intent(this, MainActivity.class).setFlags(0x10000000 | 0x20000000 | 0x04000000);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, 0x04000000 | 0x08000000); // IMMUTABLE | UPDATE_CURRENT
        return b.setContentTitle("Songbook keypad").setContentText(text)
                .setSmallIcon(getApplicationInfo().icon).setOngoing(true).setContentIntent(pi).build();
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
