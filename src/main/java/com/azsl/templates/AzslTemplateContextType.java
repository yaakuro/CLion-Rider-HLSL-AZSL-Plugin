package com.azsl.templates;

import com.azsl.AzslLanguage;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/** Template context so AZSL live templates are offered only in AZSL files. */
public class AzslTemplateContextType extends TemplateContextType {

    public AzslTemplateContextType() {
        super("AZSL", "AZSL", null);
    }

    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        return file.getLanguage() == AzslLanguage.INSTANCE
                || file.getLanguage().isKindOf(AzslLanguage.INSTANCE);
    }
}
