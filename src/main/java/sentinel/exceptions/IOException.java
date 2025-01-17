package sentinel.exceptions;

public class IOException extends SentinelException {

    /**
     *  Allows us to pass in the stack trace and additional text to a IOException.
     *  Also wraps it in a SentinelException for easier error handling at the glue step level.
     */
    private static final long serialVersionUID = 8461255760821180367L;

    public IOException(Throwable cause) {
        super(cause);
    }
    
    public IOException(String message) {
        super(message);
    }

    public IOException(String message, Throwable cause) {
        super(message, cause);
    }
}
