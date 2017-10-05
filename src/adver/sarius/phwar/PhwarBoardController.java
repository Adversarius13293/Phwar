package adver.sarius.phwar;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import adver.sarius.phwar.ki.PhwarKI;
import adver.sarius.phwar.ki.PhwarKITest;
import adver.sarius.phwar.model.IllegalCaptureException;
import adver.sarius.phwar.model.IllegalMoveException;
import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;
import adver.sarius.phwar.view.Hexagon;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
		feedback = new SimpleStringProperty("Ready!");
		
		if(autoSkip) {
			buttonNext.fire();
		}
	}
	
	public StringProperty getFeedbackProperty() {
		return this.feedback;
	}

	// TODO: should be in the constructor. But the view also needs the controllers
	// eventListener...
	public void initGetHexagonFunc(BiFunction<Integer, Integer, Hexagon> getHexagonFunc) {
		this.getHexagonFunc = getHexagonFunc;
	}

	private void markAsToCapture(boolean isToCapture, Set<Particle> toCapture) {
		toCapture.forEach(p -> getHexagonFunc.apply(p.getPosX(), p.getPosY()).setToCapture(isToCapture));
	}
	
	private void markAsCapturer(boolean isCapturer, Set<Particle> capturer) {
		capturer.forEach(p -> getHexagonFunc.apply(p.getPosX(), p.getPosY()).setCapturer(isCapturer));
	}
	
	public void handleButtonEvent(ActionEvent e) {
		if(e.getTarget() != buttonNext) {
			System.out.println("TODO: Exception?");
		}
		feedClear();
		if(state == State.NOT_STARTED) {
			buttonNext.setText("Next player");
			state = State.READY;
			doYourMove();
		} else {
			nextPlayer();
		}
	}
	
	private StringProperty feedback;
	
	private void feed(String message) {
		if(message == null || message.isEmpty()) {
			feedClear();
		} else {
			feedback.set(message);
		}
	}
	
	private void feedClear() {
		feedback.set("");
	}
	
	private boolean checkState() {
		if(state == State.NOT_STARTED) {
			feed("Please start the game first.");
			return false;
		}
		if(state == State.COMPUTING) {
			feed("The AI is still computing its turn.");
			return false;
		}
		if(state == State.WON) {
			feed("The game is won. Please restart.");
			return false;
		}
		if(state == State.FINISHED_TURN) {
			feed("Please finish the turn first.");
			return false;
		}
		return true;
	}

	public void handleHexagonClick(MouseEvent e) {
		if (!(e.getTarget() instanceof Hexagon)) {
			System.out.println("TODO: Exception?");
		}
		if(!checkState()) {
			return;
		} else {
			feedClear();
		}
		Hexagon hexa = (Hexagon) e.getTarget();
		if (lastClicked == null) { // nothing highlighted yet. Highlight for moving or capturing.
			lastClicked = hexa;
			lastClicked.setClicked(true);
			// TODO: via state?
			if(!board.needToMove()) {
				markAsToCapture(true, board.computeParticlesToCaptureBy(lastClicked.getPosX(), lastClicked.getPosY()));
			}			
		} else {
			// TODO: when re-sizing hexagons? Object not == anymore
			if (hexa == lastClicked) { // un-highlight
				lastClicked.setClicked(false);
				markAsToCapture(false, board.computeParticlesToCaptureBy(lastClicked.getPosX(), lastClicked.getPosY()));
				lastClicked = null;
			} else {
				if (board.needToMove()) { // trying to move
					try {
						boolean won = board.move(lastClicked.getPosX(), lastClicked.getPosY(), hexa.getPosX(), hexa.getPosY());
						lastClicked.setClicked(false);
						lastClicked = null;

						if (won) {
							feed("Congratulation! You won!");
							state = State.WON;
						} else {
							Set<Particle> capturer = board.computeParticlesThatCanCapture();
							if(capturer.isEmpty()) {
								finishedTurn();
							} else {
								// highlight all possible capturer
								markAsCapturer(true, capturer);
							}
						}
					} catch (IllegalMoveException ex) {
						// TODO: show warn message?
						lastClicked.setClicked(false);
						lastClicked = null;
						feed(ex.getMessage());
					}
				} else { // need to capture
					// TODO: Test if state capturing?
					try {
						Set<Particle> capturer = board.computeParticlesThatCanCapture();
						Set<Particle> toCapture = board.computeParticlesToCaptureBy(lastClicked.getPosX(), lastClicked.getPosY());
						board.capture(lastClicked.getPosX(), lastClicked.getPosY(), hexa.getPosX(), hexa.getPosY());
						markAsCapturer(false, capturer);
						markAsToCapture(false, toCapture);
						
						lastClicked.setCapturer(false);
						lastClicked.setClicked(false);
						lastClicked = null;
						
						capturer = board.computeParticlesThatCanCapture();
						if (capturer.isEmpty()) {
							finishedTurn();
						} else {
							markAsCapturer(true, capturer);
						}

					} catch (IllegalCaptureException ex) {
						// TODO: unmark highlighted??
						feed(ex.getMessage());
					}
				}
			}
		}
	}

	private PhwarKI[] strategies = new PhwarKI[] { null,null, new PhwarKITest(), new PhwarKITest() };

	private void doYourMove() {
		buttonNext.setDisable(true);
		PhwarKI ki = strategies[board.getCurrentPlayer()];
		if (ki != null) {// not wating for player input
			// Can't modify GUI in other threads. But doing the computation there would
			// block the GUI.
			state=State.COMPUTING;
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
	
	private boolean autoSkip = true;
	private void finishedTurn() {
		state = State.FINISHED_TURN;
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
		// TODO: Exceptions
		board.nextPlayer();
		state = State.READY;
		doYourMove();
	}

	private State state=State.NOT_STARTED;
	private Hexagon lastClicked;
//	private Map<Particle, Set<Particle>> toCapture = Collections.emptyMap();

}

enum State {
	NOT_STARTED,READY, COMPUTING, FINISHED_TURN, WON;
}