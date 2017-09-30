package adver.sarius.phwar.ki;

import adver.sarius.phwar.model.PhwarBoard;

public class PhwarKITest implements PhwarKI{

	private int dir = 0;
	private int pos = -3;
	
	@Override
	public void executeTurn(PhwarBoard board) {
		if(dir == 0) {
			dir = board.getCurrentPlayer() == 0? -1:1;
			pos *= dir; 
		}
		
		board.move(0, pos, 0, pos+dir);
		pos +=dir;
		dir *=-1;
	}

	@Override
	public void computeTurn(PhwarBoard board) {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}