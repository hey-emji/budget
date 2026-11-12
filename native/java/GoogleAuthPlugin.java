package com.bujjet.app;

import android.app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import java.util.Arrays;
import java.util.List;

/**
 * Google sign-in for Drive sync inside the Android app (Google blocks its web sign-in page in app web views).
 * Uses Google's on-device authorization, so it can renew access silently without a server.
 */
@CapacitorPlugin(name = "GoogleAuth")
public class GoogleAuthPlugin extends Plugin {
    private static final List<Scope> SCOPES = Arrays.asList(
        new Scope("https://www.googleapis.com/auth/drive.appdata"),
        new Scope("openid"), new Scope("email"), new Scope("profile"));
    private ActivityResultLauncher<IntentSenderRequest> launcher;
    private PluginCall pending;

    @Override
    public void load() {
        launcher = getActivity().registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), res -> {
            PluginCall call = pending; pending = null;
            if (call == null) return;
            if (res.getResultCode() != Activity.RESULT_OK || res.getData() == null) { call.reject("cancelled"); return; }
            try {
                AuthorizationResult r = Identity.getAuthorizationClient(getActivity()).getAuthorizationResultFromIntent(res.getData());
                resolveToken(call, r);
            } catch (Exception e) { call.reject("failed", e); }
        });
    }

    private void resolveToken(PluginCall call, AuthorizationResult r) {
        String t = r.getAccessToken();
        if (t == null || t.isEmpty()) { call.reject("failed"); return; }
        JSObject o = new JSObject();
        o.put("token", t);
        o.put("expiresIn", 3300);
        call.resolve(o);
    }

    /** interactive=false: renew silently or reject with "signin". interactive=true: show Google's account/consent screen if needed. */
    @PluginMethod
    public void token(PluginCall call) {
        boolean interactive = Boolean.TRUE.equals(call.getBoolean("interactive", false));
        AuthorizationRequest req = AuthorizationRequest.builder().setRequestedScopes(SCOPES).build();
        Identity.getAuthorizationClient(getActivity()).authorize(req)
            .addOnSuccessListener(r -> {
                if (r.hasResolution()) {
                    if (!interactive || r.getPendingIntent() == null) { call.reject("signin"); return; }
                    pending = call;
                    try { launcher.launch(new IntentSenderRequest.Builder(r.getPendingIntent().getIntentSender()).build()); }
                    catch (Exception e) { pending = null; call.reject("failed", e); }
                } else {
                    resolveToken(call, r);
                }
            })
            .addOnFailureListener(e -> call.reject("failed", e));
    }
}
