package guynir.pypath.services;

import guynir.pypath.state.SourceDescriptor;

import java.util.List;

/**
 * Define the interface of state persisting service.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public interface ComponentStateService {

    /**
     * @return The state of the source descriptor listing. If this state is missing, an empty string is returned.
     */
    List<SourceDescriptor> getSourceDescriptorsListing();

    /**
     * Sets (updates) source listing.
     *
     * @param listing List to update. A {@code null} value is equivalent to empty string.
     */
    void setSourceDescriptorListing(List<SourceDescriptor> listing);

}
