package com.azsl.structure;

import com.azsl.AzslFileType;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class AzslDeclarationScannerTest extends BasePlatformTestCase {

    public void testSrgWithOptionChild() {
        String text = """
                ShaderResourceGroup MySrg : SRG_PerMaterial
                {
                    option bool o_flag = true;
                    Texture2D m_diffuse;
                };
                """;
        PsiFile file = myFixture.configureByText(AzslFileType.INSTANCE, text);
        List<AzslDeclarationScanner.Decl> decls = AzslDeclarationScanner.scan(file);

        assertEquals(1, decls.size());
        assertEquals("MySrg", decls.get(0).name);
        assertEquals(AzslStructureKind.SHADER_RESOURCE_GROUP, decls.get(0).kind);
        assertEquals(1, decls.get(0).children.size());
        assertEquals("o_flag", decls.get(0).children.get(0).name);
        assertEquals(AzslStructureKind.OPTION, decls.get(0).children.get(0).kind);
    }

    public void testStructAndFunction() {
        String text = """
                struct VSInput
                {
                    float3 Position : POSITION;
                };

                float4 MainPS(VSInput input) : SV_Target0
                {
                    return float4(0, 0, 0, 1);
                }
                """;
        PsiFile file = myFixture.configureByText(AzslFileType.INSTANCE, text);
        List<AzslDeclarationScanner.Decl> decls = AzslDeclarationScanner.scan(file);

        assertEquals(2, decls.size());
        assertEquals(AzslStructureKind.STRUCT, decls.get(0).kind);
        assertEquals("VSInput", decls.get(0).name);
        assertEquals(AzslStructureKind.FUNCTION, decls.get(1).kind);
        assertEquals("MainPS", decls.get(1).name);
    }

    public void testShaderResourceGroupSemantic() {
        String text = """
                ShaderResourceGroupSemantic SRG_PerDraw
                {
                    FrequencyId = 0;
                };
                """;
        PsiFile file = myFixture.configureByText(AzslFileType.INSTANCE, text);
        List<AzslDeclarationScanner.Decl> decls = AzslDeclarationScanner.scan(file);

        assertEquals(1, decls.size());
        assertEquals(AzslStructureKind.SHADER_RESOURCE_GROUP_SEMANTIC, decls.get(0).kind);
        assertEquals("SRG_PerDraw", decls.get(0).name);
    }
}
