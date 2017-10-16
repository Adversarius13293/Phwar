package adver.sarius.phwar.ai.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import adver.sarius.phwar.ai.MoveCapture;
import adver.sarius.phwar.ai.PhwarAI;
import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;

// TODO: better name
public class DataWriter {

	public static void main(String[] args) {

		// format: particles;rating;winInRounds;bonusRating;x/y>X/Y;capx/capy>capX/capY#caps...

		PhwarBoard board = new PhwarBoard();

		loadData();
		doSomething(board);

		// if loop: ignore completely?
		// --> return 0. Its better than losing and maybe the enemy decides different
		// next time. In worst case it ends in back and forth, but still not a loss.

		// if win --> take win

		// enemy wins, but there are other paths
		// --> every path ends in a win for either side, or 0 because of loop.
		// i don't really care if i win safely in 5 or 50 turns. so don't divide by
		// rounds?!
		// --> but losing in 1 or 10 rounds could be a difference...
		// if enemy would win, a second path where i win late is better than a second
		// path where i win next round.
		// need to modify the negative score of the winning enemy based on his other
		// paths.

		// state A leads to state B which leads to state A. To rate B i need the value
		// of A, which waits for B...
		// would work if i only compute and save state a completely. after that the
		// states after a.
		// --> way to slow, need to compute backwards to reuse every result.
		// my turn, I can win, perfect.

		// Problem: getting to a state which i have already created but not rated yet.
		// "root loop" as parameter? don't save any states once i found a loop of root.
		// --> if i find any loop of any state start new computation with this as root.
		// rate as 0 if at loop again, and dont save anything between this and root. If
		// any new loop shows up, repeat. at one point i should be able to rate a loop,
		// and then everything works.
		// --> A has loop of C which has A as a loop? --> fucked again -.-

		// compute root and every loop of any state returns 0. and don't save the states
		// between loops and root in main list. save in temp list to use value in this
		// branch
	}

	// TODO: Don't make everything static...
	
	private static void loadData() {
		// TODO: other way to work with files
		try (Stream<String> stream = Files.lines(Paths.get("src/adver/sarius/phwar/ai/data", "PhwarMoves.txt"))) {
			stream.forEach(s -> {
				String[] splitted = s.split(";");
				TurnResult res = new TurnResult();
				res.oneTurnBonusRating = Double.parseDouble(splitted[3]);
				res.rating = Double.parseDouble(splitted[1]);
				res.winInTurns = Integer.parseInt(splitted[2]);
				bestTurnResult.put(splitted[0], res);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TODO: load not rated boards
	}

	private static Map<String, TurnResult> bestTurnResult = new HashMap<>();
	private static Set<String> notRatedBoards = new HashSet<>();
	private static Set<String> visitedBoards = new HashSet<>();

	private static TurnResult doSomething(PhwarBoard board) {
		String normalized = board.getNormalizedParticlesString();
		if (bestTurnResult.containsKey(normalized)) {
			System.out.println("returned normalized");
			return bestTurnResult.get(normalized);
		} else if (visitedBoards.contains(normalized)) {
//			System.out.println("entered loop");
			// still trying to compute the value of this board, so treat as drawn
			TurnResult loopResult = new TurnResult();
			loopResult.rating = 0;
			loopResult.insideLoops.add(normalized);
			return loopResult;
		} else {
			visitedBoards.add(normalized);
		}
		// all possible move+capture results that can be done from the current board
		Set<TurnResult> results = new HashSet<>();
		// can't move anything. But maybe still can capture?
		if (!board.needToMove()) {
			rateAllPossibleCapturesAndProceed(board, results, null);
		} else {
			for (Particle part : board.getParticles().stream().filter(p -> p.getPlayer() == board.getCurrentPlayer())
					.collect(Collectors.toSet())) {
				Set<MoveCapture> moves = MoveCapture.getAllPossibleMoves(board, part.getPosX(), part.getPosY());
				// test all possible moves per particle
				for (MoveCapture m : moves) {
					PhwarBoard copy = new PhwarBoard(board);
					if (copy.moveUnchecked(m.getStartX(), m.getStartY(), m.getTargetX(), m.getTargetY())) {
						TurnResult result = new TurnResult();
						result.move = m;
						results.add(result);
					} else {
						rateAllPossibleCapturesAndProceed(copy, results, m);
					}
				}
			}
		}

		TurnResult best = null;
		Set<String> loops = new HashSet<>();
		double bestRating = Double.NEGATIVE_INFINITY;
		int wins = 0;
		int loss = 0;
		int lossDist = 0;
		for (TurnResult tr : results) {
			// prefer fast wins, otherwise late loss.
			double rating = (tr.rating + tr.oneTurnBonusRating) / tr.winInTurns;
			if (rating < 0) {
				loss++;
				lossDist += tr.winInTurns;
			} else if (rating > 0) {
				wins++;
			} else {
			}
			if (rating > bestRating) {
				bestRating = rating;
				best = tr;
			}
			loops.addAll(tr.insideLoops);
		}
		// enemy has only a few winning alternatives --> good for me --> bad for enemy.
		// enemy could lose late --> higher chance to mess up --> bad for enemy.
		// only matters if enemy doesn't pick the best move --> then he also would not
		// pick other winning moves intentionally. So I don't care about the distance of
		// his other wins.
		best.oneTurnBonusRating = winNotWinRatio * ((results.size() - wins) / wins) + lossDist / loss;
		best.insideLoops.addAll(loops);
		best.insideLoops.remove(normalized);

		tryToSaveResult(normalized, best);
		return best;
	}
	// TODO: Are my loop preventions working for mirrored boards where turn 2 is the
	// loop of turn 1?

	private static void tryToSaveResult(String normalizedBoard, TurnResult best) {
		if (best.insideLoops.isEmpty()) {
			bestTurnResult.put(normalizedBoard, best);
			System.out.println("saving result");
			try {
				Files.write(Paths.get("src/adver/sarius/phwar/ai/data", "PhwarMoves.txt"), (normalizedBoard+best.getAppendingToFile()).getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("skip save because loop");
			// don't save boards within a loop, since the rating and move may be incorrect.
			// TODO: Save not rated boards
			notRatedBoards.add(normalizedBoard);
		}
	}

	/** Maximum points for having a lot of not-winning alternative moves. **/
	private static double winNotWinRatio = 4;

	private static void rateAllPossibleCapturesAndProceed(PhwarBoard board, Set<TurnResult> results, MoveCapture move) {
		Map<Particle, Set<Particle>> capturer = board.computeParticlesThatCanCapture();
		if (capturer.isEmpty()) {
			board.nextPlayerUnchecked();
			TurnResult bestResult = doSomething(board);
			bestResult.captures = null;
			bestResult.move = move;
			bestResult.winInTurns++;
			bestResult.rating = -bestResult.rating;
			results.add(bestResult);
		} else {
			// test capturing with each capturer
			for (Particle p : capturer.keySet()) {
				// test every capture path when starting with that capturer
				for (List<MoveCapture> l : MoveCapture.getAllPossibleCaptureCombinations(board, p, capturer.get(p))) {
					PhwarBoard copy = new PhwarBoard(board);
					if (PhwarAI.captureAllUnchecked(copy, l)) {
						TurnResult result = new TurnResult();
						result.captures = l;
						result.move = move;
						results.add(result);
					} else {
						copy.nextPlayerUnchecked();
						TurnResult bestResult = doSomething(copy);
						bestResult.captures = l;
						bestResult.move = move;
						bestResult.winInTurns++;
						bestResult.rating = -bestResult.rating;
						results.add(bestResult);
					}
				}
			}
		}
	}
}