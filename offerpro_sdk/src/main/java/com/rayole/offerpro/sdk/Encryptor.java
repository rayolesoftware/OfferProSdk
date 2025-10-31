package com.rayole.offerpro.sdk;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

final class Encryptor {
    private Encryptor() {}

    // === EXACT Dart equivalent of:
    // String encryptData(Map<String, dynamic> data, String encKey)
    public static String encryptData(Map<String, Object> data, String keyUtf8) throws Exception {
        byte[] key = keyUtf8.getBytes(StandardCharsets.UTF_8);
        if (!(key.length == 16 || key.length == 24 || key.length == 32)) {
            throw new IllegalArgumentException("key must be 16/24/32 bytes (AES-128/192/256)");
        }

        // 1) JSON payload (same as JSON.stringify in JS)
        String json = new JSONObject(data).toString();
        byte[] plain = json.getBytes(StandardCharsets.UTF_8);

        // 2) Random 16-byte IV (CryptoJS.lib.WordArray.random(16))
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        // 3) AES/CBC/PKCS7 (JCE calls it PKCS5Padding but it’s equivalent)
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv)
        );
        byte[] ct = cipher.doFinal(plain);

        // 4) Prepend IV to ciphertext
        byte[] combined = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ct, 0, combined, iv.length, ct.length);

        // 5) Base64url (no padding, no wraps) — CryptoJS.enc.Base64url.stringify
        return Base64.encodeToString(combined, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

}
