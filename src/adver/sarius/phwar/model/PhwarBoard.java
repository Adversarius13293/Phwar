package adver.sarius.phwar.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import adver.sarius.phwar.view.ModelListener;

public class PhwarBoard {

	/**
	 * Radius of the hexagon field. Size=0 means just the middle hexagon, size=1
	 * means the middle hexagon with 6 surrounding ones.
	 */
	private int size;

	// TODO: LogHistory?!
	private int numberOfPlayers;
	private int currentPlayer;
	private State state;

	private Set<ModelListener> listener = new HashSet<>();

	private Set<Particle> particles;

	private Map<Particle, Set<Particle>> toCapture = new HashMap<>();

	// TODO: fields durch cells ersetzen
	// TODO: particles nach draußen nur in lesend?

	public Map<Particle, Set<Particle>> move(int startX, int startY, int targetX, int targetY) {
		if (state != State.STARTED) {
			// TODO: Exception
		}
		if (startX == targetX && startY == targetY) {
			throw new IllegalMoveException("You need to move a distance of at least 1.");
		}
		if (!isStraightLine(startX, startY, targetX, targetY)) {
			throw new IllegalMoveException("You can only move in a straight line.");
		}
		if (!isInsideBoard(targetX, targetY)) {
			throw new IllegalMoveException("You can't move outside of the board.");
		}

		Optional<Particle> start = getParticle(startX, startY, particles);

		if (!start.isPresent()) {
			throw new IllegalMoveException("There has to be a particle on the start position.");
		}
		Particle pStart = start.get();
		if (pStart.getPlayer() != currentPlayer) {
			throw new IllegalMoveException("Only the current player is allowed to move.");
		}
		if (!isUnobstructedLine(startX, startY, targetX, targetY)) {
			throw new IllegalMoveException("You can't move on top or over particles.");
		}

		if (targetX == 0 && targetY == 0 && pStart.getCharge() == 0) {
			pStart.setPos(targetX, targetY);
			state = State.WON;
			informListener();
			return null;
		}

		Set<Particle> lineOfSight = getParticlesInLineOfSight(startX, startY);
		pStart.setPos(targetX, targetY);
		lineOfSight.addAll(getParticlesInLineOfSight(targetX, targetY));

		Set<Particle> capture = getParticlesToCapture(lineOfSight);

		capture.forEach(p -> {
			toCapture.put(p, getParticlesInLineOfSight(p.getPosX(), p.getPosY()).stream()
					.filter(p2 -> p2.getPlayer() == currentPlayer).collect(Collectors.toSet()));
		});

		state = State.MOVED;
		informListener();
		return copyCaptureMap(toCapture);
	}
	
	public int getCurrentPlayer() {
		return this.currentPlayer;
	}
	
	public int getPlayerCount() {
		return this.numberOfPlayers;
	}

	public Map<Particle, Set<Particle>> getToCaptureMap() {
		return copyCaptureMap(toCapture);
	}

	public void registerModelListener(ModelListener listener) {
		this.listener.add(listener);
	}

	private void informListener() {
		listener.forEach(l -> l.modelChanged());
	}

	// since I also keep the map for validation when capturing
	private Map<Particle, Set<Particle>> copyCaptureMap(Map<Particle, Set<Particle>> toCopy) {
		Map<Particle, Set<Particle>> ret = new HashMap<>();
		toCopy.forEach((p, pSet) -> {
			ret.put(p, Collections.unmodifiableSet(pSet));
		});
		return ret;
	}

	private Set<Particle> getParticlesToCapture(Set<Particle> particles) {
		Set<Particle> ret = new HashSet<>();

		particles.stream().filter(p -> p.getPlayer() != this.currentPlayer).forEach(p -> {
			int charge = 0;
			int ownParticles = 0;

			for (Particle p2 : getParticlesInLineOfSight(p.getPosX(), p.getPosY())) {
				charge += p2.getCharge();
				if (p2.getPlayer() == currentPlayer) {
					ownParticles++;
				}
			}
			if (charge == 0 && ownParticles >= 2) {
				ret.add(p);
			}
		});

		return ret;
	}

	// TODO: JavaDoc von @params einheitlich machen

	/**
	 * Tests if the line from start to target has any blocking particles. The line
	 * has to be valid (see {@link #isStraightLine(int, int, int, int)
	 * isStraightLine}). The start can be occupied, while the target has to be free.
	 * 
	 * @param startX
	 *            x coordinate of the start position
	 * @param startY
	 *            y coordinate of the start position
	 * @param targetX
	 *            x coordinate of the target position
	 * @param targetY
	 *            y coordinate of the target position
	 * @return true if there are no particles between start and target, and no
	 *         particle on target.
	 */
	private boolean isUnobstructedLine(int startX, int startY, int targetX, int targetY) {
		int signX = (int) Math.signum(startX - targetX);
		int signY = (int) Math.signum(startY - targetY);
		int x = 0;
		int y = 0;
		while (targetX + x != startX || targetY + y != startY) {
			if (getParticle(targetX + x, targetY + y, particles).isPresent()) {
				return false;
			}
			x += signX;
			y += signY;
		}
		return true;
	}

	/**
	 * @param posX
	 *            x coordinate
	 * @param posY
	 *            y coordinate
	 * @return true if position is inside the hexagon board.
	 */
	public boolean isInsideBoard(int posX, int posY) {
		if (Math.abs(posX) > size || Math.abs(posY) > size) {
			return false;
		}
		// TODO: optimize?
		// missing: 5/-1, 5/-2,4/-2, 5/-3,4/-3,3/-3, 5/-4,4/-4,3/-4,2/-4,
		// 5/-5,4/-5,3/-5,2/-5,1/-5
		// same for -5/1, -5/2,-4/2, ...
		for (int i = 1; i <= size; i++) {
			for (int j = size; j > size - i; j--) {
				if ((i == posX && -j == posY) || (-i == posX && j == posY)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isStraightLine(int startX, int startY, int targetX, int targetY) {
		int diffX = startX - targetX;
		int diffY = startY - targetY;
		// x=0 --> on y-axis | y=0 --> on x-axis | y=x --> diagonal
		return diffX == 0 || diffY == 0 || diffX == diffY;
	}

	public boolean hasWon() {
		return state == State.WON;
	}
	
	public void capture(int ownX, int ownY, int oppX, int oppY) {
		// TODO: test state?
		Optional<Particle> opp = getParticle(oppX, oppY, toCapture.keySet());
		if (opp.isPresent()) {
			Optional<Particle> own = getParticle(ownX, ownY, toCapture.get(opp.get()));
			if (own.isPresent()) {
				toCapture.remove(opp.get());
				toCapture.values().forEach(pset -> pset.remove(own.get())); // TODO: Test if callByReference
				particles.remove(opp.get());
				own.get().setPos(oppX, oppY);
				state = State.CAPTURED;

				// TODO: players >2
				Set<Particle> opponentParticles = particles.stream().filter(p -> p.getPlayer() != currentPlayer)
						.collect(Collectors.toSet());
				if (checkParticleCount(opponentParticles)
						|| (own.get().getCharge() == 0 && own.get().getPosX() == 0 && own.get().getPosY() == 0)) {
					state = State.WON;
				}
				informListener();
			} else {
				throw new IllegalCaptureException("You need to pick one particle from the current player.");
			}
		} else {
			throw new IllegalCaptureException("You need to pick one particle from the enemy player.");
		}
	}

	/**
	 * Checks if the given set of particles contain at least 1 electron, positron
	 * and neutron. If not, remove the particles from the game.
	 * 
	 * @param particlesOfPlayer
	 *            particles to be checked. Should be all particles from one player.
	 * @return true if removed the given particles, because the player lost.
	 */
	private boolean checkParticleCount(Set<Particle> particlesOfPlayer) {
		Map<Integer, Integer> counts = new HashMap<>();
		// opponentParticles.stream().collect(Collectors.toMap(
		// p -> p.getCharge(), p -> 1, (count1, count2) -> count1 + count2));
		particlesOfPlayer.forEach(p -> counts.merge(p.getCharge(), 1, (i1, i2) -> i1 + i2));
		if (counts.getOrDefault(0, 0) <= 0 || counts.getOrDefault(1, 0) <= 0 || counts.getOrDefault(-1, 0) <= 0) {
			particles.removeAll(particlesOfPlayer);
			return true;
		}
		return false;
	}

	public Optional<Particle> getParticle(int posX, int posY, Set<Particle> particles) {
		return particles.stream().filter(p -> p.getPosX() == posX && p.getPosY() == posY).findAny();
	}

	public int nextPlayer() {
		// check all states
		// TODO: RemovedPlayers if >2
		// captured all particles, or can't capture anymore?
		if (!toCapture.values().stream().allMatch(pSet -> pSet.isEmpty())) {
			System.out.println("TODO: Need to kick first.");
			// TODO: Exception
		}
		toCapture.clear();

		// TODO: Test if can skip

		currentPlayer = (currentPlayer + 1) % numberOfPlayers;
		informListener();
		return currentPlayer;
	}

	/**
	 * Returns a set of all the first particles found in each of the 6 directions.
	 * Particle on starting position will be ignored.
	 * 
	 * @param posX
	 *            starting x coordinate
	 * @param posY
	 *            starting y coordinate
	 * @return set containing 0 to 6 particles.
	 */
	private Set<Particle> getParticlesInLineOfSight(int posX, int posY) {
		// TODO: Find better solution?
		// corners outside the board also checked...
		Set<Particle> ret = new HashSet<>();
		for (int i = posX + 1; i <= size; i++) { // diagonal top right
			Optional<Particle> part = getParticle(i, posY, particles);
			if (part.isPresent()) {
				ret.add(part.get());
				break;
			}
		}
		for (int i = posX - 1; i >= -size; i--) { // diagonal bottom left
			Optional<Particle> part = getParticle(i, posY, particles);
			if (part.isPresent()) {
				ret.add(part.get());
				break;
			}
		}
		for (int i = posY + 1; i <= size; i++) { // top
			Optional<Particle> part = getParticle(posX, i, particles);
			if (part.isPresent()) {
				ret.add(part.get());
				break;
			}
		}
		for (int i = posY - 1; i >= -size; i--) { // bottom
			Optional<Particle> part = getParticle(posX, i, particles);
			if (part.isPresent()) {
				ret.add(part.get());
				break;
			}
		}
		for (int i = 1; posX + i <= size && posY + i <= size; i++) { // bottom right
			Optional<Particle> part = getParticle(posX + i, posY + i, particles);
			if (part.isPresent()) {
				ret.add(part.get());
				break;
			}
		}
		for (int i = -1; posX + i >= -size && posY + i >= -size; i--) { // top left
			Optional<Particle> part = getParticle(posX + i, posY + i, particles);
			if (part.isPresent()) {
				ret.add(part.get());
				break;
			}
		}
		return ret;
	}

	public void resetDefaultBoard() {
		size = 5;
		numberOfPlayers = 2;
		currentPlayer = 0;
		state = State.STARTED;
		particles = new HashSet<>(); // TODO: positions relative to size?
		particles.add(new Particle(0, -1, 0, 3));
		particles.add(new Particle(0, -1, 0, 4));
		particles.add(new Particle(0, 0, 0, 5));
		particles.add(new Particle(0, 1, 1, 4));
		particles.add(new Particle(0, 1, 1, 5));
		particles.add(new Particle(0, -1, 2, 5));
		particles.add(new Particle(0, 1, -1, 3));
		particles.add(new Particle(0, 1, -1, 4));
		particles.add(new Particle(0, -1, -2, 3));
		particles.add(new Particle(1, -1, 0, -3));
		particles.add(new Particle(1, -1, 0, -4));
		particles.add(new Particle(1, 0, 0, -5));
		particles.add(new Particle(1, 1, 1, -3));
		particles.add(new Particle(1, 1, 1, -4));
		particles.add(new Particle(1, -1, 2, -3));
		particles.add(new Particle(1, 1, -1, -4));
		particles.add(new Particle(1, 1, -1, -5));
		particles.add(new Particle(1, -1, -2, -5));
	}
	// KIs in Board oder Controller? Board kann kein UserInput, aber hat alle Daten
	// --> nicht ins board...

	public int getSize() {
		return this.size;
	}

	public Set<Particle> getParticles() {
		return Collections.unmodifiableSet(particles);
	}

	@Override
	public String toString() {
		// TODO: More player, different sizes ...
		StringBuilder builder = new StringBuilder();
		String prefix = "                         ";

		// 0/-5
		// -1/-5 1/-4
		// -2/-5 0/-5 2/-3
		// -3/

		return builder.toString();
	}
}

enum State {
	STARTED, MOVED, CAPTURED, WON
}

// capture, and after that another capture possible?
// opponent puts in bad position --> need to move first. But can I capture that
// bad position, even if I changed nothing with my move? --> would so no.
// can i capture particles by moving my particle away, or only the ones i can
// capture from target position
// multiple captures, then all at once --> no chains? and what if you take away
// all valid particles for other captures?
// particle can only capture once!?