package com.onlinediscussion.core;

import java.security.SecureRandom;

/**
 * Generates the passwords that the tool hands out to new student accounts.
 *
 * <p>The generated password is eight characters long and mixes upper case letters, lower case
 * letters, digits and special characters.
 */
public final class PasswordGenerator {

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIALS = "!@#$%^&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    /** Returns a fresh random password. */
    public static String generate() {
        char[] characters = new char[8];
        characters[0] = pick(LETTERS);
        characters[1] = pick(LETTERS);
        characters[2] = pick(LETTERS);
        characters[3] = pick(LETTERS);
        characters[4] = pick(DIGITS);
        characters[5] = pick(DIGITS);
        characters[6] = pick(SPECIALS);
        characters[7] = pick(SPECIALS);
        for (int i = characters.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char swap = characters[i];
            characters[i] = characters[j];
            characters[j] = swap;
        }
        return new String(characters);
    }

    private static char pick(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}
