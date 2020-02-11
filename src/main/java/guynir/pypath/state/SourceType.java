package guynir.pypath.state;

/**
 * Enumeration of source types.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public enum SourceType {

    /**
     * Represents a standard source root directory.
     */
    SOURCE_ROOT,

    /**
     * Represents resource root directory.
     */
    RESOURCE_ROOT,

    /**
     * Represents source root directory for tests.
     */
    TEST_SOURCE_ROOT,

    /**
     * Represents resource root directory for tests.
     */
    TEST_RESOURCE_ROOT
}
