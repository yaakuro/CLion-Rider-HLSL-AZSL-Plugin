package com.azsl.validation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service(Service.Level.APP)
@State(name = "AzslcSettings", storages = @Storage("azslcSettings.xml"))
public final class AzslcSettings implements PersistentStateComponent<AzslcSettings.State> {

    public enum ValidationMode {
        SYNTAX,      // azslc --syntax (fast)
        SEMANTIC,    // azslc --semantic (full)
        FULL         // azslc --full
    }

    public static class State {
        public String azslcPath = "";
        public String o3deEnginePath = "";
        public String o3deProjectPath = "";
        public List<String> additionalIncludePaths = new ArrayList<>();
        public ValidationMode validationMode = ValidationMode.SEMANTIC;
        public boolean enableValidation = true;
        public boolean validateOnSaveOnly = false;
        public String cppPath = "";
    }

    private State state = new State();

    public static AzslcSettings getInstance() {
        return ApplicationManager.getApplication().getService(AzslcSettings.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public String getAzslcPath() { return state.azslcPath; }
    public void setAzslcPath(String path) { state.azslcPath = path; }

    public String getO3deEnginePath() { return state.o3deEnginePath; }
    public void setO3deEnginePath(String path) { state.o3deEnginePath = path; }

    public String getO3deProjectPath() { return state.o3deProjectPath; }
    public void setO3deProjectPath(String path) { state.o3deProjectPath = path; }

    public List<String> getAdditionalIncludePaths() { return state.additionalIncludePaths; }
    public void setAdditionalIncludePaths(List<String> paths) { state.additionalIncludePaths = paths; }

    public ValidationMode getValidationMode() { return state.validationMode; }
    public void setValidationMode(ValidationMode mode) { state.validationMode = mode; }

    public boolean isEnableValidation() { return state.enableValidation; }
    public void setEnableValidation(boolean enable) { state.enableValidation = enable; }

    public boolean isValidateOnSaveOnly() { return state.validateOnSaveOnly; }
    public void setValidateOnSaveOnly(boolean validateOnSaveOnly) { state.validateOnSaveOnly = validateOnSaveOnly; }

    public String getCppPath() { return state.cppPath; }
    public void setCppPath(String path) { state.cppPath = path; }

    /**
     * Returns the resolved azslc path: user setting if non-empty, otherwise auto-detected.
     */
    public @Nullable String getResolvedAzslcPath() {
        String path = state.azslcPath;
        if (path != null && !path.isBlank()) {
            return path;
        }
        return autoDetectAzslc();
    }

    /**
     * Try to find azslc in common O3DE install locations.
     */
    public static @Nullable String autoDetectAzslc() {
        String home = System.getProperty("user.home");
        if (home == null) return null;

        // Check ~/.o3de/3rdParty/packages/azslc-*/azslc/bin/Release/azslc
        Path o3deThirdParty = Path.of(home, ".o3de", "3rdParty", "packages");
        if (Files.isDirectory(o3deThirdParty)) {
            try (Stream<Path> versions = Files.list(o3deThirdParty)) {
                return versions
                        .filter(p -> p.getFileName().toString().startsWith("azslc-"))
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(v -> {
                            String osName = System.getProperty("os.name").toLowerCase();
                            if (osName.contains("win")) {
                                return v.resolve("azslc").resolve("bin").resolve("Release").resolve("azslc.exe");
                            } else {
                                return v.resolve("azslc").resolve("bin").resolve("Release").resolve("azslc");
                            }
                        })
                        .filter(Files::isRegularFile)
                        .map(Path::toString)
                        .findFirst()
                        .orElse(null);
            } catch (Exception ignored) {}
        }

        // Check PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String exeName = System.getProperty("os.name").toLowerCase().contains("win") ? "azslc.exe" : "azslc";
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File f = new File(dir, exeName);
                if (f.isFile() && f.canExecute()) return f.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Try to find cpp (preprocessor) executable.
     */
    public @Nullable String getResolvedCppPath() {
        String path = state.cppPath;
        if (path != null && !path.isBlank()) {
            return path;
        }
        return autoDetectCpp();
    }

    public static @Nullable String autoDetectCpp() {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;

        String exeName = System.getProperty("os.name").toLowerCase().contains("win") ? "cpp.exe" : "cpp";
        for (String dir : pathEnv.split(File.pathSeparator)) {
            File f = new File(dir, exeName);
            if (f.isFile() && f.canExecute()) return f.getAbsolutePath();
        }
        return null;
    }
}