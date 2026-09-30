package com.azsl.completion;

import com.azsl.AzslLanguage;
import com.azsl.lexer.AzslLexer;
import com.azsl.psi.AzslTokenTypes;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.lang.ASTNode;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AzslCompletionContributor extends CompletionContributor {

    public AzslCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(AzslLanguage.INSTANCE),
                new AzslCompletionProvider());
    }

    private static class AzslCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      @NotNull ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            PsiFile file = parameters.getOriginalFile();
            if (file.getLanguage() != AzslLanguage.INSTANCE) return;

            PsiElement position = parameters.getPosition();
            boolean afterColon = isAfterColon(position);

            // Static vocabulary
            for (String kw : AzslLexer.getKeywords()) {
                result.addElement(LookupElementBuilder.create(kw)
                        .withBoldness(true)
                        .withIcon(AllIcons.Nodes.Favorite));
            }
            for (String t : AzslLexer.getTypeKeywords()) {
                result.addElement(LookupElementBuilder.create(t)
                        .withIcon(AllIcons.Nodes.Class));
            }
            for (String fn : AzslLexer.getBuiltinFunctions()) {
                result.addElement(LookupElementBuilder.create(fn)
                        .withIcon(AllIcons.Nodes.Method)
                        .withTailText("()", true)
                        .withInsertHandler(PAREN_INSERT));
            }
            if (afterColon) {
                for (String s : AzslLexer.getSemantics()) {
                    result.addElement(LookupElementBuilder.create(s)
                            .withIcon(AllIcons.Nodes.Annotationtype));
                }
            }

            // Local identifiers from current file
            Set<String> locals = collectLocalIdentifiers(file);
            String currentWord = currentWordAt(position);
            for (String name : locals) {
                if (name.equals(currentWord)) continue;
                result.addElement(LookupElementBuilder.create(name)
                        .withIcon(AllIcons.Nodes.Variable));
            }

            // Identifiers reachable through #include directives
            AzslIncludeIndex index = file.getProject().getService(AzslIncludeIndex.class);
            if (index != null && file.getVirtualFile() != null) {
                Set<String> included = index.collectTransitiveIdentifiers(file.getVirtualFile());
                for (String name : included) {
                    if (name.equals(currentWord) || locals.contains(name)) continue;
                    result.addElement(LookupElementBuilder.create(name)
                            .withIcon(AllIcons.Nodes.Include)
                            .withTypeText("included", true));
                }
            }
        }
    }

    private static final InsertHandler<LookupElement> PAREN_INSERT = (InsertionContext ctx, LookupElement item) -> {
        int offset = ctx.getTailOffset();
        ctx.getDocument().insertString(offset, "()");
        ctx.getEditor().getCaretModel().moveToOffset(offset + 1);
    };

    private static Set<String> collectLocalIdentifiers(PsiFile file) {
        Set<String> names = new HashSet<>();
        ASTNode node = file.getNode().getFirstChildNode();
        while (node != null) {
            IElementType type = node.getElementType();
            if (type == AzslTokenTypes.IDENTIFIER
                    || type == AzslTokenTypes.STRUCT_NAME
                    || type == AzslTokenTypes.FUNCTION_CALL) {
                String text = node.getText();
                if (!text.isEmpty() && !text.contains("IntellijIdeaRulezzz")) {
                    names.add(text);
                }
            }
            node = node.getTreeNext();
        }
        return names;
    }

    private static String currentWordAt(PsiElement position) {
        String text = position.getText();
        if (text == null) return "";
        int idx = text.indexOf("IntellijIdeaRulezzz");
        return idx >= 0 ? text.substring(0, idx) : text;
    }

    private static boolean isAfterColon(PsiElement position) {
        PsiElement prev = position.getPrevSibling();
        while (prev != null && prev.getNode().getElementType() == com.intellij.psi.TokenType.WHITE_SPACE) {
            prev = prev.getPrevSibling();
        }
        return prev != null && prev.getNode().getElementType() == AzslTokenTypes.COLON;
    }
}
