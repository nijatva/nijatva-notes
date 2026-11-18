package in.nijatva.notes;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Passphrase vault. PBKDF2-HMAC-SHA256 (the platform KDF; Argon2 needs a native
 * library) derives a 256-bit key, AES-256-GCM encrypts each note with a fresh
 * nonce, and the key lives in memory only until the app goes to the background.
 */
public class Vault {

    private static final String PREFS = "vault";
    private static final String K_SALT = "salt";
    private static final String K_CHECK = "check";
    private static final String CHECK_TEXT = "nijatva-vault-check";
    private static final int ITERATIONS = 120000;
    private static final SecureRandom RNG = new SecureRandom();

    private static byte[] key;

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isSet(Context c) {
        return prefs(c).contains(K_SALT);
    }

    public static boolean isUnlocked() {
        return key != null;
    }

    public static byte[] key() {
        return key;
    }

    public static void lock() {
        if (key != null) {
            Arrays.fill(key, (byte) 0);
            key = null;
        }
    }

    public static void setup(Context c, String pass) throws Exception {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        byte[] k = derive(pass, salt);
        prefs(c).edit()
                .putString(K_SALT, b64(salt))
                .putString(K_CHECK, encrypt(k, CHECK_TEXT))
                .apply();
        key = k;
    }

    public static boolean unlock(Context c, String pass) {
        try {
            String saltStr = prefs(c).getString(K_SALT, "");
            String checkStr = prefs(c).getString(K_CHECK, "");
            if (saltStr.isEmpty() || checkStr.isEmpty()) return false;
            byte[] k = derive(pass, Base64.decode(saltStr, Base64.NO_WRAP));
            if (CHECK_TEXT.equals(decrypt(k, checkStr))) {
                key = k;
                return true;
            }
            Arrays.fill(k, (byte) 0);
        } catch (Exception ignored) {
        }
        return false;
    }

    private static byte[] derive(String pass, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pass.toCharArray(), salt, ITERATIONS, 256);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] out = f.generateSecret(spec).getEncoded();
        spec.clearPassword();
        return out;
    }

    public static String encrypt(byte[] k, String plain) throws Exception {
        byte[] iv = new byte[12];
        RNG.nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(plain.getBytes("UTF-8"));
        return b64(iv) + ":" + b64(ct);
    }

    public static String decrypt(byte[] k, String blob) throws Exception {
        int i = blob.indexOf(':');
        if (i < 0) throw new IllegalArgumentException("bad blob");
        byte[] iv = Base64.decode(blob.substring(0, i), Base64.NO_WRAP);
        byte[] ct = Base64.decode(blob.substring(i + 1), Base64.NO_WRAP);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(k, "AES"), new GCMParameterSpec(128, iv));
        return new String(c.doFinal(ct), "UTF-8");
    }

    private static String b64(byte[] b) {
        return Base64.encodeToString(b, Base64.NO_WRAP);
    }
}
