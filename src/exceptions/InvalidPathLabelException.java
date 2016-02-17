package exceptions;

public class InvalidPathLabelException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidPathLabelException() {
		super();
	}

	public InvalidPathLabelException(String message) {
		super(message);
	}
}
