package com.aiham.quotes;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends android.app.Activity {
    private static final String PREFS = "quotes_prefs";
    private final Random random = new Random();
    private final List<Quote> quotes = Arrays.asList(
            new Quote("لا تؤجل الحياة حتى تصبح الظروف مثالية.", "مقولة ملهمة", "الحياة"),
            new Quote("النجاح ليس نهاية الطريق، والفشل ليس نهايته؛ الشجاعة هي أن تكمل.", "اقتباس", "النجاح"),
            new Quote("ابدأ بما تستطيع، وبما لديك، ومن مكانك الحالي.", "مقولة ملهمة", "التحفيز"),
            new Quote("العقل الهادئ يرى في الفوضى طريقاً، لا جداراً.", "اقتباس", "الحكمة"),
            new Quote("أقوى خطوة قد تكون مجرد أن تبدأ.", "مقولة قصيرة", "التحفيز"),
            new Quote("كل يوم يمنحك فرصة صغيرة لتصبح أفضل من الأمس.", "اقتباس", "التطور"),
            new Quote("لا تقارن بدايتك بمنتصف طريق شخص آخر.", "مقولة ملهمة", "الحياة"),
            new Quote("حين تعرف لماذا تريد الوصول، يصبح الطريق أوضح.", "اقتباس", "الأهداف")
    );

    private TextView quoteText, authorText, categoryText, favoriteButton;
    private Quote current;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        showQuote(quotes.get(random.nextInt(quotes.size())));
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(18));
        root.setBackgroundColor(Color.rgb(16,17,20));

        TextView title = text("اقتباسات", 30, Color.WHITE, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(4), dp(20), dp(4), dp(10));

        categoryText = text("", 14, Color.rgb(170,150,255), Typeface.BOLD);
        categoryText.setGravity(Gravity.CENTER);
        content.addView(categoryText, lp(-1, dp(35)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(32), dp(24), dp(32));
        card.setBackgroundColor(Color.rgb(30,31,36));

        quoteText = text("", 25, Color.WHITE, Typeface.NORMAL);
        quoteText.setGravity(Gravity.CENTER);
        quoteText.setLineSpacing(0f, 1.25f);
        card.addView(quoteText, lp(-1, -2));

        authorText = text("", 15, Color.LTGRAY, Typeface.NORMAL);
        authorText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ap = lp(-1, dp(45)); ap.topMargin = dp(18);
        card.addView(authorText, ap);
        content.addView(card, lp(-1, -2));

        favoriteButton = button("♡ حفظ في المفضلة");
        content.addView(favoriteButton, lp(-1, dp(52)));
        favoriteButton.setOnClickListener(v -> toggleFavorite());

        Button next = button("اقتباس جديد  ↻");
        content.addView(next, lp(-1, dp(52)));
        next.setOnClickListener(v -> nextQuote());

        Button copy = button("نسخ الاقتباس");
        content.addView(copy, lp(-1, dp(52)));
        copy.setOnClickListener(v -> copyQuote());

        Button share = button("مشاركة");
        content.addView(share, lp(-1, dp(52)));
        share.setOnClickListener(v -> shareQuote());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showQuote(Quote q) {
        current = q;
        quoteText.setText("“ " + q.text + " ”");
        authorText.setText("— " + q.author);
        categoryText.setText("# " + q.category);
        updateFavoriteState();
    }

    private void nextQuote() {
        Quote q = quotes.get(random.nextInt(quotes.size()));
        if (quotes.size() > 1 && q == current) q = quotes.get((quotes.indexOf(q) + 1) % quotes.size());
        showQuote(q);
    }

    private String key() { return "fav_" + quotes.indexOf(current); }
    private void toggleFavorite() {
        boolean now = !prefs.getBoolean(key(), false);
        prefs.edit().putBoolean(key(), now).apply();
        updateFavoriteState();
        Toast.makeText(this, now ? "تم الحفظ في المفضلة" : "تمت الإزالة من المفضلة", Toast.LENGTH_SHORT).show();
    }
    private void updateFavoriteState() {
        if (favoriteButton != null) favoriteButton.setText(prefs.getBoolean(key(), false) ? "♥ محفوظ في المفضلة" : "♡ حفظ في المفضلة");
    }
    private void copyQuote() {
        ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("اقتباس", current.text + "\n— " + current.author));
        Toast.makeText(this, "تم نسخ الاقتباس", Toast.LENGTH_SHORT).show();
    }
    private void shareQuote() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, "“ " + current.text + " ”\n— " + current.author);
        startActivity(Intent.createChooser(i, "مشاركة الاقتباس"));
    }

    private TextView text(String s, int size, int color, int style) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.DEFAULT, style); return t;
    }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextSize(15); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setBackgroundColor(Color.rgb(45,46,53)); return b;
    }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private static class Quote {
        final String text, author, category;
        Quote(String text, String author, String category) { this.text=text; this.author=author; this.category=category; }
    }
}
