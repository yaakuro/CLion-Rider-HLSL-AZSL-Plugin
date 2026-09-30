package com.azsl.navigation;

import com.azsl.psi.AzslTokenTypes;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

public class AzslGotoDeclarationHandler implements GotoDeclarationHandler {
    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                              int offset,
                                                              Editor editor) {
        if (sourceElement == null || sourceElement.getNode() == null) return null;
        IElementType type = sourceElement.getNode().getElementType();
        if (!AzslTokenTypes.IDENTIFIER.equals(type)
                && !AzslTokenTypes.FUNCTION_CALL.equals(type)
                && !AzslTokenTypes.FIELD_ACCESS.equals(type)
                && !AzslTokenTypes.INSTANCE_METHOD_CALL.equals(type)) {
            return null;
        }
        PsiElement target = new AzslSymbolReference(sourceElement).resolve();
        return target != null ? new PsiElement[]{target} : null;
    }
}
