package com.nall.shortener_link.controllers;

import com.nall.shortener_link.services.LinkService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@AllArgsConstructor
public class RedirectController {
    private final LinkService linkService;

    @GetMapping("/{shortCode:[0-9a-zA-Z]{7}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode){
        String target = linkService.resolveAndCount(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(target).toString())
                .build();
    }
}
