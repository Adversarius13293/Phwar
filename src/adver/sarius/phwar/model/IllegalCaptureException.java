package adver.sarius.phwar.model;

public class IllegalCaptureException extends PhwarBoardException {

	private static final long serialVersionUID = -3521021513250099415L;

	public IllegalCaptureException() {
		super();
	}

	public IllegalCaptureException(String message) {
		super(message);
	}
}