package guynir.pypath.utils;

/**
 * Contains various assertion methods.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public class Asserts {

    /**
     * Assert that a given object <i>obj</i> is not {@code null}.
     *
     * @param obj     Object to assess.
     * @param message Error message to include in case of assertion failure.
     * @throws IllegalArgumentException If <i>obj</i> is {@code null}.
     */
    public static void notNull(Object obj, String message) throws IllegalArgumentException {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that a given string is neither {@code null} nor empty (zero length).
     *
     * @param str     String to assert.
     * @param message Error message to include in case of assertion failure.
     * @throws IllegalArgumentException If <i>str</i> is {@code null} or empty.
     */
    public static void notEmpty(String str, String message) throws IllegalArgumentException {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
