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
		if (move != null) {
			move(move, board);
		}
		if (captures != null) {
			captureAll(board, captures);
		}
	}
	
	/**
	 * Executes just the computed move on the given board.
	 * 
	 * @param board
	 *            the board to execute the move for.
	 */
	public void executeComputedMove(PhwarBoard board) {
		captureIndex = 0; // TODO: somehow have to ensure that it gets reset after a new computation.
		if (move != null) {
			move(move, board);
		}
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
	 * Execute the given MoveCapture as a move action on the board.
	 * 
	 * @param move
	 *            the move to execute. Don't move if it is null.
	 * @param board
	 *            the board to execute the move for.
	 * @return true if the game is won by this move, otherwise false.
	 */
	protected boolean move(MoveCapture move, PhwarBoard board) {
		return move == null ? false : board.move(move.startX, move.startY, move.targetX, move.targetY);
	}

	/**
	 * Execute all the captures in the given list order.
	 * 
	 * @param board
	 *            the board to execute the captures for.
	 * @param captures
	 *            list of all captures to be executed. Empty list to do nothing.
	 * @return true if the game is won by this captures, otherwise false.
	 */
	protected boolean captureAll(PhwarBoard board, List<MoveCapture> captures) {
		for (MoveCapture cap : captures) {
			if (board.capture(cap.startX, cap.startY, cap.targetX, cap.targetY)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Computes all possible moves the particle at the given position can execute.
	 * There does not have to be a particle on the starting position, and the
	 * current state of the board or player don't matter.
	 * 
	 * @param board
	 *            the board to compute on.
	 * @param posX
	 *            the x coordinate of the start position.
	 * @param posY
	 *            the y coordinate of the start position.
	 * @return all the possible moves, or an empty set.
	 */
	protected Set<MoveCapture> getAllPossibleMoves(PhwarBoard board, int posX, int posY) {
		// TODO: get from PhwarBoard?!
		// see board.computeParticlesInLineOfSight()
		Set<MoveCapture> moves = new HashSet<>();

		int size = board.getSize();

		for (int i = posX + 1; i <= size; i++) { // diagonal top right
			if (board.getParticle(i, posY, board.getParticles()).isPresent() || !board.isInsideBoard(i, posY)
					|| board.isCrossingCenter(posX, posY, i, posY)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, i, posY));
			}
		}
		for (int i = posX - 1; i >= -size; i--) { // diagonal bottom left
			if (board.getParticle(i, posY, board.getParticles()).isPresent() || !board.isInsideBoard(i, posY)
					|| board.isCrossingCenter(posX, posY, i, posY)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, i, posY));
			}
		}
		for (int i = posY + 1; i <= size; i++) { // top
			if (board.getParticle(posX, i, board.getParticles()).isPresent() || !board.isInsideBoard(posX, i)
					|| board.isCrossingCenter(posX, posY, posX, i)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, posX, i));
			}
		}
		for (int i = posY - 1; i >= -size; i--) { // bottom
			if (board.getParticle(posX, i, board.getParticles()).isPresent() || !board.isInsideBoard(posX, i)
					|| board.isCrossingCenter(posX, posY, posX, i)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, posX, i));
			}
		}
		for (int i = 1; posX + i <= size && posY + i <= size; i++) { // bottom right
			if (board.getParticle(posX + i, posY + i, board.getParticles()).isPresent()
					|| !board.isInsideBoard(posX + i, posY + i)
					|| board.isCrossingCenter(posX, posY, posX + i, posY + i)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, posX + i, posY + i));
			}
		}
		for (int i = -1; posX + i >= -size && posY + i >= -size; i--) { // top left
			if (board.getParticle(posX + i, posY + i, board.getParticles()).isPresent()
					|| !board.isInsideBoard(posX + i, posY + i)
					|| board.isCrossingCenter(posX, posY, posX + i, posY + i)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, posX + i, posY + i));
			}
		}
		return moves;
	}

	/**
	 * Computes all possible orders of captures the given particle can do, and all
	 * orders of new captures possible by doing a capture.
	 * 
	 * @param board
	 *            the board to compute on.
	 * @param capturer
	 *            the particle to start the captures with.
	 * @return all combinations of captures possible.
	 */
	protected Set<List<MoveCapture>> getAllPossibleCaptureCombinations(PhwarBoard board, Particle capturer, Set<Particle> toCapture) {
		Set<List<MoveCapture>> ret = new HashSet<>();
		toCapture.forEach(p -> {
			PhwarBoard copy = new PhwarBoard(board);
			MoveCapture cMove = new MoveCapture(capturer.getPosX(), capturer.getPosY(), p.getPosX(), p.getPosY());
			boolean won = copy.capture(capturer.getPosX(), capturer.getPosY(), p.getPosX(), p.getPosY());

			Map<Particle, Set<Particle>> canCapture = copy.computeParticlesThatCanCapture();

			if (won || canCapture.isEmpty()) { // end of recursion
				List<MoveCapture> captures = new ArrayList<>();
				captures.add(cMove);
				ret.add(captures);
			} else {
				canCapture.forEach((cap, toCap) -> {
					getAllPossibleCaptureCombinations(copy, cap, toCap).forEach(s -> {
						s.add(0, cMove);
						ret.add(s);
					});
					;
				});
			}
		});
		return ret;
	}
}