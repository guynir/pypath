package guynir.pypath;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.util.ThrowableRunnable;
import guynir.pypath.managers.SourceFoldersManager;
import guynir.pypath.services.FileNotFoundException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public class PluginTestHelper {

    /**
     * Intellij platform project component.
     */
    private final Project project;

    /**
     * In-memory VFS implementation for testing.
     */
    private final TempDirTestFixture fixture;

    /**
     * List of created files (required for disposing  during tear down).
     */
    private final Collection<VirtualFile> createdFiles = new HashSet<>();

    /**
     * Class constructor.
     *
     * @param project            Intellij platform project component.
     * @param tempDirTestFixture Temporary VFS for testing.
     */
    public PluginTestHelper(Project project, TempDirTestFixture tempDirTestFixture) {
        this.project = project;
        fixture = tempDirTestFixture;
    }

    /**
     * Write to listing file.
     *
     * @param contents Lines to write.
     */
    public void writeListingFile(String... contents) {
        writeOp(() -> {
            VirtualFile listingFile = getFile(SourceFoldersManager.SOURCE_DIR_FILE);
            try (PrintWriter printer = new PrintWriter(new OutputStreamWriter(listingFile.getOutputStream(this)))) {
                Arrays.stream(contents).forEach(printer::println);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to write to listing file -- source_dirs", ex);
            }
        });
    }

    /**
     * Fetch {@code VirtualFile} representing a file.
     *
     * @param path Path of a file.
     * @return Virtual file.
     * @throws FileNotFoundException If file does not exists.
     */
    public VirtualFile getFile(String path) throws FileNotFoundException {
        VirtualFile vf = this.fixture.getFile(path);
        if (vf == null) {
            throw new FileNotFoundException("File not found: " + path);
        }
        return vf;
    }

    /**
     * Locate content entry a given file resides in, and invoke a callback with it.
     *
     * @param file     File to locate content entry for.
     * @param consumer Callback to issue when content entry found.
     */
    public void executeOnContentEntry(VirtualFile file, Consumer<ContentEntry> consumer) {
        writeOp(() -> {
            Module module = ModuleUtil.findModuleForFile(file, project);
            ModuleRootManager moduleRootManager = module != null ? ModuleRootManager.getInstance(module) : null;
            ModifiableRootModel modifiableRootModel = moduleRootManager != null ? moduleRootManager.getModifiableModel() : null;
            ContentEntry contentEntry = modifiableRootModel != null ? modifiableRootModel.getContentEntries()[0] : null;
            if (contentEntry != null) {
                consumer.accept(contentEntry);
                modifiableRootModel.commit();
            }
        });
    }

    /**
     * @return Names of all source folders.
     */
    public List<String> getAllSourceFoldersNames() {
        return getAllSourceFolders()
                .stream()
                .map(sourceFolder -> sourceFolder.getFile() != null ? sourceFolder.getFile().getPath() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * @return List of all source folders within the project.
     */
    public List<SourceFolder> getAllSourceFolders() {
        List<SourceFolder> projectSourceFolders = new LinkedList<>();
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            ContentEntry[] contentEntries = ModuleRootManager.getInstance(module).getContentEntries();
            for (ContentEntry entry : contentEntries) {
                SourceFolder[] sourceFolders = entry.getSourceFolders();
                projectSourceFolders.addAll(Arrays.asList(sourceFolders));
            }
        }

        return projectSourceFolders;
    }

    /**
     * Execute a write operation in a dedicated thread. Blocks until operation completes.
     *
     * @param runnable Runnable to execute.
     */
    public void writeOp(ThrowableRunnable<Exception> runnable) {
        try {
            WriteAction.runAndWait(runnable);
        } catch (Exception ex) {
            throw new RuntimeException("Write execution error.", ex);
        }
    }

    /**
     * Create a new directory.
     *
     * @param directory Path to directory (relative to workspace root).
     */
    public void createDirectory(String directory) {
        writeOp(() -> {
            try {
                VirtualFile vf = fixture.findOrCreateDir(directory);
                createdFiles.add(vf);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create directory -- " + directory, ex);
            }
        });
    }

    /**
     * Rename existing directory.
     *
     * @param oldName Directory to rename.
     * @param newName New name for directory.
     */
    public void renameDirectory(String oldName, String newName) {
        writeOp(() -> {
            try {
                getFile(oldName).rename(this, newName);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to rename directory from " + oldName + " to " + newName, ex);
            }
        });
    }

    /**
     * Rename existing directory.
     *
     * @param directoryName Directory to delete.
     */
    protected void deleteDirectory(String directoryName) {
        writeOp(() -> {
            try {
                VirtualFile vf = getFile(directoryName);
                vf.delete(this);
                createdFiles.remove(vf);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to delete directory -- " + directoryName, ex);
            }
        });
    }

    /**
     * Remove generated resources, such as files and folders.
     * Typically called during test tear down.
     */
    public void managedResourceCleanup() {
        // Delete files created during test.
        writeOp(() -> createdFiles.forEach(file -> {
            try {
                file.delete(this);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }));
    }


    /**
     * Debug utility for printing VFS tree to the console.
     */
    public void listAllFiles() {
        VfsUtilCore.visitChildrenRecursively(getFile("//"), new VirtualFileVisitor<Object>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                System.out.println((file.isDirectory() ? "[DIR]  " : "[FILE] ") + file.getPath());
                return file.isDirectory();
            }
        });
    }
}
