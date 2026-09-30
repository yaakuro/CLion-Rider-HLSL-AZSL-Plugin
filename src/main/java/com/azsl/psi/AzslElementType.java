package com.azsl.psi;

import com.azsl.AzslLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AzslElementType extends IElementType {
    public AzslElementType(@NonNls @NotNull String debugName) {
        super(debugName, AzslLanguage.INSTANCE);
    }
}
