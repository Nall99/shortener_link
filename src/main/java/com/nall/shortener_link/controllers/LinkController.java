package com.nall.shortener_link.controllers;

import com.nall.shortener_link.dtos.CreateLinkRequest;
import com.nall.shortener_link.dtos.LinkResponse;
import com.nall.shortener_link.models.Link;
import com.nall.shortener_link.services.LinkService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService linkService;
    private final String baseUrl;

    public LinkController(LinkService linkService,
                          @Value("${app.base-url}") String baseUrl) {
        this.linkService = linkService;
        this.baseUrl = baseUrl;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(@Valid @RequestBody CreateLinkRequest createLinkRequest){
        Link link = linkService.create(createLinkRequest.url());
        return LinkResponse.from(link, baseUrl);
    }

    @GetMapping
    public List<LinkResponse> list(){
        return linkService.findAll().stream()
                .map(l -> LinkResponse.from(l, baseUrl))
                .toList();
    }

    @GetMapping("/{shortCode}")
    public LinkResponse get(@PathVariable String shortCode){
        return LinkResponse.from(linkService.findByShortCode(shortCode), baseUrl);
    }

    @DeleteMapping("/{shortCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String shortCode){
        linkService.delete(shortCode);
    }
}
