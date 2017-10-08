package adver.sarius.phwar.ai;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;

/**
 * Determines the best move just on the best end result after a defined amount
 * of turns. A move resulting in one way to win after 5 turns will be rated the
 * same as a move resulting in 10 ways to win after 5 turns. The enemy is
 * expected to use the same AI and therefore picking the best turn for him. The
 * end result will be the move which leads to a win within the shortest amount
 * of turns. If there is no way to win, then pick the move which creates the
 * biggest advantage over particle counts without losing. And if that is not
 * possible, pick the move which delays the loss the longest.
 */
public class SinglePathAI extends PhwarAI {
	private int maxTurns;
	private double time;
	private int counts;
	private final double winValue = 1000000;
	private int winInTurn = Integer.MAX_VALUE;

	public SinglePathAI(int maxTurns) {
		this.maxTurns = maxTurns;
	}
	
	@Override
	public void computeTurn(PhwarBoard board) {
		long start = System.currentTimeMillis();
		this.move = null;
		this.captures = null;
		this.captureIndex = 0;

		PhwarBoard copy = new PhwarBoard(board);
		rateTurns(copy, 1);
		time += System.currentTimeMillis() - start;
		counts++;
		System.out.println("Average computation time player "+board.getCurrentPlayer() + ": "+(time/counts));
	}

	private double computeBoardValue(PhwarBoard board) {
		return board.getParticles().stream().filter(p -> p.getPlayer() == board.getCurrentPlayer()).count() * 2
				- board.getParticles().size();
	}

	// modifies the board
	private double rateAllPossibleCapturesAndProceed(PhwarBoard board, int currentTurn, double bestValueBefore) {
		double bestValue = Double.NEGATIVE_INFINITY;

		Map<Particle, Set<Particle>> capturer = board.computeParticlesThatCanCapture();
		if (capturer.isEmpty()) {
			if (currentTurn >= maxTurns) {
				bestValue = computeBoardValue(board);
			} else {
				board.nextPlayer();
				// good value for next player is bad value for current player
				bestValue = -rateTurns(board, currentTurn + 1);
			}

			if (currentTurn == 1 && bestValue > bestValueBefore) {
				winInTurn = getWinTurnForValue(bestValue);
				this.captures = null;
			}
			return bestValue;
		} else {
			// test capturing with each capturer
			for (Particle p : capturer.keySet()) {
				// test every capture path when starting with that capturer
				for (List<MoveCapture> l : getAllPossibleCaptureCombinations(board, p, capturer.get(p))) {
					PhwarBoard copy = new PhwarBoard(board);
					if (captureAll(copy, l)) {
						if (currentTurn == 1) {
							this.captures = l;
						}
						return winValue;
					} else {
						double value;
						if (currentTurn >= maxTurns) {
							value = computeBoardValue(copy);
						} else {
							copy.nextPlayer();
							// +l.size() to make him capture early?
							value = -rateTurns(copy, currentTurn + 1);
						}

						if (value > bestValue) {
							bestValue = value;
							if (currentTurn == 1 && bestValue > bestValueBefore) {
								winInTurn = getWinTurnForValue(bestValue);
								this.captures = l;
							}
						}
					}
				}
			}
		}
		return bestValue;
	}

	// TODO: Only useful with >3 turns... and even then only 1 round before save win. 
	private int getWinTurnForValue(double value) {
		if (value <= 0) {
			return winInTurn;
		}
		for (int i = 1; i <= maxTurns; i++) {
			if (winValue / i == value) {
				if(winInTurn < i) { // TODO: remove
					System.out.println("TODO: getWinTurnForValue() not like expected!");
				}
				System.out.println("Win in turn " + i);
				return i;
			}
		}
		return winInTurn;
	}

	// modifies the board
	private double rateTurns(PhwarBoard board, int currentTurn) {
		double bestValue = Double.NEGATIVE_INFINITY;
		if (currentTurn == winInTurn - 1) {
			// Example:
			// Don't need to compute my turn 5 if I already found a way to win in 5 turns.
			// Also don't need the enemy on turn 4 since that are all paths I won't pick
			// anyways. So in turn 3 I keep searching until I find a way to win in 3 turns.
			// Otherwise all possible moves are rated the same. But since I will pick the
			// winning move with 5, it doesn't matter what value (below the winning value)
			// is returned.
			return 0;
		}
		// can't move anything. But maybe still can capture?
		if (!board.needToMove()) {
			bestValue = rateAllPossibleCapturesAndProceed(board, currentTurn, bestValue);
			if (currentTurn == 1) {
				this.move = null;
			}
			return bestValue;
		}
		for (Particle part : board.getParticles().stream().filter(p -> p.getPlayer() == board.getCurrentPlayer())
				.collect(Collectors.toSet())) {
			Set<MoveCapture> moves = getAllPossibleMoves(board, part.getPosX(), part.getPosY());
			// test all possible moves per particle
			for (MoveCapture m : moves) {
				PhwarBoard copy = new PhwarBoard(board);
				if (move(m, copy)) {
					if (currentTurn == 1) { // first round win, pick this move.
						this.move = m;
						this.captures = null;
					}
					// win in 2 turns is better than win in 5 turns
					return winValue / currentTurn;
				}

				double rating = rateAllPossibleCapturesAndProceed(copy, currentTurn, bestValue);
				if (rating > bestValue) {
					bestValue = rating;
					if (currentTurn == 1) {
						this.move = m;
					}
					if (bestValue == winValue) { // instant win by capturing
						return winValue / currentTurn;
					}
				}

				/**
				 * Set<Particle> capturer = copy.computeParticlesThatCanCapture(); if
				 * (capturer.isEmpty()) { double value; if (currentTurn >= maxTurns) { value =
				 * computeBoardValue(copy); } else { // good value for next player is bad value
				 * for current player value = -xy(copy, currentTurn + 1); }
				 * 
				 * if (value >= bestValue) { bestValue = value; if (currentTurn == 1) {
				 * this.move = m; this.captures = null; } } } else { // test capturing with each
				 * capturer for (Particle p : capturer) { // test every capture path when
				 * starting with that capturer for (List<MoveCapture> l :
				 * getAllPossibleCaptureCombinations(copy, p)) { PhwarBoard copy2 = new
				 * PhwarBoard(copy); if (captureAll(copy2, l)) { if (currentTurn == 1) {
				 * this.move = m; this.captures = l; } return winValue / currentTurn; } else {
				 * 
				 * double value; if (currentTurn >= maxTurns) { value =
				 * computeBoardValue(copy2); } else { // +l.size() not really necessary? I just
				 * like capturing particles early value = -xy(copy2, 1) + l.size(); }
				 * 
				 * if (value >= bestValue) { bestValue = value; if (currentTurn == 1) {
				 * this.move = m; this.captures = l; } } } } } }
				 **/
			}
		}
		return bestValue;
	}
}