package guynir.pypath.state;

import java.util.Objects;

/**
 * Describe a source folder (whether it's a source folder, resource folder, main
 */
public class SourceDescriptor {

    /**
     * Type of source folder.
     */
    public SourceType type;

    /**
     * Relative path.
     */
    public String pathname;

    /**
     * Class constructor.
     */
    public SourceDescriptor() {
        this(null, null);
    }

    /**
     * Class constructor.
     *
     * @param type     Type of source.
     * @param pathname Pathname to the source.
     * @throws IllegalArgumentException If either arguments are {@code null}.
     */
    public SourceDescriptor(SourceType type, String pathname) throws IllegalArgumentException {
        this.type = type;
        this.pathname = pathname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceDescriptor that = (SourceDescriptor) o;
        return type == that.type &&
                Objects.equals(pathname, that.pathname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, pathname);
    }

    @Override
    public String toString() {
        return String.format("%s { type: %s, pathname: %s }",
                SourceDescriptor.class.getSimpleName(),
                type != null ? type.name() : "null",
                pathname != null ? pathname : "null");
    }
}
