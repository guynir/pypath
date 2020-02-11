package guynir.pypath.utils;

import com.intellij.openapi.vfs.StandardFileSystems;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

public class ObjectUtils {

    private static final Collection<String> SUPPORTED_PROTOCOLS = Arrays.asList(
            StandardFileSystems.FILE_PROTOCOL_PREFIX,
            StandardFileSystems.JAR_PROTOCOL,
            StandardFileSystems.JRT_PROTOCOL,
            "temp://");

    /**
     * Check if two given objects are {@link Object#equals(Object) equal} while they are not {@code null}.
     *
     * @param obj1 First object.
     * @param obj2 Second object.
     * @return {@code true} if both object are non-{@code null} and are deeply equals. {@code false} otherwise.
     */
    public static boolean isEqualsNonNull(Object obj1, Object obj2) {
        return obj1 != null && obj1.equals(obj2);
    }

    /**
     * Helper to elegantly concatenate objects to single string.
     *
     * @param objects Variable length array of objects. Some may be {@code null}.
     * @return Concatenated string of all objects.
     */
    public static String toString(Object... objects) {
        StringBuilder buf = new StringBuilder(256);
        for (Object o : objects) {
            buf.append(o != null ? o : "null");
        }
        return buf.toString();
    }

    /**
     * Normalize path (e.g.: /dir1/dir2/../dir3 -> /dir1/dir3). The following rules are applied:
     * <ul>
     *     <li>Leading and trailing spaces are removed.</li>
     *     <li>Leading and trailing slashes are removed.</li>
     *     <li>Path is normalized (as described in {@link Path#normalize()}.</li>
     *     <li>A root directory (e.g. "/" or empty string) is always represented as "/".</li>
     * </ul>
     * NOTE: If the provided <i>path</i> is {@code null}, the result is also {@code null}.
     *
     * @param path Path to normalize.
     * @return Normalized path or {@code null} if <i>path</i> is {@code null}.
     */
    public static String normalizePath(String path) {
        if (path != null) {
            Path p;
            path = Paths.get(path.trim()).normalize().toString();
            if (path.isEmpty()) {
                path = "/";
            } else if (path.length() > 1) {
                int startIndex = path.startsWith("/") ? 1 : 0;
                int endIndex = path.endsWith("/") ? path.length() - 1 : path.length();
                if (endIndex - startIndex != path.length()) {
                    path = path.substring(startIndex, endIndex);
                }
            }

        }
        return path;
    }
}
