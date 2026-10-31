package com.recepparlak.studio;

import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@OptIn(markerClass = UnstableApi.class)
public class VideoActivity extends AppCompatActivity {
    private VideoView vv;
    private SeekBar sbStart, sbEnd;
    private TextView tvTime, tvStatus, tvMusic;
    private RadioGroup rg;
    private Button btnMusic, btnSave, btnShare;
    private ProgressBar pb;
    private Uri videoUri, audioUri, outUri;
    private long audioDurUs;
    private int dur, startMs, endMs;
    private int idKeep, idMute, idMusic;
    private Transformer transformer;
    private final Handler h = new Handler(Looper.getMainLooper());

    private final Runnable watcher = new Runnable() {
        @Override public void run() {
            if (!vv.isPlaying()) return;
            if (vv.getCurrentPosition() >= endMs) vv.pause();
            else h.postDelayed(this, 100);
        }
    };

    private final ActivityResultLauncher<String> pickVideo = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                videoUri = uri;
                outUri = null;
                btnShare.setVisibility(View.GONE);
                vv.setVideoURI(uri);
                vv.setOnPreparedListener(mp -> {
                    dur = mp.getDuration();
                    startMs = 0;
                    endMs = dur;
                    sbStart.setMax(dur);
                    sbEnd.setMax(dur);
                    sbStart.setProgress(0);
                    sbEnd.setProgress(dur);
                    updateTime();
                    vv.seekTo(1);
                });
            });

    private final ActivityResultLauncher<String> pickAudio = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                MediaMetadataRetriever r = new MediaMetadataRetriever();
                try {
                    r.setDataSource(this, uri);
                    String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    audioDurUs = Long.parseLong(d) * 1000L;
                    audioUri = uri;
                    tvMusic.setText("🎵 Müzik seçildi (" + fmt((int) (audioDurUs / 1000)) + ")");
                } catch (Exception e) {
                    Toast.makeText(this, "Ses dosyası okunamadı", Toast.LENGTH_SHORT).show();
                } finally {
                    try { r.release(); } catch (Exception ignored) {}
                }
            });

    private abstract static class Sb implements SeekBar.OnSeekBarChangeListener {
        public void onStartTrackingTouch(SeekBar s) {}
        public void onStopTrackingTouch(SeekBar s) {}
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = Ui.root(this);
        l.addView(Ui.text(this, "Video Düzenle", 26, Ui.ACC, true));
        l.addView(Ui.btn(this, "🎬  Video seç", Ui.BLUE, v -> pickVideo.launch("video/*")));

        FrameLayout box = new FrameLayout(this);
        box.setBackgroundColor(0xFF000000);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 240));
        bp.topMargin = Ui.dp(this, 12);
        box.setLayoutParams(bp);
        vv = new VideoView(this);
        vv.setLayoutParams(new FrameLayout.LayoutParams(-1, -1, android.view.Gravity.CENTER));
        box.addView(vv);
        l.addView(box);

        l.addView(Ui.btn(this, "▶  Seçili aralığı oynat", Ui.CARD, v -> {
            if (videoUri == null) return;
            vv.seekTo(startMs);
            vv.start();
            h.postDelayed(watcher, 250);
        }));

        tvTime = Ui.text(this, "Başlangıç 0:00  •  Bitiş 0:00", 14, Ui.TXT, false);
        l.addView(tvTime);
        l.addView(Ui.text(this, "Başlangıç (baştan kes)", 12, Ui.SUB, false));
        sbStart = new SeekBar(this);
        sbStart.setOnSeekBarChangeListener(new Sb() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (!user) return;
                if (p > endMs - 500) { p = Math.max(0, endMs - 500); s.setProgress(p); }
                startMs = p;
                updateTime();
                vv.seekTo(p);
            }
        });
        l.addView(sbStart);
        l.addView(Ui.text(this, "Bitiş (sondan kes)", 12, Ui.SUB, false));
        sbEnd = new SeekBar(this);
        sbEnd.setOnSeekBarChangeListener(new Sb() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (!user) return;
                if (p < startMs + 500) { p = Math.min(dur, startMs + 500); s.setProgress(p); }
                endMs = p;
                updateTime();
                vv.seekTo(p);
            }
        });
        l.addView(sbEnd);

        l.addView(Ui.text(this, "Ses", 16, Ui.ACC, true));
        rg = new RadioGroup(this);
        RadioButton keep = radio("Orijinal sesi koru");
        RadioButton mute = radio("Sesi kapat");
        RadioButton music = radio("Müzik ekle (orijinal sesin yerine)");
        idKeep = View.generateViewId();
        idMute = View.generateViewId();
        idMusic = View.generateViewId();
        keep.setId(idKeep);
        mute.setId(idMute);
        music.setId(idMusic);
        rg.addView(keep);
        rg.addView(mute);
        rg.addView(music);
        rg.check(idKeep);
        l.addView(rg);

        btnMusic = Ui.btn(this, "🎵  Müzik / ses dosyası seç", Ui.CARD, v -> pickAudio.launch("audio/*"));
        btnMusic.setVisibility(View.GONE);
        l.addView(btnMusic);
        tvMusic = Ui.text(this, "", 13, Ui.SUB, false);
        l.addView(tvMusic);
        rg.setOnCheckedChangeListener((g, id) -> {
            int vis = id == idMusic ? View.VISIBLE : View.GONE;
            btnMusic.setVisibility(vis);
            tvMusic.setVisibility(vis);
        });

        btnSave = Ui.btn(this, "💾  Kaydet", Ui.ACC, v -> export());
        l.addView(btnSave);
        pb = new ProgressBar(this);
        pb.setIndeterminate(true);
        pb.setVisibility(View.GONE);
        l.addView(pb);
        tvStatus = Ui.text(this, "", 14, Ui.TXT, false);
        l.addView(tvStatus);
        btnShare = Ui.btn(this, "📤  Paylaş", Ui.BLUE, v -> share());
        btnShare.setVisibility(View.GONE);
        l.addView(btnShare);
    }

    private RadioButton radio(String t) {
        RadioButton r = new RadioButton(this);
        r.setText(t);
        r.setTextColor(Ui.TXT);
        r.setTextSize(15);
        return r;
    }

    private String fmt(int ms) {
        int s = ms / 1000;
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    private void updateTime() {
        tvTime.setText("Başlangıç " + fmt(startMs) + "  •  Bitiş " + fmt(endMs)
                + "  •  Süre " + fmt(endMs - startMs));
    }

    private void export() {
        if (videoUri == null || dur <= 0) {
            Toast.makeText(this, "Önce video seç", Toast.LENGTH_SHORT).show();
            return;
        }
        int mode = rg.getCheckedRadioButtonId();
        if (mode == idMusic && audioUri == null) {
            Toast.makeText(this, "Önce müzik dosyası seç", Toast.LENGTH_SHORT).show();
            return;
        }
        vv.pause();
        btnSave.setEnabled(false);
        btnShare.setVisibility(View.GONE);
        pb.setVisibility(View.VISIBLE);
        tvStatus.setText("İşleniyor, bekle...");

        MediaItem.ClippingConfiguration clip = new MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs).setEndPositionMs(endMs).build();
        MediaItem mi = new MediaItem.Builder().setUri(videoUri).setClippingConfiguration(clip).build();
        EditedMediaItem.Builder vb = new EditedMediaItem.Builder(mi);
        if (mode != idKeep) vb.setRemoveAudio(true);

        List<EditedMediaItemSequence> seqs = new ArrayList<>();
        seqs.add(new EditedMediaItemSequence(Collections.singletonList(vb.build())));
        if (mode == idMusic) {
            EditedMediaItem a = new EditedMediaItem.Builder(MediaItem.fromUri(audioUri))
                    .setDurationUs(audioDurUs).build();
            seqs.add(new EditedMediaItemSequence(Collections.singletonList(a), true));
        }
        Composition comp = new Composition.Builder(seqs).build();
        File out = new File(getCacheDir(), "cikti_" + System.currentTimeMillis() + ".mp4");

        transformer = new Transformer.Builder(this)
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition c, ExportResult r) {
                        pb.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        outUri = saveToMovies(out);
                        if (outUri != null) {
                            tvStatus.setText("✅ Kaydedildi: Filmler/3K Studio");
                            btnShare.setVisibility(View.VISIBLE);
                        }
                        out.delete();
                    }

                    @Override
                    public void onError(Composition c, ExportResult r, ExportException e) {
                        pb.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        tvStatus.setText("Hata: " + e.getErrorCodeName());
                    }
                }).build();
        transformer.start(comp, out.getAbsolutePath());
    }

    private Uri saveToMovies(File f) {
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Video.Media.DISPLAY_NAME, "3K_video_" + System.currentTimeMillis() + ".mp4");
            v.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            v.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/3K Studio");
            Uri u = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v);
            try (InputStream in = new FileInputStream(f); OutputStream o = getContentResolver().openOutputStream(u)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            }
            return u;
        } catch (Exception e) {
            tvStatus.setText("Kaydedilemedi: " + e.getMessage());
            return null;
        }
    }

    private void share() {
        if (outUri == null) return;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("video/mp4");
        i.putExtra(Intent.EXTRA_STREAM, outUri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Paylaş"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        h.removeCallbacksAndMessages(null);
        if (transformer != null) transformer.cancel();
    }
}
