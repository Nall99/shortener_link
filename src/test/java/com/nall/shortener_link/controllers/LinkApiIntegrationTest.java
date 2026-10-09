package com.nall.shortener_link.controllers;


import com.jayway.jsonpath.JsonPath;
import com.nall.shortener_link.repositories.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LinkApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LinkRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    /** Cria um link via API e devolve o short_code gerado. */
    private String criarLink(String url) throws Exception {
        String body = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.shortCode");
    }

    // ---------- criação ----------

    @Test
    void criaLinkComSucesso() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://www.google.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value(org.hamcrest.Matchers.hasLength(7)))
                .andExpect(jsonPath("$.shortURL", startsWith("http://localhost:8080/")))
                .andExpect(jsonPath("$.originalUrl").value("https://www.google.com"))
                .andExpect(jsonPath("$.clickCount").value(0));
    }

    @Test
    void rejeitaUrlSemProtocolo() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaUrlComEsquemaPerigoso() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaUrlVazia() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- redirecionamento ----------

    @Test
    void redirecionaParaUrlOriginal() throws Exception {
        String code = criarLink("https://www.google.com");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.google.com"));
    }

    @Test
    void redirecionamentoIncrementaContadorDeCliques() throws Exception {
        String code = criarLink("https://www.google.com");

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        mockMvc.perform(get("/api/links/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(2));
    }

    @Test
    void codigoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/naoexis"))
                .andExpect(status().isNotFound());
    }

    @Test
    void caminhoForaDoPadraoNaoCaiNoRedirecionamento() throws Exception {
        // Só 3 caracteres: não casa com a regex {7} da rota
        mockMvc.perform(get("/abc"))
                .andExpect(status().isNotFound());
    }

    // ---------- gerenciamento ----------

    @Test
    void listaLinks() throws Exception {
        criarLink("https://um.com");
        criarLink("https://dois.com");

        mockMvc.perform(get("/api/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void consultaLinkPorCodigo() throws Exception {
        String code = criarLink("https://www.google.com");

        mockMvc.perform(get("/api/links/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(code))
                .andExpect(jsonPath("$.originalUrl").value("https://www.google.com"));
    }

    @Test
    void consultaDeCodigoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/links/naoexis"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletaLinkEDepoisNaoEncontra() throws Exception {
        String code = criarLink("https://www.google.com");

        mockMvc.perform(delete("/api/links/" + code))
                .andExpect(status().isNoContent());

        assertThat(repository.existsByShortCode(code)).isFalse();

        mockMvc.perform(get("/" + code))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletarCodigoInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/links/naoexis"))
                .andExpect(status().isNotFound());
    }
}