package com.azsl;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AzsliFileType extends LanguageFileType {
    public static final AzsliFileType INSTANCE = new AzsliFileType();

    private AzsliFileType() {
        super(AzslLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "AZSL Include";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "AZSL include file (.azsli)";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "AZSL Include";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "azsli";
    }

    @Override
    public Icon getIcon() {
        return AzslIcons.INCLUDE;
    }
}