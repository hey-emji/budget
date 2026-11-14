package com.bujjet.app;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.provider.Settings;
import androidx.core.app.NotificationManagerCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "NotifReader")
public class NotifReaderPlugin extends Plugin {
    private static NotifReaderPlugin instance;

    @Override
    public void load() { instance = this; }

    static void emit(JSONObject o) {
        NotifReaderPlugin p = instance;
        if (p == null) return;
        try { p.notifyListeners("captured", JSObject.fromJSONObject(o)); } catch (Exception ignored) { }
    }

    @PluginMethod
    public void isEnabled(PluginCall call) {
        boolean on = NotificationManagerCompat.getEnabledListenerPackages(getContext()).contains(getContext().getPackageName());
        JSObject r = new JSObject();
        r.put("enabled", on);
        android.content.SharedPreferences sp = getContext().getSharedPreferences("bujjet_notifs", android.content.Context.MODE_PRIVATE);
        r.put("connectedAt", sp.getLong("connectedAt", 0));
        r.put("seenCount", sp.getLong("seenCount", 0));
        r.put("lastSeenAt", sp.getLong("lastSeenAt", 0));
        call.resolve(r);
    }

    @PluginMethod
    public void openSettings(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod
    public void readClipboard(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            JSObject r = new JSObject();
            String t = MainActivity.readClipboard(getContext());
            r.put("text", t == null ? "" : (t.length() > 1500 ? t.substring(0, 1500) : t));
            call.resolve(r);
        });
    }

    /** Save a backup into Downloads/bujjet. */
    @PluginMethod
    public void saveFile(PluginCall call) {
        String name = call.getString("name", "bujjet-backup.bujjet"), data = call.getString("data", "");
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/bujjet");
                Uri uri = getContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new Exception("no uri");
                try (OutputStream os = getContext().getContentResolver().openOutputStream(uri)) { os.write(data.getBytes(StandardCharsets.UTF_8)); }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "bujjet");
                dir.mkdirs();
                try (FileOutputStream os = new FileOutputStream(new File(dir, name))) { os.write(data.getBytes(StandardCharsets.UTF_8)); }
            }
            call.resolve();
        } catch (Exception e) { call.reject("Couldn't save the backup file", e); }
    }

    /** Share a backup through Android's share sheet (Drive, Messenger, email…). */
    @PluginMethod
    public void shareFile(PluginCall call) {
        String name = call.getString("name", "bujjet-backup.bujjet"), data = call.getString("data", "");
        try {
            File dir = new File(getContext().getCacheDir(), "share");
            dir.mkdirs();
            File f = new File(dir, name);
            try (FileOutputStream os = new FileOutputStream(f)) { os.write(data.getBytes(StandardCharsets.UTF_8)); }
            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("application/octet-stream");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(i, "Share bujjet backup");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(chooser);
            call.resolve();
        } catch (Exception e) { call.reject("Couldn't share the backup file", e); }
    }

    @PluginMethod
    public void takePending(PluginCall call) {
        try {
            JSONArray q = NotifStore.take(getContext());
            JSObject r = new JSObject();
            r.put("items", new JSArray(q.toString()));
            call.resolve(r);
        } catch (Exception e) {
            call.reject("Couldn't read captured notifications", e);
        }
    }
}
