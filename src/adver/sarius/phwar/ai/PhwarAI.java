package adver.sarius.phwar.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;

public abstract class PhwarAI {

	/** The computed move to execute. **/
	protected MoveCapture move;
	/** The computed captures to execute. **/
	protected List<MoveCapture> captures;
	/** Index of the next capture to execute. **/
	protected int captureIndex = 0;

	/**
	 * Precompute the move and captures for the current player. The board object
	 * will not be modified.
	 * 
	 * @param board
	 *            current board to compute the turn for.
	 */
	abstract public void computeTurn(PhwarBoard board);

	/**
	 * Execute all the computed moves and captures at once.
	 * 
	 * @param board
	 *            Board to execute the turn on.
	 */
	public void executeTurn(PhwarBoard board) {
		move.move(board);
		captureAll(board, captures);
	}

	/**
	 * Executes just the computed move on the given board.
	 * 
	 * @param board
	 *            the board to execute the move for.
	 */
	public void executeComputedMove(PhwarBoard board) {
		captureIndex = 0; // TODO: somehow have to ensure that it gets reset after a new computation.
		move.move(board);
	}

	/**
	 * Executes just one computed capture on the given board. Needs to have another
	 * capture.
	 * 
	 * @param board
	 *            the board to execute the one capture for.
	 */
	public void executeComputedSingleCapture(PhwarBoard board) {
		if (captures != null) {
			MoveCapture cap = captures.get(captureIndex);
			if (board.capture(cap.startX, cap.startY, cap.targetX, cap.targetY)) { // TODO: is that needed?
				captureIndex = captures.size();
			}
		}
		captureIndex++;
	}

	/**
	 * If there aren't any more captures to execute, executeComputedSingleCapture()
	 * should not be called.
	 * 
	 * @return true if there is another capture to execute.
	 */
	public boolean hasAnotherCapture() {
		return captures != null && captures.size() > captureIndex;
	}

	/**
	 * Execute all the captures in the given list order.
	 * 
	 * @param board
	 *            the board to execute the captures for.
	 * @param captures
	 *            list of all captures to be executed. Empty list or null to do
	 *            nothing.
	 * @return true if the game is won by this captures, otherwise false.
	 */
	public static boolean captureAll(PhwarBoard board, List<MoveCapture> captures) {
		if (captures == null) {
			return false;
		}
		for (MoveCapture cap : captures) {
			if (cap.capture(board)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Does not check any rules or board conditions! You should always use
	 * {@link #captureAll(PhwarBoard, List) captureAll}, unless you need a fast
	 * execution and are sure, that the captures are valid.
	 * 
	 * @param board
	 *            the board to execute the captures for.
	 * @param captures
	 *            list of all captures to be executed. Empty list to do nothing.
	 * @return true if the game is won by this captures, otherwise false.
	 */
	public static boolean captureAllUnchecked(PhwarBoard board, List<MoveCapture> captures) {
		for (MoveCapture cap : captures) {
			if (board.captureUnchecked(cap.startX, cap.startY, cap.targetX, cap.targetY)) {
				return true;
			}
		}
		return false;
	}
}