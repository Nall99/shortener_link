package com.nall.shortener_link.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "links")
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name =  "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "click_count", nullable = false)
    private long clickCount = 0;

    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
    }
}
