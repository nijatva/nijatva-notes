package in.devira.nijatva.notes;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Appearance: theme and accent colour, applied live. */
public class AppearanceActivity extends Activity {

    private LinearLayout themeRow;
    private LinearLayout accentRow;

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

        TextView title = Ui.text(this, "Appearance", 24f, Ui.TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(head);

        TextView section = Ui.text(this, "Appearance", 13f, Ui.MUTED);
        section.setTypeface(Typeface.DEFAULT_BOLD);
        section.setPadding(Ui.dp(this, 4), Ui.dp(this, 26), 0, Ui.dp(this, 10));
        root.addView(section);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.round(this, Ui.CARD, 22));
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 20));

        TextView themeLabel = Ui.text(this, "Theme", 16f, Ui.TEXT);
        themeLabel.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(themeLabel);

        themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeRow.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(themeRow);

        TextView accentLabel = Ui.text(this, "Accent colour", 16f, Ui.TEXT);
        accentLabel.setTypeface(Typeface.DEFAULT_BOLD);
        accentLabel.setPadding(0, Ui.dp(this, 26), 0, 0);
        card.addView(accentLabel);

        accentRow = new LinearLayout(this);
        accentRow.setOrientation(LinearLayout.HORIZONTAL);
        accentRow.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(accentRow);

        root.addView(card);
        sc.addView(root);

        fillChips();
        return sc;
    }

    private void fillChips() {
        String theme = Theme.theme(this);
        String accent = Theme.accent(this);

        themeRow.removeAllViews();
        addChip(themeRow, "System", Theme.SYSTEM, theme, true);
        addChip(themeRow, "Dark", Theme.DARK, theme, true);
        addChip(themeRow, "Light", Theme.LIGHT, theme, true);
        addChip(themeRow, "Black", Theme.BLACK, theme, true);

        accentRow.removeAllViews();
        addChip(accentRow, "Green", Theme.GREEN, accent, false);
        addChip(accentRow, "Saffron", Theme.SAFFRON, accent, false);
        addChip(accentRow, "Indigo", Theme.INDIGO, accent, false);
    }

    private void addChip(LinearLayout row, String label, final String key,
                         String current, final boolean isTheme) {
        TextView c = Ui.chip(this, label, key.equals(current));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(this, 8);
        c.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Theme.set(AppearanceActivity.this,
                        isTheme ? key : Theme.theme(AppearanceActivity.this),
                        isTheme ? Theme.accent(AppearanceActivity.this) : key);
                recreate();
            }
        });
        row.addView(c, lp);
    }
}
