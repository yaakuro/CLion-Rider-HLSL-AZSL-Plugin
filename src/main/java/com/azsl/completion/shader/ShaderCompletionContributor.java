package com.azsl.completion.shader;

import com.azsl.AzslFileType;
import com.azsl.AzslIcons;
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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Code completion for O3DE {@code .shader} files: render pipeline state keys
 * (DepthStencilState / RasterState / BlendState), their enum values and the rest of the
 * {@code ShaderSourceData} schema.
 *
 * <p>Registered for {@code language="any"} because CLion/Rider own the {@code .shader} file
 * type; everything is decided from the file name and the raw text, no PSI assumptions.</p>
 */
public class ShaderCompletionContributor extends CompletionContributor {

    public ShaderCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(),
                new ShaderCompletionProvider());
    }

    static boolean isShaderFile(PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        String name = virtualFile != null ? virtualFile.getName() : file.getName();
        return name.toLowerCase(Locale.ROOT).endsWith(".shader");
    }

    private static final class ShaderCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      @NotNull ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            PsiFile file = parameters.getOriginalFile();
            if (!isShaderFile(file)) {
                return;
            }
            String text = file.getText();
            int offset = Math.min(parameters.getOffset(), text.length());
            ShaderPathScanner.Context ctx = ShaderPathScanner.scan(text, offset);

            if (ctx.position() == ShaderPathScanner.Position.KEY) {
                addKeys(ctx, result);
            } else {
                addValues(file, ctx, result);
            }
        }

        private void addKeys(ShaderPathScanner.Context ctx, CompletionResultSet result) {
            Set<String> present = new HashSet<>();
            for (String existing : ctx.containerKeys()) {
                present.add(existing.toLowerCase(Locale.ROOT));
            }
            for (ShaderFileSchema.Key key : ShaderFileSchema.childKeys(ctx.path())) {
                if (present.contains(key.name().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                LookupElementBuilder element = LookupElementBuilder.create(key.name())
                        .withIcon(AllIcons.Nodes.Field)
                        .withTypeText(key.type(), true);
                if (!ctx.inString()) {
                    element = element.withInsertHandler(QUOTED_KEY_INSERT);
                }
                result.addElement(element);
            }
        }

        private void addValues(PsiFile file, ShaderPathScanner.Context ctx, CompletionResultSet result) {
            String[] options = ShaderFileSchema.valueOptions(ctx.path(), ctx.valueKey());
            if (options != null) {
                boolean bool = ShaderFileSchema.isBoolOptions(options);
                InsertHandler<LookupElement> handler =
                        ctx.inString() || bool ? null : QUOTED_INSERT;
                for (String option : options) {
                    LookupElementBuilder element = LookupElementBuilder.create(option)
                            .withIcon(bool ? AllIcons.Nodes.Constant : AllIcons.Nodes.Enum);
                    if (handler != null) {
                        element = element.withInsertHandler(handler);
                    }
                    result.addElement(element);
                }
                return;
            }
            if (isSourceMember(ctx)) {
                addAzslFiles(file, ctx, result);
            }
        }

        private boolean isSourceMember(ShaderPathScanner.Context ctx) {
            return ctx.path().isEmpty()
                    && ctx.valueKey() != null
                    && "source".equals(ctx.valueKey().toLowerCase(Locale.ROOT));
        }

        /**
         * Completes the {@code "Source"} member with the {@code .azsl} files of the project,
         * relative to the directory of the {@code .shader} file.
         */
        private void addAzslFiles(PsiFile file, ShaderPathScanner.Context ctx, CompletionResultSet result) {
            VirtualFile shaderFile = file.getVirtualFile();
            VirtualFile shaderDir = shaderFile != null ? shaderFile.getParent() : null;
            Project project = file.getProject();
            Collection<VirtualFile> azslFiles;
            try {
                azslFiles = FileTypeIndex.getFiles(AzslFileType.INSTANCE, GlobalSearchScope.projectScope(project));
            } catch (IndexNotReadyException indexing) {
                return;
            }
            for (VirtualFile azsl : azslFiles) {
                String path = shaderDir != null
                        ? VfsUtilCore.getRelativePath(azsl, shaderDir)
                        : null;
                if (path == null) {
                    path = azsl.getName();
                }
                LookupElementBuilder element = LookupElementBuilder.create(path)
                        .withIcon(AzslIcons.FILE)
                        .withTypeText(".azsl", true);
                if (!ctx.inString()) {
                    element = element.withInsertHandler(QUOTED_INSERT);
                }
                result.addElement(element);
            }
        }
    }

    /**
     * Wraps an inserted key in quotes and appends {@code " : "} so that a bare key typed at
     * {@code { } / , } becomes a complete member. Inside an existing string the quotes are
     * already there, so no handler is attached.
     */
    private static final InsertHandler<LookupElement> QUOTED_KEY_INSERT = (ctx, item) ->
            wrap(ctx, " : ");

    /** Wraps an inserted value in quotes (enum strings, file paths). */
    private static final InsertHandler<LookupElement> QUOTED_INSERT = (ctx, item) -> wrap(ctx, "");

    private static void wrap(InsertionContext ctx, String tailText) {
        Document document = ctx.getDocument();
        int start = ctx.getStartOffset();
        int tail = ctx.getTailOffset();
        document.insertString(tail, "\"" + tailText);
        document.insertString(start, "\"");
        int newTail = tail + tailText.length() + 2;
        ctx.setTailOffset(newTail);
        ctx.getEditor().getCaretModel().moveToOffset(newTail);
    }
}
