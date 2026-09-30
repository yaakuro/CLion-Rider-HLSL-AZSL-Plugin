package com.azsl.intentions;

import com.azsl.AzslFileType;
import com.azsl.AzsliFileType;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/** Inserts a {@code #pragma once} include guard at the top of an AZSL include file. */
public class AzslAddPragmaOnceIntention extends BaseIntentionAction {

    @Override
    public @NotNull String getText() {
        return "Add '#pragma once'";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "AZSL";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file.getFileType() instanceof AzslFileType)
                && !(file.getFileType() instanceof AzsliFileType)) {
            return false;
        }
        Document document = editor.getDocument();
        String text = document.getText();
        // Only offer when a pragma once is not already present near the top.
        String head = text.length() > 400 ? text.substring(0, 400) : text;
        return !head.contains("#pragma once");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        Document document = editor.getDocument();
        document.insertString(0, "#pragma once\n\n");
    }
}
