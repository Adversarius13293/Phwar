package adver.sarius.phwar.model;

public class IllegalMoveException extends PhwarBoardException {
	private static final long serialVersionUID = 5808822772391771681L;

	public IllegalMoveException() {
		super();
	}

	public IllegalMoveException(String message) {
		super(message);
	}
}