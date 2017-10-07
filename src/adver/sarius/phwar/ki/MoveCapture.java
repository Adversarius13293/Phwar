package adver.sarius.phwar.ki;

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
	
	@Override
	public String toString() {
		return startX+"/"+startY+">"+targetX+"/"+targetY;
	}
}