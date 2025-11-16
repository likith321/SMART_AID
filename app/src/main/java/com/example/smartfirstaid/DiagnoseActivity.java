package com.example.smartfirstaid; // change to your package

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DiagnoseActivity extends AppCompatActivity {
    private static final String TAG = "DiagnoseActivity";

    // injected via buildConfigField from local.properties
    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/responses";

    private final OkHttpClient client = new OkHttpClient();

    private TextView tvDiseaseName, tvCriticality, tvActions, tvWorsen, tvDisclaimer;
    private LinearLayout containerImmediate, containerWorsen;
    private ProgressBar progressBar;
    private Button btnRetry;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnose);

        tvDiseaseName = findViewById(R.id.tv_disease_name);
        tvCriticality = findViewById(R.id.tv_criticality);
        tvActions = findViewById(R.id.tv_actions);
        tvWorsen = findViewById(R.id.tv_worsen);
        containerImmediate = findViewById(R.id.container_immediate);
        containerWorsen = findViewById(R.id.container_worsen);
        tvDisclaimer = findViewById(R.id.tv_disclaimer);
        progressBar = findViewById(R.id.progressBar);
        btnRetry = findViewById(R.id.button_retry);

        tvDisclaimer.setText("Disclaimer: AI suggestions are informational only. In emergencies call local services.");

        ArrayList<String> symptoms = getIntent().getStringArrayListExtra("symptoms");
        if (symptoms == null || symptoms.isEmpty()) {
            Toast.makeText(this, "No symptoms provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final String prompt = buildPrompt(symptoms);

        // initial call
        callGroqResponses(prompt);

        // retry
        btnRetry.setOnClickListener(v -> callGroqResponses(prompt));
    }

    private String buildPrompt(ArrayList<String> symptoms) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an experienced emergency paramedic. ");
        sb.append("Analyze the following symptoms and RETURN ONLY a VALID JSON object EXACTLY in this format:\n");
        sb.append("{\n");
        sb.append("  \"disease\": \"most likely condition or differential\",\n");
        sb.append("  \"criticality\": \"low | moderate | high\",\n");
        sb.append("  \"immediate_actions\": [\"step1\",\"step2\"],\n");
        sb.append("  \"worsen_actions\": [\"step1\",\"step2\"]\n");
        sb.append("}\n\n");
        sb.append("Symptoms:\n");
        for (int i = 0; i < symptoms.size(); i++) {
            sb.append((i + 1)).append(". ").append(symptoms.get(i)).append("\n");
        }
        sb.append("\nReturn JSON only. NOTHING else.");
        return sb.toString();
    }

    private void callGroqResponses(String prompt) {
        progressBar.setVisibility(View.VISIBLE);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        JSONObject req = new JSONObject();
        try {
            req.put("model", "llama-3.1-8b-instant");
            req.put("input", prompt);

            // optional parameters
            JSONObject params = new JSONObject();
            params.put("temperature", 0.2);
            params.put("max_output_tokens", 512);
            req.put("parameters", params);

        } catch (Exception e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to build request", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(req.toString(), JSON);

        Request request = new Request.Builder()
                .url(GROQ_URL)
                .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(DiagnoseActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                final String raw = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "HTTP " + response.code() + " RAW: " + raw);

                runOnUiThread(() -> progressBar.setVisibility(View.GONE));

                if (!response.isSuccessful()) {
                    String serverMsg = extractServerMessage(raw);
                    runOnUiThread(() -> Toast.makeText(DiagnoseActivity.this,
                            "API Error: HTTP " + response.code() + ". " + serverMsg, Toast.LENGTH_LONG).show());
                    return;
                }

                try {
                    JSONObject root = new JSONObject(raw);

                    // Groq Responses typically have "output" array. We'll extract text defensively.
                    String text = extractTextFromGroqResponse(root);

                    if (text == null || text.trim().isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(DiagnoseActivity.this,
                                "Model returned empty text. See Logcat.", Toast.LENGTH_LONG).show());
                        return;
                    }

                    // Find JSON block in text
                    JSONObject outJson = extractJsonBlock(text);
                    if (outJson == null) {
                        final String preview = text.length() > 500 ? text.substring(0,500) + "..." : text;
                        Log.e(TAG, "No JSON found inside model output. Preview: " + preview);
                        runOnUiThread(() -> Toast.makeText(DiagnoseActivity.this,
                                "Model did not return JSON. See Logcat (preview).", Toast.LENGTH_LONG).show());
                        return;
                    }

                    populateUi(outJson);

                } catch (Exception e) {
                    Log.e(TAG, "Parse error", e);
                    runOnUiThread(() -> Toast.makeText(DiagnoseActivity.this,
                            "Parsing error. See Logcat.", Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    // Try to extract a human-readable server message from error body
    private String extractServerMessage(String raw) {
        if (raw == null || raw.isEmpty()) return "empty response";
        try {
            JSONObject r = new JSONObject(raw);
            if (r.has("error")) {
                JSONObject err = r.optJSONObject("error");
                if (err != null) return err.optString("message", raw);
                return r.optString("error", raw);
            }
        } catch (Exception ignored) { }
        return raw.length() > 300 ? raw.substring(0,300) + "..." : raw;
    }

    // Defensive extraction of textual content from Groq's response shape
    private String extractTextFromGroqResponse(JSONObject root) {
        try {
            // 1) new responses API uses "output" array
            if (root.has("output")) {
                JSONArray outputs = root.getJSONArray("output");
                if (outputs.length() > 0) {
                    // search for content blocks inside outputs
                    for (int i = 0; i < outputs.length(); i++) {
                        JSONObject out = outputs.getJSONObject(i);
                        if (out.has("content")) {
                            JSONArray content = out.getJSONArray("content");
                            for (int j = 0; j < content.length(); j++) {
                                Object c = content.get(j);
                                if (c instanceof JSONObject) {
                                    JSONObject cobj = (JSONObject) c;
                                    // common key is "text"
                                    if (cobj.has("text")) return cobj.optString("text", null);
                                    // sometimes content items have "type" and "text"
                                    if (cobj.has("type") && cobj.has("text")) return cobj.optString("text", null);
                                } else if (c instanceof String) {
                                    return (String) c;
                                }
                            }
                        }
                        // sometimes the output item itself may contain "text"
                        if (out.has("text")) return out.optString("text", null);
                    }
                }
            }

            // 2) fallback to "choices" like classic OpenAI responses
            if (root.has("choices")) {
                JSONArray choices = root.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject first = choices.getJSONObject(0);
                    if (first.has("message")) {
                        JSONObject message = first.getJSONObject("message");
                        if (message.has("content")) {
                            Object content = message.get("content");
                            if (content instanceof String) return (String) content;
                            if (content instanceof JSONObject) {
                                JSONObject cobj = (JSONObject) content;
                                if (cobj.has("text")) return cobj.optString("text", null);
                            }
                        }
                    }
                    if (first.has("text")) return first.optString("text", null);
                }
            }

            // 3) fallback: entire raw string
            return root.toString();

        } catch (Exception e) {
            Log.w(TAG, "extractTextFromGroqResponse warning", e);
            return root.toString();
        }
    }

    // Find first {...} JSON block in text
    private JSONObject extractJsonBlock(String text) {
        if (text == null) return null;
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start < 0 || end <= start) return null;
        String candidate = text.substring(start, end + 1);
        try {
            return new JSONObject(candidate);
        } catch (Exception e) {
            Log.w(TAG, "JSON parse failed for candidate block", e);
            return null;
        }
    }

    private void populateUi(JSONObject o) {
        runOnUiThread(() -> {
            try {
                tvDiseaseName.setText(o.optString("disease", "—"));
                tvCriticality.setText(o.optString("criticality", "—"));

                // Show possibility text in tvActions if present
                String possibility = o.optString("possibility", null);
                if (possibility != null && !possibility.isEmpty()) tvActions.setText(possibility);

                // immediate_actions
                containerImmediate.removeAllViews();
                JSONArray imm = o.optJSONArray("immediate_actions");
                if (imm != null && imm.length() > 0) {
                    for (int i = 0; i < imm.length(); i++) addBullet(containerImmediate, imm.optString(i, ""));
                } else {
                    addBullet(containerImmediate, "—");
                }

                // worsen_actions
                containerWorsen.removeAllViews();
                JSONArray wors = o.optJSONArray("worsen_actions");
                if (wors != null && wors.length() > 0) {
                    for (int i = 0; i < wors.length(); i++) addBullet(containerWorsen, wors.optString(i, ""));
                } else {
                    addBullet(containerWorsen, "—");
                }

            } catch (Exception e) {
                Log.e(TAG, "populateUi error", e);
                Toast.makeText(DiagnoseActivity.this, "UI populate error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addBullet(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText("• " + text);
        tv.setTextSize(15f);
        tv.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(8,6,8,6);
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }
}
