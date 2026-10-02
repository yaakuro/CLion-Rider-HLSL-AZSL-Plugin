package com.azsl.completion.shader;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Schedules completion while {@code .shader} files are edited.
 *
 * <p>The platform only auto-pops on identifier characters, and a file type owned by CLion/Rider
 * provides no language specific trigger for JSON structure characters — without this handler
 * the completion after a brace, colon, comma or quote would need a manual invocation. The
 * popup is only scheduled when the caret position resolves to a schema position, so free-form
 * values (draw list names, shader option keys, …) stay quiet.</p>
 */
public final class ShaderTypedHandler extends TypedHandlerDelegate {

    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (Character.isISOControl(c) || !ShaderCompletionContributor.isShaderFile(file)) {
            return Result.CONTINUE;
        }
        String text = file.getText();
        int offset = Math.min(editor.getCaretModel().getOffset(), text.length());
        ShaderPathScanner.Context ctx = ShaderPathScanner.scan(text, offset);

        boolean wanted;
        if (ctx.position() == ShaderPathScanner.Position.KEY) {
            wanted = !ShaderFileSchema.childKeys(ctx.path()).isEmpty();
        } else {
            wanted = ShaderFileSchema.valueOptions(ctx.path(), ctx.valueKey()) != null || isSource(ctx);
        }
        if (wanted) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        }
        return Result.CONTINUE;
    }

    private static boolean isSource(ShaderPathScanner.Context ctx) {
        List<String> path = ctx.path();
        return path.isEmpty()
                && ctx.valueKey() != null
                && "source".equals(ctx.valueKey().toLowerCase(Locale.ROOT));
    }
}
