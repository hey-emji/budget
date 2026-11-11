package com.bujjet.app;

import android.app.Notification;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads notifications on the phone and keeps only the ones that mention money
 * (e.g. "PHP 250.00", "₱1,000"). Everything stays on the device.
 */
public class NotifService extends NotificationListenerService {
    private static final Pattern MONEY = Pattern.compile("(PHP|Php|php|₱|\\bP)\\s?\\d|\\d[\\d,]*\\.\\d{2}");
    // Chat and social apps are never read.
    private static final Set<String> SKIP = new HashSet<>(Arrays.asList(
        "com.facebook.orca", "com.facebook.katana", "com.facebook.lite", "com.facebook.mlite",
        "com.whatsapp", "com.whatsapp.w4b", "com.viber.voip", "org.telegram.messenger", "org.thunderdog.challegram",
        "com.instagram.android", "com.discord", "com.snapchat.android", "com.twitter.android", "com.zhiliaoapp.musically",
        "jp.naver.line.android", "com.kakao.talk", "com.tencent.mm", "com.google.android.apps.dynamite"));

    @Override
    public void onListenerConnected() {
        try {
            StatusBarNotification[] all = getActiveNotifications();
            if (all != null) for (StatusBarNotification s : all) handle(s);
        } catch (Exception ignored) { }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try { handle(sbn); } catch (Exception ignored) { }
    }

    private void handle(StatusBarNotification sbn) throws Exception {
        String pkg = sbn.getPackageName();
        if (pkg == null || pkg.equals(getPackageName()) || SKIP.contains(pkg)) return;
        Notification n = sbn.getNotification();
        if (n == null || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        Bundle ex = n.extras;
        if (ex == null) return;
        CharSequence title = ex.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence big = ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
        CharSequence text = ex.getCharSequence(Notification.EXTRA_TEXT);
        String body = big != null ? big.toString() : (text != null ? text.toString() : "");
        CharSequence[] lines = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null && lines.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence l : lines) sb.append(l).append('\n');
            if (sb.length() > body.length()) body = sb.toString();
        }
        String t = title == null ? "" : title.toString();
        if (!MONEY.matcher(t + " " + body).find()) return;
        String label = pkg;
        try {
            PackageManager pm = getPackageManager();
            label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception ignored) { }
        JSONObject o = new JSONObject();
        o.put("pkg", pkg);
        o.put("app", label);
        o.put("title", t);
        o.put("text", body.length() > 1000 ? body.substring(0, 1000) : body);
        o.put("time", sbn.getPostTime());
        o.put("id", pkg + "|" + (t + "|" + body).hashCode());
        if (NotifStore.add(this, o)) NotifReaderPlugin.emit(o);
    }
}
