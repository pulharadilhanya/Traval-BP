package controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles password hashing and verification.
 *
 * Passwords are never stored in plain text. Each password is hashed with
 * SHA-256 combined with a random salt (a random value mixed in before
 * hashing so two users with the same password don't end up with the same
 * stored hash, and so precomputed "rainbow table" attacks don't work).
 *
 * Stored format in the database: "<saltBase64>:<hashBase64>"
 * e.g. "k3F2b1Q9...==:9fJ0...=="
 */
public class PasswordUtil {

    private static final int SALT_LENGTH_BYTES = 16;

    /** Generates a new hash (with a fresh random salt) for a plain-text password. */
    public static String hashPassword(String plainPassword) {
        byte[] salt = generateSalt();
        byte[] hash = sha256(plainPassword, salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Checks a plain-text password attempt against a stored "salt:hash" value.
     * Returns true if it matches.
     */
    public static boolean verifyPassword(String plainPassword, String storedValue) {
        if (storedValue == null || !storedValue.contains(":")) {
            // Not in our salt:hash format — e.g. an old plain-text password
            // that hasn't been migrated yet.
            return false;
        }
        String[] parts = storedValue.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

        byte[] actualHash = sha256(plainPassword, salt);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    /** True if the stored value is already in our "salt:hash" format. */
    public static boolean isHashed(String storedValue) {
        return storedValue != null && storedValue.contains(":");
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] sha256(String plainPassword, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(plainPassword.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            // SHA-256 and UTF-8 are guaranteed to exist on every JVM, so this
            // should never actually happen.
            throw new RuntimeException(e);
        }
    }
}
