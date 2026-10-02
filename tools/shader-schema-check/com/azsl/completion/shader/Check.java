package com.azsl.completion.shader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Standalone checks for ShaderFileSchema + ShaderPathScanner (no IntelliJ on the classpath).
 * Args: engine root directory containing .shader files.
 */
public final class Check {

    private static int failures;
    private static int assertions;

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            failures++;
            if (failures <= 40) {
                System.out.println("FAIL: " + message);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        schemaTests();
        scannerTests();
        if (args.length > 0) {
            sweep(Path.of(args[0]));
        } else {
            System.out.println("no engine dir given - skipping sweep");
        }
        System.out.printf("%d assertions, %d failures%n", assertions, failures);
        if (failures > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ schema

    private static void schemaTests() {
        List<ShaderFileSchema.Key> root = ShaderFileSchema.rootKeys();
        check(root.size() == 15, "root has 15 keys, got " + root.size());
        Set<String> rootNames = new HashSet<>();
        for (ShaderFileSchema.Key k : root) {
            rootNames.add(k.name());
            check(!k.name().isEmpty() && !k.type().isEmpty(), "root key complete: " + k);
        }
        for (String expected : new String[]{"Source", "DrawList", "DepthStencilState", "RasterState",
                "BlendState", "GlobalTargetBlendState", "TargetBlendStates", "ProgramSettings",
                "RemoveBuildArguments", "AddBuildArguments", "Definitions", "ShaderOptions",
                "DisabledRHIBackends", "Supervariants", "KeepTempFolder"}) {
            check(rootNames.contains(expected), "root contains " + expected);
        }

        check(names(ShaderFileSchema.childKeys(List.of("DepthStencilState")))
                        .equals(Set.of("depth", "stencil")),
                "DepthStencilState children");
        check(names(ShaderFileSchema.childKeys(list("DepthStencilState", "Depth")))
                        .equals(Set.of("enable", "writeMask", "compareFunc")),
                "DepthState children");
        check(names(ShaderFileSchema.childKeys(list("DepthStencilState", "Stencil", "FrontFace")))
                        .equals(Set.of("failOp", "depthFailOp", "passOp", "func")),
                "StencilOpState children (path case from real files)");
        check(names(ShaderFileSchema.childKeys(list("RasterState"))).size() == 9,
                "RasterState has 9 keys");
        check(names(ShaderFileSchema.childKeys(list("BlendState", "targets"))).size() == 8,
                "TargetBlendState has 8 keys");
        check(names(ShaderFileSchema.childKeys(list("TargetBlendStates", "0"))).size() == 8,
                "numeric map key dropped");
        check(names(ShaderFileSchema.childKeys(list("ProgramSettings", "EntryPoints")))
                        .equals(Set.of("Name", "Type")),
                "entry point keys");
        check(names(ShaderFileSchema.childKeys(list("Supervariants"))).contains("AddBuildArguments"),
                "supervariant keys");
        check(names(ShaderFileSchema.childKeys(list("Supervariants", "AddBuildArguments")))
                        .contains("spirv-cross"),
                "nested build arguments");
        check(names(ShaderFileSchema.childKeys(list("NoSuchThing"))).isEmpty(),
                "unknown section -> empty");

        String[] compare = ShaderFileSchema.valueOptions(list("DepthStencilState", "Depth"), "compareFunc");
        check(compare != null && Arrays.asList(compare).contains("GreaterEqual"), "compareFunc options");
        check(ShaderFileSchema.valueOptions(list("DepthStencilState", "Depth"), "Enable")
                        == ShaderFileSchema.BOOL_VALUES, "bool by key name");
        check(ShaderFileSchema.valueOptions(list("DepthStencilState", "Stencil"), "writeMask") == null,
                "stencil writeMask is numeric, no options");
        check(Arrays.asList(ShaderFileSchema.valueOptions(list("RasterState"), "cullMode")).contains("None"),
                "cullMode options");
        check(ShaderFileSchema.valueOptions(list("ProgramSettings", "EntryPoints"), "type")
                        == ShaderFileSchema.SHADER_STAGE, "entry point stage options");
        check(ShaderFileSchema.valueOptions(list("DisabledRHIBackends"), null) == ShaderFileSchema.RHI_BACKENDS,
                "array element options use path only");
        check(ShaderFileSchema.valueOptions(new ArrayList<>(), "Source") == null, "Source is free-form here");
        check(Arrays.asList(ShaderFileSchema.valueOptions(
                        list("BlendState", "targets"), "blendOp")).contains("SubtractReverse"),
                "target blend op options");
        check(Arrays.asList(ShaderFileSchema.valueOptions(
                        list("DepthStencilState", "Stencil", "frontFace"), "func")).contains("LessEqual"),
                "stencil func options");
    }

    // ------------------------------------------------------------------ scanner

    private static void scannerTests() {
        ShaderPathScanner.Context c = ShaderPathScanner.scan("", 0);
        check(c.position() == ShaderPathScanner.Position.KEY, "empty doc -> key");
        check(c.path().isEmpty(), "empty doc path");

        c = ShaderPathScanner.scan("{", 1);
        check(c.position() == ShaderPathScanner.Position.KEY, "after open brace -> key");

        String doc = "{\n  \"DepthStencilState\" : {\n    \"Depth\" : {\n      \"Enable\" : true,\n      \"CompareFunc\" : \"\"\n    }\n  }\n}";
        int valueOffset = doc.indexOf("\"CompareFunc\" : \"") + "\"CompareFunc\" : \"".length();
        c = ShaderPathScanner.scan(doc, valueOffset);
        check(c.position() == ShaderPathScanner.Position.VALUE, "in enum string -> value");
        check(c.inString(), "in enum string -> inString");
        check(c.path().equals(list("DepthStencilState", "Depth")), "value path: " + c.path());
        check("CompareFunc".equals(c.valueKey()), "value key: " + c.valueKey());
        check(ShaderFileSchema.valueOptions(c.path(), c.valueKey()) != null, "enum options resolve");

        int keyOffset = doc.indexOf("\"Depth\"") + 1;
        c = ShaderPathScanner.scan(doc, keyOffset);
        check(c.position() == ShaderPathScanner.Position.KEY && c.inString(), "inside key string");
        check(c.path().equals(list("DepthStencilState")), "key path: " + c.path());

        // after the DepthStencilState value closes: sibling keys of the root
        String doc2 = "{\"Source\" : \"a.azsl\", }";
        c = ShaderPathScanner.scan(doc2, doc2.indexOf(',') + 1);
        check(c.position() == ShaderPathScanner.Position.KEY, "after comma -> key");
        check(c.containerKeys().contains("Source"), "container keys tracked: " + c.containerKeys());

        // array of strings
        String doc3 = "{\"DisabledRHIBackends\" : [\"met";
        int off3 = doc3.length();
        c = ShaderPathScanner.scan(doc3, off3);
        check(c.position() == ShaderPathScanner.Position.VALUE && c.inString(), "array element string");
        check(c.path().equals(list("DisabledRHIBackends")), "array path: " + c.path());
        check(c.valueKey() == null, "array element valueKey null");
        check(ShaderFileSchema.valueOptions(c.path(), null) == ShaderFileSchema.RHI_BACKENDS,
                "backend options");

        // entry point element
        String doc4 = "{\"ProgramSettings\" : { \"EntryPoints\" : [ { \"type\" : \"Ver";
        c = ShaderPathScanner.scan(doc4, doc4.length());
        check(c.position() == ShaderPathScanner.Position.VALUE && c.inString(), "entry point stage string");
        check(c.path().equals(list("ProgramSettings", "EntryPoints")), "entry point path: " + c.path());
        check("type".equals(c.valueKey()), "entry point value key");
        check(ShaderFileSchema.valueOptions(c.path(), c.valueKey()) == ShaderFileSchema.SHADER_STAGE,
                "stage options");

        // escaped quote inside a string must not end it
        String doc5 = "{\"Source\" : \"a\\";
        c = ShaderPathScanner.scan(doc5, doc5.length());
        check(c.inString(), "backslash at caret keeps string open");

        // comments (invalid JSON, but defensively handled)
        String doc6 = "{\n // \"DepthStencilState\" : { \"Depth\": \"oops\" }\n \"Source\" : \"";
        c = ShaderPathScanner.scan(doc6, doc6.length());
        check(c.position() == ShaderPathScanner.Position.VALUE && "Source".equals(c.valueKey()),
                "line comment skipped: " + c.valueKey());
    }

    // ------------------------------------------------------------------ engine sweep

    private static void sweep(Path engineRoot) throws Exception {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(engineRoot)) {
            walk.filter(p -> p.toString().endsWith(".shader")).forEach(files::add);
        }
        check(!files.isEmpty(), "found .shader files under " + engineRoot);
        int totalOffsets = 0;
        int unknownKeyGaps = 0;
        int badEnumValues = 0;

        // Paths where free-form keys are expected (not part of the schema).
        Set<String> freeFormPaths = Set.of("shaderoptions");

        for (Path file : files) {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            String display = engineRoot.relativize(file).toString();

            // 1. every offset must scan without exceptions and keep invariants
            for (int offset = 0; offset <= text.length(); offset++) {
                ShaderPathScanner.Context ctx = ShaderPathScanner.scan(text, offset);
                totalOffsets++;
                for (String segment : ctx.path()) {
                    check(!segment.isEmpty() && segment.length() < 80,
                            display + ": sane path segment '" + segment + "' at " + offset);
                }
                if (ctx.position() == ShaderPathScanner.Position.KEY) {
                    check(ctx.valueKey() == null, display + ": key position has no value key @" + offset);
                }
            }

            // 2. every key string in the file must be known at its path (schema completeness)
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != '"') {
                    continue;
                }
                ShaderPathScanner.Context ctx = ShaderPathScanner.scan(text, i + 1);
                if (ctx.position() != ShaderPathScanner.Position.KEY || !ctx.inString()) {
                    continue;
                }
                int close = findStringEnd(text, i + 1);
                if (close < 0) {
                    continue;
                }
                String key = text.substring(i + 1, close);
                if (key.isEmpty() || isAllDigits(key)) {
                    continue; // map keys like "0"
                }
                String pathKey = normalize(ctx.path());
                if (freeFormPaths.contains(pathKey)) {
                    continue;
                }
                boolean known = false;
                for (ShaderFileSchema.Key candidate : ShaderFileSchema.childKeys(ctx.path())) {
                    if (candidate.name().equalsIgnoreCase(key)) {
                        known = true;
                        break;
                    }
                }
                if (!known) {
                    unknownKeyGaps++;
                    if (unknownKeyGaps <= 20) {
                        System.out.println("UNKNOWN KEY " + display + " path='" + pathKey
                                + "' key='" + key + "'");
                    }
                }

                // 3. enum values must be offered for this key
                int colon = skipWs(text, close + 1);
                if (colon < text.length() && text.charAt(colon) == ':') {
                    int valueQuote = skipWs(text, colon + 1);
                    if (valueQuote < text.length() && text.charAt(valueQuote) == '"') {
                        int valueEnd = findStringEnd(text, valueQuote + 1);
                        if (valueEnd >= 0) {
                            String value = text.substring(valueQuote + 1, valueEnd);
                            String[] options = ShaderFileSchema.valueOptions(ctx.path(), key);
                            if (options != null && !containsIgnoreCase(options, value)) {
                                badEnumValues++;
                                if (badEnumValues <= 20) {
                                    System.out.println("BAD VALUE " + display + " path='" + pathKey
                                            + "' " + key + "='" + value + "'");
                                }
                            }
                        }
                    }
                }
            }
        }
        check(unknownKeyGaps == 0, "schema knows every key used in engine files (gaps: "
                + unknownKeyGaps + ")");
        check(badEnumValues == 0, "schema knows every enum value used in engine files (bad: "
                + badEnumValues + ")");
        System.out.printf("swept %d files, %d offsets, %d unknown keys, %d bad values%n",
                files.size(), totalOffsets, unknownKeyGaps, badEnumValues);
    }

    // ------------------------------------------------------------------ utils

    private static int findStringEnd(String text, int from) {
        int i = from;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '"') {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static int skipWs(String text, int i) {
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private static boolean containsIgnoreCase(String[] values, String value) {
        for (String v : values) {
            if (v.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(List<String> path) {
        StringBuilder sb = new StringBuilder();
        for (String segment : path) {
            if (segment == null || segment.isEmpty() || isAllDigits(segment)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(segment.toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private static boolean isAllDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return !s.isEmpty();
    }

    private static Set<String> names(List<ShaderFileSchema.Key> keys) {
        Set<String> names = new HashSet<>();
        for (ShaderFileSchema.Key key : keys) {
            names.add(key.name());
        }
        return names;
    }

    private static List<String> list(String... segments) {
        return Arrays.asList(segments);
    }
}
