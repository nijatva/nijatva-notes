package in.devira.nijatva.notes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** About the developer. Counts are the live GitHub numbers at build time. */
public class AboutActivity extends Activity {

    private static final String PROFILE = "https://github.com/vasudev-work";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.apply(this);
        setContentView(build());
    }

    private View build() {
        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(Ui.BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 26), Ui.dp(this, 20), Ui.dp(this, 28));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = Ui.text(this, "\u2190", 24f, Ui.TEXT);
        back.setPadding(0, 0, Ui.dp(this, 16), 0);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        head.addView(back);
        TextView title = Ui.text(this, "About", 24f, Ui.TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(head);

        TextView section = Ui.text(this, "Developer", 13f, Ui.ACCENT);
        section.setTypeface(Typeface.DEFAULT_BOLD);
        section.setPadding(Ui.dp(this, 4), Ui.dp(this, 22), 0, Ui.dp(this, 6));
        root.addView(section);

        TextView sub = Ui.text(this, "About the developer", 16f, Ui.TEXT);
        sub.setTypeface(Typeface.DEFAULT_BOLD);
        sub.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 12));
        root.addView(sub);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(Ui.round(this, Ui.CARD, 22));
        card.setPadding(Ui.dp(this, 20), Ui.dp(this, 26), Ui.dp(this, 20), Ui.dp(this, 22));

        // avatar
        TextView avatar = Ui.text(this, "VD", 34f, Ui.ON_ACCENT);
        avatar.setTypeface(Typeface.DEFAULT_BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.circle(this, Ui.ACCENT));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(Ui.dp(this, 96), Ui.dp(this, 96));
        card.addView(avatar, alp);

        TextView name = Ui.text(this, "Vasudev Dangaya", 22f, Ui.TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, Ui.dp(this, 16), 0, 0);
        card.addView(name);

        TextView handle = Ui.text(this, "@vasudev-work", 14f, Ui.ACCENT);
        handle.setGravity(Gravity.CENTER);
        handle.setPadding(0, Ui.dp(this, 4), 0, 0);
        card.addView(handle);

        TextView tagline = Ui.text(this,
                "I build Android apps with clean UI and real, working features.\n"
                        + "Kotlin \u00b7 Android SDK \u00b7 no unnecessary dependencies.",
                14f, Ui.TEXT);
        tagline.setGravity(Gravity.CENTER);
        tagline.setLineSpacing(Ui.dp(this, 3), 1f);
        tagline.setPadding(0, Ui.dp(this, 14), 0, 0);
        card.addView(tagline);

        // stats
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        stats.setBackground(Ui.round(this, Ui.CARD_HI, 18));
        stats.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 20);
        stats.addView(stat("30", "Repositories"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(stat("10", "Nijatva apps"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(stat("0.2.0", "This build"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(stats, slp);

        // maintains
        LinearLayout maintains = new LinearLayout(this);
        maintains.setOrientation(LinearLayout.HORIZONTAL);
        maintains.setGravity(Gravity.CENTER_VERTICAL);
        maintains.setPadding(0, Ui.dp(this, 20), 0, 0);

        TextView mLabel = Ui.text(this, "Maintains", 15f, Ui.TEXT);
        mLabel.setTypeface(Typeface.DEFAULT_BOLD);
        maintains.addView(mLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        maintains.addView(badge("B"));
        maintains.addView(badge("C"));
        maintains.addView(badge("D"));
        maintains.addView(badge("+7"));
        card.addView(maintains);

        card.addView(row("Location", "India"));
        card.addView(row("Website", "github.com/vasudev-work"));
        card.addView(row("Email", "vasudev.work@rediffmail.com"));

        TextView open = Ui.text(this, "\u2039\u2039  View GitHub Profile", 15f, Ui.ON_ACCENT);
        open.setTypeface(Typeface.DEFAULT_BOLD);
        open.setGravity(Gravity.CENTER);
        open.setBackground(Ui.round(this, Ui.ACCENT, 26));
        open.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        olp.topMargin = Ui.dp(this, 22);
        open.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PROFILE)));
                } catch (Exception e) {
                    Toast.makeText(AboutActivity.this, PROFILE, Toast.LENGTH_LONG).show();
                }
            }
        });
        card.addView(open, olp);

        root.addView(card);
        sc.addView(root);
        return sc;
    }

    private View stat(String value, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        TextView v = Ui.text(this, value, 22f, Ui.TEXT);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.CENTER);
        TextView l = Ui.text(this, label, 12f, Ui.MUTED);
        l.setGravity(Gravity.CENTER);
        l.setPadding(0, Ui.dp(this, 4), 0, 0);
        col.addView(v);
        col.addView(l);
        return col;
    }

    private View badge(String label) {
        TextView t = Ui.text(this, label, 12f, Ui.ON_ACCENT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.round(this, "+7".equals(label) ? Ui.CARD_HI : Ui.ACCENT, 12));
        if ("+7".equals(label)) t.setTextColor(Ui.TEXT);
        t.setPadding(Ui.dp(this, 11), Ui.dp(this, 9), Ui.dp(this, 11), Ui.dp(this, 9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 6);
        t.setLayoutParams(lp);
        return t;
    }

    private View row(String label, String value) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, Ui.dp(this, 16), 0, 0);
        TextView l = Ui.text(this, label, 14f, Ui.TEXT);
        l.setTypeface(Typeface.DEFAULT_BOLD);
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = Ui.text(this, value, 13f, Ui.MUTED);
        v.setGravity(Gravity.END);
        r.addView(v);
        return r;
    }
}
