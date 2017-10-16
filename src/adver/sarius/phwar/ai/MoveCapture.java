package adver.sarius.phwar.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;

public class MoveCapture {

	int startX;
	int startY;
	int targetX;
	int targetY;

	public MoveCapture(int startX, int startY, int targetX, int targetY) {
		this.startX = startX;
		this.startY = startY;
		this.targetX = targetX;
		this.targetY = targetY;
	}

	public int getStartX() {
		return startX;
	}

	public void setStartX(int startX) {
		this.startX = startX;
	}

	public int getStartY() {
		return startY;
	}

	public void setStartY(int startY) {
		this.startY = startY;
	}

	public int getTargetX() {
		return targetX;
	}

	public void setTargetX(int targetX) {
		this.targetX = targetX;
	}

	public int getTargetY() {
		return targetY;
	}

	public void setTargetY(int targetY) {
		this.targetY = targetY;
	}

	/**
	 * Execute the move action on the board.
	 * 
	 * @param board
	 *            the board to execute the move for.
	 * @return true if the game is won by this move, otherwise false.
	 */
	public boolean move(PhwarBoard board) {
		return board.move(startX, startY, targetX, targetY);
	}

	/**
	 * Execute the capture action on the board.
	 * 
	 * @param board
	 *            the board to execute the capture for.
	 * @return true if the game is won by this capture, otherwise false.
	 */
	public boolean capture(PhwarBoard board) {
		return board.capture(startX, startY, targetX, targetY);
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
	public static Set<MoveCapture> getAllPossibleMoves(PhwarBoard board, int posX, int posY) {
		// TODO: get from PhwarBoard?!
		// see board.computeParticlesInLineOfSight()
		Set<MoveCapture> moves = new HashSet<>();

		int size = board.getSize();

		for (int i = posX + 1; i <= size; i++) { // diagonal top right
			if (PhwarBoard.getParticle(i, posY, board.getParticles()).isPresent() || !board.isInsideBoard(i, posY)
					|| board.isCrossingCenter(posX, posY, i, posY)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, i, posY));
			}
		}
		for (int i = posX - 1; i >= -size; i--) { // diagonal bottom left
			if (PhwarBoard.getParticle(i, posY, board.getParticles()).isPresent() || !board.isInsideBoard(i, posY)
					|| board.isCrossingCenter(posX, posY, i, posY)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, i, posY));
			}
		}
		for (int i = posY + 1; i <= size; i++) { // top
			if (PhwarBoard.getParticle(posX, i, board.getParticles()).isPresent() || !board.isInsideBoard(posX, i)
					|| board.isCrossingCenter(posX, posY, posX, i)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, posX, i));
			}
		}
		for (int i = posY - 1; i >= -size; i--) { // bottom
			if (PhwarBoard.getParticle(posX, i, board.getParticles()).isPresent() || !board.isInsideBoard(posX, i)
					|| board.isCrossingCenter(posX, posY, posX, i)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, posX, i));
			}
		}
		for (int i = 1; posX + i <= size && posY + i <= size; i++) { // bottom right
			if (PhwarBoard.getParticle(posX + i, posY + i, board.getParticles()).isPresent()
					|| !board.isInsideBoard(posX + i, posY + i)
					|| board.isCrossingCenter(posX, posY, posX + i, posY + i)) {
				break;
			} else {
				moves.add(new MoveCapture(posX, posY, posX + i, posY + i));
			}
		}
		for (int i = -1; posX + i >= -size && posY + i >= -size; i--) { // top left
			if (PhwarBoard.getParticle(posX + i, posY + i, board.getParticles()).isPresent()
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
	 * orders of new captures possible by doing a capture. Does not check any rules
	 * or board conditions when executing the captures!
	 * 
	 * @param board
	 *            the board to compute on.
	 * @param capturer
	 *            the particle to start the captures with.
	 * @return all combinations of captures possible.
	 */
	public static Set<List<MoveCapture>> getAllPossibleCaptureCombinations(PhwarBoard board, Particle capturer,
			Set<Particle> toCapture) {
		Set<List<MoveCapture>> ret = new HashSet<>();
		toCapture.forEach(p -> {
			PhwarBoard copy = new PhwarBoard(board);
			MoveCapture cMove = new MoveCapture(capturer.getPosX(), capturer.getPosY(), p.getPosX(), p.getPosY());
			boolean won = copy.captureUnchecked(capturer.getPosX(), capturer.getPosY(), p.getPosX(), p.getPosY());

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

	@Override
	public String toString() {
		return startX + "/" + startY + ">" + targetX + "/" + targetY;
	}
}