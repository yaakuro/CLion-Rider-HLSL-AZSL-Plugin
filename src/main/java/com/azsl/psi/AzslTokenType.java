package com.azsl.psi;

import com.azsl.AzslLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AzslTokenType extends IElementType {
    public AzslTokenType(@NonNls @NotNull String debugName) {
        super(debugName, AzslLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "AzslTokenType." + super.toString();
    }
}
