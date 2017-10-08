package adver.sarius.phwar.model;

public class Particle {
	private int player;
	private int charge;
	private int posX;
	private int posY;

	public Particle(int player, int charge) {
		this.player = player;
		this.charge = charge;
	}

	public Particle(int player, int charge, int posX, int posY) {
		this.player = player;
		this.charge = charge;
		this.posX = posX;
		this.posY = posY;
	}
	
	public Particle(Particle particle) {
		this.player = particle.player;
		this.charge = particle.charge;
		this.posX = particle.posX;
		this.posY = particle.posY;
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
	
	public String getName() {
		if(charge == -1) {
			return "electron";
		} else if(charge == 0) {
			return "neutron";
		} else if(charge == 1) {
			return "positron";
		} else {
			return "UNKNOWN";
		}
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