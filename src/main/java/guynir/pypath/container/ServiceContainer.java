package guynir.pypath.container;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import guynir.pypath.managers.SourceFoldersManager;
import guynir.pypath.managers.VfsChangesListenerManager;
import guynir.pypath.services.ComponentStateService;
import guynir.pypath.services.VfsService;

/**
 * A simple IoC implementation that creates all relevant services and inject references.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public class ServiceContainer {

    /**
     * Maintains our state.
     */
    public ComponentStateService componentStateService;

    /**
     * Manages source-folders.
     */
    public SourceFoldersManager sourceFoldersManager;

    /**
     * Listener for VFS changes.
     */
    public VfsChangesListenerManager changesListenerManager;

    /**
     * Abstraction over Intellij platform SDK VFS.
     */
    public VfsService vfsService;

    /**
     * Class constructor.
     */
    public ServiceContainer() {
    }

    /**
     * Create and setup system components.
     */
    public void init(Project project) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir == null) {
            throw new IllegalStateException("Cannot initialize plugin -- no project root path could be detected.");
        }
        VirtualFileSystem vfs = projectDir.getFileSystem();

        this.vfsService = VfsService.create(vfs, project);

        // State service.
        this.componentStateService = ServiceManager.getService(project, ComponentStateService.class);

        // Source folder management service.
        this.sourceFoldersManager = new SourceFoldersManager(project, this.componentStateService, vfsService);

        // Register VFS change listener.
        this.changesListenerManager = new VfsChangesListenerManager(sourceFoldersManager, project, vfsService);
        this.changesListenerManager.registerHandlers();
    }
}
