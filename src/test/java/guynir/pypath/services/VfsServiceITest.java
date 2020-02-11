package guynir.pypath.services;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import guynir.pypath.PyPathException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link VfsService} service.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public class VfsServiceITest extends BasePlatformTestCase {

    /**
     * Service under test.
     */
    private VfsService service;

    /**
     * Listing file.
     */
    private VirtualFile listingFile;

    /**
     * Sample file.
     */
    private VirtualFile sampleFile;

    /**
     * Path to sample file.
     */
    private static final String SAMPLE_FILE = "sample.dat";

    /**
     * Test fixture -- create listing and sample file before tests.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        TempDirTestFixture tempDir = this.myFixture.getTempDirFixture();

        // Create empty listing file.
        listingFile = tempDir.createFile(VfsService.LISTING_FILE_NAME, "");
        sampleFile = tempDir.createFile(SAMPLE_FILE, "");

        VirtualFileSystem vfs = listingFile.getFileSystem();

        service = VfsService.create(vfs, getProject());
    }

    /**
     * Test conversion of existing file to Intellij platform {@code VirtualFile}.
     */
    public void testConversionOfExistingFile() {
        VirtualFile file = service.toVFile(SAMPLE_FILE);

        assertThat(file).isNotNull();
        assertThat(file.getName()).isEqualTo(SAMPLE_FILE);
    }

    /**
     * Test that an attempt to convert related path of non existing file results in an exception.
     */
    public void testConversionOfNonExistingFile() {
        assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> service.toVFile("non-existing-file"));
    }

    /**
     * Test detection of listing file.
     */
    public void testDetectionOfListingFile() {
        assertThat(service.isListingFile(listingFile)).isTrue();
        assertThat(service.isListingFile(sampleFile)).isFalse();
    }

    /**
     * Test conversion of absolute path pointing to file within our workspace.
     */
    public void testConversionOfAbsolutePathToWorkspacePath() {
        assertThat(service.localizePath("/src/file1.dat")).isEqualTo("file1.dat");
        assertThat(service.localizePath("/src/src/main/java/sample/App.java")).isEqualTo("src/main/java/sample/App.java");
        assertThat(service.localizePath("/src")).isEqualTo("");
        assertThat(service.localizePath("/src/")).isEqualTo("");
    }

    /**
     * Test conversion of relative path to full URL.
     */
    public void testConversionOfUrls() {
        assertThat(service.toURL("")).isEqualTo("temp:///src");
        assertThat(service.toURL("/")).isEqualTo("temp:///src");
        assertThat(service.toURL("/dir1")).isEqualTo("temp:///src/dir1");
        assertThat(service.toURL("dir1/")).isEqualTo("temp:///src/dir1");
        assertThat(service.toURL("/dir1/")).isEqualTo("temp:///src/dir1");
        assertThat(service.toURL("/dir1///dir2//dir3")).isEqualTo("temp:///src/dir1/dir2/dir3");
    }

    /**
     * Test conversion of a file residing outside the scope of our workspace.
     */
    public void testConversionOfNonWorkspacePath() {
        assertThatExceptionOfType(PyPathException.class).isThrownBy(() -> service.localizePath("/tmp/file.dat"));
    }
}
