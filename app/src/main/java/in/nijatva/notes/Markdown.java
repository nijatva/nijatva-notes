package in.nijatva.notes;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

/** A deliberately small CommonMark-ish renderer: headings, bold, italic, code,
 *  highlight, quotes, bullets, checklists and rules. Enough to read a note,
 *  not a full CommonMark implementation. */
public class Markdown {

    public static CharSequence render(String md) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        if (md == null) return out;
        String[] lines = md.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int start = out.length();
            if (line.startsWith("### ")) {
                inline(line.substring(4), out);
                size(out, start, 1.05f);
                bold(out, start);
            } else if (line.startsWith("## ")) {
                inline(line.substring(3), out);
                size(out, start, 1.18f);
                bold(out, start);
            } else if (line.startsWith("# ")) {
                inline(line.substring(2), out);
                size(out, start, 1.35f);
                bold(out, start);
            } else if (line.equals("---") || line.equals("***")) {
                out.append("\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014");
                out.setSpan(new ForegroundColorSpan(Ui.LINE), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.startsWith("> ")) {
                out.append("  ");
                int qs = out.length();
                inline(line.substring(2), out);
                out.setSpan(new ForegroundColorSpan(Ui.MUTED), qs, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new StyleSpan(Typeface.ITALIC), qs, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.trim().startsWith("- [ ] ") || line.trim().startsWith("- [x] ")
                    || line.trim().startsWith("- [X] ")) {
                boolean done = !line.contains("[ ]");
                out.append(done ? "\u2611  " : "\u2610  ");
                int cs = out.length();
                inline(line.trim().substring(6), out);
                if (done) {
                    out.setSpan(new ForegroundColorSpan(Ui.MUTED), cs, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                out.append("\u2022  ");
                inline(line.substring(2), out);
            } else if (line.matches("^\\d+\\. .*")) {
                int dot = line.indexOf('.');
                out.append(line.substring(0, dot + 1)).append("  ");
                inline(line.substring(dot + 2), out);
            } else {
                inline(line, out);
            }
            if (i < lines.length - 1) out.append("\n");
        }
        return out;
    }

    private static void size(SpannableStringBuilder sb, int start, float mult) {
        sb.setSpan(new RelativeSizeSpan(mult), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static void bold(SpannableStringBuilder sb, int start) {
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /** Handles **bold**, *italic*, `code` and ==highlight== inline. */
    private static void inline(String s, SpannableStringBuilder out) {
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("**", i)) {
                int end = s.indexOf("**", i + 2);
                if (end > i + 2) {
                    int st = out.length();
                    out.append(s, i + 2, end);
                    out.setSpan(new StyleSpan(Typeface.BOLD), st, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 2;
                    continue;
                }
            }
            if (s.startsWith("==", i)) {
                int end = s.indexOf("==", i + 2);
                if (end > i + 2) {
                    int st = out.length();
                    out.append(s, i + 2, end);
                    out.setSpan(new BackgroundColorSpan(0x55F5B841), st, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 2;
                    continue;
                }
            }
            if (s.charAt(i) == '`') {
                int end = s.indexOf('`', i + 1);
                if (end > i + 1) {
                    int st = out.length();
                    out.append(s, i + 1, end);
                    out.setSpan(new TypefaceSpan("monospace"), st, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new ForegroundColorSpan(Ui.ACCENT), st, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 1;
                    continue;
                }
            }
            if (s.charAt(i) == '*') {
                int end = s.indexOf('*', i + 1);
                if (end > i + 1) {
                    int st = out.length();
                    out.append(s, i + 1, end);
                    out.setSpan(new StyleSpan(Typeface.ITALIC), st, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 1;
                    continue;
                }
            }
            out.append(s.charAt(i));
            i++;
        }
    }
}
