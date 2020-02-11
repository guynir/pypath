package guynir.pypath.utils;

/**
 * Functional interface for 3-arguments consumers.
 *
 * @param <T> Generic type of first argument.
 * @param <U> Generic type of second argument.
 * @param <V> Generic type of third argument.
 * @author Guy Raz Nir
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 * @since 2020/02/08
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    /**
     * Perform operation given 3 arguments.
     *
     * @param t First argument.
     * @param u Second argument.
     * @param v Third argument.
     */
    void accept(T t, U u, V v);
}
