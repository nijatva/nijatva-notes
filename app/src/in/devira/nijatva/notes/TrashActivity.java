package in.devira.nijatva.notes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Recently deleted. Notes are held for 30 days, then purged on app start. */
public class TrashActivity extends Activity {

    private Store store;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
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
        TextView title = Ui.text(this, "Recently deleted", 22f, Ui.TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView empty = Ui.text(this, "Empty", 13f, Ui.DANGER);
        empty.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                emptyTrash();
            }
        });
        head.addView(empty);
        root.addView(head);

        TextView note = Ui.text(this, "Notes stay here for 30 days.", 12f, Ui.MUTED);
        note.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 14));
        root.addView(note);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        sc.addView(root);
        setContentView(sc);
        store = new Store(this);
        fill();
    }

    private void fill() {
        list.removeAllViews();
        List<Store.Note> gone = store.list("trash", "");
        if (gone.isEmpty()) {
            TextView e = Ui.text(this, "Nothing deleted.", 14f, Ui.MUTED);
            e.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), 0, 0);
            list.addView(e);
            return;
        }
        for (final Store.Note n : gone) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackground(Ui.round(this, Ui.CARD, 14));
            row.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Ui.dp(this, 8);
            row.setLayoutParams(lp);

            TextView t = Ui.text(this, n.title.isEmpty() ? "Untitled note" : n.title, 15f, Ui.TEXT);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(t);
            TextView m = Ui.text(this, "Deleted " + Ui.when(n.deletedAt) + "  \u00b7  tap to restore", 12f, Ui.MUTED);
            m.setPadding(0, Ui.dp(this, 5), 0, 0);
            row.addView(m);

            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    store.restore(n);
                    fill();
                    Toast.makeText(TrashActivity.this, "Restored", Toast.LENGTH_SHORT).show();
                }
            });
            row.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(TrashActivity.this,
                            android.R.style.Theme_DeviceDefault_Dialog_Alert)
                            .setTitle("Delete forever?")
                            .setMessage("This cannot be undone.")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int w) {
                                    store.deleteForever(n);
                                    fill();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    return true;
                }
            });
            list.addView(row);
        }
    }

    private void emptyTrash() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Empty Recently deleted?")
                .setMessage("Every note here is deleted permanently. This cannot be undone.")
                .setPositiveButton("Delete all", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        store.emptyTrash();
                        fill();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
