package com.bujjet.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.common.GenAiException;
import com.google.mlkit.genai.prompt.GenerateContentRequest;
import com.google.mlkit.genai.prompt.GenerateContentResponse;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.TextPart;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** On-device Gemini Nano (ML Kit GenAI Prompt API). Only used when the phone supports it. */
@CapacitorPlugin(name = "Nano")
public class NanoPlugin extends Plugin {
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private GenerativeModelFutures model;

    private GenerativeModelFutures model() {
        if (model == null) model = GenerativeModelFutures.from(Generation.INSTANCE.getClient());
        return model;
    }

    private static String name(int s) {
        if (s == FeatureStatus.AVAILABLE) return "available";
        if (s == FeatureStatus.DOWNLOADABLE) return "downloadable";
        if (s == FeatureStatus.DOWNLOADING) return "downloading";
        return "unavailable";
    }

    @PluginMethod
    public void status(PluginCall call) {
        bg.execute(() -> {
            JSObject r = new JSObject();
            try {
                Integer s = model().checkStatus().get(15, TimeUnit.SECONDS);
                r.put("status", name(s == null ? FeatureStatus.UNAVAILABLE : s));
            } catch (Throwable e) {
                r.put("status", "unavailable");
                r.put("detail", String.valueOf(e.getMessage()));
            }
            call.resolve(r);
        });
    }

    @PluginMethod
    public void download(PluginCall call) {
        bg.execute(() -> {
            try {
                model().download(new DownloadCallback() {
                    @Override public void onDownloadStarted(long bytes) { }
                    @Override public void onDownloadProgress(long bytes) { }
                    @Override public void onDownloadCompleted() { }
                    @Override public void onDownloadFailed(GenAiException e) { }
                });
                call.resolve();
            } catch (Throwable e) {
                call.reject(String.valueOf(e.getMessage()));
            }
        });
    }

    @PluginMethod
    public void ask(PluginCall call) {
        String prompt = call.getString("prompt", "");
        bg.execute(() -> {
            try {
                GenerateContentRequest.Builder b = new GenerateContentRequest.Builder(new TextPart(prompt));
                b.setTemperature(0f);
                GenerateContentResponse res = model().generateContent(b.build()).get(20, TimeUnit.SECONDS);
                String text = res.getCandidates().isEmpty() ? "" : res.getCandidates().get(0).getText();
                JSObject r = new JSObject();
                r.put("text", text == null ? "" : text);
                call.resolve(r);
            } catch (Throwable e) {
                call.reject(String.valueOf(e.getMessage()));
            }
        });
    }
}
