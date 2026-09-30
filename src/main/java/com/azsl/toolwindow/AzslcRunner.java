package com.azsl.toolwindow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs the O3DE azslc toolchain (cpp preprocess -> azslc) for the tool window.
 * Pure I/O: the caller supplies the file content and include roots.
 */
public final class AzslcRunner {

    private AzslcRunner() {
    }

    public static final class Mode {
        public static final String SEMANTIC = "--semantic";
        public static final String SYNTAX = "--syntax";
        public static final String SRG = "--srg";
        public static final String OPTIONS = "--options";
        public static final String ENTRY_POINTS = "--ia";
        public static final String BINDING_DEP = "--bindingdep";
        public static final String FULL = "--full";
    }

    public static String run(String azslcPath,
                             String cppPath,
                             String fileName,
                             String content,
                             List<String> includeRoots,
                             String flag) {
        if (azslcPath == null || azslcPath.isBlank()) {
            return "azslc not found. Configure it in Settings -> Tools -> AZSL / azslc.";
        }
        if (cppPath == null || cppPath.isBlank()) {
            return "cpp (C preprocessor) not found. Windows: install MSYS2/MinGW; Linux/macOS: build-essential / Xcode CLT.";
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("azsl_tw_");
            Path src = tempDir.resolve(fileName.isEmpty() ? "input.azsl" : fileName);
            Files.writeString(src, content, StandardCharsets.UTF_8);

            Path flat = tempDir.resolve("flat.azsl");
            String cppError = preprocess(cppPath, src, flat, includeRoots);
            if (cppError != null) {
                return "cpp preprocessing failed:\n" + cppError;
            }

            return runAzslc(azslcPath, flat, flag);
        } catch (Exception e) {
            return "azslc execution failed: " + e.getMessage();
        } finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                            .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
                } catch (IOException ignored) {}
            }
        }
    }

    /** Returns null on success, or the error text. Writes a marker-stripped flat file. */
    private static String preprocess(String cppPath, Path src, Path flat, List<String> includeRoots)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(cppPath);
        command.add("-nostdinc");
        command.add("-undef");
        command.add("-w");
        command.add("-x");
        command.add("c");
        for (String inc : includeRoots) {
            command.add("-I");
            command.add(inc);
        }
        command.add(src.toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        List<String> errorLines = new ArrayList<>();
        Thread errThread = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String l;
                while ((l = r.readLine()) != null) errorLines.add(l);
            } catch (IOException ignored) {}
        }, "azsl-toolwindow-cpp-stderr");
        errThread.setDaemon(true);
        errThread.start();

        List<String> output = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) output.add(l);
        }
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return "cpp timed out after 30 seconds";
        }
        errThread.join(1000);

        if (process.exitValue() != 0) {
            return String.join("\n", errorLines);
        }

        // Strip GNU #line markers
        List<String> stripped = new ArrayList<>(output.size());
        for (String line : output) {
            if (line.matches("^#\\s+\\d+\\s+\"[^\"]+\".*$")) continue;
            stripped.add(line);
        }
        Files.writeString(flat, String.join("\n", stripped), StandardCharsets.UTF_8);
        return null;
    }

    private static String runAzslc(String azslcPath, Path input, String flag)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(azslcPath);
        command.add(flag);
        command.add(input.toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) {
                sb.append(l).append('\n');
            }
        }
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return "azslc timed out after 60 seconds";
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? "(no output — no errors)" : out;
    }
}
