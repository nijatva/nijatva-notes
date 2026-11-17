package in.devira.nijatva.notes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Editor: plain Markdown in, plain Markdown out. Autosave, a syntax toolbar and
 *  a preview. No rich-text layer, so the file on disk is exactly what you see. */
public class EditActivity extends Activity {

    private Store store;
    private Store.Note note;
    private EditText title, body;
    private TextView meta, preview;
    private ScrollView previewWrap;
    private boolean showingPreview = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable save = new Runnable() {
        public void run() {
            commit();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.apply(this);
        store = new Store(this);
        note = store.get(getIntent().getStringExtra("note_id"));
        if (note == null) {
            finish();
            return;
        }
        if (note.locked && !Vault.isUnlocked()) {
            Toast.makeText(this, "Unlock the vault to edit this note", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setContentView(buildUi());
        title.setText(note.title);
        body.setText(note.body);
        updateMeta();
    }

    @Override
    protected void onPause() {
        super.onPause();
        commit();
    }

    private void commit() {
        handler.removeCallbacks(save);
        if (note == null || title == null) return;
        note.title = title.getText().toString().trim();
        note.body = body.getText().toString();
        store.save(note);
        updateMeta();
    }

    private void updateMeta() {
        if (meta == null) return;
        meta.setText(Ui.stamp(note.modified) + "   \u00b7   " + store.notebookName(note.notebook));
    }

    // ---------------- ui ----------------

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), 0);

        // top bar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = Ui.text(this, "\u2190", 22f, Ui.TEXT);
        back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commit();
                finish();
            }
        });
        bar.addView(back);

        TextView toggle = Ui.button(this, "Preview");
        toggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showingPreview = !showingPreview;
                ((TextView) v).setText(showingPreview ? "Edit" : "Preview");
                renderPreview();
            }
        });
        bar.addView(toggle);

        View gap = new View(this);
        bar.addView(gap, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView done = Ui.text(this, "Done", 15f, Ui.ACCENT);
        done.setTypeface(Typeface.DEFAULT_BOLD);
        done.setPadding(Ui.dp(this, 12), 0, 0, 0);
        done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commit();
                finish();
            }
        });
        bar.addView(done);
        root.addView(bar);

        meta = Ui.text(this, "", 11f, Ui.MUTED);
        meta.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 10));
        meta.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickNotebook();
            }
        });
        root.addView(meta);

        title = new EditText(this);
        title.setHint("Title");
        title.setHintTextColor(Ui.MUTED);
        title.setTextColor(Ui.TEXT);
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setBackground(null);
        title.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        title.addTextChangedListener(watcher);
        root.addView(title);

        body = new EditText(this);
        body.setHint("Start writing in Markdown\u2026");
        body.setHintTextColor(Ui.MUTED);
        body.setTextColor(Ui.TEXT);
        body.setTextSize(16f);
        body.setGravity(Gravity.TOP);
        body.setBackground(null);
        body.setLineSpacing(Ui.dp(this, 3), 1f);
        body.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        body.addTextChangedListener(watcher);
        root.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        previewWrap = new ScrollView(this);
        previewWrap.setVisibility(View.GONE);
        preview = Ui.text(this, "", 16f, Ui.TEXT);
        preview.setLineSpacing(Ui.dp(this, 4), 1f);
        previewWrap.addView(preview);
        root.addView(previewWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // formatting toolbar
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 8));
        String[][] specs = {
                {"B", "**", "**"}, {"I", "*", "*"}, {"S", "~~", "~~"}, {"`", "`", "`"},
                {"H1", "# ", ""}, {"H2", "## ", ""}, {"H3", "### ", ""},
                {"\u2022", "- ", ""}, {"1.", "1. ", ""}, {"\u2610", "- [ ] ", ""},
                {">", "> ", ""}, {"===", "==", "=="}, {"Link", "[", "](url)"}, {"\u2014", "\n---\n", ""}
        };
        for (final String[] s : specs) {
            TextView b = Ui.button(this, s[0]);
            b.setTextSize(13f);
            b.setTypeface(s[0].equals("B") ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(this, 6);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    insert(s[1], s[2]);
                }
            });
            tools.addView(b, lp);
        }
        hsv.addView(tools);

        // The formatting bar is the bottom-most strip: give it a hairline and keep
        // it clear of the navigation bar and the keyboard.
        LinearLayout bar2 = new LinearLayout(this);
        bar2.setOrientation(LinearLayout.VERTICAL);
        bar2.setBackgroundColor(Ui.BG);
        bar2.addView(Ui.divider(this));
        bar2.addView(hsv);
        root.addView(bar2);

        final int padH = Ui.dp(this, 16);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int bottom = insets.getSystemWindowInsetBottom();
                // adjustResize already shrinks the window for the IME, so only the
                // navigation bar needs padding; a tall inset is the keyboard.
                int pad = bottom > Ui.dp(EditActivity.this, 120) ? 0 : bottom;
                v.setPadding(padH, Ui.dp(EditActivity.this, 16), padH, pad);
                return insets;
            }
        });

        return root;
    }

    private final TextWatcher watcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }

        public void afterTextChanged(Editable s) {
            handler.removeCallbacks(save);
            handler.postDelayed(save, 800);
            if (showingPreview) renderPreview();
        }
    };

    private void renderPreview() {
        boolean p = showingPreview;
        body.setVisibility(p ? View.GONE : View.VISIBLE);
        title.setVisibility(p ? View.GONE : View.VISIBLE);
        previewWrap.setVisibility(p ? View.VISIBLE : View.GONE);
        if (p) {
            String text = body.getText().toString();
            String head = title.getText().toString();
            CharSequence rendered = Markdown.render(
                    (head.isEmpty() ? "" : "# " + head + "\n\n") + text);
            preview.setText(rendered);
        }
    }

    /** Inserts Markdown syntax around the selection, or prefixes the current line. */
    private void insert(String before, String after) {
        int start = Math.max(body.getSelectionStart(), 0);
        int end = Math.max(body.getSelectionEnd(), 0);
        if (start > end) {
            int t = start;
            start = end;
            end = t;
        }
        Editable e = body.getText();
        if (after.isEmpty()) {
            // line prefix
            int lineStart = start;
            while (lineStart > 0 && e.charAt(lineStart - 1) != '\n') lineStart--;
            String prefix = e.toString().substring(lineStart, start);
            if (prefix.length() >= before.length() && prefix.startsWith(before)) {
                e.delete(lineStart, lineStart + before.length());
                body.setSelection(Math.max(lineStart, start - before.length()));
            } else {
                e.insert(lineStart, before);
                body.setSelection(start + before.length());
            }
        } else {
            String sel = e.toString().substring(start, end);
            e.replace(start, end, before + sel + after);
            body.setSelection(start + before.length() + sel.length());
        }
        body.requestFocus();
    }

    private void pickNotebook() {
        List<Store.Notebook> nbs = store.notebooks;
        String[] names = new String[nbs.size()];
        for (int i = 0; i < nbs.size(); i++) names[i] = nbs.get(i).name;
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Notebook")
                .setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        Store.Notebook target = store.notebooks.get(which);
                        if (!target.id.equals(note.notebook)) {
                            store.deleteForeverFileOnly(note);
                            note.notebook = target.id;
                            store.save(note);
                            updateMeta();
                        }
                    }
                }).show();
    }

    @Override
    public void onBackPressed() {
        commit();
        super.onBackPressed();
    }
}
