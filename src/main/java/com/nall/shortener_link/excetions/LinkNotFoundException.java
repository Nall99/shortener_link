package com.nall.shortener_link.excetions;

public class LinkNotFoundException extends RuntimeException{
    public LinkNotFoundException(String shortCode){
        super("Link não encontrado: " + shortCode);
    }
}
