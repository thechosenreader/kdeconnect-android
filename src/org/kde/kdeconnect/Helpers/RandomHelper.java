package org.kde.kdeconnect.Helpers;


import java.security.SecureRandom;

public class RandomHelper {
    public static final SecureRandom secureRandom = new SecureRandom();

    private static final char[] chars   = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                           "abcdefghijklmnopqrstuvwxyz")
                                           .toCharArray();

    private static final char[] symbols = (new String(chars) +
                                          "0123456789")
                                          .toCharArray();


    public static String randomString(int length) {
        char[] buffer = new char[length];
        for (int idx = 0; idx < length; ++idx) {
            buffer[idx] = symbols[secureRandom.nextInt(symbols.length)];
        }
        return new String(buffer);
    }

    public static String randomStringCharOnly(int length) {
        char[] buffer = new char[length];
        for (int idx = 0; idx < length; ++idx) {
            buffer[idx] = chars[secureRandom.nextInt(chars.length)];
        }
        return new String(buffer);
    }

}
