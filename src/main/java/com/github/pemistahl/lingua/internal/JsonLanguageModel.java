package com.github.pemistahl.lingua.internal;

import java.util.Map;

import com.github.pemistahl.lingua.api.Language;

public class JsonLanguageModel {
    private final Language language;
    private final Map<Fraction, String> ngrams;

    public JsonLanguageModel(Language language, Map<Fraction, String> ngrams) {
        this.language = language;
        this.ngrams = ngrams;
    }
}
