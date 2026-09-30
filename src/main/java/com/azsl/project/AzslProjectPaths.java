package com.azsl.project;

import com.azsl.validation.AzslcSettings;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves O3DE engine/project include roots, using the azslc settings when configured
 * and falling back to auto-detection from the project layout.
 */
@Service(Service.Level.PROJECT)
public final class AzslProjectPaths {

    private static final int MAX_WALK = 12;

    private final Project project;
    private final LocalFileSystem lfs = LocalFileSystem.getInstance();

    public AzslProjectPaths(@SuppressWarnings("unused") Project project) {
        this.project = project;
    }

    public static AzslProjectPaths getInstance(@NotNull Project project) {
        return project.getService(AzslProjectPaths.class);
    }

    /** Ordered include roots applicable to a file (file dir first, then O3DE roots). */
    public @NotNull List<VirtualFile> getIncludeRoots(@Nullable VirtualFile fromFile) {
        Set<VirtualFile> roots = new LinkedHashSet<>();

        if (fromFile != null && fromFile.getParent() != null) {
            roots.add(fromFile.getParent());
        }

        AzslcSettings settings = AzslcSettings.getInstance();
        String engine = nonBlank(settings.getO3deEnginePath());
        if (engine == null) engine = autoDetectEngineRoot();
        if (engine != null) {
            addDir(roots, engine + "/Gems");
            addDir(roots, engine + "/Gems/Atom/Feature/Common/Assets/ShaderLib");
            addDir(roots, engine + "/Gems/Atom/RPI/Assets/ShaderLib");
            addDir(roots, engine + "/Gems/Atom/Feature/Common/Assets/ShaderResourceGroups");
        }

        String proj = nonBlank(settings.getO3deProjectPath());
        if (proj == null) proj = autoDetectProjectRoot();
        if (proj != null) {
            addDir(roots, proj + "/ShaderLib");
            addDir(roots, proj);
        }

        for (String p : settings.getAdditionalIncludePaths()) {
            if (p != null && !p.isBlank()) addDir(roots, p);
        }

        return new ArrayList<>(roots);
    }

    /** Resolve an {@code #include} target to a virtual file, trying the file dir then O3DE roots. */
    public @Nullable VirtualFile resolveInclude(@Nullable VirtualFile fromFile, @NotNull String includePath) {
        String normalized = includePath.replace('\\', '/').trim();
        if (normalized.isEmpty()) return null;

        if (fromFile != null && fromFile.getParent() != null) {
            VirtualFile local = fromFile.getParent().findFileByRelativePath(normalized);
            if (local != null && !local.isDirectory()) return local;
        }

        for (VirtualFile root : getIncludeRoots(fromFile)) {
            VirtualFile found = root.findFileByRelativePath(normalized);
            if (found != null && !found.isDirectory()) return found;
        }
        return null;
    }

    public @Nullable String autoDetectEngineRoot() {
        VirtualFile base = project.getBaseDir();
        if (base == null) return null;
        for (VirtualFile dir = base; dir != null; dir = dir.getParent()) {
            if (dir.findFileByRelativePath("Gems/Atom") != null) {
                return dir.getPath();
            }
        }
        return null;
    }

    public @Nullable String autoDetectProjectRoot() {
        // 1. A directory containing project.json
        for (VirtualFile root : contentRoots()) {
            for (VirtualFile dir = root; dir != null; dir = dir.getParent()) {
                if (dir.findChild("project.json") != null) return dir.getPath();
            }
        }
        // 2. A directory containing ShaderLib/viewsrg.srgi
        for (VirtualFile root : contentRoots()) {
            int depth = 0;
            for (VirtualFile dir = root; dir != null && depth < MAX_WALK; dir = dir.getParent(), depth++) {
                if (dir.findFileByRelativePath("ShaderLib/viewsrg.srgi") != null) return dir.getPath();
            }
        }
        return null;
    }

    private @NotNull List<VirtualFile> contentRoots() {
        List<VirtualFile> roots = new ArrayList<>();
        com.intellij.openapi.roots.ProjectRootManager rootManager =
                com.intellij.openapi.roots.ProjectRootManager.getInstance(project);
        for (VirtualFile root : rootManager.getContentRoots()) {
            roots.add(root);
        }
        VirtualFile base = project.getBaseDir();
        if (base != null && !roots.contains(base)) roots.add(base);
        return roots;
    }

    private void addDir(Set<VirtualFile> out, String path) {
        VirtualFile vf = lfs.findFileByPath(path);
        if (vf != null && vf.isDirectory()) out.add(vf);
    }

    private static @Nullable String nonBlank(@Nullable String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /** Convenience for callers that only have a filesystem path. */
    public @Nullable VirtualFile toVirtualFile(@Nullable String path) {
        if (path == null || path.isBlank()) return null;
        return lfs.findFileByPath(new File(path).getAbsolutePath());
    }
}
