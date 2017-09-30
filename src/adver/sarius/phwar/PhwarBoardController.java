package adver.sarius.phwar;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

import adver.sarius.phwar.ki.PhwarKI;
import adver.sarius.phwar.ki.PhwarKITest;
import adver.sarius.phwar.model.IllegalCaptureException;
import adver.sarius.phwar.model.IllegalMoveException;
import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;
import adver.sarius.phwar.view.Hexagon;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

public class PhwarBoardController {

	private PhwarBoard board;
	private BiFunction<Integer, Integer, Hexagon> getHexagonFunc;
	private Button buttonNext;

	public PhwarBoardController(PhwarBoard board, Button buttonNext) {
		this.board = board;
		this.buttonNext = buttonNext;
		buttonNext.setOnAction(this::handleButtonEvent);
	}

	// TODO: should be in the constructor. But the view also needs the controllers
	// eventListener...
	public void initGetHexagonFunc(BiFunction<Integer, Integer, Hexagon> getHexagonFunc) {
		this.getHexagonFunc = getHexagonFunc;
	}

	private void markPossibleCapturer(boolean isCapturer, Hexagon hexaToCapture) {
		if (!toCapture.isEmpty()) {
			Particle particle = toCapture.keySet().stream()
					.filter(p -> p.getPosX() == hexaToCapture.getPosX() && p.getPosY() == hexaToCapture.getPosY())
					.findAny().orElse(null);
			if (particle != null) {
				toCapture.get(particle)
						.forEach(p -> getHexagonFunc.apply(p.getPosX(), p.getPosY()).setCapturer(isCapturer));
			}
		}
	}
	
	public void handleButtonEvent(ActionEvent e) {
		if(e.getTarget() != buttonNext) {
			System.out.println("TODO: Exception?");
		}
		if(state == State.NOT_STARTED) {
			buttonNext.setText("Next player");
			state = State.TODO;
			doYourMove();
		} else {
			nextPlayer();
		}
	}

	public void handleHexagonClick(MouseEvent e) {
		if (!(e.getTarget() instanceof Hexagon)) {
			System.out.println("TODO: Exception?");
		}
		Hexagon hexa = (Hexagon) e.getTarget();
		if (lastClicked == null) {
			lastClicked = hexa;
			lastClicked.setClicked(true);
			markPossibleCapturer(true, lastClicked);
		} else {
			if (hexa == lastClicked) { // TODO: when re-sizing hexagons? Object not == anymore
				lastClicked.setClicked(false);
				markPossibleCapturer(false, lastClicked);
				lastClicked = null;
			} else {
				if (toCapture.isEmpty()) {
					// trying to make a move
					try {
						toCapture = board.move(lastClicked.getPosX(), lastClicked.getPosY(), hexa.getPosX(),
								hexa.getPosY());
						lastClicked.setClicked(false);
						lastClicked = null;

						if (toCapture == null) {
							System.out.println("WON"); // TODO:
						} else if (toCapture.isEmpty()) {
							finishedTurn();
						} else {
							// highlight all toCapture.
							toCapture.keySet()
									.forEach(p -> getHexagonFunc.apply(p.getPosX(), p.getPosY()).setToCapture(true));
						}
					} catch (IllegalMoveException ex) {
						// TODO: show warn message?
						lastClicked.setClicked(false);
						lastClicked = null;
						System.out.println(ex.getMessage());
					}
				} else {
					// need to capture
					try {
						board.capture(hexa.getPosX(), hexa.getPosY(), lastClicked.getPosX(), lastClicked.getPosY());
						markPossibleCapturer(false, lastClicked);
						hexa.setCapturer(false);
						lastClicked.setToCapture(false);
						lastClicked.setClicked(false);
						lastClicked = null;
						toCapture = board.getToCaptureMap();
						if (toCapture.values().stream().allMatch(s -> s.isEmpty())) {
							if (!toCapture.isEmpty()) {
								toCapture.keySet().forEach(
										p -> getHexagonFunc.apply(p.getPosX(), p.getPosY()).setToCapture(false));
							}
							finishedTurn();
						}

					} catch (IllegalCaptureException ex) {
						// TODO: unmark highlighted?
						System.out.println(ex.getMessage());
					}
				}
			}
		}
	}

	private PhwarKI[] strategies = new PhwarKI[] { null,new PhwarKITest(), new PhwarKITest() };

	private void doYourMove() {
		buttonNext.setDisable(true);
		PhwarKI ki = strategies[board.getCurrentPlayer()];
		if (ki != null) {// not wating for player input
			// Can't modify GUI in other threads. But doing the computation there would
			// block the GUI.
			Thread th = new Thread(() -> {
				ki.computeTurn(board);
				Platform.runLater(() -> {
					ki.executeTurn(board);
					finishedTurn();
				});
			});
			th.setDaemon(true);
			th.start();
		}
	}
	
	private void finishedTurn() {
		boolean autoSkip = false;
		if(autoSkip) {
			nextPlayer();
		} else {
			buttonNext.setDisable(false);
		}
	}
	
	private void nextPlayer() {
		if (board.hasWon()) {
			state = State.WON;
			return;
		}
		board.nextPlayer();
		doYourMove();
	}

	private State state=State.NOT_STARTED;
	private Hexagon lastClicked;
	private Map<Particle, Set<Particle>> toCapture = Collections.emptyMap();

}

enum State {
	NOT_STARTED,TODO, COMPUTING, WON;
}