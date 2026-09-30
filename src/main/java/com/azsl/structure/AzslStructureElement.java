package com.azsl.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.util.Iconable;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AzslStructureElement implements StructureViewTreeElement {

    private final PsiFile file;
    private final AzslDeclarationScanner.Decl decl;
    private StructureViewTreeElement[] children;

    public AzslStructureElement(PsiFile file, AzslDeclarationScanner.Decl decl) {
        this.file = file;
        this.decl = decl;
    }

    @Override
    public Object getValue() {
        PsiElement element = file.findElementAt(decl.nameOffset);
        return element != null ? element : file;
    }

    @Nullable
    private Navigatable getNavigatable() {
        PsiElement element = file.findElementAt(decl.nameOffset);
        if (element instanceof Navigatable navigatable) return navigatable;
        if (element != null && element.getParent() instanceof Navigatable navigatable) return navigatable;
        return null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        Navigatable navigatable = getNavigatable();
        if (navigatable != null) navigatable.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        Navigatable navigatable = getNavigatable();
        return navigatable != null && navigatable.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        PsiElement element = file.findElementAt(decl.nameOffset);
        return element instanceof NavigationItem navigationItem && navigationItem.canNavigateToSource();
    }

    @Override
    public @NotNull ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public @NotNull String getPresentableText() {
                return decl.name;
            }

            @Override
            public @Nullable String getLocationString() {
                return decl.kind.getLabel();
            }

            @Override
            public @NotNull Icon getIcon(boolean unused) {
                return decl.kind.getIcon();
            }
        };
    }

    @Override
    public StructureViewTreeElement @NotNull [] getChildren() {
        if (children == null) {
            children = new StructureViewTreeElement[decl.children.size()];
            for (int i = 0; i < decl.children.size(); i++) {
                children[i] = new AzslStructureElement(file, decl.children.get(i));
            }
        }
        return children;
    }

    @Override
    public String toString() {
        return decl.name;
    }
}
