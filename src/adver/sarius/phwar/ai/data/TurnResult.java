package adver.sarius.phwar.ai.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import adver.sarius.phwar.ai.MoveCapture;

public class TurnResult {

	public MoveCapture move = null;
	public List<MoveCapture> captures = null;
	public int winInTurns = 1;
	public double rating = 1000000;
	public double oneTurnBonusRating = 0;
	public Set<String> insideLoops = new HashSet<>();

	public TurnResult() {

	}

	public String getAppendingToFile() {
		StringBuilder builder = new StringBuilder();
		builder.append(';').append(rating).append(';').append(winInTurns).append(';').append(oneTurnBonusRating)
				.append(';').append(move).append(';');
		captures.forEach(m -> builder.append(m).append('#'));
		// format:
		// particles;rating;winInRounds;bonusRating;x/y>X/Y;capx/capy>capX/capY#caps...

		return builder.toString();
	}
}