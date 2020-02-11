package guynir.pypath.services;

import com.intellij.openapi.vfs.VirtualFile;
import guynir.pypath.state.SourceType;
import guynir.pypath.utils.Asserts;
import guynir.pypath.utils.ObjectUtils;

import java.util.Objects;

/**
 * Represents an entity in the system.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public class ManagedFolderEntry {

    /**
     * Intellij platform SDK virtual file representing a source folder.
     */
    public final VirtualFile file;

    /**
     * Type of source folder (e.g.: src, resource, test-src, test-resource).
     */
    public final SourceType type;

    /**
     * Class constructor.
     *
     * @param file Virtual file representing a source folder.
     * @param type Type of source folder.
     * @throws IllegalArgumentException If either arguments are {@code null}.
     */
    public ManagedFolderEntry(VirtualFile file, SourceType type) throws IllegalArgumentException {
        Asserts.notNull(file, "File cannot be null.");
        Asserts.notNull(type, "Entry type cannot be null.");
        this.file = file;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagedFolderEntry that = (ManagedFolderEntry) o;
        return Objects.equals(file, that.file) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, type);
    }

    @Override
    public String toString() {
        return ObjectUtils.toString("[ ", getClass().getSimpleName(), ": { file: ", file.getCanonicalPath(),
                ", type: ", type.name(), " } ]");
    }
}
