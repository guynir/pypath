package guynir.pypath;

import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.assertj.core.api.Assertions;

import java.io.IOException;
import java.util.List;

/**
 * This test is intended to demonstrate a bug in Intellij platform, where a folder marked as "source folder" residing in
 * a temporary file system, after being deleted, is still marked as "source folder" (even though the resource does not
 * exists anymore).
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public class SourceFolderBugTest extends BasePlatformTestCase {

    /**
     * Helper class for tests.
     */
    private PluginTestHelper testHelper;

    /**
     * Test fixture -- setup before each test.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.testHelper = new PluginTestHelper(getProject(), this.myFixture.getTempDirFixture());
    }

    /**
     * Test fixture -- cleanup after test is run.
     */
    @Override
    protected void tearDown() throws Exception {
        // We need to create the file in order to access its associated SourceFolder resource.
        // (the file was previously deleted during the test).
        VirtualFile file = myFixture.getTempDirFixture().findOrCreateDir("src2");

        testHelper.executeOnContentEntry(file, contentEntry -> {
            SourceFolder[] sourceFolders = contentEntry.getSourceFolders();
            for (SourceFolder sourceFolder : sourceFolders) {
                if (file.equals(sourceFolder.getFile())) {
                    contentEntry.removeSourceFolder(sourceFolder);
                }
            }
        });

        testHelper.managedResourceCleanup();

        super.tearDown();
    }

    /**
     * Test deletion of a source folder.
     */
    public void testDeletingSourceFolder() throws IOException {
        // Make sure we only have a single (root) source folder.
        List<String> folderNames = testHelper.getAllSourceFoldersNames();
        Assertions.assertThat(folderNames).hasSize(1);
        Assertions.assertThat(folderNames).contains("/src");

        // Create a new folder and mark is as source folder.
        VirtualFile file = myFixture.getTempDirFixture().findOrCreateDir("src2");
        testHelper.executeOnContentEntry(file, contentEntry -> contentEntry.addSourceFolder(file, false));

        // Assert that our folder is marked as "source folder".
        folderNames = testHelper.getAllSourceFoldersNames();
        Assertions.assertThat(folderNames).contains("/src/src2");

        // Delete the source folder.
        testHelper.writeOp(() -> testHelper.getFile("/src2").delete(this));

        // Make sure the deleted folder is no longer managed as "source folder".
        folderNames = testHelper.getAllSourceFoldersNames();
        Assertions.assertThat(folderNames).hasSize(1);
        Assertions.assertThat(folderNames).doesNotContain("/src/src2");
    }

}
