package com.recepparlak.studio;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;

public class CollageActivity extends AppCompatActivity {
    private Bitmap before, after, result;
    private Uri savedUri;
    private ImageView preview;
    private RadioGroup layoutGroup;
    private EditText footerEt;
    private int idSide, idStack;

    private final ActivityResultLauncher<String> pickBefore = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) { before = load(uri); build(); }
            });
    private final ActivityResultLauncher<String> pickAfter = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) { after = load(uri); build(); }
            });

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = Ui.root(this);
        l.addView(Ui.text(this, "Önce / Sonra Kolaj", 26, Ui.ACC, true));
        l.addView(Ui.btn(this, "1) ÖNCE fotoğrafını seç", Ui.RED, v -> pickBefore.launch("image/*")));
        l.addView(Ui.btn(this, "2) SONRA fotoğrafını seç", Ui.GREEN, v -> pickAfter.launch("image/*")));

        layoutGroup = new RadioGroup(this);
        layoutGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton side = radio("Yan yana");
        RadioButton stack = radio("Alt alta");
        idSide = View.generateViewId();
        idStack = View.generateViewId();
        side.setId(idSide);
        stack.setId(idStack);
        layoutGroup.addView(side);
        layoutGroup.addView(stack);
        layoutGroup.check(idSide);
        layoutGroup.setOnCheckedChangeListener((g, id) -> build());
        l.addView(layoutGroup);

        footerEt = new EditText(this);
        footerEt.setText("3K Koltuk Yıkama  0 543 544 67 42");
        footerEt.setTextColor(Ui.TXT);
        footerEt.setHintTextColor(Ui.SUB);
        footerEt.setHint("Alt yazı (boş bırakılabilir)");
        footerEt.setBackground(Ui.round(this, Ui.CARD, 12));
        footerEt.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        footerEt.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable e) { build(); }
        });
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = Ui.dp(this, 10);
        footerEt.setLayoutParams(fp);
        l.addView(footerEt);

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.topMargin = Ui.dp(this, 14);
        preview.setLayoutParams(pp);
        l.addView(preview);

        l.addView(Ui.btn(this, "💾  Galeriye kaydet", Ui.ACC, v -> {
            if (save() != null) Toast.makeText(this, "Kaydedildi: Resimler/3K Studio", Toast.LENGTH_LONG).show();
        }));
        l.addView(Ui.btn(this, "📤  Paylaş (WhatsApp, Instagram...)", Ui.BLUE, v -> share()));
    }

    private RadioButton radio(String t) {
        RadioButton r = new RadioButton(this);
        r.setText(t);
        r.setTextColor(Ui.TXT);
        r.setTextSize(15);
        return r;
    }

    private Bitmap load(Uri uri) {
        try {
            ImageDecoder.Source s = ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(s, (d, info, src) -> {
                d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                int m = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                if (m > 2000) d.setTargetSampleSize(m / 2000);
            });
        } catch (Exception e) {
            Toast.makeText(this, "Fotoğraf açılamadı", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void build() {
        savedUri = null;
        if (before == null || after == null) { preview.setImageDrawable(null); return; }
        boolean side = layoutGroup.getCheckedRadioButtonId() == idSide;
        String footer = footerEt.getText().toString().trim();
        int gap = 8, fh = footer.isEmpty() ? 0 : 130;
        int cw = 1080, ch = side ? 1350 : 1000;
        int w = side ? cw * 2 + gap : cw;
        int h = (side ? ch : ch * 2 + gap) + fh;

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawColor(0xFF0F172A);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        int x2 = side ? cw + gap : 0, y2 = side ? 0 : ch + gap;
        crop(c, before, 0, 0, cw, ch, paint);
        crop(c, after, x2, y2, cw, ch, paint);
        label(c, "ÖNCE", 0, 0, Ui.RED);
        label(c, "SONRA", x2, y2, Ui.GREEN);

        if (fh > 0) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Ui.ACC);
            c.drawRect(0, h - fh, w, h - fh + 6, p);
            p.setColor(Color.WHITE);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextAlign(Paint.Align.CENTER);
            float size = 52;
            p.setTextSize(size);
            while (p.measureText(footer) > w - 60 && size > 20) { size -= 2; p.setTextSize(size); }
            c.drawText(footer, w / 2f, h - fh / 2f + size / 3f, p);
        }
        result = out;
        preview.setImageBitmap(out);
    }

    private void crop(Canvas c, Bitmap b, int x, int y, int w, int h, Paint paint) {
        float s = Math.max(w / (float) b.getWidth(), h / (float) b.getHeight());
        int sw = Math.round(w / s), sh = Math.round(h / s);
        Rect src = new Rect((b.getWidth() - sw) / 2, (b.getHeight() - sh) / 2,
                (b.getWidth() + sw) / 2, (b.getHeight() + sh) / 2);
        c.drawBitmap(b, src, new Rect(x, y, x + w, y + h), paint);
    }

    private void label(Canvas c, String t, int x, int y, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(54);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        float w = p.measureText(t);
        p.setColor(color);
        c.drawRoundRect(new RectF(x + 24, y + 24, x + 24 + w + 60, y + 124), 28, 28, p);
        p.setColor(Color.WHITE);
        c.drawText(t, x + 54, y + 96, p);
    }

    private Uri save() {
        if (result == null) {
            Toast.makeText(this, "Önce iki fotoğraf seç", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (savedUri != null) return savedUri;
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, "3K_kolaj_" + System.currentTimeMillis() + ".jpg");
            v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/3K Studio");
            Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            try (OutputStream o = getContentResolver().openOutputStream(u)) {
                result.compress(Bitmap.CompressFormat.JPEG, 92, o);
            }
            savedUri = u;
            return u;
        } catch (Exception e) {
            Toast.makeText(this, "Kaydedilemedi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private void share() {
        Uri u = save();
        if (u == null) return;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("image/jpeg");
        i.putExtra(Intent.EXTRA_STREAM, u);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Paylaş"));
    }
}
