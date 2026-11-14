package in.devira.nijatva.notes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Notebooks, the trash and the vault entry points. */
public class NotebooksActivity extends Activity {

    private Store store;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        store = new Store(this);
        fill();
    }

    private View build() {
        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 22), Ui.dp(this, 18), Ui.dp(this, 24));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = Ui.text(this, "\u2190", 22f, Ui.TEXT);
        back.setPadding(0, 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        head.addView(back);

        TextView title = Ui.text(this, "Notebooks", 24f, Ui.TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView add = Ui.text(this, "New", 14f, Ui.ACCENT);
        add.setTypeface(Typeface.DEFAULT_BOLD);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                newNotebook();
            }
        });
        head.addView(add);
        root.addView(head);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, Ui.dp(this, 18), 0, 0);
        root.addView(list);

        sc.addView(root);
        return sc;
    }

    private void fill() {
        list.removeAllViews();
        addRow("All notes", store.count("all") + "", null);
        for (Store.Notebook nb : store.notebooks) {
            addRow(nb.name, store.count(nb.id) + "", nb);
        }
        addSection("System");
        addNavRow("Recently deleted", store.countTrash() + "", new Runnable() {
            public void run() {
                startActivity(new android.content.Intent(NotebooksActivity.this, TrashActivity.class));
            }
        });
        addNavRow("Vault", Vault.isSet(this) ? (Vault.isUnlocked() ? "unlocked" : "locked") : "not set up",
                new Runnable() {
                    public void run() {
                        vault();
                    }
                });
    }

    private void addSection(String name) {
        TextView h = Ui.text(this, name, 12f, Ui.MUTED);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setPadding(Ui.dp(this, 4), Ui.dp(this, 18), 0, Ui.dp(this, 6));
        list.addView(h);
    }

    private void addRow(String name, String count, final Store.Notebook nb) {
        LinearLayout row = row(name, count);
        if (nb != null && !nb.system) {
            row.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    notebookMenu(nb);
                    return true;
                }
            });
        }
        list.addView(row);
    }

    private void addNavRow(String name, String count, final Runnable action) {
        LinearLayout row = row(name, count);
        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                action.run();
            }
        });
        list.addView(row);
    }

    private LinearLayout row(String name, String count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Ui.round(this, Ui.CARD, 14));
        row.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 8);
        row.setLayoutParams(lp);

        TextView t = Ui.text(this, name, 15f, Ui.TEXT);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView c = Ui.text(this, count, 13f, Ui.MUTED);
        row.addView(c);
        return row;
    }

    private void notebookMenu(final Store.Notebook nb) {
        String[] items = {"Rename", "Delete notebook"};
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(nb.name)
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            promptName("Rename notebook", nb.name, new NameSink() {
                                public void accept(String name) {
                                    store.renameNotebook(nb, name);
                                    fill();
                                }
                            });
                        } else {
                            confirmDelete(nb);
                        }
                    }
                }).show();
    }

    private void confirmDelete(final Store.Notebook nb) {
        int n = store.count(nb.id);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Delete \u201c" + nb.name + "\u201d?")
                .setMessage(n == 0 ? "This notebook is empty."
                        : n + (n == 1 ? " note moves" : " notes move") + " to Default notebook. No note is deleted.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        store.deleteNotebook(nb);
                        fill();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void newNotebook() {
        promptName("New notebook", "", new NameSink() {
            public void accept(String name) {
                store.addNotebook(name);
                fill();
            }
        });
    }

    private interface NameSink {
        void accept(String name);
    }

    private void promptName(String title, String initial, final NameSink sink) {
        final EditText e = new EditText(this);
        e.setHint("Notebook name");
        e.setText(initial);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        box.addView(e);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String name = e.getText().toString().trim();
                        if (name.isEmpty()) return;
                        sink.accept(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void vault() {
        if (!Vault.isSet(this)) {
            toast("Set up the vault from the Notes menu, then lock notes from the list.");
            return;
        }
        if (Vault.isUnlocked()) {
            Vault.lock();
            toast("Vault locked");
        } else {
            final EditText e = new EditText(this);
            e.setHint("Passphrase");
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            LinearLayout box = new LinearLayout(this);
            box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
            box.addView(e);
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Unlock vault")
                    .setView(box)
                    .setPositiveButton("Unlock", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            toast(Vault.unlock(NotebooksActivity.this, e.getText().toString())
                                    ? "Vault unlocked" : "Wrong passphrase");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
        fill();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
