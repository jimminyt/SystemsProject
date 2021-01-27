package systems.team040.functions;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * Hasher is a class of static functions used to generate a digest of a password to be stored in the database
 * and to validate a password entered with the one stored on the database
 */
public class Hasher {
    private static final int ITERATIONS = 1000;
    private static final int SALT_LEN = 8;
    private static final int KEY_LEN = 64 * 4;
    private static final String RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA1";

    /**
     * Generates a random string of bytes to be used as a salt in the hashing algorithm, length decided by
     * SALT_LEN variable
     */
    private static byte[] generateSalt() {
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstance(RANDOM_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("This shouldn't happen");
            e.printStackTrace();
            return null;
        }

        byte[] retVal = new byte[SALT_LEN];
        sr.nextBytes(retVal);
        return retVal;
    }

    /**
     * Given a password, this generates the digest that we put into the database, in the form of
     * salt + '$' + hash(salt + password)
     */
    static String generateDigest(char[] password) {
        byte[] salt = generateSalt();
        byte[] hash = hash(password, salt);

        return DatatypeConverter.printHexBinary(salt) + "$" + DatatypeConverter.printHexBinary(hash);
    }

    /**
     * Hashes a given password + salt combo using the PBKDF2 algorithm
     */
    private static byte[] hash(char[] password, byte[] salt) {
        SecretKeyFactory skf;
        try {
            skf = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LEN);

            return skf.generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks that a password patches what is stored on the database
     */
    public static boolean validatePassword(char[] entered, String stored) {
        String[] parts = stored.split("\\$");
        byte[] salt = DatatypeConverter.parseHexBinary(parts[0]);
        byte[] hash = DatatypeConverter.parseHexBinary(parts[1]);


        return Arrays.equals(hash(entered, salt), hash);
    }
}
