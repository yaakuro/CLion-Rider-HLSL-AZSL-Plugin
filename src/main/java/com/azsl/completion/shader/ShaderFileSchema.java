package com.azsl.completion.shader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Static description of the O3DE {@code .shader} file format and the render pipeline states it
 * can configure. Transcribed from the engine sources:
 *
 * <ul>
 *   <li>Top level: {@code AZ::RPI::ShaderSourceData} — Gems/Atom/RPI/Code/Source/RPI.Edit/Shader/ShaderSourceData.cpp</li>
 *   <li>Render states: {@code AZ::RHI} — Gems/Atom/RHI/Code/Source/RHI.Reflect/RenderStates.cpp and Include/Atom/RHI.Reflect/RenderStates.h</li>
 *   <li>Comparison functions: Gems/Atom/RHI/Code/Include/Atom/RHI.Reflect/SamplerState.h</li>
 *   <li>Entry point stages: Gems/Atom/RPI/Code/Include/Atom/RPI.Reflect/Shader/ShaderCommonTypes.h</li>
 *   <li>Build arguments: Gems/Atom/RHI/Code/Source/RHI.Edit/ShaderBuildArguments.cpp</li>
 * </ul>
 *
 * <p>The engine matches JSON member names case-insensitively ({@code AZ::Crc32(string_view)}
 * lower-cases its input and the serializer compares those CRCs), so shipped files freely mix
 * {@code "Depth"} and {@code "depth"}. Suggested keys use the canonical serialize-context
 * spelling; any casing the user picks keeps working.</p>
 *
 * <p>This class has no IntelliJ dependencies so it can be exercised by a plain unit harness.</p>
 */
public final class ShaderFileSchema {

    /** A completable JSON key together with the type it expects. */
    public record Key(String name, String type) {
    }

    // ------------------------------------------------------------------ enums

    public static final String[] COMPARISON_FUNC =
            {"Never", "Less", "Equal", "LessEqual", "Greater", "NotEqual", "GreaterEqual", "Always"};
    public static final String[] DEPTH_WRITE_MASK = {"Zero", "All"};
    public static final String[] STENCIL_OP =
            {"Keep", "Zero", "Replace", "IncrementSaturate", "DecrementSaturate", "Invert", "Increment", "Decrement"};
    public static final String[] CULL_MODE = {"None", "Front", "Back"};
    public static final String[] FILL_MODE = {"Solid", "Wireframe"};
    public static final String[] BLEND_FACTOR = {
            "Zero", "One", "ColorSource", "ColorSourceInverse", "AlphaSource", "AlphaSourceInverse",
            "AlphaDest", "AlphaDestInverse", "ColorDest", "ColorDestInverse", "AlphaSourceSaturate",
            "Factor", "FactorInverse", "ColorSource1", "ColorSource1Inverse", "AlphaSource1", "AlphaSource1Inverse"};
    public static final String[] BLEND_OP = {"Add", "Subtract", "SubtractReverse", "Minimum", "Maximum"};
    public static final String[] SHADER_STAGE = {
            "Vertex", "Geometry", "TessellationControl", "TessellationEvaluation",
            "Fragment", "Compute", "RayTracing"};
    public static final String[] BOOL_VALUES = {"false", "true"};
    /** ShaderPlatformInterface API names (GetAPIName); used by DisabledRHIBackends. */
    public static final String[] RHI_BACKENDS = {"dx12", "vulkan", "metal"};

    // ------------------------------------------------------------------ tables

    private static final Map<String, List<Key>> SECTIONS = new HashMap<>();
    private static final Map<String, String[]> VALUE_OPTIONS = new HashMap<>();
    private static final Set<String> BOOL_KEYS = new HashSet<>(Arrays.asList(
            "enable", "alphaToCoverageEnable", "independentBlendEnable", "multisampleEnable",
            "depthClipEnable", "conservativeRasterEnable", "keepTempFolder", "debug"));

    private ShaderFileSchema() {
    }

    /** Keys of the document root object ({@code ShaderSourceData}). */
    public static List<Key> rootKeys() {
        return childKeys(List.of());
    }

    /**
     * Keys that may appear in the object at {@code path} (ancestor container keys, raw casing).
     * Returns an empty list for unknown/unsupported containers.
     */
    public static List<Key> childKeys(List<String> path) {
        List<Key> keys = SECTIONS.get(normalizePath(path));
        return keys != null ? keys : List.of();
    }

    /**
     * Values offered for a string/bool member: {@code path} are the ancestor container keys and
     * {@code key} is the member owning the value ({@code null} for elements typed directly in an
     * array, whose path already ends with the array key). Returns {@code null} when the member
     * takes a free-form value.
     */
    public static String[] valueOptions(List<String> path, String key) {
        String full = normalizePath(path);
        if (key != null) {
            String k = key.toLowerCase(Locale.ROOT);
            if (BOOL_KEYS.contains(k)) {
                return BOOL_VALUES;
            }
            full = full.isEmpty() ? k : full + "." + k;
        }
        return VALUE_OPTIONS.get(full);
    }

    /** True when {@code options} are the {@code true}/{@code false} pair (must not be quoted). */
    public static boolean isBoolOptions(String[] options) {
        return options == BOOL_VALUES;
    }

    // ------------------------------------------------------------------ helpers

    private static String normalizePath(List<String> path) {
        StringBuilder sb = new StringBuilder();
        for (String segment : path) {
            if (segment == null || segment.isEmpty() || isAllDigits(segment)) {
                // Numeric segments are map keys (e.g. TargetBlendStates."0") — they are not
                // part of the schema path.
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

    private static Key k(String name, String type) {
        return new Key(name, type);
    }

    private static void section(String path, Key... keys) {
        SECTIONS.put(path, List.of(keys));
    }

    private static void option(String pathKey, String[] values) {
        VALUE_OPTIONS.put(pathKey, values);
    }

    private static Key[] stencilOpState() {
        return new Key[]{
                k("failOp", "StencilOp"),
                k("depthFailOp", "StencilOp"),
                k("passOp", "StencilOp"),
                k("func", "ComparisonFunc")};
    }

    private static Key[] targetBlendState() {
        return new Key[]{
                k("enable", "bool"),
                k("writeMask", "uint32"),
                k("blendSource", "BlendFactor"),
                k("blendDest", "BlendFactor"),
                k("blendOp", "BlendOp"),
                k("blendAlphaSource", "BlendFactor"),
                k("blendAlphaDest", "BlendFactor"),
                k("blendAlphaOp", "BlendOp")};
    }

    private static Key[] shaderBuildArguments() {
        return new Key[]{
                k("debug", "bool"),
                k("preprocessor", "string[]"),
                k("azslc", "string[]"),
                k("dxc", "string[]"),
                k("spirv-cross", "string[]"),
                k("metalair", "string[]"),
                k("metallib", "string[]")};
    }

    // ------------------------------------------------------------------ schema

    static {
        section("",
                k("Source", "string"),
                k("DrawList", "string"),
                k("DepthStencilState", "DepthStencilState"),
                k("RasterState", "RasterState"),
                k("BlendState", "BlendState"),
                k("GlobalTargetBlendState", "TargetBlendState"),
                k("TargetBlendStates", "map<uint32, TargetBlendState>"),
                k("ProgramSettings", "ProgramSettings"),
                k("RemoveBuildArguments", "ShaderBuildArguments"),
                k("AddBuildArguments", "ShaderBuildArguments"),
                k("Definitions", "string[]"),
                k("ShaderOptions", "map<Name, Name>"),
                k("DisabledRHIBackends", "string[]"),
                k("Supervariants", "SupervariantInfo[]"),
                k("KeepTempFolder", "bool"));

        // DepthStencilState -> DepthState / StencilState -> StencilOpState
        section("depthstencilstate",
                k("depth", "DepthState"),
                k("stencil", "StencilState"));
        section("depthstencilstate.depth",
                k("enable", "bool"),
                k("writeMask", "DepthWriteMask"),
                k("compareFunc", "ComparisonFunc"));
        section("depthstencilstate.stencil",
                k("enable", "bool"),
                k("readMask", "uint32"),
                k("writeMask", "uint32"),
                k("frontFace", "StencilOpState"),
                k("backFace", "StencilOpState"));
        section("depthstencilstate.stencil.frontface", stencilOpState());
        section("depthstencilstate.stencil.backface", stencilOpState());

        // RasterState
        section("rasterstate",
                k("depthBias", "int32"),
                k("depthBiasClamp", "float"),
                k("depthBiasSlopeScale", "float"),
                k("fillMode", "FillMode"),
                k("cullMode", "CullMode"),
                k("multisampleEnable", "bool"),
                k("depthClipEnable", "bool"),
                k("conservativeRasterEnable", "bool"),
                k("forcedSampleCount", "uint32"));

        // Blend states
        section("blendstate",
                k("alphaToCoverageEnable", "bool"),
                k("independentBlendEnable", "bool"),
                k("targets", "TargetBlendState[]"));
        section("blendstate.targets", targetBlendState());
        section("globaltargetblendstate", targetBlendState());
        section("targetblendstates", targetBlendState());

        // Program settings / entry points
        section("programsettings",
                k("entryPoints", "EntryPoint[]"));
        section("programsettings.entrypoints",
                k("Name", "string"),
                k("Type", "ShaderStageType"));

        // Supervariants (array element keys + nested build arguments)
        section("supervariants",
                k("Name", "string"),
                k("RemoveBuildArguments", "ShaderBuildArguments"),
                k("AddBuildArguments", "ShaderBuildArguments"),
                k("Definitions", "string[]"));
        section("supervariants.addbuildarguments", shaderBuildArguments());
        section("supervariants.removebuildarguments", shaderBuildArguments());
        section("addbuildarguments", shaderBuildArguments());
        section("removebuildarguments", shaderBuildArguments());

        // ---------------------------------------------------------------- values
        option("depthstencilstate.depth.writemask", DEPTH_WRITE_MASK);
        option("depthstencilstate.depth.comparefunc", COMPARISON_FUNC);
        for (String face : new String[]{"frontface", "backface"}) {
            option("depthstencilstate.stencil." + face + ".failop", STENCIL_OP);
            option("depthstencilstate.stencil." + face + ".depthfailop", STENCIL_OP);
            option("depthstencilstate.stencil." + face + ".passop", STENCIL_OP);
            option("depthstencilstate.stencil." + face + ".func", COMPARISON_FUNC);
        }
        option("rasterstate.fillmode", FILL_MODE);
        option("rasterstate.cullmode", CULL_MODE);
        for (String owner : new String[]{"globaltargetblendstate", "blendstate.targets", "targetblendstates"}) {
            option(owner + ".blendsource", BLEND_FACTOR);
            option(owner + ".blenddest", BLEND_FACTOR);
            option(owner + ".blendalphasource", BLEND_FACTOR);
            option(owner + ".blendalphadest", BLEND_FACTOR);
            option(owner + ".blendop", BLEND_OP);
            option(owner + ".blendalphaop", BLEND_OP);
        }
        option("programsettings.entrypoints.type", SHADER_STAGE);
        option("disabledrhibackends", RHI_BACKENDS);
    }

    // ------------------------------------------------------------------ introspection (tests)

    /** Snapshot of every completable container path, for test harnesses. */
    static Map<String, List<Key>> allSections() {
        return Map.copyOf(new HashMap<>(SECTIONS));
    }

    /** Snapshot of every value-option path, for test harnesses. */
    static Map<String, String[]> allValueOptions() {
        return Map.copyOf(new HashMap<>(VALUE_OPTIONS));
    }

    /** Convenience for harnesses: unmodifiable copy of the root keys. */
    static List<Key> unmodifiableRoot() {
        return new ArrayList<>(rootKeys());
    }
}
