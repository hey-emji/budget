package com.bujjet.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
    static final String LOG_CLIPBOARD = "com.bujjet.app.LOG_CLIPBOARD";
    private boolean wantClipboard = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(NotifReaderPlugin.class);
        registerPlugin(GoogleAuthPlugin.class);
        super.onCreate(savedInstanceState);
        // Back button / swipe back: let the app close its own panels first, and only leave after a second back.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    getBridge().getWebView().evaluateJavascript(
                        "(window.__bujjetBack ? window.__bujjetBack() : 'exit')",
                        v -> { if (v == null || v.contains("exit")) finish(); });
                } catch (Exception e) { finish(); }
            }
        });
        handleText(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleText(intent);
    }

    /** Android only lets the app read the clipboard once its window has focus. */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus || !wantClipboard) return;
        wantClipboard = false;
        String txt = readClipboard(this);
        if (txt == null || txt.isEmpty()) return;
        try {
            JSONObject o = new JSONObject();
            o.put("pkg", "bujjet.selection"); o.put("app", ""); o.put("title", "");
            o.put("text", txt.length() > 1500 ? txt.substring(0, 1500) : txt);
            o.put("time", System.currentTimeMillis()); o.put("id", "clip|" + System.currentTimeMillis());
            if (NotifStore.add(this, o)) NotifReaderPlugin.emit(o);
        } catch (Exception ignored) { }
    }

    static String readClipboard(Context c) {
        try {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return null;
            ClipData cd = cm.getPrimaryClip();
            if (cd == null || cd.getItemCount() == 0) return null;
            CharSequence t = cd.getItemAt(0).coerceToText(c);
            return t == null ? null : t.toString();
        } catch (Exception e) { return null; }
    }

    /** "Log to bujjet" from the text-selection menu, or Share → bujjet with text. */
    private void handleText(Intent intent) {
        if (intent == null) return;
        if (LOG_CLIPBOARD.equals(intent.getAction())) { wantClipboard = true; intent.setAction(Intent.ACTION_MAIN); return; }
        try {
            CharSequence txt = null;
            String action = intent.getAction();
            if (Intent.ACTION_PROCESS_TEXT.equals(action)) txt = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            else if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(intent.getType())) txt = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (txt == null || txt.length() == 0) return;
            String s = txt.toString();
            JSONObject o = new JSONObject();
            o.put("pkg", "bujjet.selection");
            o.put("app", "");
            o.put("title", "");
            o.put("text", s.length() > 1500 ? s.substring(0, 1500) : s);
            o.put("time", System.currentTimeMillis());
            o.put("id", "sel|" + System.currentTimeMillis());
            if (NotifStore.add(this, o)) NotifReaderPlugin.emit(o);
            intent.setAction(Intent.ACTION_MAIN); // don't handle it twice on rotation
        } catch (Exception ignored) { }
    }
}
