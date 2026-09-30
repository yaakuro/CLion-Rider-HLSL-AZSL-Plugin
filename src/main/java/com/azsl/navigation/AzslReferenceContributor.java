package com.azsl.navigation;

import com.azsl.AzslLanguage;
import com.azsl.psi.AzslTokenTypes;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class AzslReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement().withLanguage(AzslLanguage.INSTANCE),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                           @NotNull ProcessingContext context) {
                        if (element.getNode() == null) return PsiReference.EMPTY_ARRAY;
                        var type = element.getNode().getElementType();
                        if (!type.equals(AzslTokenTypes.IDENTIFIER)
                                && !type.equals(AzslTokenTypes.FUNCTION_CALL)
                                && !type.equals(AzslTokenTypes.FIELD_ACCESS)
                                && !type.equals(AzslTokenTypes.INSTANCE_METHOD_CALL)) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        return new PsiReference[]{new AzslSymbolReference(element)};
                    }
                }
        );
    }
}
