package com.azsl;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AzslFileType extends LanguageFileType {
    public static final AzslFileType INSTANCE = new AzslFileType();

    private AzslFileType() {
        super(AzslLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "AZSL File";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "AZSL shader file (.azsl, .srgi)";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "AZSL File";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "azsl";
    }

    @Override
    public Icon getIcon() {
        return AzslIcons.FILE;
    }
}