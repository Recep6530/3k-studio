package com.recepparlak.studio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = Ui.root(this);

        l.addView(Ui.text(this, "3K Studio", 36, Ui.ACC, true));
        l.addView(Ui.text(this, "Önce/sonra kolaj ve video düzenleme.\nİnternet gerekmez, her şey telefonunda.", 15, Ui.SUB, false));

        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(-1, Ui.dp(this, 36));
        android.view.View g = new android.view.View(this);
        g.setLayoutParams(gap);
        l.addView(g);

        l.addView(Ui.btn(this, "📷  Önce / Sonra Kolaj", Ui.ACC,
                v -> startActivity(new Intent(this, CollageActivity.class))));
        l.addView(Ui.btn(this, "🎬  Video Düzenle (kes, ses)", Ui.BLUE,
                v -> startActivity(new Intent(this, VideoActivity.class))));

        l.addView(Ui.spacer(this));
        TextView f = Ui.text(this, "Recep Parlak tarafından düzenlenmiştir", 13, Ui.SUB, false);
        Ui.center(f);
        l.addView(f);
    }
}
