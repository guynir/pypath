package guynir.pypath;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import guynir.pypath.container.ServiceContainer;
import guynir.pypath.managers.SourceFoldersManager;
import guynir.pypath.services.VfsService;
import org.assertj.core.api.Assertions;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Plugin integration tests for file-system changes (creation, renaming and deletion of files) and listing file updates.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
@SuppressWarnings({"SameParameterValue"})
public class PluginIntegrationTest extends BasePlatformTestCase {

    /**
     * Collection of helper functions for testing.
     */
    private PluginTestHelper testHelper;

    /**
     * VFS service.
     */
    private VfsService vfsService;

    /**
     * Source folder manager.
     */
    private SourceFoldersManager sourceFoldersManager;

    /**
     * Test fixture - executed before every test.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.testHelper = new PluginTestHelper(getProject(), this.myFixture.getTempDirFixture());

        // Create empty listing file.
        VirtualFile listingFile = this.myFixture.getTempDirFixture().createFile(SourceFoldersManager.SOURCE_DIR_FILE, "");

        // Get access to plugin services and components.
        ServiceContainer container = ServiceManager.getService(getProject(), ServiceContainer.class);
        vfsService = container.vfsService;
        sourceFoldersManager = container.sourceFoldersManager;
    }

    /**
     * Test fixture -- cleanup after every test.
     */
    @Override
    protected void tearDown() throws Exception {
        // Remove all previously generated source folders.
        sourceFoldersManager.getManagedFolders().forEach(sourceFoldersManager::removeSourceFolder);

        // Delete files created during test.
        testHelper.managedResourceCleanup();

        super.tearDown();
    }

    /**
     * Test that an empty listing file should result no source folders.
     */
    public void testShouldResultInRootSourceFolderOnly() {
        assertFolders();
    }

    /**
     * Test that adding a non-existing folder to listing file does not affect the current state.
     */
    public void testShouldResultInRootSourceFolderOnlyWhenNonExistingFolderIsSpecified() {
        // Create listing file with a non existing folder -- src2.
        testHelper.writeListingFile("src2");

        // Assert.
        assertFolders();
    }

    /**
     * Test that adding a new folder included in the listing file will mark it as source folder.
     */
    public void testShouldAddNewSourceFolderAfterFolderCreation() {
        // Create listing file with a non-existing folder.
        testHelper.writeListingFile("src2");

        // Create the new folder. A refresh process should be triggered by Intellij platform and our plugin should
        // register this folder as 'source folder'.
        testHelper.createDirectory("src2");

        // Assert.
        assertFolders("src2");
    }

    /**
     * Test should mark an existing folder as source folder.
     */
    public void testShouldAddNewSourceFolderAfterListingFileUpdate() {
        // Create a new folder.
        testHelper.createDirectory("src2");

        // Update listing file. This should cause a refresh and registration of existing 'src2' folder as source
        // folder.
        testHelper.writeListingFile("src2");

        // Assert.
        assertFolders("src2");
    }

    /**
     * Test should remove a managed folder after it is renamed to an unmanaged folder.
     */
    public void testShouldRemoveSourceFolderAfterRenameToUnlisted() {
        // New source folder.
        testHelper.createDirectory("src2");
        testHelper.writeListingFile("src2");

        // Rename directory. This should cause 'src3' to become non-source-folder, as it is not registered in the
        // listing file.
        testHelper.renameDirectory("src2", "src3");

        // Assert.
        assertFolders();
    }

    /**
     * Test that after renaming a managed folder to a new name (which is also a managed folder), the folder remains
     * marked as 'source folder'.
     */
    public void testShouldNotChangeManagedFolderStateAfterRename() {
        // Add source folder and listing file.
        testHelper.createDirectory("src2");
        testHelper.writeListingFile("src2", "src3");

        // Rename managed source folder to a new name, which is also managed folder.
        testHelper.renameDirectory("src2", "src3");

        // Assert that after renaming, the new folder is still managed.
        assertFolders("src3");
    }

    /**
     * Test that after deleting a managed source folder, it is no longer exists.
     * <p>
     * NOTE: There's a bug in Intellij platform that does not remove the source folder from its content entry.
     */
    public void _testShouldResultInNoManagedFolders() {
        // Add source folder and listing file.
        testHelper.createDirectory("managed_dir");
        testHelper.writeListingFile("managed_dir");

        testHelper.deleteDirectory("managed_dir");

        assertFolders();
    }

    /**
     * Test that after renaming a managed folder into unmanaged folder, the source folder marking is removed.
     */
    public void testShouldRemoveManagedFolderAfterRename() {
        testHelper.writeListingFile("managed_src_dir");
        testHelper.createDirectory("managed_src_dir");

        testHelper.renameDirectory("managed_src_dir", "unmanaged_src_dir");

        assertFolders("");
    }

    /**
     * Test that after renaming a managed folder to a new name, which is also managed folder, the "source folder"
     * marking is not removed.
     */
    public void testShouldNotChangeManagedFolderStatusAfterRename() {
        testHelper.writeListingFile("managed_src_dir_1", "managed_src_dir_2");
        testHelper.createDirectory("managed_src_dir_1");

        testHelper.renameDirectory("managed_src_dir_1", "managed_src_dir_2");

        assertFolders("managed_src_dir_2");
    }

    /**
     *
     */
    public void testShouldMakeFolderManagedAfterRenaming() {
        testHelper.writeListingFile("managed_src_dir");
        testHelper.createDirectory("UNMANAGED_DIR");

        testHelper.renameDirectory("UNMANAGED_DIR", "managed_src_dir");

        assertFolders("managed_src_dir");
    }

    /**
     * Assert that a given list of folder names are registered as source folders.
     *
     * @param names List of names.
     */
    protected void assertFolders(String... names) {
        List<String> folderNames = Arrays.asList(names);
        boolean containsRootFolder = folderNames.contains("") || folderNames.contains("/");

        List<SourceFolder> sourceFolders = getAllSourceFolders();
        Assertions.assertThat(sourceFolders).hasSize(names.length + (containsRootFolder ? 0 : 1));

        for (String folderName : names) {
            Assertions.assertThat(contains(folderName, sourceFolders)).isTrue();
        }
    }

    /**
     * @return List of all source folders within the project.
     */
    protected List<SourceFolder> getAllSourceFolders() {
        List<SourceFolder> projectSourceFolders = new LinkedList<>();
        Module[] modules = ModuleManager.getInstance(this.getProject()).getModules();
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
     * Check if a given folder name exists within list of source folders.
     *
     * @param folderName    Folder name.
     * @param sourceFolders Source folder to inspect.
     * @return {code true} if <i>folderName</i> found inside <i>sourceFolders</i>, {@code false} if not.
     */
    protected boolean contains(String folderName, Collection<SourceFolder> sourceFolders) {
        String name = vfsService.toURL(folderName);

        for (SourceFolder sourceFolder : sourceFolders) {
            VirtualFile file = sourceFolder.getFile();
            if (file != null && file.getUrl().equals(name)) {
                return true;
            }
        }

        return false;
    }
}
