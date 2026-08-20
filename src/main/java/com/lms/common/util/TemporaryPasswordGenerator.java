package com.lms.common.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the one-time password issued when an administrator creates an
 * account.
 *
 * <p>Satisfies the same policy as a user-chosen password, so the account is not
 * weaker while the temporary credential is live. Visually ambiguous characters
 * (0/O, 1/l/I) are excluded because a person may have to retype this from an
 * email into a login form.
 */
public final class TemporaryPasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%*?-";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private static final int LENGTH = 14;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        List<Character> characters = new ArrayList<>(LENGTH);

        // One from each class first, so the result always meets the policy.
        characters.add(pick(UPPER));
        characters.add(pick(LOWER));
        characters.add(pick(DIGITS));
        characters.add(pick(SYMBOLS));

        while (characters.size() < LENGTH) {
            characters.add(pick(ALL));
        }

        // Without this the first four positions would be predictable by class.
        shuffle(characters);

        StringBuilder password = new StringBuilder(LENGTH);
        characters.forEach(password::append);
        return password.toString();
    }

    private static char pick(String alphabet) {
        return alphabet.charAt(RANDOM.nextInt(alphabet.length()));
    }

    private static void shuffle(List<Character> characters) {
        for (int i = characters.size() - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            Character swap = characters.get(i);
            characters.set(i, characters.get(j));
            characters.set(j, swap);
        }
    }
}
