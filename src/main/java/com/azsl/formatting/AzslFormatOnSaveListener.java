package com.azsl.formatting;

import com.azsl.AzslFileType;
import com.azsl.AzsliFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Formats AZSL files with clang-format on save when enabled in settings. */
public class AzslFormatOnSaveListener implements FileDocumentManagerListener {

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        AzslClangFormatSettings settings = AzslClangFormatSettings.getInstance();
        if (!settings.isFormatOnSave() || !settings.isEnabled()) return;

        VirtualFile vf = FileDocumentManager.getInstance().getFile(document);
        if (vf == null) return;
        if (!(vf.getFileType() instanceof AzslFileType) && !(vf.getFileType() instanceof AzsliFileType)) {
            return;
        }

        String clangFormat = settings.getResolvedClangFormatPath();
        if (clangFormat == null) return;

        String text = document.getText();
        String formatted = format(clangFormat, vf, text, settings.getFallbackStyle());
        if (formatted != null && !formatted.equals(text)) {
            document.setText(formatted);
        }
    }

    private static String format(String clangFormat, VirtualFile vf, String text, String fallback) {
        List<String> command = new ArrayList<>();
        command.add(clangFormat);
        command.add("-style=file");
        command.add("-fallback-style=" + (fallback == null || fallback.isBlank() ? "LLVM" : fallback));
        command.add("-assume-filename=" + vf.getPath() + ".cpp");
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            File dir = new File(vf.getPath()).getParentFile();
            if (dir != null && dir.isDirectory()) pb.directory(dir);
            pb.redirectErrorStream(false);
            Process p = pb.start();
            try (OutputStream os = p.getOutputStream()) {
                os.write(text.getBytes(StandardCharsets.UTF_8));
            }
            byte[] out = p.getInputStream().readAllBytes();
            p.getErrorStream().readAllBytes();
            int exit = p.waitFor();
            if (exit == 0) return new String(out, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        return null;
    }
}
