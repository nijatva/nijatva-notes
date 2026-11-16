package in.devira.nijatva.notes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;

/**
 * Appearance: four themes (system, dark, light, black) and three accents.
 * Resolved into a palette that Ui mirrors, so every view built after
 * {@link Ui#apply(Activity)} picks up the current colours.
 */
public class Theme {

    public static final String SYSTEM = "system";
    public static final String DARK = "dark";
    public static final String LIGHT = "light";
    public static final String BLACK = "black";

    public static final String GREEN = "green";
    public static final String SAFFRON = "saffron";
    public static final String INDIGO = "indigo";

    private static final String PREFS = "appearance";
    private static final String K_THEME = "theme";
    private static final String K_ACCENT = "accent";

    public static int bg, card, cardHi, text, muted, line, accent, danger, onAccent;
    public static int rev = 0;
    public static boolean isLight;

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String theme(Context c) {
        return prefs(c).getString(K_THEME, SYSTEM);
    }

    public static String accent(Context c) {
        return prefs(c).getString(K_ACCENT, GREEN);
    }

    public static void set(Context c, String theme, String accent) {
        prefs(c).edit().putString(K_THEME, theme).putString(K_ACCENT, accent).apply();
        rev++;
    }

    public static boolean resolvesLight(Context c) {
        String t = theme(c);
        if (LIGHT.equals(t)) return true;
        if (DARK.equals(t) || BLACK.equals(t)) return false;
        int mode = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode != Configuration.UI_MODE_NIGHT_YES;
    }

    /** Fills in the palette for the current settings. */
    public static void resolve(Context c) {
        String t = theme(c);
        boolean black = BLACK.equals(t);
        isLight = resolvesLight(c);

        if (black) {
            bg = 0xFF000000;
            card = 0xFF121212;
            cardHi = 0xFF1B1B1B;
            line = 0xFF262626;
        } else if (!isLight) {
            bg = 0xFF0E0E0E;
            card = 0xFF1C1C1C;
            cardHi = 0xFF262626;
            line = 0xFF2C2C2C;
        } else {
            bg = 0xFFF6F8FA;
            card = 0xFFFFFFFF;
            cardHi = 0xFFEDF1F5;
            line = 0xFFE1E6EC;
        }

        text = isLight ? 0xFF12151C : 0xFFEDEDED;
        muted = isLight ? 0xFF5B6472 : 0xFF8C8C8C;
        danger = isLight ? 0xFFC0392B : 0xFFE0645A;

        String a = accent(c);
        if (SAFFRON.equals(a)) accent = 0xFFE8A33D;
        else if (INDIGO.equals(a)) accent = 0xFF4C5BD4;
        else accent = 0xFF0FA958;

        onAccent = isLight ? 0xFFFFFFFF : 0xFF10130F;
    }

    public static String themeLabel(Context c) {
        String t = theme(c);
        if (DARK.equals(t)) return "Dark";
        if (LIGHT.equals(t)) return "Light";
        if (BLACK.equals(t)) return "Black";
        return "System";
    }

    public static String accentLabel(Context c) {
        String a = accent(c);
        if (SAFFRON.equals(a)) return "Saffron";
        if (INDIGO.equals(a)) return "Indigo";
        return "Green";
    }

    /** Applies the palette to an activity's window, including status-bar icons. */
    public static void window(Activity a) {
        Window w = a.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(bg));
        w.setStatusBarColor(bg);
        w.setNavigationBarColor(bg);
        View d = w.getDecorView();
        int flags = d.getSystemUiVisibility();
        if (isLight) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        d.setSystemUiVisibility(flags);
    }
}
