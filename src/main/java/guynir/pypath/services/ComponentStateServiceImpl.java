package guynir.pypath.services;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import guynir.pypath.state.SourceDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Service to persist plugin state via Intellij platform component state persistence service.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
@State(name = "PyPathPlugin", storages = @Storage("pypath.xml"))
public class ComponentStateServiceImpl implements ComponentStateService, PersistentStateComponent<ComponentStateServiceImpl.State> {

    public static class State {

        /**
         * List of all managed source folders.
         */
        public List<SourceDescriptor> descriptors = new LinkedList<>();

        /**
         * Default constructor.
         */
        public State() {
        }
    }

    /**
     * Maintain the state of the plugin.
     */
    private State state = new State();

    /**
     * @return The state of the plugin.
     */
    @Nullable
    @Override
    public State getState() {
        return state;
    }

    /**
     * Loads a new state into this service.
     *
     * @param state State to load (must not be {@code null}).
     */
    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    @Override
    public List<SourceDescriptor> getSourceDescriptorsListing() {
        return this.state.descriptors;
    }

    @Override
    public void setSourceDescriptorListing(List<SourceDescriptor> listing) {
        this.state.descriptors = listing != null ? listing : new LinkedList<>();
    }
}
