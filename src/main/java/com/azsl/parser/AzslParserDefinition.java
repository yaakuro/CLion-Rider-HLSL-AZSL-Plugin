package com.azsl.parser;

import com.azsl.AzslLanguage;
import com.azsl.lexer.AzslLexer;
import com.azsl.psi.AzslFile;
import com.azsl.psi.AzslTokenTypes;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class AzslParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE =
            new IFileElementType(AzslLanguage.INSTANCE);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new AzslLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new AzslParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return AzslTokenTypes.COMMENTS;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return AzslTokenTypes.WHITESPACES;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return AzslTokenTypes.STRINGS;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return com.intellij.psi.impl.source.tree.LeafPsiElement.class
                .isAssignableFrom(node.getClass())
                ? new com.intellij.psi.impl.source.tree.LeafPsiElement(node.getElementType(), node.getText())
                : new com.intellij.psi.impl.source.tree.CompositePsiElement(node.getElementType()) {};
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new AzslFile(viewProvider);
    }
}