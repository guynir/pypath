package guynir.pypath.services;

import guynir.pypath.PyPathException;

/**
 * This exception indicates that an attempt was made to access non-existing file.
 *
 * @author Guy Raz Nir
 * @since 2020/02/08
 */
public class FileNotFoundException extends PyPathException {

    public FileNotFoundException() {
    }

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
