package exceptions;

public class TimingException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TimingException() {
		super();
	}

	public TimingException(String message) {
		super(message);
	}
}
