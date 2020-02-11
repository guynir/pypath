package guynir.pypath.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Tests for {@link ObjectUtils} utilities.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public class ObjectUtilsTest {

    /**
     * Test that several representation of root directory always results in "/".
     */
    @Test
    public void testShouldResultInRootPath() {
        // Test reference to base directory.
        Assertions.assertThat(ObjectUtils.normalizePath("")).isEqualTo("/");
        Assertions.assertThat(ObjectUtils.normalizePath("/")).isEqualTo("/");
        Assertions.assertThat(ObjectUtils.normalizePath("///")).isEqualTo("/");
    }

    /**
     * Test should trim leading and trailing slashes.
     */
    @Test
    public void testShouldTrimLeadingAndTrailingSlashes() {
        Assertions.assertThat(ObjectUtils.normalizePath("/dir1/")).isEqualTo("dir1");
    }

    /**
     * Test should normalize path by reducing relative links (e.g.: "." and "..").
     */
    @Test
    public void testShouldNormalizeLinkedPath() {
        Assertions.assertThat(ObjectUtils.normalizePath("/dir1/dir2/../dir3")).isEqualTo("dir1/dir3");
        Assertions.assertThat(ObjectUtils.normalizePath("/dir1/././dir2/")).isEqualTo("dir1/dir2");
    }

    /**
     * Test should drop redundant slashes.
     */
    @Test
    public void testShouldDropRedundantSlashes() {
        Assertions.assertThat(ObjectUtils.normalizePath("/dir1///dir2/////dir3")).isEqualTo("dir1/dir2/dir3");
    }

}
