package guynir.pypath.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import guynir.pypath.PyPathException;
import guynir.pypath.utils.Asserts;

/**
 * This service mediates between Intellij platform SDK virtual file system (VFS) and this plugin implementation. The
 * Intellij VFS spans over multiple implementation, such as {@link com.intellij.openapi.vfs.LocalFileSystem},
 * {@link com.intellij.openapi.vfs.ex.temp.TempFileSystem} (for testing purposes),
 * {@link com.intellij.openapi.vfs.JarFileSystem} and more.<p>
 * <p>
 * This service intend to abstract the plugin file-system requirements from VFS complexity.
 */
public class VfsService {

    /**
     * Path to listing file.
     */
    public static final String LISTING_FILE_NAME = "source_dirs";

    /**
     * Intellij virtual file system.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final VirtualFileSystem vfs;

    /**
     * Reference to project object.
     */
    private final Project project;

    /**
     * Workspace / base-directory.<p>
     * <b>NOTE:</b> Intellij platform does not have an exact concept of "base-directory". It is an attempt to represent
     * the workspace directory.
     */
    private VirtualFile baseDir;

    /**
     * Base directory path, represented as string.
     */
    private String baseDirPath;

    /**
     * Class logger.
     */
    private static final Logger logger = Logger.getInstance(VfsService.class);

    /**
     * Class constructor.
     *
     * @param vfs     Virtual file system.
     * @param project Project component.
     */
    private VfsService(VirtualFileSystem vfs, Project project) {
        this.vfs = vfs;
        this.project = project;
    }

    /**
     * Factory method to create new service.
     *
     * @param vfs     Virtual file system implementation.
     * @param project Project component.
     * @return Newly initialized service.
     * @throws IllegalArgumentException If any argument is missing.
     */
    public static VfsService create(VirtualFileSystem vfs, Project project) throws IllegalArgumentException {
        Asserts.notNull(vfs, "Virtual file system cannot be null.");
        Asserts.notNull(project, "Project cannot be null.");

        VfsService service = new VfsService(vfs, project);
        service.init();
        return service;
    }

    /**
     * @return Base directory path, in string representation.
     */
    public String getBaseDirPath() {
        return baseDirPath;
    }

    /**
     * Convert a path of a file to Intellij platform {@code VirtualFile}. The file must exist. The path is considered
     * relative to workspace base directory.
     *
     * @param path Relative path to file.
     * @return {@code VirtualFile} representing the file.
     * @throws FileNotFoundException If file could not be found.
     */
    public VirtualFile toVFile(String path) throws FileNotFoundException {
        Asserts.notNull(path, "Path cannot be null.");
        VirtualFile vf = baseDir.findFileByRelativePath(path);
        if (vf == null) {
            throw new FileNotFoundException("File not found in workspace: " + path);
        }
        return vf;
    }

    /**
     * Determine if the given two virtual files reference the same file.
     *
     * @param file1 First file.
     * @param file2 Second file.
     * @return {@code true} if both files reference the same file, {@code false} if not.
     * @throws IllegalArgumentException If either arguments are {@code null}.
     */
    public boolean isSame(VirtualFile file1, VirtualFile file2) throws IllegalArgumentException {
        Asserts.notNull(file1, "First file cannot be null.");
        Asserts.notNull(file2, "Second file cannot be null.");

        return file1.getUrl().equals(file2.getUrl());
    }

    /**
     * Determine if two given files are the same.
     *
     * @param file1 First file represented as localized workspace path (relative to project's base directory).
     * @param file2 Second file to compare to.
     * @return {@code true} if both references the same file, {@code false} if not.
     * @throws IllegalArgumentException If either arguments are {@code null}.
     */
    public boolean isSame(String file1, VirtualFile file2) throws FileNotFoundException, IllegalArgumentException {
        return isSame(toVFile(file1), file2);
    }

    /**
     * Determines if a given virtual file references the plugin's listing file (e.g.: {@link #LISTING_FILE_NAME}).
     *
     * @param file File to evaluate.
     * @return {@code true} if file reference listing file, {@code false} if not.
     * @throws IllegalArgumentException If <i>file</i> is {@code null}.
     */
    public boolean isListingFile(VirtualFile file) throws IllegalArgumentException {
        Asserts.notNull(file, "File cannot be null.");
        String name = file.getName();
        return LISTING_FILE_NAME.equals(name);
    }

    /**
     * Attempt to convert an absolute path to workspace relative path, e.g.:
     * <code>
     * /home/user/IdeaProject/helloWorld/src/main/java/App.java -> src/main/java/App.java
     * </code>
     * when workspace is <i>/home/user/IdeaProject/helloWorld</i>
     *
     * @param path Absolute path to convert.
     * @return Project relative path.
     * @throws PyPathException If path is not within workspace.
     */
    public String localizePath(String path) throws PyPathException {
        Asserts.notNull(path, "Path cannot be null.");

        // If given path start with the same sequence as our base-dir -- it's a file within our
        // workspace.
        if (path.startsWith(baseDirPath)) {
            return path.substring(baseDirPath.length());
        }

        // If path is exactly our base dir (e.g.: when base dir is '/home/user/workspace/' and the path is
        // either '/home/user/workspace/' or '/home/user/workspace' -- it's a match.
        if (path.equals(baseDirPath) || path.equals(baseDirPath.substring(0, baseDirPath.length() - 1))) {
            return "";
        }

        throw new PyPathException("Path is not within workspace: " + path);
    }

    /**
     * Attempt to convert an virtual file to workspace relative path, e.g.:
     * <code>
     * /home/user/IdeaProject/helloWorld/src/main/java/App.java -> src/main/java/App.java
     * </code>
     * when workspace is <i>/home/user/IdeaProject/helloWorld</i>
     *
     * @param path Absolute path to convert.
     * @return Project relative path.
     * @throws PyPathException If path is not within workspace.
     */
    public String localizePath(VirtualFile path) throws PyPathException {
        Asserts.notNull(path, "Path cannot be null.");
        return localizePath(path.getPath());
    }

    /**
     * Expand a local path to global path by prepending base directory path to given local resource path.
     *
     * @param localPath Local path to expand.
     * @return Absolute path of resource.
     * @throws IllegalArgumentException If <i>localPath</i> is {code null}.
     */
    public String expandPath(String localPath) throws IllegalArgumentException {
        Asserts.notNull(localPath, "Local path cannot be null.");

        if (localPath.startsWith("/")) {
            localPath = localPath.substring(1);
        }

        if (localPath.endsWith("/")) {
            localPath = localPath.substring(0, localPath.length() - 1);
        }
        return baseDirPath + localPath;
    }

    public String toURL(String relativePath) throws IllegalArgumentException {
        Asserts.notNull(relativePath, "Relative path cannot be null.");

        //
        // Normalize path.
        //
        StringBuilder path = new StringBuilder(relativePath.trim());

        // Remove double-slashes (e.g.: '//' -> '/').
        int index;
        while ((index = path.indexOf("//")) >= 0) {
            path.deleteCharAt(index);
        }

        // Remove leading slash, if any.
        if (path.length() > 0 && path.charAt(0) == '/') {
            path.deleteCharAt(0);
        }

        // Remove trailing slash, if any.
        if (path.length() > 0 && path.charAt(path.length() - 1) == '/') {
            path.deleteCharAt(path.length() - 1);
        }

        if (path.length() > 0) {
            path.insert(0, '/');
        }

        String url = baseDir.getUrl();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url + path.toString();
    }

    /**
     * Initialize the service by determining the workspace base directory.
     */
    protected void init() {
        baseDir = ProjectUtil.guessProjectDir(project);
        if (baseDir == null) {
            throw new IllegalStateException("Could not detecet project root directory.");
        }
        baseDirPath = baseDir.getPath();
        if (!baseDirPath.endsWith("/")) {
            baseDirPath = baseDirPath + "/";
        }

        logger.info("Project base directory (workspace dir): " + baseDirPath);
    }

}
