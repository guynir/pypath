package guynir.pypath.managers;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import guynir.pypath.PyPathException;
import guynir.pypath.services.ComponentStateService;
import guynir.pypath.services.FileNotFoundException;
import guynir.pypath.services.VfsService;
import guynir.pypath.state.SourceDescriptor;
import guynir.pypath.state.SourceType;
import guynir.pypath.utils.Asserts;
import guynir.pypath.utils.ObjectUtils;
import guynir.pypath.utils.TriConsumer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This service performs the actual process of marking/un-marking folders as source/test roots.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public class SourceFoldersManager {

    /**
     * Intellij platform project component.
     */
    private final Project project;

    /**
     * Manages persistent state (configuration) via Intellij platform.
     */
    private final ComponentStateService stateService;

    /**
     * Service for accessing Intellij platform SDK Virtual File System.
     */
    private final VfsService vfsService;

    /**
     * List of managed folders (as source folders).
     */
    private final Collection<VirtualFile> managedFolders = new LinkedList<>();
    /**
     * Name of file containing listing of source directories, relative to workspace root.
     */
    public static final String SOURCE_DIR_FILE = "/source_dirs";

    /**
     * Class logger.
     */
    private static final Logger logger = Logger.getInstance(SourceFoldersManager.class);

    /**
     * Class constructor.
     *
     * @param project      Intellij project component.
     * @param stateService Component's state manager.
     * @param vfsService   Provide access to Intellij platform VFS.
     */
    public SourceFoldersManager(Project project, ComponentStateService stateService, VfsService vfsService) {
        Asserts.notNull(project, "Project cannot be null.");
        Asserts.notNull(stateService, "Component state service cannot be null.");
        Asserts.notNull(vfsService, "VFS service cannot be null.");

        this.project = project;
        this.vfsService = vfsService;
        this.stateService = stateService;
    }

    /**
     * @return The file representing the file containing source folders listing.
     */
    public String getSourceFoldersFile() {
        return SOURCE_DIR_FILE;
    }

    /**
     * Loads and parse sources file. The loader will skip empty lines (lines that are actually empty or contains
     * spaces/tabs) and comment lines (lines starting with "#" or "//").<p>
     * The loader will also adjust the paths by removing leading slash, if exists (e.g.: <i>/src_dir</i> will become
     * <i>src_dir</i>).
     *
     * @param listingFile Listing file to read from.
     * @return List of source descriptors.
     * @throws PyPathException If file could not be opened, read of there was an error parsing the file.
     */
    public List<SourceDescriptor> loadListings(String listingFile) throws PyPathException {
        Asserts.notNull(listingFile, "Listing file cannot be null.");
        VirtualFile source;
        try {
            source = vfsService.toVFile(ObjectUtils.normalizePath(listingFile));
        } catch (FileNotFoundException ex) {
            // If file does not exists, return empty list.
            return Collections.emptyList();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.getInputStream()))) {
            Predicate<String> filterLines = str -> !str.isEmpty() && !str.startsWith("#") && !str.startsWith("//");
            return reader
                    .lines()                          // Read all lines from file.
                    .map(String::trim)                // Trim lines (remove leading and trailing spaces).
                    .filter(filterLines)              // Filter empty and comment lines.
                    .map(ObjectUtils::normalizePath)  // Normalize paths.
                    .map(line -> new SourceDescriptor(SourceType.SOURCE_ROOT, line))
                    .collect(Collectors.toList());    // Collect to a java.util.List.
        } catch (IOException ex) {
            throw new PyPathException("Failed to read source dirs file: " + source.getCanonicalPath());
        }
    }

    /**
     * Based on source directory file (configuration file) and current state of managed sources, this method will
     * mark all required source folders and un-mark deprecated ones.<p>
     * Once complete, the new state is persisted via Intellij platform SDK component state service.
     */
    public void handleDirectoryMarking() {
        // Load list of folders to mark as "source folders".
        List<SourceDescriptor> listOfExpectedSources = loadListings(SOURCE_DIR_FILE);
        handleDirectoryMarking(listOfExpectedSources);
    }

    /**
     * Based on source directory file (configuration file) and current state of managed sources, this method will
     * mark all required source folders and un-mark deprecated ones.<p>
     * Once complete, the new state is persisted via Intellij platform SDK component state service.
     *
     * @param listOfExpectedSources List of directories expected to be marked as "sources".
     */
    public void handleDirectoryMarking(List<SourceDescriptor> listOfExpectedSources) {
        // Get list of previously managed sources.
        List<SourceDescriptor> listOfManagedSources = this.stateService.getSourceDescriptorsListing();

        // Mark/un-mark source folders.
        handleDirectoryMarking(listOfExpectedSources, listOfManagedSources);

        // Persist list of managed sources.
        this.stateService.setSourceDescriptorListing(listOfExpectedSources);
    }

    /**
     * Handles a special case of a folder renaming. If the folder is marked as "source folder" and is managed by
     * this plugin, it should be examined if it should still be managed on not, based on the source dirs configuration.
     */
    public void handleFolderRenaming(String oldFolder, String newFolder) {
        Asserts.notNull(oldFolder, "Old folder file cannot be null.");
        Asserts.notNull(newFolder, "New folder file cannot be null.");

        // Just to be on the safe side -- we're normalizing the old/new paths.
        oldFolder = vfsService.localizePath(oldFolder);
        newFolder = vfsService.localizePath(newFolder);

        List<SourceDescriptor> existingFolders = this.stateService.getSourceDescriptorsListing();
        List<String> names = existingFolders.stream().map(d -> d.pathname).collect(Collectors.toList());

        // Apply changes on new file, if needed.
        VirtualFile newFile = vfsService.toVFile(newFolder);
        String oldFileAbsolutePath = vfsService.expandPath(oldFolder);
        if (names.contains(oldFolder) && !names.contains(newFolder)) {
            removeSourceFolder(newFile);
            managedFolders.remove(newFile);
        } else if (!names.contains(oldFolder) && names.contains(newFolder)) {
            addSourceFolder(newFile);
            managedFolders.add(newFile);
        }
    }

    public void handleFolderDeletion(VirtualFile folder) {
        Asserts.notNull(folder, "Folder cannot be null.");

        String folderName = vfsService.localizePath(folder.getPath());
        List<SourceDescriptor> existingFolders = this.stateService.getSourceDescriptorsListing();
        List<String> names = existingFolders.stream().map(d -> d.pathname).collect(Collectors.toList());
        if (names.contains(folderName)) {
            removeSourceFolder(folder);
        }
    }

    /**
     * @return List of all directories representing managed source folders.
     */
    public Collection<VirtualFile> getManagedFolders() {
        return Collections.unmodifiableCollection(managedFolders);
    }

    /**
     * Lookup source folder representing a file.
     *
     * @param directory Directory to resolve fo {@code SourceFolder}.
     * @return Source folder representing the <i>directory</i> if relevant, or {@link Optional#empty()} is none.
     */
    public Optional<SourceFolder> getSourceFolder(VirtualFile directory) {
        Optional<ContentEntry[]> entries = getModuleContentEntries(directory);
        return entries.map(e -> {
            for (ContentEntry entry : e) {
                for (SourceFolder folder : entry.getSourceFolders()) {
                    if (directory.equals(folder.getFile())) {
                        return folder;
                    }
                }
            }
            return null;
        });
    }

    /**
     * Register a directory as a source folder.
     *
     * @param directory Directory to mark.
     */
    public void addSourceFolder(VirtualFile directory) {
        getModifiableRootModel(directory).ifPresent(model -> Arrays.stream(model.getContentEntries()).forEach(entry -> {
            if (entry.getFile() != null && VfsUtil.isAncestor(entry.getFile(), directory, true)) {
                WriteAction.run(() -> {
                    entry.addSourceFolder(directory, false);
                    model.commit();
                });
            }
        }));
    }

    /**
     * Unregister a source folder from being a source folder.
     *
     * @param directory Directory to un-mark.
     */
    public void removeSourceFolder(VirtualFile directory) {
        Asserts.notNull(directory, "Directory cannot be null.");

        Module module = ModuleUtil.findModuleForFile(directory, project);
        if (module != null) {
            ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
            for (ContentEntry entry : model.getContentEntries()) {
                for (SourceFolder sourceFolder : entry.getSourceFolders()) {
                    if (directory.equals(sourceFolder.getFile())) {
                        WriteAction.run(() -> {
                            entry.removeSourceFolder(sourceFolder);
                            model.commit();
                        });
                        return;
                    }
                }
            }
        }
    }

    /**
     * Given list of expected source folders and existing source folders, this method will mark expected folders as
     * "source" and will un-mark deprecated source folders.
     *
     * @param expected List of expected source folders.
     * @param actual   List of currently managed as source folders.
     */
    protected void handleDirectoryMarking(Collection<SourceDescriptor> expected, Collection<SourceDescriptor> actual) {
        // Calculate the list of source folder currently marked and expected to be removed.
        Collection<SourceDescriptor> forRemoval = new LinkedList<>(actual);
        forRemoval.removeAll(expected);

        // Add new source folders.
        processDescriptors(expected, (contentEntry, modifiableRootModel, file) -> {
            contentEntry.addSourceFolder(file, false);
            managedFolders.add(file);
        });

        // Remove deprecated folders marked a source roots.
        processDescriptors(forRemoval, (contentEntry, modifiableRootModel, file) -> {
            for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
                if (file.equals(sourceFolder.getFile())) {
                    contentEntry.removeSourceFolder(sourceFolder);
                    managedFolders.remove(sourceFolder.getFile());
                }
            }
        });
    }

    /**
     * For every given {@code SourceDescriptor}, locate it parent content entry and invoke 'processor' to perform
     * a custom operation.
     *
     * @param sources   List of source to iterate.
     * @param processor A callback to issue for each source.
     */
    protected void processDescriptors(Collection<SourceDescriptor> sources, TriConsumer<ContentEntry, ModifiableRootModel, VirtualFile> processor) {
        for (SourceDescriptor sourceDescriptor : sources) {
            handleSingleSource(sourceDescriptor, processor);
        }
    }

    /**
     * Locate parent content root
     *
     * @param descriptor A descriptor to first content root for.
     * @param processor  Handle that accepts callback with file's content root and mutable model to apply changes.
     */
    protected void handleSingleSource(SourceDescriptor descriptor,
                                      TriConsumer<ContentEntry, ModifiableRootModel, VirtualFile> processor) {
        VirtualFile file;

        try {
            file = vfsService.toVFile(descriptor.pathname);
        } catch (FileNotFoundException ex) {
            logger.info("Skipping non existing file: " + descriptor.pathname);
            return;
        }


        WriteAction.runAndWait(() -> {
            // Find the module this file resides in.
            Module module = ModuleUtil.findModuleForFile(file, project);
            if (module == null) {
                logger.warn("File " + descriptor.pathname + " is not associated with any module.");
                return;
            }

            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);

            ModifiableRootModel model = rootManager.getModifiableModel();

            //
            // Find the first content entry that matches our file location and use it to register
            // a source root.
            //
            for (ContentEntry entry : model.getContentEntries()) {
                if (entry.getFile() != null && VfsUtil.isAncestor(entry.getFile(), file, true)) {
                    processor.accept(entry, model, file);
                    model.commit();
                    return;
                }
            }
        });
    }

    /**
     * Lookup the {@code ModifiableRootModel} where a given <i>file</i> resides in.
     *
     * @param file File to look by.
     * @return Modifiable root model where the <i>file</i> resides at.
     */
    private Optional<ModifiableRootModel> getModifiableRootModel(VirtualFile file) {
        Asserts.notNull(file, "File cannot be null.");

        Module module = ModuleUtil.findModuleForFile(file, project);
        return Optional.ofNullable(module != null ? ModuleRootManager.getInstance(module).getModifiableModel() : null);
    }

    /**
     * Locate all {@code ContentEntry}s found in a module the <i>file</i> resides in.
     *
     * @param file File to look module by.
     * @return List of {@code ContentEntry}s within the <i>files</i>'s module.
     */
    private Optional<ContentEntry[]> getModuleContentEntries(VirtualFile file) {
        return getModifiableRootModel(file).map(ModuleRootModel::getContentEntries);
    }
}
