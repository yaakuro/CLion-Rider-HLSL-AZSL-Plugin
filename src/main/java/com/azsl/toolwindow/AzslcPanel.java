package com.azsl.toolwindow;

import com.azsl.project.AzslProjectPaths;
import com.azsl.validation.AzslcSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.Disposable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Tool window panel that runs azslc in various inspection modes on the current AZSL file. */
public class AzslcPanel extends JPanel implements Disposable {

    private final Project project;
    private final JTextArea output = new JTextArea();

    public AzslcPanel(Project project) {
        super(new BorderLayout());
        this.project = project;

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(button("Validate", AzslcRunner.Mode.SEMANTIC));
        toolbar.add(button("SRG layout", AzslcRunner.Mode.SRG));
        toolbar.add(button("Options", AzslcRunner.Mode.OPTIONS));
        toolbar.add(button("Entry points", AzslcRunner.Mode.ENTRY_POINTS));
        toolbar.add(button("Bindings", AzslcRunner.Mode.BINDING_DEP));

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        output.setText("Select an AZSL file and choose an azslc command.");

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
    }

    private JButton button(String label, String flag) {
        JButton b = new JButton(label);
        b.addActionListener(e -> run(flag));
        return b;
    }

    private void run(String flag) {
        VirtualFile file = currentAzslFile();
        if (file == null) {
            output.setText("No AZSL file is selected in the editor.");
            return;
        }

        AzslcSettings settings = AzslcSettings.getInstance();
        String azslcPath = settings.getResolvedAzslcPath();
        String cppPath = settings.getResolvedCppPath();

        String content;
        Document doc = FileDocumentManager.getInstance().getDocument(file);
        if (doc != null) {
            content = doc.getText();
        } else {
            try {
                content = new String(file.contentsToByteArray(), file.getCharset());
            } catch (IOException ex) {
                output.setText("Could not read file: " + ex.getMessage());
                return;
            }
        }

        List<String> roots = new ArrayList<>();
        AzslProjectPaths paths = AzslProjectPaths.getInstance(project);
        if (paths != null) {
            for (VirtualFile root : paths.getIncludeRoots(file)) {
                roots.add(root.getPath());
            }
        }

        String fileName = file.getName();
        output.setText("Running: azslc " + flag + " " + fileName + " ...");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String result = AzslcRunner.run(azslcPath, cppPath, fileName, content, roots, flag);
            ApplicationManager.getApplication().invokeLater(() -> {
                output.setText(result);
                output.setCaretPosition(0);
            });
        });
    }

    private VirtualFile currentAzslFile() {
        VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
        for (VirtualFile f : files) {
            String ext = f.getExtension();
            if (ext != null && (ext.equals("azsl") || ext.equals("azsli") || ext.equals("srgi"))) {
                return f;
            }
        }
        return files.length > 0 ? files[0] : null;
    }

    @Override
    public void dispose() {
        // nothing to release
    }
}
