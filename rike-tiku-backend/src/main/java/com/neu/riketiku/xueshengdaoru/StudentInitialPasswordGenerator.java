package com.neu.riketiku.xueshengdaoru;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class StudentInitialPasswordGenerator {
    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] LOWER = "abcdefghjkmnpqrstuvwxyz".toCharArray();
    private static final char[] DIGIT = "23456789".toCharArray();
    private static final char[] ALL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] password = new char[12];
        password[0] = pick(UPPER);
        password[1] = pick(LOWER);
        password[2] = pick(DIGIT);
        for (int index = 3; index < password.length; index++) password[index] = pick(ALL);
        for (int index = password.length - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            char value = password[index]; password[index] = password[swap]; password[swap] = value;
        }
        return new String(password);
    }
    private char pick(char[] values) { return values[random.nextInt(values.length)]; }
}
