package adver.sarius.phwar.ki;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;

public class FirstKI extends PhwarKI {

	private final int turns = 0;

	@Override
	public void computeTurn(PhwarBoard board) {
		this.move = null;
		this.captures = null;

		double bestValue = Double.NEGATIVE_INFINITY;
		boolean won = false;

		// TODO: cant move, but maybe capture?!
		// test all of the players particles
		for (Particle part : board.getParticles().stream().filter(p -> p.getPlayer() == board.getCurrentPlayer())
				.collect(Collectors.toSet())) {
			Set<MoveCapture> moves = getAllPossibleMoves(board, part.getPosX(), part.getPosY());
			if (moves.isEmpty()) {
				continue;
			}
			// test all possible moves per particle
			for (MoveCapture m : moves) {
				PhwarBoard copy = new PhwarBoard(board);
				won = move(m, copy);
				if (won) {
					this.move = m;
					this.captures = null;
					return;
				}

				Set<Particle> capturer = copy.computeParticlesThatCanCapture();
				if (capturer.isEmpty()) {
					double value = xy(copy, turns);
					if (value >= bestValue) {
						bestValue = value;
						this.move = m;
						this.captures = null;
					}
				} else {
					// test capturing with each capturer
					for (Particle p : capturer) {
						// test every capture path when starting with that capturer
						for (List<MoveCapture> l : getAllPossibleCaptureCombinations(copy, p)) {
							PhwarBoard copy2 = new PhwarBoard(copy);
							won = captureAll(copy2, l);
							if (won) {
								this.move = m;
								this.captures = l;
								return;
							} else {
								double value = xy(copy2, turns) + l.size();
								if (value >= bestValue) {
									bestValue = value;
									this.move = m;
									this.captures = l;
								}
							}
						}
					}
				}
			}
		}
	}

	public double xy(PhwarBoard board, int turnsToFinish) {
		// System.out.println(board);
		return 0;
	}
}