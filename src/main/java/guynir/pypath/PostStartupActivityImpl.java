package guynir.pypath;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import guynir.pypath.container.ServiceContainer;
import org.jetbrains.annotations.NotNull;

public class PostStartupActivityImpl implements StartupActivity {

    /**
     * Class logger.
     */
    private static final Logger logger = Logger.getInstance(PostStartupActivityImpl.class);

    @Override
    public void runActivity(@NotNull Project project) {
        logger.info("PyPath plugin startup.");

        ServiceContainer serviceContainer = ServiceManager.getService(project, ServiceContainer.class);
        serviceContainer.init(project);

        logger.info("Root project: '" + project.getName() + "'.");
        logger.info("Root project root: '" + serviceContainer.vfsService.getBaseDirPath() + "'.");

        serviceContainer.sourceFoldersManager.handleDirectoryMarking();

        logger.info("PyPath initialization complete.");
    }

}
