package adver.sarius.phwar.ai;

import adver.sarius.phwar.model.PhwarBoard;

/**
 * Moves the electron at 0/(-)3 one to the center and back again. computeTurn
 * will take 2 seconds. Just to test the use of the AI interface.
 */
public class PhwarAITest extends PhwarAI {

	private int dir = 0;
	private int pos = -3;

	@Override
	public void computeTurn(PhwarBoard board) {
		try {
			if (dir == 0) {
				dir = board.getCurrentPlayer() == 0 ? -1 : 1;
				pos *= dir;
			}

			move = new MoveCapture(0, pos, 0, pos + dir);
			pos += dir;
			dir *= -1;
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}