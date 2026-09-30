package com.azsl.validation;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AzslcConfigurable implements Configurable {

    private TextFieldWithBrowseButton azslcPathField;
    private TextFieldWithBrowseButton o3deEnginePathField;
    private TextFieldWithBrowseButton o3deProjectPathField;
    private TextFieldWithBrowseButton cppPathField;
    private JComboBox<AzslcSettings.ValidationMode> validationModeCombo;
    private JCheckBox enableValidationCheckBox;
    private JCheckBox validateOnSaveOnlyCheckBox;

    // Include paths list
    private final DefaultListModel<String> includePathsModel = new DefaultListModel<>();
    private JList<String> includePathsList;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "AZSL / azslc";
    }

    @Override
    public @Nullable JComponent createComponent() {
        // azslc path
        azslcPathField = new TextFieldWithBrowseButton();
        FileChooserDescriptor azslcChooser = FileChooserDescriptorFactory.createSingleFileDescriptor();
        azslcChooser.setTitle("Select azslc Executable");
        azslcChooser.setDescription("Path to azslc or azslc.exe");
        azslcPathField.addBrowseFolderListener(new TextBrowseFolderListener(azslcChooser));

        // O3DE Engine path
        o3deEnginePathField = new TextFieldWithBrowseButton();
        FileChooserDescriptor engineChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        engineChooser.setTitle("Select O3DE Engine Root");
        engineChooser.setDescription("Root folder of O3DE engine (contains Gems/, etc.)");
        o3deEnginePathField.addBrowseFolderListener(new TextBrowseFolderListener(engineChooser));

        // O3DE Project path
        o3deProjectPathField = new TextFieldWithBrowseButton();
        FileChooserDescriptor projectChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        projectChooser.setTitle("Select O3DE Project Root (optional)");
        projectChooser.setDescription("Project folder (contains ShaderLib/, etc.)");
        o3deProjectPathField.addBrowseFolderListener(new TextBrowseFolderListener(projectChooser));

        // cpp path
        cppPathField = new TextFieldWithBrowseButton();
        FileChooserDescriptor cppChooser = FileChooserDescriptorFactory.createSingleFileDescriptor();
        cppChooser.setTitle("Select cpp (C Preprocessor) Executable");
        cppChooser.setDescription("Path to cpp or cpp.exe (MSYS2/MinGW on Windows)");
        cppPathField.addBrowseFolderListener(new TextBrowseFolderListener(cppChooser));

        // Validation mode
        validationModeCombo = new JComboBox<>(AzslcSettings.ValidationMode.values());

        // Checkboxes
        enableValidationCheckBox = new JCheckBox("Enable azslc validation");
        validateOnSaveOnlyCheckBox = new JCheckBox("Validate on file save only (not on every keystroke)");

        // Include paths list with add/remove/edit
        includePathsList = new JList<>(includePathsModel);
        includePathsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String s) {
                    setText(s);
                    setToolTipText(s);
                }
                return this;
            }
        });

        JPanel includePathsPanel = new JPanel(new BorderLayout());
        includePathsPanel.setBorder(BorderFactory.createTitledBorder("Additional Include Paths"));
        
        JScrollPane scrollPane = new JScrollPane(includePathsList);
        scrollPane.setPreferredSize(new Dimension(400, 120));
        includePathsPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> addIncludePath());
        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> removeSelectedIncludePath());
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> editSelectedIncludePath());
        JButton upBtn = new JButton("Up");
        upBtn.addActionListener(e -> moveSelectedIncludePath(-1));
        JButton downBtn = new JButton("Down");
        downBtn.addActionListener(e -> moveSelectedIncludePath(1));
        buttonsPanel.add(addBtn);
        buttonsPanel.add(removeBtn);
        buttonsPanel.add(editBtn);
        buttonsPanel.add(upBtn);
        buttonsPanel.add(downBtn);
        includePathsPanel.add(buttonsPanel, BorderLayout.EAST);

        // Auto-detect hints
        String autoAzslc = AzslcSettings.autoDetectAzslc();
        String azslcHint = autoAzslc != null
                ? "Leave empty to use auto-detected: " + autoAzslc
                : "Not found in ~/.o3de/3rdParty/packages/azslc-*/azslc/bin/Release/azslc or PATH";

        String autoCpp = AzslcSettings.autoDetectCpp();
        String cppHint = autoCpp != null
                ? "Leave empty to use auto-detected: " + autoCpp
                : "Not found in PATH. Windows: install MSYS2 (pacman -S mingw-w64-x86_64-cpp)";

        return FormBuilder.createFormBuilder()
                .addLabeledComponent("azslc executable:", azslcPathField)
                .addComponentToRightColumn(new JLabel("<html><small>" + azslcHint + "</small></html>"))
                .addLabeledComponent("O3DE Engine root:", o3deEnginePathField)
                .addComponentToRightColumn(new JLabel("<html><small>Required for ShaderLib includes (Gems/Atom/.../ShaderLib)</small></html>"))
                .addLabeledComponent("O3DE Project root (optional):", o3deProjectPathField)
                .addComponentToRightColumn(new JLabel("<html><small>For project ShaderLib and gem-local includes</small></html>"))
                .addLabeledComponent("cpp (C preprocessor):", cppPathField)
                .addComponentToRightColumn(new JLabel("<html><small>" + cppHint + "</small></html>"))
                .addLabeledComponent("Validation mode:", validationModeCombo)
                .addComponentToRightColumn(new JLabel("<html><small>SYNTAX=fast, SEMANTIC=full (default), FULL=all dumps</small></html>"))
                .addComponent(enableValidationCheckBox)
                .addComponent(validateOnSaveOnlyCheckBox)
                .addLabeledComponent("Additional include paths:", includePathsPanel)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private void addIncludePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Add Include Path");
        if (chooser.showOpenDialog(includePathsList) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!includePathsModel.contains(path)) {
                includePathsModel.addElement(path);
            }
        }
    }

    private void removeSelectedIncludePath() {
        int idx = includePathsList.getSelectedIndex();
        if (idx >= 0) {
            includePathsModel.remove(idx);
        }
    }

    private void editSelectedIncludePath() {
        int idx = includePathsList.getSelectedIndex();
        if (idx >= 0) {
            String oldPath = includePathsModel.get(idx);
            JFileChooser chooser = new JFileChooser(oldPath);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Edit Include Path");
            if (chooser.showOpenDialog(includePathsList) == JFileChooser.APPROVE_OPTION) {
                String newPath = chooser.getSelectedFile().getAbsolutePath();
                if (!includePathsModel.contains(newPath)) {
                    includePathsModel.set(idx, newPath);
                }
            }
        }
    }

    private void moveSelectedIncludePath(int delta) {
        int idx = includePathsList.getSelectedIndex();
        if (idx >= 0) {
            int newIdx = idx + delta;
            if (newIdx >= 0 && newIdx < includePathsModel.size()) {
                String item = includePathsModel.get(idx);
                includePathsModel.remove(idx);
                includePathsModel.add(newIdx, item);
                includePathsList.setSelectedIndex(newIdx);
            }
        }
    }

    @Override
    public boolean isModified() {
        AzslcSettings settings = AzslcSettings.getInstance();
        List<String> currentIncludes = new ArrayList<>(includePathsModel.size());
        for (int i = 0; i < includePathsModel.size(); i++) {
            currentIncludes.add(includePathsModel.get(i));
        }

        return !azslcPathField.getText().equals(settings.getAzslcPath())
                || !o3deEnginePathField.getText().equals(settings.getO3deEnginePath())
                || !o3deProjectPathField.getText().equals(settings.getO3deProjectPath())
                || !cppPathField.getText().equals(settings.getCppPath())
                || validationModeCombo.getSelectedItem() != settings.getValidationMode()
                || enableValidationCheckBox.isSelected() != settings.isEnableValidation()
                || validateOnSaveOnlyCheckBox.isSelected() != settings.isValidateOnSaveOnly()
                || !currentIncludes.equals(settings.getAdditionalIncludePaths());
    }

    @Override
    public void apply() {
        AzslcSettings settings = AzslcSettings.getInstance();
        settings.setAzslcPath(azslcPathField.getText());
        settings.setO3deEnginePath(o3deEnginePathField.getText());
        settings.setO3deProjectPath(o3deProjectPathField.getText());
        settings.setCppPath(cppPathField.getText());
        settings.setValidationMode((AzslcSettings.ValidationMode) validationModeCombo.getSelectedItem());
        settings.setEnableValidation(enableValidationCheckBox.isSelected());
        settings.setValidateOnSaveOnly(validateOnSaveOnlyCheckBox.isSelected());

        List<String> includes = new ArrayList<>(includePathsModel.size());
        for (int i = 0; i < includePathsModel.size(); i++) {
            includes.add(includePathsModel.get(i));
        }
        settings.setAdditionalIncludePaths(includes);
    }

    @Override
    public void reset() {
        AzslcSettings settings = AzslcSettings.getInstance();
        azslcPathField.setText(settings.getAzslcPath());
        o3deEnginePathField.setText(settings.getO3deEnginePath());
        o3deProjectPathField.setText(settings.getO3deProjectPath());
        cppPathField.setText(settings.getCppPath());
        validationModeCombo.setSelectedItem(settings.getValidationMode());
        enableValidationCheckBox.setSelected(settings.isEnableValidation());
        validateOnSaveOnlyCheckBox.setSelected(settings.isValidateOnSaveOnly());

        includePathsModel.clear();
        for (String p : settings.getAdditionalIncludePaths()) {
            includePathsModel.addElement(p);
        }
    }
}