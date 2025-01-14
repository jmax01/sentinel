package sentinel.exceptions;

/**
 * Thrown when an element is not currently visible on a web page when the automation
 * is attempting to interact with it.
 * @author Doug Noël
 *
 */
public class ElementNotVisibleException extends SentinelException {

    private static final long serialVersionUID = 8469225740522080247L;

    public ElementNotVisibleException(String message) {
        super(message);
    }

    public ElementNotVisibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
