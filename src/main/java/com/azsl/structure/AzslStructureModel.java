package com.azsl.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class AzslStructureModel extends StructureViewModelBase
        implements StructureViewModel.ElementInfoProvider {

    public AzslStructureModel(@NotNull PsiFile file, @Nullable Editor editor) {
        super(file, editor, new RootElement(file));
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return element instanceof AzslStructureElement e && e.getChildren().length == 0;
    }

    private static final class RootElement implements StructureViewTreeElement {
        private final PsiFile file;
        private StructureViewTreeElement[] children;

        RootElement(PsiFile file) {
            this.file = file;
        }

        @Override
        public Object getValue() {
            return file;
        }

        @Override
        public void navigate(boolean requestFocus) {
            // no-op
        }

        @Override
        public boolean canNavigate() {
            return false;
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }

        @Override
        public @NotNull ItemPresentation getPresentation() {
            return new ItemPresentation() {
                @Override
                public @NotNull String getPresentableText() {
                    return file.getName();
                }

                @Override
                public @NotNull Icon getIcon(boolean unused) {
                    return file.getFileType().getIcon();
                }
            };
        }

        @Override
        public TreeElement @NotNull [] getChildren() {
            if (children == null) {
                List<AzslDeclarationScanner.Decl> decls = AzslDeclarationScanner.scan(file);
                children = new StructureViewTreeElement[decls.size()];
                for (int i = 0; i < decls.size(); i++) {
                    children[i] = new AzslStructureElement(file, decls.get(i));
                }
            }
            return children;
        }
    }

    public static class Builder extends TreeBasedStructureViewBuilder {
        private final PsiFile file;

        public Builder(PsiFile file) {
            this.file = file;
        }

        @Override
        public @NotNull StructureViewModel createStructureViewModel(@Nullable Editor editor) {
            return new AzslStructureModel(file, editor);
        }
    }
}
