package guynir.pypath;

/**
 * Top-most exception for all plugin's exceptions.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public class PyPathException extends RuntimeException {

    public PyPathException() {
    }

    public PyPathException(String message) {
        super(message);
    }

    public PyPathException(String message, Throwable cause) {
        super(message, cause);
    }

    public PyPathException(Throwable cause) {
        super(cause);
    }

    public PyPathException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
