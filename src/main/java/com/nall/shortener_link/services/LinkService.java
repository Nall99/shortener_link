package com.nall.shortener_link.services;

import com.nall.shortener_link.excetions.LinkNotFoundException;
import com.nall.shortener_link.models.Link;
import com.nall.shortener_link.repositories.LinkRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class LinkService {
    private static final int MAX_ATTEMPTS = 5;

    private final LinkRepository linkRepository;
    private final ShortCodeGenerator shortCodeGenerator;


    @Transactional
    public Link create(String originalUrl){
        String code = generateUniqueCode();

        Link link = new Link();
        link.setShortCode(code);
        link.setOriginalURL(originalUrl);
        return linkRepository.save(link);
    }

    @Transactional
    public List<Link> findAll(){
        return linkRepository.findAll();
    }

    @Transactional
    public Link findByShortCode(String shortCode){
        return linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));
    }

    @Transactional
    public void delete(String shortCode){
        Link link = findByShortCode(shortCode);
        linkRepository.delete(link);
    }

    @Transactional
    public String resolveAndCount(String shortCode){
        Link link = findByShortCode(shortCode);
        linkRepository.incrementClickCount(shortCode);
        return link.getOriginalURL();
    }

    private String generateUniqueCode(){
        for (int i = 0; i < MAX_ATTEMPTS; i++){
            String code = shortCodeGenerator.generate();
            if (!linkRepository.existsByShortCode(code)){
                return code;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um short_code único");
    }
}
