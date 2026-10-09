package com.nall.shortener_link.services;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ShortCodeGeneratorTest {
    private final ShortCodeGenerator shortCodeGenerator = new ShortCodeGenerator();

    @Test
    void geraCodigoComSeteCaracteresAlfanumericos(){
        String code = shortCodeGenerator.generate();
        assertThat(code).hasSize(7).matches("[0-9a-zA-Z]{7}");
    }

    @Test
    void geraCodigosDiferentes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(shortCodeGenerator.generate());
        }

        assertThat(codes).hasSize(1000);
    }
}
