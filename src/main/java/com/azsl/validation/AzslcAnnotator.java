package com.azsl.validation;

import com.azsl.AzslFileType;
import com.azsl.AzsliFileType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AzslcAnnotator extends ExternalAnnotator<AzslcAnnotator.CollectedInfo, List<AzslcAnnotator.AzslcDiagnostic>> {

    private static final Logger LOG = Logger.getInstance(AzslcAnnotator.class);

    // azslc diagnostic format: <file>(<line>,<col>) : <Kind text> #<num>: <message>
    // e.g.  foo.azsl(3,23) : Semantic error #11: rootconstant or option qualifier ...
    //       foo.azsl(1,1) : syntax error #1: extraneous input ...
    private static final Pattern DIAG_PATTERN = Pattern.compile(
            "^(.+?)\\((\\d+),(\\d+)\\)\\s*:\\s*([A-Za-z][A-Za-z ]*?)\\s*(?:#\\d+)?\\s*:\\s*(.*)$"
    );

    public static class CollectedInfo {
        final String fileContent;
        final String filePath;
        final String fileName;
        final PsiFile psiFile;
        final java.util.List<String> includeRoots;

        CollectedInfo(String fileContent, String filePath, String fileName, PsiFile psiFile,
                      java.util.List<String> includeRoots) {
            this.fileContent = fileContent;
            this.filePath = filePath;
            this.fileName = fileName;
            this.psiFile = psiFile;
            this.includeRoots = includeRoots;
        }
    }

    public static class AzslcDiagnostic {
        final int line;       // 1-based, 0 = unknown
        final int column;     // 1-based, 0 = unknown
        final String severity; // "error", "warning", "note", "info"
        final String message;

        AzslcDiagnostic(int line, int column, String severity, String message) {
            this.line = line;
            this.column = column;
            this.severity = severity;
            this.message = message;
        }
    }

    @Override
    public @Nullable CollectedInfo collectInformation(@NotNull PsiFile file) {
        AzslcSettings settings = AzslcSettings.getInstance();
        if (!settings.isEnableValidation()) return null;

        VirtualFile vFile = file.getVirtualFile();
        if (vFile == null) return null;
        if (!(file.getFileType() instanceof AzslFileType) && !(file.getFileType() instanceof AzsliFileType)) return null;

        Document doc = FileDocumentManager.getInstance().getDocument(vFile);
        if (doc == null) return null;

        // Skip validation on every keystroke if "validate on save only" is enabled
        // This is handled by the external annotator framework's debouncing
        // but we can check here for the setting
        if (settings.isValidateOnSaveOnly()) {
            // The framework will call this on save anyway; we can return null to skip
            // intermediate runs, but ExternalAnnotator doesn't easily distinguish.
            // We'll handle this in doAnnotate by checking the setting.
        }

        return new CollectedInfo(doc.getText(), vFile.getPath(), vFile.getName(), file,
                resolveIncludeRoots(file, vFile));
    }

    /** Resolve O3DE include roots (settings + auto-detect) while we still hold read access. */
    private static java.util.List<String> resolveIncludeRoots(PsiFile file, VirtualFile vFile) {
        java.util.List<String> roots = new ArrayList<>();
        try {
            com.azsl.project.AzslProjectPaths paths =
                    com.azsl.project.AzslProjectPaths.getInstance(file.getProject());
            if (paths != null) {
                for (VirtualFile root : paths.getIncludeRoots(vFile)) {
                    roots.add(root.getPath());
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not resolve O3DE include roots: " + e.getMessage());
        }
        return roots;
    }

    @Override
    public @Nullable List<AzslcDiagnostic> doAnnotate(CollectedInfo info) {
        if (info == null) return null;

        // Clear line mappings for new file
        lineMappings.clear();

        AzslcSettings settings = AzslcSettings.getInstance();
        if (settings.isValidateOnSaveOnly()) {
            // In on-save-only mode, we still run but the framework debounces.
            // To truly implement on-save-only, we'd need a different approach.
            // For now, we run but could add a check for document modification stamp.
        }

        String azslcPath = settings.getResolvedAzslcPath();
        String cppPath = settings.getResolvedCppPath();

        List<AzslcDiagnostic> diagnostics = new ArrayList<>();

        if (azslcPath == null || azslcPath.isBlank()) {
            diagnostics.add(new AzslcDiagnostic(0, 0, "info",
                    "azslc not found. Configure path in Settings → Tools → AZSL / azslc, " +
                    "or ensure ~/.o3de/3rdParty/packages/azslc-*/azslc/bin/Release/azslc exists."));
            return diagnostics;
        }

        if (!new File(azslcPath).isFile() && !isInPath(azslcPath)) {
            diagnostics.add(new AzslcDiagnostic(0, 0, "info",
                    "azslc executable not found at: " + azslcPath +
                    ". Update path in Settings → Tools → AZSL / azslc."));
            return diagnostics;
        }

        if (cppPath == null || cppPath.isBlank()) {
            diagnostics.add(new AzslcDiagnostic(0, 0, "info",
                    "cpp (C preprocessor) not found. On Windows, install MSYS2/MinGW (pacman -S mingw-w64-x86_64-cpp). " +
                    "On Linux/macOS, install build-essential or Xcode Command Line Tools."));
            return diagnostics;
        }

        // Build include paths for cpp
        List<String> includePaths = info.includeRoots.isEmpty()
                ? buildIncludePaths(settings, info.filePath)
                : info.includeRoots;
        if (includePaths.isEmpty()) {
            diagnostics.add(new AzslcDiagnostic(0, 0, "warning",
                    "No include paths configured. azslc requires O3DE engine path for ShaderLib includes. " +
                    "Configure in Settings → Tools → AZSL / azslc."));
        }

        Path tempDir = null;
        Path preprocessedFile = null;
        try {
            tempDir = Files.createTempDirectory("azslc_validate_");

            // Step 1: Preprocess with cpp
            preprocessedFile = tempDir.resolve("preprocessed.azsl");
            preprocessWithCpp(cppPath, info.filePath, preprocessedFile, includePaths);

            // Step 2: Run azslc on preprocessed file
            runAzslc(azslcPath, preprocessedFile, info.filePath, settings.getValidationMode(), diagnostics);

            return diagnostics;

        } catch (Exception e) {
            LOG.warn("azslc validation failed: " + e.getMessage(), e);
            diagnostics.add(new AzslcDiagnostic(0, 0, "info",
                    "azslc execution failed: " + e.getMessage()));
            return diagnostics;
        } finally {
            // Cleanup temp files
            try {
                if (preprocessedFile != null) Files.deleteIfExists(preprocessedFile);
                if (tempDir != null) Files.deleteIfExists(tempDir);
            } catch (Exception ignored) {}
        }
    }

    private List<String> buildIncludePaths(AzslcSettings settings, String filePath) {
        List<String> includes = new ArrayList<>();

        // 1. File's directory (for local includes)
        File file = new File(filePath);
        String fileDir = file.getParent();
        if (fileDir != null) {
            includes.add(fileDir);
        }

        // 2. O3DE Engine ShaderLib paths
        String enginePath = settings.getO3deEnginePath();
        if (enginePath != null && !enginePath.isBlank()) {
            File engineDir = new File(enginePath);
            if (engineDir.isDirectory()) {
                // Repo root for engine-prefixed includes like
                // <Atom/Feature/Common/Assets/ShaderResourceGroups/...>
                addIfExists(includes, new File(engineDir, "Gems"));
                // Standard O3DE ShaderLib locations
                addIfExists(includes, new File(engineDir, "Gems/Atom/Feature/Common/Assets/ShaderLib"));
                addIfExists(includes, new File(engineDir, "Gems/Atom/RPI/Assets/ShaderLib"));
                addIfExists(includes, new File(engineDir, "Gems/Atom/Feature/Common/Assets/ShaderResourceGroups"));
            }
        }

        // 3. Project ShaderLib
        String projectPath = settings.getO3deProjectPath();
        if (projectPath != null && !projectPath.isBlank()) {
            File projectDir = new File(projectPath);
            if (projectDir.isDirectory()) {
                addIfExists(includes, new File(projectDir, "ShaderLib"));
                // Also check for project-specific gems
                addIfExists(includes, projectDir);
            }
        }

        // 4. Additional user-configured paths
        for (String p : settings.getAdditionalIncludePaths()) {
            File f = new File(p);
            if (f.isDirectory()) {
                includes.add(p);
            }
        }

        return includes;
    }

    private void addIfExists(List<String> list, File dir) {
        if (dir.isDirectory()) {
            list.add(dir.getAbsolutePath());
        }
    }

    // Maps a line in the *stripped* preprocessed file back to original (file, line)
    private static class LineMapping {
        final int originalLine;
        final String originalFile;

        LineMapping(int originalLine, String originalFile) {
            this.originalLine = originalLine;
            this.originalFile = originalFile;
        }
    }

    // Index: strippedLine (1-based) -> LineMapping
    private final List<LineMapping> lineMappings = new ArrayList<>();

    private void preprocessWithCpp(String cppPath, String sourceFile, Path outputFile, List<String> includePaths)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(cppPath);
        command.add("-nostdinc");
        command.add("-undef");
        command.add("-w");               // suppress cpp warnings (we only want the preprocessed text)
        command.add("-x");
        command.add("c");
        // Keep #line markers so we can build the mapping, then strip them ourselves
        // (azslc does not understand GNU-style markers and would emit bogus errors).

        for (String inc : includePaths) {
            command.add("-I");
            command.add(inc);
        }

        command.add(sourceFile);

        ProcessBuilder pb = new ProcessBuilder(command);
        // IMPORTANT: do NOT merge stderr into stdout. cpp writes diagnostics to stderr,
        // and merging would inject them into the preprocessed source (appearing near line 1).
        pb.redirectErrorStream(false);
        Process process = pb.start();

        // Drain stderr on a separate thread to avoid pipe-buffer deadlocks
        final List<String> errorLines = new ArrayList<>();
        Thread errThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorLines.add(line);
                }
            } catch (IOException ignored) {}
        }, "azslc-cpp-stderr");
        errThread.setDaemon(true);
        errThread.start();

        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("cpp timed out after 30 seconds");
        }
        errThread.join(1000);

        if (process.exitValue() != 0) {
            String full = String.join("\n", errorLines);
            LOG.info("cpp preprocessing failed:\n" + full);
            throw new IOException("cpp: " + summarizeCppError(errorLines));
        }

        // Strip GNU #line markers, building a mapping of stripped-line -> original (file, line).
        // GNU cpp emits:   # <line> "<file>" [flags]
        // and also:        # <line> "<file>"  (for builtins, flags 1/2/3/4)
        lineMappings.clear();
        Pattern markerPattern = Pattern.compile("^#\\s+(\\d+)\\s+\"([^\"]+)\".*$");
        List<String> stripped = new ArrayList<>(outputLines.size());
        String currentFile = null;
        int currentOriginalLine = 0;

        for (String line : outputLines) {
            Matcher m = markerPattern.matcher(line);
            if (m.matches()) {
                currentOriginalLine = Integer.parseInt(m.group(1));
                currentFile = m.group(2);
                continue; // drop the marker
            }
            if (line.startsWith("#")) {
                // Any other preprocessor marker line (e.g. "#pragma") is kept below
                // but pure line markers were already handled above.
            }
            stripped.add(line);
            // Map this stripped line (1-based) to the current original position
            lineMappings.add(new LineMapping(currentOriginalLine, currentFile));
            if (currentOriginalLine > 0) {
                currentOriginalLine++;
            }
        }

        Files.writeString(outputFile, String.join("\n", stripped), StandardCharsets.UTF_8);
    }

    /**
     * Maps a 1-based line number in the stripped preprocessed file back to the
     * original source line. Returns the original line if known, else 0.
     */
    private int mapToOriginalLine(int strippedLine) {
        if (strippedLine <= 0 || lineMappings.isEmpty()) return 0;
        int idx = Math.min(strippedLine - 1, lineMappings.size() - 1);
        LineMapping mapping = lineMappings.get(idx);
        return mapping.originalLine;
    }

    private LineMapping mapToOriginal(int strippedLine) {
        if (strippedLine <= 0 || lineMappings.isEmpty()) return null;
        int idx = Math.min(strippedLine - 1, lineMappings.size() - 1);
        return lineMappings.get(idx);
    }

    private void runAzslc(String azslcPath, Path inputFile, String sourceFile,
                          AzslcSettings.ValidationMode mode,
                          List<AzslcDiagnostic> diagnostics) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(azslcPath);

        switch (mode) {
            case SYNTAX -> command.add("--syntax");
            case SEMANTIC -> command.add("--semantic");
            case FULL -> command.add("--full");
        }

        command.add(inputFile.toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }

        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            diagnostics.add(new AzslcDiagnostic(0, 0, "warning", "azslc timed out after 60 seconds."));
            return;
        }

        // Normalize the main source file for comparison
        String normalizedSource;
        try {
            normalizedSource = new File(sourceFile).getCanonicalPath();
        } catch (IOException e) {
            normalizedSource = new File(sourceFile).getAbsolutePath();
        }

        // Parse output lines for diagnostics
        for (String line : outputLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Skip known engine-wide warnings (safe to ignore)
            if (trimmed.contains("non-class enumeration") && trimmed.contains("can't qualify names")) continue;

            Matcher m = DIAG_PATTERN.matcher(trimmed);
            if (!m.matches()) {
                LOG.debug("Could not parse diagnostic line: " + trimmed);
                continue;
            }

            String reportedFile = m.group(1);
            int diagLine = Integer.parseInt(m.group(2));
            int diagCol = Integer.parseInt(m.group(3));
            String kind = m.group(4).trim();
            String message = m.group(5).trim();

            String severity = resolveSeverity(kind);

            // Map stripped line number back to original (file, line)
            LineMapping mapping = mapToOriginal(diagLine);
            if (mapping == null || mapping.originalLine <= 0 || mapping.originalFile == null) {
                LOG.debug("Skipping diagnostic without usable line mapping: " + message);
                continue;
            }

            // Only report diagnostics that originate from the file the user is editing.
            // Diagnostics from included engine/gem files are not actionable in this file.
            String diagFileCanonical;
            try {
                diagFileCanonical = new File(mapping.originalFile).getCanonicalPath();
            } catch (IOException e) {
                diagFileCanonical = new File(mapping.originalFile).getAbsolutePath();
            }
            boolean fromEditedFile = diagFileCanonical.equals(normalizedSource);

            if (!fromEditedFile) {
                // Diagnostic originates from an included file. These are usually
                // engine-wide noise; surface only real errors as a weak note.
                if ("error".equals(severity)) {
                    diagnostics.add(new AzslcDiagnostic(1, 0, "note",
                            new File(mapping.originalFile).getName() + ":" + mapping.originalLine + ": " + message));
                }
                continue;
            }

            diagnostics.add(new AzslcDiagnostic(mapping.originalLine, diagCol, severity, message));
        }

        // azslc --syntax may exit 0 on failure; and sometimes prints unparseable lines.
        // Only surface a raw fallback when nothing else was parsed, to avoid line-1 noise.
        if (process.exitValue() != 0 && diagnostics.isEmpty() && !outputLines.isEmpty()) {
            String rawOutput = String.join(" | ", outputLines).trim();
            if (!rawOutput.isEmpty()) {
                if (rawOutput.length() > 500) {
                    rawOutput = rawOutput.substring(0, 500) + "…";
                }
                diagnostics.add(new AzslcDiagnostic(0, 0, "error", rawOutput));
            }
        }
    }

    @Override
    public void apply(@NotNull PsiFile file, List<AzslcDiagnostic> diagnostics, @NotNull AnnotationHolder holder) {
        if (diagnostics == null || diagnostics.isEmpty()) return;

        Document document = FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
        if (document == null) return;

        for (AzslcDiagnostic diag : diagnostics) {
            HighlightSeverity severity = switch (diag.severity) {
                case "error" -> HighlightSeverity.ERROR;
                case "warning" -> HighlightSeverity.WARNING;
                case "note", "info" -> HighlightSeverity.WEAK_WARNING;
                default -> HighlightSeverity.INFORMATION;
            };

            if (diag.line > 0 && diag.line <= document.getLineCount()) {
                int lineStartOffset = document.getLineStartOffset(diag.line - 1);
                int lineEndOffset = document.getLineEndOffset(diag.line - 1);

                int startOffset = lineStartOffset;
                if (diag.column > 0) {
                    startOffset = Math.min(lineStartOffset + diag.column - 1, lineEndOffset);
                }
                if (startOffset >= lineEndOffset) {
                    startOffset = lineStartOffset;
                }

                TextRange range = new TextRange(startOffset, lineEndOffset);
                holder.newAnnotation(severity, "azslc: " + truncate(diag.message))
                        .range(range)
                        .create();
            } else {
                if (document.getLineCount() > 0) {
                    TextRange range = new TextRange(
                            document.getLineStartOffset(0),
                            document.getLineEndOffset(0));
                    holder.newAnnotation(severity, "azslc: " + truncate(diag.message))
                            .range(range)
                            .create();
                }
            }
        }
    }

    private static String truncate(String message) {
        if (message == null) return "";
        if (message.length() > 400) {
            return message.substring(0, 400) + "…";
        }
        return message;
    }

    private static boolean isInPath(String executable) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return false;
        String exeName = System.getProperty("os.name").toLowerCase().contains("win") ? executable + ".exe" : executable;
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (new File(dir, exeName).isFile()) return true;
        }
        return false;
    }

    private static String resolveSeverity(String kind) {
        String k = kind.toLowerCase();
        if (k.contains("error")) return "error";
        if (k.contains("warning")) return "warning";
        if (k.contains("note") || k.contains("info")) return "note";
        return "info";
    }

    /**
     * Turns a verbose cpp stderr (include chain + fatal error) into a single concise line,
     * e.g. "viewsrg_all.srgi:17:10: fatal error: viewsrg.srgi: No such file or directory".
     */
    private static String summarizeCppError(List<String> errorLines) {
        for (String line : errorLines) {
            if (line.contains("fatal error:")) {
                return shortenPath(line.trim());
            }
        }
        for (int i = errorLines.size() - 1; i >= 0; i--) {
            String l = errorLines.get(i).trim();
            if (!l.isEmpty() && !l.startsWith("In file included from") && !l.startsWith("from ")) {
                return shortenPath(l);
            }
        }
        return "preprocessing failed";
    }

    private static String shortenPath(String line) {
        int slash = Math.max(line.lastIndexOf('/'), line.lastIndexOf('\\'));
        int fatal = line.indexOf("fatal error:");
        if (slash > 0 && (fatal < 0 || slash < fatal)) {
            return line.substring(slash + 1);
        }
        return line;
    }
}