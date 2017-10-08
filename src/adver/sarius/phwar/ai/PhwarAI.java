package adver.sarius.phwar.ki;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;

public abstract class PhwarKI {

	protected MoveCapture move;
	protected List<MoveCapture> captures;

	abstract public void computeTurn(PhwarBoard board);

	public void executeTurn(PhwarBoard board) {
		System.out.println("execute turn:");
		System.out.println(move);
		System.out.println(captures == null ? "" : captures.size());
		if (move != null) {
			move(move, board);
		}
		if (captures != null) {
			captures.forEach(c -> board.capture(c.startX, c.startY, c.targetX, c.targetY));
		}
	}

	protected boolean move(MoveCapture move, PhwarBoard board) {
		return move == null ? false : board.move(move.startX, move.startY, move.targetX, move.targetY);
	}

	protected boolean captureAll(PhwarBoard board, List<MoveCapture> captures) {
		for (MoveCapture cap : captures) {
			if (board.capture(cap.startX, cap.startY, cap.targetX, cap.targetY)) {
				return true;
			}
		}
		return false;
	}

	// TODO: get from PhwarBoard?!
	// see board.computeParticlesInLineOfSight()
	protected Set<MoveCapture> getAllPossibleMoves(PhwarBoard board, int posX, int posY) {
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

	protected Set<List<MoveCapture>> getAllPossibleCaptureCombinations(PhwarBoard board, Particle capturer) {
		Set<List<MoveCapture>> ret = new HashSet<>();
		board.computeParticlesToCaptureBy(capturer).forEach(p -> {
			PhwarBoard copy = new PhwarBoard(board);
			MoveCapture cMove = new MoveCapture(capturer.getPosX(), capturer.getPosY(), p.getPosX(), p.getPosY());
			boolean won = copy.capture(capturer.getPosX(), capturer.getPosY(), p.getPosX(), p.getPosY());

			Set<Particle> canCapture = copy.computeParticlesThatCanCapture();

			if (won || canCapture.isEmpty()) { // end of recursion
				List<MoveCapture> captures = new ArrayList<>();
				captures.add(cMove);
				ret.add(captures);
			} else {
				canCapture.forEach(cap -> {
					getAllPossibleCaptureCombinations(copy, cap).forEach(s -> {
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