package com.azsl.formatting;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AzslClangFormatConfigurable implements Configurable {

    private TextFieldWithBrowseButton pathField;
    private JTextField fallbackStyleField;
    private JCheckBox enabledCheckBox;
    private JCheckBox formatOnSaveCheckBox;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "AZSL / clang-format";
    }

    @Override
    public @Nullable JComponent createComponent() {
        pathField = new TextFieldWithBrowseButton();
        FileChooserDescriptor chooser = FileChooserDescriptorFactory.createSingleFileDescriptor();
        chooser.setTitle("Select clang-format Executable");
        chooser.setDescription("Path to clang-format or clang-format.exe");
        pathField.addBrowseFolderListener(new TextBrowseFolderListener(chooser));

        fallbackStyleField = new JTextField();
        enabledCheckBox = new JCheckBox("Enable clang-format for AZSL files (uses Reformat Code action)");
        formatOnSaveCheckBox = new JCheckBox("Format with clang-format on save");

        String auto = AzslClangFormatSettings.autoDetect();
        String hint = auto != null
                ? "Leave empty to use auto-detected: " + auto
                : "clang-format not found in PATH";

        return FormBuilder.createFormBuilder()
                .addLabeledComponent("clang-format executable path:", pathField)
                .addComponentToRightColumn(new JLabel("<html><small>" + hint + "</small></html>"))
                .addLabeledComponent("Fallback style (when no .clang-format found):", fallbackStyleField)
                .addComponentToRightColumn(new JLabel("<html><small>e.g. LLVM, Google, Chromium, Mozilla, WebKit, Microsoft</small></html>"))
                .addComponent(enabledCheckBox)
                .addComponent(formatOnSaveCheckBox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        AzslClangFormatSettings s = AzslClangFormatSettings.getInstance();
        return !pathField.getText().equals(s.getClangFormatPath())
                || !fallbackStyleField.getText().equals(s.getFallbackStyle())
                || enabledCheckBox.isSelected() != s.isEnabled()
                || formatOnSaveCheckBox.isSelected() != s.isFormatOnSave();
    }

    @Override
    public void apply() {
        AzslClangFormatSettings s = AzslClangFormatSettings.getInstance();
        s.setClangFormatPath(pathField.getText());
        s.setFallbackStyle(fallbackStyleField.getText());
        s.setEnabled(enabledCheckBox.isSelected());
        s.setFormatOnSave(formatOnSaveCheckBox.isSelected());
    }

    @Override
    public void reset() {
        AzslClangFormatSettings s = AzslClangFormatSettings.getInstance();
        pathField.setText(s.getClangFormatPath());
        fallbackStyleField.setText(s.getFallbackStyle());
        enabledCheckBox.setSelected(s.isEnabled());
        formatOnSaveCheckBox.setSelected(s.isFormatOnSave());
    }
}
