package com.azsl;

import com.intellij.lang.Language;

public class AzslLanguage extends Language {
    public static final AzslLanguage INSTANCE = new AzslLanguage();

    private AzslLanguage() {
        super("AZSL", "text/x-azsl");
    }
}