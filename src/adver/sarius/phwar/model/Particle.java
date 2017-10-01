package adver.sarius.phwar.model;

public class Particle {
	private int player;
	private int charge;
	private int posX;
	private int posY;

	public Particle(int player, int type) {
		this.player = player;
		this.charge = type;
	}

	public Particle(int player, int type, int posX, int posY) {
		this.player = player;
		this.charge = type;
		this.posX = posX;
		this.posY = posY;
	}

	void setPos(int posX, int posY) {
		this.posX = posX;
		this.posY = posY;
	}

	public int getPlayer() {
		return player;
	}

	public int getCharge() {
		return charge;
	}

	public int getPosX() {
		return posX;
	}

	public int getPosY() {
		return posY;
	}

	@Override
	public String toString() {
		return posX + "/" + posY + "|" + charge + "|" + player;
	}
}

// TODO: do I want them as own classes?
class ParticleNeutron extends Particle {
	public ParticleNeutron(int player) {
		super(player, 0);
	}

	public ParticleNeutron(int player, int posX, int posY) {
		super(player, 0, posX, posY);
	}
}