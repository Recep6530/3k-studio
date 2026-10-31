package com.recepparlak.studio;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Ui {
    static final int BG = 0xFF0F172A, CARD = 0xFF1E293B, ACC = 0xFFF59E0B, TXT = 0xFFF8FAFC,
            SUB = 0xFF94A3B8, GREEN = 0xFF10B981, BLUE = 0xFF3B82F6, RED = 0xFFEF4444;

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable round(Context c, int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radius));
        return g;
    }

    static LinearLayout root(Activity a) {
        a.getWindow().setStatusBarColor(BG);
        a.getWindow().setNavigationBarColor(BG);
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(a, 20);
        l.setPadding(p, p, p, p);
        sv.addView(l);
        a.setContentView(sv);
        return l;
    }

    static Button btn(Context c, String t, int color, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(color == ACC ? 0xFF1A1A1A : Color.WHITE);
        b.setBackground(round(c, color, 14));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(c, 54));
        p.topMargin = dp(c, 10);
        b.setLayoutParams(p);
        return b;
    }

    static TextView text(Context c, String t, int size, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(c, 8);
        v.setLayoutParams(p);
        return v;
    }

    static View spacer(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));
        return v;
    }

    static void center(TextView v) {
        v.setGravity(Gravity.CENTER);
    }
}
