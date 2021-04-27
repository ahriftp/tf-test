package com.tecforte.blog.service.dto;

import java.io.Serializable;

import io.micrometer.core.lang.NonNull;

/**
 * A DTO for the {@link com.tecforte.blog.domain.Blog} entity.
 */
public class KeywordsDTO implements Serializable {
    @NonNull
    private String[] keywords;

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    public String[] getKeywords() {
        return this.keywords;
    }
}
