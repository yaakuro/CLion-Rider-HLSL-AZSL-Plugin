package com.azsl.highlighting;

import com.azsl.AzslIcons;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class AzslColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Keyword", AzslSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Type", AzslSyntaxHighlighter.TYPE_KEYWORD),
            new AttributesDescriptor("Built-in function", AzslSyntaxHighlighter.BUILTIN_FUNCTION),
            new AttributesDescriptor("Function call", AzslSyntaxHighlighter.FUNCTION_CALL),
            new AttributesDescriptor("Instance method call", AzslSyntaxHighlighter.INSTANCE_METHOD_CALL),
            new AttributesDescriptor("Field access", AzslSyntaxHighlighter.FIELD_ACCESS),
            new AttributesDescriptor("Semantic", AzslSyntaxHighlighter.SEMANTIC),
            new AttributesDescriptor("SRG frequency semantic", AzslSyntaxHighlighter.SRG_SEMANTIC),
            new AttributesDescriptor("Sampler state keyword", AzslSyntaxHighlighter.SAMPLER_KEYWORD),
            new AttributesDescriptor("Shader option keyword", AzslSyntaxHighlighter.SHADER_OPTION),
            new AttributesDescriptor("Preprocessor directive", AzslSyntaxHighlighter.PREPROCESSOR),
            new AttributesDescriptor("Number", AzslSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("String", AzslSyntaxHighlighter.STRING),
            new AttributesDescriptor("Line comment", AzslSyntaxHighlighter.LINE_COMMENT),
            new AttributesDescriptor("Block comment", AzslSyntaxHighlighter.BLOCK_COMMENT),
            new AttributesDescriptor("Operator", AzslSyntaxHighlighter.OPERATOR),
            new AttributesDescriptor("Semicolon", AzslSyntaxHighlighter.SEMICOLON),
            new AttributesDescriptor("Comma", AzslSyntaxHighlighter.COMMA),
            new AttributesDescriptor("Dot", AzslSyntaxHighlighter.DOT),
            new AttributesDescriptor("Parentheses", AzslSyntaxHighlighter.PAREN),
            new AttributesDescriptor("Braces", AzslSyntaxHighlighter.BRACE),
            new AttributesDescriptor("Brackets", AzslSyntaxHighlighter.BRACKET),
            new AttributesDescriptor("Struct / Class name", AzslSyntaxHighlighter.STRUCT_NAME),
            new AttributesDescriptor("Identifier", AzslSyntaxHighlighter.IDENTIFIER),
            new AttributesDescriptor("Bad character", AzslSyntaxHighlighter.BAD_CHARACTER),
    };

    @Override
    public @Nullable Icon getIcon() {
        return AzslIcons.FILE;
    }

    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new AzslSyntaxHighlighter();
    }

    @Override
    public @NotNull String getDemoText() {
        return """
                // ShaderResourceGroup declaration
                ShaderResourceGroup MaterialSrg : SRG_PerMaterial
                {
                    // Constant buffer
                    cbuffer MaterialData : register(b0)
                    {
                        float4 m_baseColor;
                        float m_roughness;
                        float m_metallic;
                    };

                    // Textures
                    Texture2D m_diffuseTexture;
                    Texture2D m_normalTexture;

                    // Sampler with state
                    Sampler m_samplerLinear
                    {
                        AddressU = Wrap;
                        AddressV = Wrap;
                        MinFilter = Linear;
                        MagFilter = Linear;
                        MipFilter = Linear;
                    };

                    // Shader options
                    option bool o_enableNormalMap = true;
                    option enum class BlendMode { Opaque, Transparent, Additive } o_blendMode;
                };

                // Vertex shader
                struct VSInput
                {
                    float3 Position : POSITION;
                    float3 Normal : NORMAL;
                    float2 TexCoord : TEXCOORD0;
                };

                struct VSOutput
                {
                    float4 Position : SV_Position;
                    float2 TexCoord : TEXCOORD0;
                    float3 Normal : TEXCOORD1;
                };

                VSOutput MainVS(VSInput input)
                {
                    VSOutput output;
                    output.Position = float4(input.Position, 1.0);
                    output.TexCoord = input.TexCoord;
                    output.Normal = input.Normal;
                    return output;
                }

                // Pixel shader
                float4 MainPS(VSOutput input) : SV_Target0
                {
                    float4 baseColor = MaterialSrg.m_baseColor;
                    float4 texColor = MaterialSrg.m_diffuseTexture.Sample(MaterialSrg.m_samplerLinear, input.TexCoord);
                    return baseColor * texColor;
                }
                """;
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "AZSL";
    }
}