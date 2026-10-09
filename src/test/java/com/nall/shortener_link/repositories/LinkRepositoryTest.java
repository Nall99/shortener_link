package com.nall.shortener_link.repositories;

import com.nall.shortener_link.models.Link;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
class LinkRepositoryTest {

    @Autowired
    private LinkRepository repository;

    @Autowired
    private EntityManager em;

    private Link novoLink(String code, String url) {
        Link link = new Link();
        link.setShortCode(code);
        link.setOriginalURL(url);
        return link;
    }

    @Test
    void encontraPorShortCode() {
        repository.save(novoLink("abc1234", "https://www.google.com"));

        assertThat(repository.findByShortCode("abc1234")).isPresent();
        assertThat(repository.findByShortCode("outro12")).isEmpty();
    }

    @Test
    void verificaExistenciaPorShortCode() {
        repository.save(novoLink("abc1234", "https://www.google.com"));

        assertThat(repository.existsByShortCode("abc1234")).isTrue();
        assertThat(repository.existsByShortCode("outro12")).isFalse();
    }

    @Test
    void incrementaCliquesDiretamenteNoBanco() {
        repository.saveAndFlush(novoLink("abc1234", "https://www.google.com"));

        int afetados = repository.incrementClickCount("abc1234");
        repository.incrementClickCount("abc1234");
        em.clear(); // descarta o cache para reler do banco

        assertThat(afetados).isEqualTo(1);
        assertThat(repository.findByShortCode("abc1234").orElseThrow().getClickCount())
                .isEqualTo(2);
    }

    @Test
    void incrementoEmCodigoInexistenteNaoAfetaNada() {
        assertThat(repository.incrementClickCount("naoexis")).isZero();
    }

    @Test
    void naoPermiteShortCodeDuplicado() {
        repository.saveAndFlush(novoLink("abc1234", "https://um.com"));

        assertThatThrownBy(() ->
                repository.saveAndFlush(novoLink("abc1234", "https://dois.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
