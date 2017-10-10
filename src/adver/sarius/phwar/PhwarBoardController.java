package adver.sarius.phwar;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;

import adver.sarius.phwar.ai.PhwarAI;
import adver.sarius.phwar.ai.PhwarAITest;
import adver.sarius.phwar.ai.SinglePathAI;
import adver.sarius.phwar.model.IllegalCaptureException;
import adver.sarius.phwar.model.IllegalMoveException;
import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;
import adver.sarius.phwar.model.PhwarBoardException;
import adver.sarius.phwar.view.Hexagon;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

public class PhwarBoardController {

	private PhwarBoard board;
	private BiFunction<Integer, Integer, Hexagon> getHexagonFunc;
	private Button buttonNext;
	private Hexagon lastClicked;
	private StringProperty feedback;
	private State state = State.NOT_STARTED;

	private PhwarAI[] strategies = new PhwarAI[] { new SinglePathAI(2), new SinglePathAI(2), new PhwarAITest() };
	private int captureDelay = 000;
	private boolean autoSkip = true;

	public PhwarBoardController(PhwarBoard board, Button buttonNext) {
		this.board = board;
		this.buttonNext = buttonNext;
		buttonNext.setOnAction(this::handleButtonEvent);
		feedback = new SimpleStringProperty("Ready!");
		if (autoSkip) {
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
		if (e.getTarget() != buttonNext) {
			System.out.println("TODO: Exception?");
		}
		feedClear();
		if (state == State.NOT_STARTED) {
			buttonNext.setText("Next player");
			state = State.READY;
			doYourMove();
		} else {
			nextPlayer();
		}
	}

	private void feed(String message) {
		if (message == null || message.isEmpty()) {
			feedClear();
		} else {
			feedback.set(message);
		}
	}

	private void feedClear() {
		feedback.set("");
	}

	private boolean checkState() {
		if (state == State.NOT_STARTED) {
			feed("Please start the game first.");
			return false;
		}
		if (state == State.COMPUTING) {
			feed("The AI is still computing its turn.");
			return false;
		}
		if (state == State.WON) {
			feed("The game is won. Please restart.");
			return false;
		}
		if (state == State.FINISHED_TURN) {
			feed("Please finish the turn first.");
			return false;
		}
		return true;
	}

	public void handleHexagonClick(MouseEvent e) {
		if (!(e.getTarget() instanceof Hexagon)) {
			System.out.println("TODO: Exception?");
		}
		if (!checkState()) {
			return;
		} else {
			feedClear();
		}
		Hexagon hexa = (Hexagon) e.getTarget();
		if (lastClicked == null) { // nothing highlighted yet. Highlight for moving or capturing.
			// TODO: via state?
			if (!board.needToMove()) {
				try { // TODO: Can still highlight enemy particle
					markAsToCapture(true, board.computeParticlesToCaptureBy(hexa.getPosX(), hexa.getPosY()));
				} catch (PhwarBoardException ex) {
					feed(ex.getMessage());
					return;
				}
			}
			lastClicked = hexa;
			lastClicked.setClicked(true);
		} else {
			// TODO: when re-sizing hexagons? Object not == anymore
			if (hexa == lastClicked) { // un-highlight
				lastClicked.setClicked(false);
				// TODO: via state?
				if (!board.needToMove()) {
					markAsToCapture(false,
							board.computeParticlesToCaptureBy(lastClicked.getPosX(), lastClicked.getPosY()));
				}
				lastClicked = null;
				System.out.println(board);
			} else {
				if (board.needToMove()) { // trying to move
					try {
						boolean won = board.move(lastClicked.getPosX(), lastClicked.getPosY(), hexa.getPosX(),
								hexa.getPosY());
						lastClicked.setClicked(false);
						lastClicked = null;

						if (won) {
							feed("Congratulation! You have won!");
							state = State.WON;
						} else {
							Set<Particle> capturer = board.computeParticlesThatCanCapture().keySet();
							if (capturer.isEmpty()) {
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
						Map<Particle, Set<Particle>> capturer = board.computeParticlesThatCanCapture();
						Set<Particle> toCapture = capturer.get(board
								.getParticle(lastClicked.getPosX(), lastClicked.getPosY(), capturer.keySet()).get());
						board.capture(lastClicked.getPosX(), lastClicked.getPosY(), hexa.getPosX(), hexa.getPosY());
						markAsCapturer(false, capturer.keySet());
						markAsToCapture(false, toCapture);

						lastClicked.setCapturer(false);
						lastClicked.setClicked(false);
						lastClicked = null;

						capturer = board.computeParticlesThatCanCapture();
						if (capturer.isEmpty()) {
							finishedTurn();
						} else {
							markAsCapturer(true, capturer.keySet());
						}

					} catch (IllegalCaptureException ex) {
						// TODO: unmark highlighted??
						feed(ex.getMessage());
					}
				}
			}
		}
	}

	private void doYourMove() {
		buttonNext.setDisable(true);
		PhwarAI ai = strategies[board.getCurrentPlayer()];
		if (ai != null) {// not wating for player input
			state = State.COMPUTING;
			// Can't modify GUI in other threads. But doing the computation there would
			// block the GUI.
			Service<Void> service = new Service<Void>() {
				@Override
				protected Task<Void> createTask() {
					return new Task<Void>() {
						@Override
						protected Void call() throws Exception {
							ai.computeTurn(board);
							CountDownLatch latch1 = new CountDownLatch(1);
							Platform.runLater(() -> {
								ai.executeComputedMove(board);
								latch1.countDown();
							});
							Thread.sleep(captureDelay);
							latch1.await();
							while (ai.hasAnotherCapture()) {
								CountDownLatch latch2 = new CountDownLatch(1);
								Platform.runLater(() -> {
									ai.executeComputedSingleCapture(board);
									latch2.countDown();
								});
								Thread.sleep(captureDelay);
								latch2.await();
							}
							Platform.runLater(() -> finishedTurn());
							return null;
						}
					};
				}
			};
			service.start();
		} else {
			if (!board.needToMove() && board.computeParticlesThatCanCapture().isEmpty()) {
				finishedTurn();
			}
		}
	}

	private void finishedTurn() {
		state = State.FINISHED_TURN;
		if (autoSkip) {
			nextPlayer();
		} else {
			buttonNext.setDisable(false);
		}
	}

	private void nextPlayer() {
		if (board.hasWon()) {
			state = State.WON;
			feed("Congratulation! You have won!");
			return;
		}
		// TODO: Exceptions
		board.nextPlayer();
		state = State.READY;
		doYourMove();
	}
}

enum State {
	NOT_STARTED, READY, COMPUTING, FINISHED_TURN, WON;
}