package com.nall.shortener_link.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nall.shortener_link.models.Link;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long>{
    Optional<Link> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortcode);

    @Modifying
    @Query("update Link l set l.clickCount = l.clickCount + 1 where l.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode);

}
