package com.azsl.docs;

import com.azsl.psi.AzslTokenTypes;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Provides hover documentation for AZSL built-ins, types, identifiers, SRG semantics and states. */
public class AzslDocumentationProvider extends AbstractDocumentationProvider {

    private static final Map<String, String> BUILTINS = Map.ofEntries(
            Map.entry("saturate", "Clamps each component of <code>x</code> to the range [0, 1]."),
            Map.entry("dot", "Returns the dot product of two vectors."),
            Map.entry("cross", "Returns the cross product of two 3-component vectors."),
            Map.entry("mul", "Multiplies a vector/matrix or matrix/matrix."),
            Map.entry("normalize", "Returns a vector of unit length (same direction)."),
            Map.entry("length", "Returns the length (magnitude) of a vector."),
            Map.entry("distance", "Returns the distance between two points."),
            Map.entry("lerp", "Linearly interpolates between two values."),
            Map.entry("clamp", "Clamps a value to the range [min, max]."),
            Map.entry("smoothstep", "Returns a smooth Hermite interpolation between 0 and 1."),
            Map.entry("step", "Returns 0 or 1 based on a comparison with a threshold."),
            Map.entry("frac", "Returns the fractional part of a value."),
            Map.entry("floor", "Returns the largest integer <= x (per component)."),
            Map.entry("ceil", "Returns the smallest integer >= x (per component)."),
            Map.entry("round", "Rounds to the nearest integer (per component)."),
            Map.entry("abs", "Returns the absolute value (per component)."),
            Map.entry("min", "Returns the minimum of the components."),
            Map.entry("max", "Returns the maximum of the components."),
            Map.entry("pow", "Returns x raised to the power y."),
            Map.entry("sqrt", "Returns the square root (per component)."),
            Map.entry("rsqrt", "Returns the reciprocal square root (per component)."),
            Map.entry("exp", "Returns the base-e exponential."),
            Map.entry("exp2", "Returns the base-2 exponential."),
            Map.entry("log", "Returns the natural logarithm."),
            Map.entry("log2", "Returns the base-2 logarithm."),
            Map.entry("sin", "Returns the sine of x radians."),
            Map.entry("cos", "Returns the cosine of x radians."),
            Map.entry("tan", "Returns the tangent of x radians."),
            Map.entry("reflect", "Returns the reflection vector for an incident ray and a normal."),
            Map.entry("refract", "Returns the refraction vector for an incident ray and a normal."),
            Map.entry("transpose", "Returns the transpose of a matrix."),
            Map.entry("determinant", "Returns the determinant of a square matrix."),
            Map.entry("ddx", "Returns the partial derivative with respect to screen x."),
            Map.entry("ddy", "Returns the partial derivative with respect to screen y."),
            Map.entry("fwidth", "Returns abs(ddx(x)) + abs(ddy(x))."),
            Map.entry("tex2D", "Samples a 2D texture (legacy sampler)."),
            Map.entry("tex2Dlod", "Samples a 2D texture at an explicit LOD."),
            Map.entry("asfloat", "Reinterprets the bits of the input as a float."),
            Map.entry("asuint", "Reinterprets the bits of the input as a uint."),
            Map.entry("asint", "Reinterprets the bits of the input as an int."),
            Map.entry("clip", "Discards the current pixel if any component is negative."),
            Map.entry("discard", "Discards the current pixel (statement).")
    );

    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("float", "32-bit floating point scalar."),
            Map.entry("half", "16-bit floating point scalar."),
            Map.entry("double", "64-bit floating point scalar."),
            Map.entry("int", "32-bit signed integer scalar."),
            Map.entry("uint", "32-bit unsigned integer scalar."),
            Map.entry("bool", "Boolean scalar."),
            Map.entry("float2", "2-component float vector."),
            Map.entry("float3", "3-component float vector."),
            Map.entry("float4", "4-component float vector."),
            Map.entry("float4x4", "4x4 float matrix."),
            Map.entry("Texture2D", "2D texture resource."),
            Map.entry("TextureCube", "Cube texture resource."),
            Map.entry("SamplerState", "Sampler state resource."),
            Map.entry("StructuredBuffer", "Read-only structured buffer."),
            Map.entry("RWStructuredBuffer", "Read-write structured buffer."),
            Map.entry("ShaderResourceGroup", "O3DE Shader Resource Group — a named group of resources bound at a specific frequency."),
            Map.entry("ShaderResourceGroupSemantic", "Declares an SRG binding-frequency semantic (e.g. per-draw, per-object, per-scene)."),
            Map.entry("vector", "Generic vector type: vector<float, N>."),
            Map.entry("matrix", "Generic matrix type: matrix<float, R, C>.")
    );

    private static final Map<String, String> KEYWORDS = Map.ofEntries(
            Map.entry("option", "Declares a shader option — a compile/run-time configurable value. Must appear at ShaderResourceGroup (top-level SRG) scope."),
            Map.entry("partial", "Marks a ShaderResourceGroup as a partial definition that is merged with others."),
            Map.entry("rootconstant", "Root constant — must appear at top-level ShaderResourceGroup scope."),
            Map.entry("row_major", "Stores matrix rows consecutively in memory."),
            Map.entry("column_major", "Stores matrix columns consecutively in memory."),
            Map.entry("groupshared", "Declares group-shared memory for compute shaders."),
            Map.entry("nointerpolation", "Interpolation modifier: do not interpolate across the primitive."),
            Map.entry("centroid", "Interpolation modifier: sample at the centroid."),
            Map.entry("noperspective", "Interpolation modifier: no perspective correction."),
            Map.entry("linear", "Interpolation modifier: linear (perspective-correct).")
    );

    private static final Map<String, String> SRG_SEMANTICS = Map.ofEntries(
            Map.entry("SRG_PerDraw", "Bind frequency: changes once per draw call."),
            Map.entry("SRG_PerObject", "Bind frequency: changes once per object."),
            Map.entry("SRG_PerMaterial", "Bind frequency: changes once per material."),
            Map.entry("SRG_PerSubPass", "Bind frequency: changes once per sub-pass."),
            Map.entry("SRG_PerPass", "Bind frequency: changes once per render pass."),
            Map.entry("SRG_PerPass_WithFallback", "Bind frequency: per pass, with a shader-variant fallback."),
            Map.entry("SRG_PerView", "Bind frequency: changes once per view (camera)."),
            Map.entry("SRG_PerScene", "Bind frequency: changes once per scene."),
            Map.entry("SRG_Bindless", "Bindless resource group."),
            Map.entry("SRG_RayTracingGlobal", "Ray tracing global resources."),
            Map.entry("SRG_RayTracingScene", "Ray tracing scene resources."),
            Map.entry("SRG_RayTracingMaterial", "Ray tracing material resources.")
    );

    private static final Map<String, String> SAMPLER = Map.ofEntries(
            Map.entry("AddressU", "Texture U-coordinate address mode."),
            Map.entry("AddressV", "Texture V-coordinate address mode."),
            Map.entry("AddressW", "Texture W-coordinate address mode."),
            Map.entry("MinFilter", "Minification filter."),
            Map.entry("MagFilter", "Magnification filter."),
            Map.entry("MipFilter", "Mip filter."),
            Map.entry("ComparisonFunc", "Comparison function (for comparison samplers)."),
            Map.entry("BorderColor", "Border color used when the address mode is Border."),
            Map.entry("MaxAnisotropy", "Maximum anisotropy (for Anisotropic filtering)."),
            Map.entry("MipLODBias", "Mipmap LOD bias."),
            Map.entry("MaxLOD", "Clamp upper bound of the LOD range."),
            Map.entry("MinLOD", "Clamp lower bound of the LOD range."),
            Map.entry("Wrap", "Address mode: wrap/repeat."),
            Map.entry("Clamp", "Address mode: clamp to edge."),
            Map.entry("Mirror", "Address mode: mirror/repeat."),
            Map.entry("Border", "Address mode: use the border color."),
            Map.entry("MirrorOnce", "Address mode: mirror once, then clamp."),
            Map.entry("Point", "Filter: nearest (point) sampling."),
            Map.entry("Linear", "Filter: linear interpolation.")
    );

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (element.getNode() == null) return null;
        IElementType type = element.getNode().getElementType();
        String text = element.getText();
        if (text == null || text.isEmpty()) return null;

        String doc = null;
        String header = null;

        if (type == AzslTokenTypes.BUILTIN_FUNCTION) {
            doc = BUILTINS.get(text);
            if (doc != null) header = text + "( ... )";
        } else if (type == AzslTokenTypes.TYPE_KEYWORD) {
            doc = TYPES.get(text);
            if (doc != null) header = text;
        } else if (type == AzslTokenTypes.KEYWORD) {
            doc = KEYWORDS.get(text);
            if (doc == null && SRG_SEMANTICS.containsKey(text)) doc = SRG_SEMANTICS.get(text);
            if (doc == null && SAMPLER.containsKey(text)) doc = SAMPLER.get(text);
            if (doc == null && BUILTINS.containsKey(text)) doc = BUILTINS.get(text);
            if (doc != null) header = text;
        } else if (type == AzslTokenTypes.IDENTIFIER || type == AzslTokenTypes.FUNCTION_CALL) {
            doc = SRG_SEMANTICS.get(text);
            if (doc == null) doc = BUILTINS.get(text);
            if (doc != null) header = text;
        }

        if (doc == null) return null;

        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        if (header != null) {
            html.append("<b>").append(escape(header)).append("</b><br/>");
        }
        html.append(doc);
        html.append("</body></html>");
        return html.toString();
    }

    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        return null;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
