package in.nijatva.notes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nijatva.signature.SignatureTheme;

import java.util.Calendar;

/** Colour, spacing and small widget helpers. The palette comes from nijatva-core's
 *  SignatureTheme and is refreshed by {@link #apply(Activity)} on every screen, so
 *  Notes follows the theme and accent the user picks in the shared Appearance screen. */
public class Ui {

    public static int BG = 0xFF0E0E0E;
    public static int CARD = 0xFF1C1C1C;
    public static int CARD_HI = 0xFF262626;
    public static int TEXT = 0xFFEDEDED;
    public static int MUTED = 0xFF8C8C8C;
    public static int ACCENT = 0xFFF5B841;
    public static int LINE = 0xFF2C2C2C;
    public static int DANGER = 0xFFE0645A;
    public static int ON_ACCENT = 0xFF10130F;

    public static void apply(Activity a) {
        SignatureTheme.applyTo(a);
        BG = SignatureTheme.background(a);
        CARD = SignatureTheme.surface(a);
        CARD_HI = SignatureTheme.soft(SignatureTheme.accent(a), 0x1F);
        TEXT = SignatureTheme.onSurface(a);
        MUTED = SignatureTheme.muted(a);
        ACCENT = SignatureTheme.accent(a);
        LINE = SignatureTheme.hairline(a);
        DANGER = SignatureTheme.isDark(a) ? 0xFFE0645A : 0xFFC0392B;
        ON_ACCENT = SignatureTheme.onAccent(ACCENT);
        // SignatureTheme handles the status bar; the navigation bar icons follow here.
        int flags = a.getWindow().getDecorView().getSystemUiVisibility();
        if (SignatureTheme.isDark(a)) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        a.getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable round(Context c, int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable outline(Context c, int color, int stroke, float radiusDp) {
        GradientDrawable d = round(c, Color.TRANSPARENT, radiusDp);
        d.setStroke(dp(c, stroke), color);
        return d;
    }

    public static GradientDrawable circle(Context c, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    public static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView button(Context c, String label) {
        TextView t = text(c, label, 14f, TEXT);
        t.setGravity(Gravity.CENTER);
        int px = dp(c, 14), py = dp(c, 8);
        t.setPadding(px, py, px, py);
        t.setBackground(round(c, CARD, 18));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    public static TextView chip(Context c, String label, boolean selected) {
        TextView t = text(c, label, 13f, selected ? ON_ACCENT : TEXT);
        t.setGravity(Gravity.CENTER);
        int px = dp(c, 14), py = dp(c, 7);
        t.setPadding(px, py, px, py);
        t.setBackground(selected ? round(c, ACCENT, 16) : outline(c, LINE, 1, 16));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.7f))));
        return v;
    }

    public static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, heightDp)));
        return v;
    }

    public static String when(long millis) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);
        boolean sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR);
        String pattern = sameYear ? "d/M" : "d/M/yyyy";
        return DateFormat.format(pattern, millis).toString();
    }

    public static String stamp(long millis) {
        return DateFormat.format("d/M/yyyy, h:mm a", millis).toString();
    }

    public static Typeface mono() {
        return Typeface.MONOSPACE;
    }
}
