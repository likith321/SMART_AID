package com.example.smartfirstaid;

import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.smartfirstaid.data.db.MongoHelper;
import com.mongodb.client.MongoCollection;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmergencyDetailActivity extends AppCompatActivity {

    private String key;
    private String title;

    private TextView tvTitle, tvDo, tvDont;
    private ProgressBar progress;
    private Button btnVoice;

    private List<String> doList = new ArrayList<>();
    private List<String> dontList = new ArrayList<>();
    private List<String> voiceScript = new ArrayList<>();
    private List<String> imageUrls = new ArrayList<>();

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean dataLoaded = false;

    private ViewPager2 vpImages;
    private ImagePagerAdapter imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_detail);

        // extras
        key = getIntent().getStringExtra("key");
        title = getIntent().getStringExtra("title");

        // views
        tvTitle = findViewById(R.id.tvTitle);
        tvDo    = findViewById(R.id.tvDoList);
        tvDont  = findViewById(R.id.tvDontList);
        progress= findViewById(R.id.progress);
        btnVoice= findViewById(R.id.btnVoiceGuide);
        vpImages= findViewById(R.id.vpImages);

        tvTitle.setText(title != null ? title : "Emergency");

        // set up viewpager and adapter
        imageAdapter = new ImagePagerAdapter(new ArrayList<>());
        vpImages.setAdapter(imageAdapter);
        vpImages.setOffscreenPageLimit(1);

        btnVoice.setEnabled(false);

        // init TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int res = tts.setLanguage(Locale.US);
                tts.setSpeechRate(0.95f);
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                ttsReady = true;
                setupUtteranceListener();
                updateButtonState();
            } else {
                Toast.makeText(EmergencyDetailActivity.this,
                        "TTS initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        btnVoice.setOnClickListener(v -> speakDoList());

        // fetch data
        new LoadProcedureTask().execute(key);
    }

    /** AsyncTask to load one procedure document by key */
    private class LoadProcedureTask extends AsyncTask<String, Void, Document> {
        private String error;

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
            btnVoice.setEnabled(false);
        }

        @Override
        protected Document doInBackground(String... keys) {
            try {
                MongoCollection<Document> col = MongoHelper.procedures();
                return col.find(new Document("key", keys[0]))
                        .projection(new Document("_id", 0))
                        .first();
            } catch (Exception e) {
                error = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Document d) {
            progress.setVisibility(View.GONE);

            if (d == null) {
                Toast.makeText(EmergencyDetailActivity.this,
                        "Failed to load instructions: " + (error == null ? "Unknown error" : error),
                        Toast.LENGTH_LONG).show();
                // hide carousel if nothing
                vpImages.setVisibility(View.GONE);
                return;
            }

            // lists
            doList       = castStringList(d.get("do"));
            dontList     = castStringList(d.get("dont"));
            voiceScript  = castStringList(d.get("voiceScript"));
            imageUrls    = castStringList(d.get("images")); // <--- images array from Mongo

            tvDo.setText(toBullets(doList));
            tvDont.setText(toBullets(dontList));

            // update images
            if (imageUrls != null && !imageUrls.isEmpty()) {
                imageAdapter.setItems(imageUrls);

                if (imageUrls.size() > 1) {
                    // center the adapter position so user can scroll left and right
                    int middle = Integer.MAX_VALUE / 2;
                    int startPos = middle - (middle % imageUrls.size());
                    vpImages.setCurrentItem(startPos, false);
                } else {
                    // single image -> set to first and disable infinite trick
                    vpImages.setCurrentItem(0, false);
                }
                vpImages.setVisibility(View.VISIBLE);
            } else {
                vpImages.setVisibility(View.GONE);
            }

            dataLoaded = true;
            updateButtonState();
        }
    }

    /** Defensive cast helper */
    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object o) {
        if (o instanceof List<?>) {
            List<?> raw = (List<?>) o;
            List<String> out = new ArrayList<>();
            for (Object item : raw) if (item != null) out.add(item.toString());
            return out;
        }
        return new ArrayList<>();
    }

    /** Render bullet list */
    private String toBullets(List<String> items) {
        if (items == null || items.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (String s : items) sb.append("• ").append(s).append("\n\n");
        return sb.toString().trim();
    }

    private void updateButtonState() {
        boolean hasDo = doList != null && !doList.isEmpty();
        runOnUiThread(() -> btnVoice.setEnabled(ttsReady && dataLoaded && hasDo));
    }

    private void setupUtteranceListener() {
        if (tts == null) return;
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) { }
            @Override public void onDone(String utteranceId) {
                runOnUiThread(() -> Toast.makeText(EmergencyDetailActivity.this,
                        "Voice guidance finished", Toast.LENGTH_SHORT).show());
            }
            @Override public void onError(String utteranceId) {
                runOnUiThread(() -> Toast.makeText(EmergencyDetailActivity.this,
                        "Error while speaking", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void speakDoList() {
        if (!ttsReady) {
            Toast.makeText(this, "Voice not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> toSpeakList = (doList != null && !doList.isEmpty()) ? doList : voiceScript;
        if (toSpeakList == null || toSpeakList.isEmpty()) {
            Toast.makeText(this, "Nothing to speak", Toast.LENGTH_SHORT).show();
            return;
        }

        final String UTTERANCE_BASE = "SFA_UTTER_";
        tts.stop();

        int i = 0;
        for (String line : toSpeakList) {
            if (line == null) continue;
            String sanitized = line.trim();
            if (sanitized.isEmpty()) continue;
            if (!sanitized.endsWith(".") && !sanitized.endsWith("!") && !sanitized.endsWith("?")) {
                sanitized = sanitized + ".";
            }
            String utteranceId = UTTERANCE_BASE + (++i);
            tts.speak(sanitized, TextToSpeech.QUEUE_ADD, null, utteranceId);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    // ----------------------------
    // Adapter for ViewPager2 (infinite loop trick)
    // ----------------------------
    private static class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.VH> {

        private final List<String> items;
        private static final int INFINITE = Integer.MAX_VALUE;

        ImagePagerAdapter(List<String> urls) {
            this.items = new ArrayList<>(urls);
        }

        void setItems(List<String> urls) {
            items.clear();
            if (urls != null) items.addAll(urls);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            int pad = (int) (parent.getContext().getResources().getDisplayMetrics().density * 6);
            iv.setPadding(pad, pad, pad, pad);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            if (items.isEmpty()) return;
            int realPos = (items.size() == 0) ? 0 : (position % items.size());
            String url = items.get(realPos);

            // load with Glide
            Glide.with(holder.imageView.getContext())
                    .load(url)
                    .apply(new RequestOptions().centerCrop())
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            if (items.size() <= 1) return items.size();
            return INFINITE;
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView imageView;
            VH(@NonNull View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
            }
        }
    }
}