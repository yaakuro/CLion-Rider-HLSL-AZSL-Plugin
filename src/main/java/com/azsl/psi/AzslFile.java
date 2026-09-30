package com.azsl.psi;

import com.azsl.AzslLanguage;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class AzslFile extends PsiFileBase {
    public AzslFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, AzslLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        // Use the actual file type from the view provider so that .azsl, .azsli,
        // .srgi and .shader report their own registered file type (and icon).
        return getViewProvider().getFileType();
    }

    @Override
    public String toString() {
        return "AZSL File";
    }
}