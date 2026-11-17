package in.devira.nijatva.notes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private Store store;
    private String filter = "all";
    private String query = "";
    private ListView list;
    private LinearLayout chips;
    private TextView countLabel;
    private TextView emptyLabel;
    private final List<Object> rows = new ArrayList<>();
    private Store.Note pendingExport;
    private int uiRev;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.apply(this);
        uiRev = Theme.rev;
        setContentView(buildUi());
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Theme.rev != uiRev) {
            recreate();
            return;
        }
        reload();
    }

    // ---------------- ui ----------------

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 18), Ui.dp(this, 22), Ui.dp(this, 18), 0);

        // header
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = Ui.text(this, "Notes", 30f, Ui.TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView menu = Ui.text(this, "\u22ee", 26f, Ui.TEXT);
        menu.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 4), 0);
        menu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showMenu(v);
            }
        });
        head.addView(menu);
        col.addView(head);

        countLabel = Ui.text(this, "", 13f, Ui.MUTED);
        countLabel.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        col.addView(countLabel);

        // search
        EditText search = new EditText(this);
        search.setHint("Search notes");
        search.setHintTextColor(Ui.MUTED);
        search.setTextColor(Ui.TEXT);
        search.setTextSize(14f);
        search.setSingleLine(true);
        search.setBackground(Ui.round(this, Ui.CARD, 14));
        search.setPadding(Ui.dp(this, 14), Ui.dp(this, 9), Ui.dp(this, 14), Ui.dp(this, 9));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b2, int c) {
                query = s.toString();
                refresh();
            }

            public void afterTextChanged(Editable s) {
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.bottomMargin = Ui.dp(this, 12);
        col.addView(search, slp);

        // chips
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(chips);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = Ui.dp(this, 8);
        col.addView(hsv, hlp);

        list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setBackgroundColor(Ui.BG);
        list.setAdapter(new Rows());
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Store.Note) open((Store.Note) o);
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Store.Note) {
                    noteMenu((Store.Note) o);
                    return true;
                }
                return false;
            }
        });
        col.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        emptyLabel = Ui.text(this, "", 14f, Ui.MUTED);
        emptyLabel.setGravity(Gravity.CENTER);
        emptyLabel.setVisibility(View.GONE);
        col.addView(emptyLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView fab = Ui.text(this, "+", 30f, Ui.BG);
        fab.setGravity(Gravity.CENTER);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setBackground(Ui.round(this, Ui.ACCENT, 32));
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                newNote();
            }
        });
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(Ui.dp(this, 60), Ui.dp(this, 60));
        flp.gravity = Gravity.BOTTOM | Gravity.END;
        flp.setMargins(0, 0, Ui.dp(this, 20), Ui.dp(this, 24));
        root.addView(fab, flp);

        final int fabMargin = Ui.dp(this, 24);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int bottom = insets.getSystemWindowInsetBottom();
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fab.getLayoutParams();
                lp.bottomMargin = fabMargin + bottom;
                fab.setLayoutParams(lp);
                col.setPadding(col.getPaddingLeft(), col.getPaddingTop(), col.getPaddingRight(), bottom);
                return insets;
            }
        });

        return root;
    }

    // ---------------- data ----------------

    private void reload() {
        store = new Store(this);
        rebuildChips();
        refresh();
    }

    private void rebuildChips() {
        chips.removeAllViews();
        addChip("All notes", "all");
        addChip("Favourites", "fav");
        for (Store.Notebook nb : store.notebooks) {
            if (Store.NB_DEFAULT.equals(nb.id)) continue;
            addChip(nb.name, nb.id);
        }
        addChip("Notebooks", "__notebooks");
    }

    private void addChip(String label, final String key) {
        TextView c = Ui.chip(this, label, key.equals(filter));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(this, 8);
        c.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if ("__notebooks".equals(key)) {
                    startActivity(new Intent(MainActivity.this, NotebooksActivity.class));
                } else {
                    filter = key;
                    rebuildChips();
                    refresh();
                }
            }
        });
        chips.addView(c, lp);
    }

    private void refresh() {
        rows.clear();
        List<Store.Note> all = store.list(filter, query);
        int total = all.size();

        if (!"trash".equals(filter) && query.isEmpty()) {
            List<Store.Note> pinned = new ArrayList<>();
            List<Store.Note> rest = new ArrayList<>();
            for (Store.Note n : all) (n.pinned ? pinned : rest).add(n);
            if (!pinned.isEmpty()) {
                rows.add("Pinned");
                rows.addAll(pinned);
            }
            if (!rest.isEmpty()) {
                if (!pinned.isEmpty()) rows.add("Other");
                rows.addAll(rest);
            }
        } else {
            rows.addAll(all);
        }

        countLabel.setText(total == 1 ? "1 note" : total + " notes");
        emptyLabel.setText(query.isEmpty()
                ? "Nothing here yet. Tap + to write a note."
                : "No note matches \u201c" + query + "\u201d.");
        boolean empty = rows.isEmpty();
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyLabel.setVisibility(empty ? View.VISIBLE : View.GONE);
        ((BaseAdapter) list.getAdapter()).notifyDataSetChanged();
    }

    private void open(Store.Note n) {
        Intent i = new Intent(this, EditActivity.class);
        i.putExtra("note_id", n.id);
        startActivity(i);
    }

    private void newNote() {
        String nb = ("all".equals(filter) || "fav".equals(filter)) ? Store.NB_DEFAULT : filter;
        Store.Note n = store.create(nb);
        store.save(n);
        open(n);
    }

    // ---------------- menus ----------------

    private void showMenu(View anchor) {
        PopupMenu p = new PopupMenu(this, anchor);
        p.getMenu().add(0, 1, 0, "Sort by time edited");
        p.getMenu().add(0, 2, 1, "Sort by time created");
        p.getMenu().add(0, 3, 2, "Sort by title");
        p.getMenu().add(0, 4, 3, "Notebooks");
        p.getMenu().add(0, 5, 4, "Recently deleted");
        p.getMenu().add(0, 7, 5, "Appearance");
        p.getMenu().add(0, 8, 6, "About");
        p.getMenu().add(0, 6, 7, Vault.isUnlocked() ? "Lock vault now"
                : (Vault.isSet(this) ? "Unlock vault" : "Set up vault"));
        p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(android.view.MenuItem item) {
                switch (item.getItemId()) {
                    case 1:
                        store.sort = "edited";
                        store.saveIndex();
                        refresh();
                        return true;
                    case 2:
                        store.sort = "created";
                        store.saveIndex();
                        refresh();
                        return true;
                    case 3:
                        store.sort = "title";
                        store.saveIndex();
                        refresh();
                        return true;
                    case 4:
                        startActivity(new Intent(MainActivity.this, NotebooksActivity.class));
                        return true;
                    case 5:
                        startActivity(new Intent(MainActivity.this, TrashActivity.class));
                        return true;
                    case 7:
                        startActivity(new Intent(MainActivity.this, AppearanceActivity.class));
                        return true;
                    case 8:
                        startActivity(new Intent(MainActivity.this, AboutActivity.class));
                        return true;
                    case 6:
                        vaultMenu();
                        return true;
                }
                return false;
            }
        });
        p.show();
    }

    private void noteMenu(final Store.Note n) {
        final List<String> items = new ArrayList<>();
        items.add(n.pinned ? "Unpin" : "Pin to top");
        items.add(n.fav ? "Remove from favourites" : "Add to favourites");
        items.add(n.locked ? "Remove lock" : "Lock note");
        items.add("Move to notebook");
        items.add("Export as Markdown");
        items.add("Delete");

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(n.title.isEmpty() ? "Untitled note" : n.title)
                .setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        switch (which) {
                            case 0:
                                n.pinned = !n.pinned;
                                store.saveIndex();
                                refresh();
                                break;
                            case 1:
                                n.fav = !n.fav;
                                store.saveIndex();
                                refresh();
                                break;
                            case 2:
                                toggleLock(n);
                                break;
                            case 3:
                                moveTo(n);
                                break;
                            case 4:
                                export(n);
                                break;
                            case 5:
                                store.delete(n);
                                refresh();
                                toast("Moved to Recently deleted");
                                break;
                        }
                    }
                }).show();
    }

    private void toggleLock(final Store.Note n) {
        if (n.locked) {
            if (!Vault.isUnlocked()) {
                unlockDialog("Unlock the vault to change this note", new Runnable() {
                    public void run() {
                        store.unlock(n);
                        reload();
                    }
                });
                return;
            }
            store.unlock(n);
            reload();
            toast("Lock removed");
            return;
        }
        if (!Vault.isSet(this)) {
            setupDialog(new Runnable() {
                public void run() {
                    store.lock(n);
                    reload();
                }
            });
            return;
        }
        if (!Vault.isUnlocked()) {
            unlockDialog("Unlock the vault to lock this note", new Runnable() {
                public void run() {
                    store.lock(n);
                    reload();
                }
            });
            return;
        }
        store.lock(n);
        reload();
        toast("Note locked and encrypted");
    }

    private void moveTo(final Store.Note n) {
        final List<Store.Notebook> nbs = store.notebooks;
        String[] names = new String[nbs.size()];
        for (int i = 0; i < nbs.size(); i++) names[i] = nbs.get(i).name;
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Move to notebook")
                .setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        Store.Notebook target = nbs.get(which);
                        if (target.id.equals(n.notebook)) return;
                        store.deleteForeverFileOnly(n);
                        n.notebook = target.id;
                        store.save(n);
                        reload();
                        toast("Moved to " + target.name);
                    }
                }).show();
    }

    private void export(final Store.Note n) {
        pendingExport = n;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/markdown");
        String name = (n.title.isEmpty() ? "note" : n.title.replaceAll("[^A-Za-z0-9 _-]", "").trim());
        if (name.isEmpty()) name = "note";
        i.putExtra(Intent.EXTRA_TITLE, name + ".md");
        try {
            startActivityForResult(i, 41);
        } catch (Exception e) {
            toast("No file picker available");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 41 && res == RESULT_OK && data != null && data.getData() != null && pendingExport != null) {
            try {
                OutputStream out = getContentResolver().openOutputStream(data.getData());
                String md = "# " + pendingExport.title + "\n\n" + pendingExport.body;
                out.write(md.getBytes("UTF-8"));
                out.close();
                toast("Exported");
            } catch (Exception e) {
                toast("Export failed");
            }
            pendingExport = null;
        }
    }

    // ---------------- vault ----------------

    private void vaultMenu() {
        if (!Vault.isSet(this)) {
            setupDialog(null);
        } else if (Vault.isUnlocked()) {
            Vault.lock();
            reload();
            toast("Vault locked");
        } else {
            unlockDialog("Enter the vault passphrase", null);
        }
    }

    private void setupDialog(final Runnable after) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        final EditText p1 = new EditText(this);
        p1.setHint("Passphrase");
        p1.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final EditText p2 = new EditText(this);
        p2.setHint("Repeat passphrase");
        p2.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(p1);
        box.addView(p2);
        TextView warn = Ui.text(this,
                "There is no recovery. If you forget this passphrase, locked notes cannot be opened by anyone, including us.",
                12f, Ui.MUTED);
        warn.setPadding(0, Ui.dp(this, 10), 0, 0);
        box.addView(warn);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Set up vault")
                .setView(box)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String a = p1.getText().toString(), b = p2.getText().toString();
                        if (a.length() < 6) {
                            toast("Use at least 6 characters");
                            return;
                        }
                        if (!a.equals(b)) {
                            toast("Passphrases do not match");
                            return;
                        }
                        try {
                            Vault.setup(MainActivity.this, a);
                            toast("Vault ready");
                            if (after != null) after.run();
                        } catch (Exception e) {
                            toast("Could not create vault");
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unlockDialog(String title, final Runnable after) {
        final EditText p = new EditText(this);
        p.setHint("Passphrase");
        p.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        box.addView(p);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("Unlock", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        if (Vault.unlock(MainActivity.this, p.getText().toString())) {
                            toast("Vault unlocked");
                            if (after != null) after.run();
                            else reload();
                        } else {
                            toast("Wrong passphrase");
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ---------------- adapter ----------------

    private class Rows extends BaseAdapter {
        public int getCount() {
            return rows.size();
        }

        public Object getItem(int i) {
            return rows.get(i);
        }

        public long getItemId(int i) {
            return i;
        }

        public View getView(int pos, View reuse, ViewGroup parent) {
            Object o = rows.get(pos);
            if (o instanceof String) {
                TextView h = Ui.text(MainActivity.this, (String) o, 13f, Ui.MUTED);
                h.setPadding(Ui.dp(MainActivity.this, 4), Ui.dp(MainActivity.this, 14),
                        Ui.dp(MainActivity.this, 4), Ui.dp(MainActivity.this, 6));
                h.setTypeface(Typeface.DEFAULT_BOLD);
                return h;
            }
            Store.Note n = (Store.Note) o;
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackground(Ui.round(MainActivity.this, Ui.CARD, 14));
            row.setPadding(Ui.dp(MainActivity.this, 14), Ui.dp(MainActivity.this, 12),
                    Ui.dp(MainActivity.this, 14), Ui.dp(MainActivity.this, 12));

            String title = n.locked && !Vault.isUnlocked() ? "\uD83D\uDD12 Locked note"
                    : (n.title.isEmpty() ? "Untitled note" : n.title);
            TextView t = Ui.text(MainActivity.this, title, 16f, Ui.TEXT);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setSingleLine(true);
            row.addView(t);

            String snip = n.locked && !Vault.isUnlocked() ? "Unlock the vault to read this note." : n.snippet();
            if (!snip.isEmpty()) {
                TextView s = Ui.text(MainActivity.this, snip, 13f, Ui.MUTED);
                s.setMaxLines(2);
                s.setPadding(0, Ui.dp(MainActivity.this, 5), 0, 0);
                row.addView(s);
            }

            LinearLayout foot = new LinearLayout(MainActivity.this);
            foot.setOrientation(LinearLayout.HORIZONTAL);
            foot.setPadding(0, Ui.dp(MainActivity.this, 9), 0, 0);
            String meta = Ui.when(n.modified) + "  \u00b7  " + store.notebookName(n.notebook);
            TextView f = Ui.text(MainActivity.this, meta, 11f, Ui.MUTED);
            foot.addView(f, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (n.fav) {
                TextView star = Ui.text(MainActivity.this, "\u2605", 12f, Ui.ACCENT);
                foot.addView(star);
            }
            if (n.pinned) {
                TextView pin = Ui.text(MainActivity.this, "  \u2022", 12f, Ui.ACCENT);
                foot.addView(pin);
            }
            row.addView(foot);

            LinearLayout wrap = new LinearLayout(MainActivity.this);
            wrap.setPadding(0, Ui.dp(MainActivity.this, 5), 0, Ui.dp(MainActivity.this, 5));
            wrap.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return wrap;
        }
    }
}
