package guynir.pypath.managers;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import guynir.pypath.PyPathException;
import guynir.pypath.services.VfsService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Listens to virtual file system changes.
 */
public class VfsChangesListenerManager {

    /**
     * Reference to source folder manager (performs the actual work of marking and un-marking source folders).
     */
    private final SourceFoldersManager sourceFoldersManager;

    /**
     * Intellij platform project component.
     */
    private final Project project;

    /**
     * Service for accessing virtual file system resources.
     */
    private final VfsService vfsService;

    /**
     * Name of listing file.
     */
    private String sourceDirsFile;

    /**
     * Class logger.
     */
    private static final Logger logger = Logger.getInstance(VfsChangesListenerManager.class);

    /**
     * Class constructor.
     *
     * @param sourceFoldersManager Reference to source folder manager.
     * @param project              Intellij platform project component.
     * @param vfsService           Service for accessing virtual file system resources.
     */
    public VfsChangesListenerManager(SourceFoldersManager sourceFoldersManager, Project project, VfsService vfsService) {
        this.sourceFoldersManager = sourceFoldersManager;
        this.project = project;
        this.vfsService = vfsService;
    }

    /**
     * Register VFS changes listener.
     */
    public void registerHandlers() {
        logger.info("Registering VFS changes listener.");

        sourceDirsFile = sourceFoldersManager.getSourceFoldersFile();

        BulkFileListener listener = (new BulkFileListener() {

            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                handleChanges(events);
            }
        });

        this.project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, listener);
    }

    /**
     * Perform marking and un-marking of folders based on the file of events occurred.
     *
     * @param events List of events.
     */
    protected void handleChanges(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            if (event instanceof VFileMoveEvent) {
                VFileMoveEvent moveEvent = (VFileMoveEvent) event;
                sourceFoldersManager.handleFolderRenaming(moveEvent.getOldPath(), moveEvent.getNewPath());
            } else if (event instanceof VFilePropertyChangeEvent) {
                VFilePropertyChangeEvent changeEvent = (VFilePropertyChangeEvent) event;
                sourceFoldersManager.handleFolderRenaming(changeEvent.getOldPath(), changeEvent.getNewPath());
            } else if (event instanceof VFileDeleteEvent) {
                sourceFoldersManager.handleFolderDeletion(event.getFile());
            } else {
                // If either our 'source_dirs' file was change or a directory was created -- trigger
                // a refresh.
                VirtualFile file = event.getFile();
                if (file != null) {
                    boolean refresh = file.isDirectory();
                    if (!refresh) {
                        try {
                            refresh = vfsService.isSame(sourceDirsFile, file);
                        } catch (PyPathException ex) {
                            // Ignore exception.
                        }
                    }

                    if (refresh) {
                        sourceFoldersManager.handleDirectoryMarking();
                    }
                }
            }
        }
    }
}
