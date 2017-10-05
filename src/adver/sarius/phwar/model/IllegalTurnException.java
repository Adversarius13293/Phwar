package adver.sarius.phwar.model;

public class IllegalTurnException extends PhwarBoardException {
	private static final long serialVersionUID = 4038704379600791547L;

	public IllegalTurnException() {
		super();
	}

	public IllegalTurnException(String message) {
		super(message);
	}
}