package com.nall.shortener_link.services;

import com.nall.shortener_link.excetions.LinkNotFoundException;
import com.nall.shortener_link.models.Link;
import com.nall.shortener_link.repositories.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository repository;

    @Mock
    private ShortCodeGenerator generator;

    @InjectMocks
    private LinkService service;

    @Test
    void criaLinkComCodigoGerado() {
        when(generator.generate()).thenReturn("abc1234");
        when(repository.existsByShortCode("abc1234")).thenReturn(false);
        when(repository.save(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        Link link = service.create("https://www.google.com");

        assertThat(link.getShortCode()).isEqualTo("abc1234");
        assertThat(link.getOriginalURL()).isEqualTo("https://www.google.com");
        verify(repository).save(any(Link.class));
    }

    @Test
    void tentaDeNovoQuandoHaColisaoDeCodigo() {
        when(generator.generate()).thenReturn("colide1", "unico22");
        when(repository.existsByShortCode("colide1")).thenReturn(true);
        when(repository.existsByShortCode("unico22")).thenReturn(false);
        when(repository.save(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        Link link = service.create("https://exemplo.com");

        assertThat(link.getShortCode()).isEqualTo("unico22");
        verify(generator, times(2)).generate();
    }

    @Test
    void falhaDepoisDeEsgotarTentativas() {
        when(generator.generate()).thenReturn("colide1");
        when(repository.existsByShortCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.create("https://exemplo.com"))
                .isInstanceOf(IllegalStateException.class);

        verify(generator, times(5)).generate();
        verify(repository, never()).save(any());
    }

    @Test
    void buscaPorCodigoInexistenteLancaExcecao() {
        when(repository.findByShortCode("naoexis")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByShortCode("naoexis"))
                .isInstanceOf(LinkNotFoundException.class)
                .hasMessageContaining("naoexis");
    }

    @Test
    void resolveRetornaUrlOriginalEIncrementaCliques() {
        Link link = new Link();
        link.setShortCode("abc1234");
        link.setOriginalURL("https://www.google.com");
        when(repository.findByShortCode("abc1234")).thenReturn(Optional.of(link));

        String url = service.resolveAndCount("abc1234");

        assertThat(url).isEqualTo("https://www.google.com");
        verify(repository).incrementClickCount("abc1234");
    }

    @Test
    void resolveNaoIncrementaQuandoLinkNaoExiste() {
        when(repository.findByShortCode("naoexis")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAndCount("naoexis"))
                .isInstanceOf(LinkNotFoundException.class);

        verify(repository, never()).incrementClickCount(anyString());
    }

    @Test
    void deletaLinkExistente() {
        Link link = new Link();
        link.setShortCode("abc1234");
        when(repository.findByShortCode("abc1234")).thenReturn(Optional.of(link));

        service.delete("abc1234");

        verify(repository).delete(link);
    }
}
