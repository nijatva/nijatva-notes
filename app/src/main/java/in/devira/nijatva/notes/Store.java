package in.devira.nijatva.notes;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * File-first storage: every note is a Markdown file in app-private storage, and
 * index.json is a rebuildable index over that tree. Losing the index costs the
 * user their ordering and flags, never their words.
 */
public class Store {

    public static final String NB_DEFAULT = "default";
    public static final String NB_QUICK = "quick";
    private static final long TRASH_MS = 30L * 24 * 60 * 60 * 1000;

    public static class Notebook {
        public String id;
        public String name;
        public boolean system;

        public Notebook(String id, String name, boolean system) {
            this.id = id;
            this.name = name;
            this.system = system;
        }
    }

    public static class Note {
        public String id = "";
        public String notebook = NB_DEFAULT;
        public String title = "";
        public String body = "";
        public boolean pinned, fav, locked, deleted;
        public long created, modified, deletedAt;

        public String snippet() {
            String b = body.replaceAll("(?m)^#+\\s*", "")
                    .replaceAll("(?m)^\\s*[-*>]\\s*(\\[[ xX]\\]\\s*)?", "")
                    .replaceAll("\\*\\*|==|`", "")
                    .replace("\n", " ")
                    .trim();
            if (b.length() > 140) b = b.substring(0, 140) + "\u2026";
            return b;
        }
    }

    private final Context ctx;
    private final File notesDir, indexFile;
    public final List<Notebook> notebooks = new ArrayList<>();
    public final List<Note> notes = new ArrayList<>();
    public String sort = "edited";

    public Store(Context c) {
        ctx = c.getApplicationContext();
        File base = new File(ctx.getFilesDir(), "notes");
        notesDir = new File(base, "notes");
        indexFile = new File(base, "index.json");
        notesDir.mkdirs();
        load();
    }

    // ---------------- notebooks ----------------

    public Notebook notebook(String id) {
        for (Notebook n : notebooks) if (n.id.equals(id)) return n;
        return null;
    }

    public String notebookName(String id) {
        Notebook n = notebook(id);
        return n == null ? "Notes" : n.name;
    }

    public Notebook addNotebook(String name) {
        Notebook n = new Notebook("nb-" + UUID.randomUUID().toString().substring(0, 8), name, false);
        notebooks.add(n);
        saveIndex();
        return n;
    }

    public void renameNotebook(Notebook n, String name) {
        n.name = name;
        saveIndex();
    }

    public void deleteNotebook(Notebook n) {
        if (n.system) return;
        for (Note note : notes) if (note.notebook.equals(n.id)) note.notebook = NB_DEFAULT;
        notebooks.remove(n);
        saveIndex();
    }

    public int count(String notebookId) {
        int c = 0;
        for (Note n : notes) if (!n.deleted && n.notebook.equals(notebookId)) c++;
        return c;
    }

    public int countFav() {
        int c = 0;
        for (Note n : notes) if (!n.deleted && n.fav) c++;
        return c;
    }

    public int countTrash() {
        int c = 0;
        for (Note n : notes) if (n.deleted) c++;
        return c;
    }

    // ---------------- notes ----------------

    public Note create(String notebookId) {
        Note n = new Note();
        n.id = UUID.randomUUID().toString();
        n.notebook = notebookId;
        n.created = n.modified = System.currentTimeMillis();
        notes.add(n);
        return n;
    }

    public Note get(String id) {
        for (Note n : notes) if (n.id.equals(id)) return n;
        return null;
    }

    private File fileFor(Note n) {
        File dir = new File(notesDir, n.notebook);
        dir.mkdirs();
        return new File(dir, n.id + (n.locked ? ".enc" : ".md"));
    }

    public void save(Note n) {
        try {
            n.modified = System.currentTimeMillis();
            if (n.locked) {
                if (Vault.isUnlocked()) {
                    write(fileFor(n), Vault.encrypt(Vault.key(), n.title + "\n\u0000\n" + n.body));
                    deleteQuietly(new File(new File(notesDir, n.notebook), n.id + ".md"));
                }
            } else {
                write(fileFor(n), "# " + n.title + "\n\n" + n.body);
                deleteQuietly(new File(new File(notesDir, n.notebook), n.id + ".enc"));
            }
            saveIndex();
        } catch (Exception ignored) {
        }
    }

    /** Marks a note locked and encrypts it at rest. Requires an unlocked vault. */
    public boolean lock(Note n) {
        if (!Vault.isUnlocked()) return false;
        n.locked = true;
        save(n);
        return true;
    }

    /** Decrypts a locked note back to plain Markdown. Requires an unlocked vault. */
    public boolean unlock(Note n) {
        if (!Vault.isUnlocked()) return false;
        n.locked = false;
        save(n);
        return true;
    }

    public void delete(Note n) {
        n.deleted = true;
        n.deletedAt = System.currentTimeMillis();
        saveIndex();
    }

    public void restore(Note n) {
        n.deleted = false;
        n.deletedAt = 0;
        saveIndex();
    }

    public void deleteForever(Note n) {
        deleteQuietly(fileFor(n));
        deleteQuietly(new File(new File(notesDir, n.notebook), n.id + (n.locked ? ".md" : ".enc")));
        notes.remove(n);
        saveIndex();
    }

    /** Removes the note's file only, leaving the note in the index - used when a
     *  note moves between notebooks and its old path must go. */
    public void deleteForeverFileOnly(Note n) {
        deleteQuietly(new File(new File(notesDir, n.notebook), n.id + ".md"));
        deleteQuietly(new File(new File(notesDir, n.notebook), n.id + ".enc"));
    }

    public void emptyTrash() {
        List<Note> gone = new ArrayList<>();
        for (Note n : notes) if (n.deleted) gone.add(n);
        for (Note n : gone) deleteForever(n);
    }

    public void purge() {
        long now = System.currentTimeMillis();
        List<Note> gone = new ArrayList<>();
        for (Note n : notes) if (n.deleted && n.deletedAt > 0 && now - n.deletedAt > TRASH_MS) gone.add(n);
        for (Note n : gone) deleteForever(n);
    }

    /** Visible notes for a filter ("all", "fav", "trash" or a notebook id) and a query. */
    public List<Note> list(String filter, String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Note> out = new ArrayList<>();
        for (Note n : notes) {
            boolean ok;
            if ("trash".equals(filter)) ok = n.deleted;
            else if (n.deleted) ok = false;
            else if ("all".equals(filter)) ok = true;
            else if ("fav".equals(filter)) ok = n.fav;
            else ok = n.notebook.equals(filter);
            if (!ok) continue;
            if (!q.isEmpty()) {
                String hay = (n.title + "\n" + n.body).toLowerCase();
                if (!hay.contains(q)) continue;
            }
            out.add(n);
        }
        final String key = sort;
        Collections.sort(out, new Comparator<Note>() {
            @Override
            public int compare(Note a, Note b) {
                if ("trash".equals(filter)) return Long.compare(b.deletedAt, a.deletedAt);
                if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
                if ("created".equals(key)) return Long.compare(b.created, a.created);
                if ("title".equals(key)) return a.title.compareToIgnoreCase(b.title);
                return Long.compare(b.modified, a.modified);
            }
        });
        return out;
    }

    // ---------------- persistence ----------------

    public void load() {
        notebooks.clear();
        notes.clear();
        if (indexFile.exists()) {
            try {
                JSONObject o = new JSONObject(read(indexFile));
                sort = o.optString("sort", "edited");
                JSONArray nbs = o.optJSONArray("notebooks");
                if (nbs != null) {
                    for (int i = 0; i < nbs.length(); i++) {
                        JSONObject j = nbs.getJSONObject(i);
                        notebooks.add(new Notebook(j.getString("id"), j.getString("name"), j.optBoolean("system")));
                    }
                }
                JSONArray ns = o.optJSONArray("notes");
                if (ns != null) {
                    for (int i = 0; i < ns.length(); i++) {
                        JSONObject j = ns.getJSONObject(i);
                        Note n = new Note();
                        n.id = j.getString("id");
                        n.notebook = j.optString("nb", NB_DEFAULT);
                        n.title = j.optString("title", "");
                        n.pinned = j.optBoolean("pin");
                        n.fav = j.optBoolean("fav");
                        n.locked = j.optBoolean("locked");
                        n.deleted = j.optBoolean("deleted");
                        n.created = j.optLong("created");
                        n.modified = j.optLong("modified");
                        n.deletedAt = j.optLong("deletedAt");
                        n.body = readBody(n);
                        notes.add(n);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (notebooks.isEmpty()) seed();
        purge();
    }

    private String readBody(Note n) {
        try {
            File f = new File(new File(notesDir, n.notebook), n.id + (n.locked ? ".enc" : ".md"));
            if (!f.exists()) return "";
            String raw = read(f);
            if (n.locked) {
                if (!Vault.isUnlocked()) return "";
                String plain = Vault.decrypt(Vault.key(), raw);
                int sep = plain.indexOf("\n\u0000\n");
                if (sep >= 0) {
                    n.title = plain.substring(0, sep);
                    return plain.substring(sep + 3);
                }
                return plain;
            }
            if (raw.startsWith("# ")) {
                int nl = raw.indexOf('\n');
                if (nl > 0) return raw.substring(nl).replaceFirst("^\\s*\\n", "");
            }
            return raw;
        } catch (Exception e) {
            return "";
        }
    }

    public void saveIndex() {
        try {
            JSONObject o = new JSONObject();
            o.put("sort", sort);
            JSONArray nbs = new JSONArray();
            for (Notebook n : notebooks) {
                JSONObject j = new JSONObject();
                j.put("id", n.id);
                j.put("name", n.name);
                j.put("system", n.system);
                nbs.put(j);
            }
            o.put("notebooks", nbs);
            JSONArray ns = new JSONArray();
            for (Note n : notes) {
                JSONObject j = new JSONObject();
                j.put("id", n.id);
                j.put("nb", n.notebook);
                j.put("title", n.title);
                j.put("pin", n.pinned);
                j.put("fav", n.fav);
                j.put("locked", n.locked);
                j.put("deleted", n.deleted);
                j.put("created", n.created);
                j.put("modified", n.modified);
                j.put("deletedAt", n.deletedAt);
                ns.put(j);
            }
            o.put("notes", ns);
            write(indexFile, o.toString());
        } catch (Exception ignored) {
        }
    }

    private void seed() {
        notebooks.add(new Notebook(NB_DEFAULT, "Default notebook", true));
        notebooks.add(new Notebook(NB_QUICK, "Quick note", true));
        Note n = create(NB_DEFAULT);
        n.title = "Welcome to Nijatva Notes";
        n.body = "Notes are plain Markdown files on this device. Nothing is uploaded, "
                + "there is no account, and the app has no network permission at all.\n\n"
                + "## What works here\n\n"
                + "- **Bold**, *italic*, `code` and ==highlight==\n"
                + "- Lists, quotes and checklists\n"
                + "- [ ] a checklist item\n"
                + "- [x] a finished one\n\n"
                + "> Tap Done to keep a note. Everything is saved automatically.\n\n"
                + "Long-press a note in the list to pin, favourite, lock, move or delete it.";
        n.pinned = true;
        save(n);
    }

    // ---------------- io ----------------

    private static void write(File f, String s) throws Exception {
        f.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(f);
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.close();
    }

    private static String read(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) > 0) bo.write(buf, 0, r);
        in.close();
        return new String(bo.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void deleteQuietly(File f) {
        try {
            if (f != null && f.exists()) f.delete();
        } catch (Exception ignored) {
        }
    }
}
