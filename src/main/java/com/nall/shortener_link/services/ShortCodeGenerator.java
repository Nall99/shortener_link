package com.nall.shortener_link.services;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    public String generate(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LENGTH; i++){
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
