package com.bujjet.app;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(NotifReaderPlugin.class);
        super.onCreate(savedInstanceState);
        handleText(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleText(intent);
    }

    /** "Log to bujjet" from the text-selection menu, or Share → bujjet with text. */
    private void handleText(Intent intent) {
        if (intent == null) return;
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
