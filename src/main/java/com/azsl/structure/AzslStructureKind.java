package com.azsl.structure;

import com.intellij.icons.AllIcons;

import javax.swing.*;

/** Kinds of AZSL declarations shown in the structure view. */
public enum AzslStructureKind {
    SHADER_RESOURCE_GROUP("Shader Resource Group", AllIcons.Nodes.Class),
    SHADER_RESOURCE_GROUP_SEMANTIC("SRG Semantic", AllIcons.Nodes.Annotationtype),
    STRUCT("Struct", AllIcons.Nodes.Class),
    CLASS("Class", AllIcons.Nodes.Class),
    INTERFACE("Interface", AllIcons.Nodes.Interface),
    ENUM("Enum", AllIcons.Nodes.Enum),
    BUFFER("Buffer", AllIcons.Nodes.Field),
    FUNCTION("Function", AllIcons.Nodes.Method),
    OPTION("Shader Option", AllIcons.Nodes.Field),
    MACRO("Macro", AllIcons.Nodes.Static);

    private final String label;
    private final Icon icon;

    AzslStructureKind(String label, Icon icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public Icon getIcon() {
        return icon;
    }
}
