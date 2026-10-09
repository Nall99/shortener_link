package com.nall.shortener_link.dtos;

import com.nall.shortener_link.models.Link;

import java.time.Instant;

public record LinkResponse(
        String shortCode,
        String shortURL,
        String originalUrl,
        Instant createdAt,
        long clickCount
) {
    public static LinkResponse from(Link link, String baseUrl){
        return new LinkResponse(
                link.getShortCode(),
                baseUrl + "/" + link.getShortCode(),
                link.getOriginalUrl(),
                link.getCreatedAt(),
                link.getClickCount()
        );
    }
}
