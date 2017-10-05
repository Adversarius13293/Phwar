package adver.sarius.phwar.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import adver.sarius.phwar.view.ModelListener;

public class PhwarBoard {

	/**
	 * Radius of the hexagon board. Size=0 means just the middle hexagon, size=1
	 * means the middle hexagon with 6 surrounding ones.
	 */
	private int size;
	/**
	 * Queue representing all active players in their order of succession. Current
	 * player is the first element.
	 */
	private Queue<Integer> playerQueue;
	/** Current state of the turn. */
	private State state;
	/** All particles on the board. */
	private Set<Particle> particles;
	/**
	 * All the registered ModelListener. They will be informed if there are any
	 * changes in the model.
	 */
	private Set<ModelListener> listener = new HashSet<>();

	// TODO: particles only reading access.
	// TODO: LogHistory?!
	// TODO: Cant move but could kick? Need to check skipmove/ in chekcstate in
	// capture
	// TODO: isWon() method to be able to check it for predefined setups
	// TODO: "deconstruct" removes particles to avoid the use of their position?
	// TODO: JavaDoc @params uniform
	// TODO: nextPlayer() automatically?

	public PhwarBoard() {
	}

	/**
	 * Lets the current player move one of his particles. He can only move once at
	 * the start of each turn.
	 * 
	 * @param startX
	 * @param startY
	 * @param targetX
	 * @param targetY
	 * @return true if the current (moving) player has won the game with his move,
	 *         otherwise false.
	 * @throws IllegalMoveException
	 *             if the move is invalid by any reason.
	 */
	public boolean move(int startX, int startY, int targetX, int targetY) {
		if (state != State.NOT_MOVED) {
			throw new IllegalMoveException("You already moved.");
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
		if (isCrossingCenter(startX, startY, targetX, targetY)) {
			throw new IllegalMoveException("You can't skip over the center point.");
		}

		Optional<Particle> start = getParticle(startX, startY, particles);

		if (!start.isPresent()) {
			throw new IllegalMoveException("There has to be a particle on the start position.");
		}
		Particle pStart = start.get();
		if (pStart.getPlayer() != getCurrentPlayer()) {
			throw new IllegalMoveException("Only the current player is allowed to move.");
		}
		if (!isUnobstructedLine(startX, startY, targetX, targetY)) {
			throw new IllegalMoveException("You can't move on top or over particles.");
		}

		pStart.setPos(targetX, targetY);
		state = (targetX == 0 && targetY == 0 && pStart.getCharge() == 0) ? State.WON : State.MOVED;
		informListener();
		return state == State.WON;
	}

	/**
	 * Lets the current player capture an enemy particle with his particle. He needs
	 * to move first if possible.
	 * 
	 * @param ownX
	 * @param ownY
	 * @param oppX
	 * @param oppY
	 * @return true if the current (capturing) player has won the game with his
	 *         capture, otherwise false.
	 * @throws IllegalCaptureException
	 *             if the capture is invalid by any reason.
	 */
	public boolean capture(int ownX, int ownY, int oppX, int oppY) {
		if (needToMove()) {
			throw new IllegalCaptureException("You need to move first before capturing.");
		}
		if (state == State.WON) {
			throw new IllegalCaptureException("The game is already won.");
		}
		Optional<Particle> opp = getParticle(oppX, oppY, particles);
		if (opp.isPresent()) {
			Optional<Particle> own = getParticle(ownX, ownY, particles);
			if (own.isPresent()) {
				particles.remove(opp.get());
				own.get().setPos(oppX, oppY);
				state = State.CAPTURED;

				if ((checkParticleCount(opp.get().getPlayer()) && playerQueue.size() == 1)
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
		return state == State.WON;
	}

	/**
	 * Finishes the current turn and moves on to the next player
	 * 
	 * @return the new current player.
	 */
	public int nextPlayer() {
		if (needToMove()) {
			throw new IllegalTurnException("You need to move before ending the turn.");
		}
		if (state == State.WON) {
			throw new IllegalTurnException("The game is already won.");
		}
		if (!computeParticlesThatCanCapture().isEmpty()) {
			throw new IllegalTurnException("You need to capture all possible particles first.");
		}
		state = State.NOT_MOVED;
		playerQueue.add(playerQueue.poll());
		informListener();
		return playerQueue.peek();
	}

	/**
	 * @return all particles of the current player that are able to capture an
	 *         enemy.
	 */
	public Set<Particle> computeParticlesThatCanCapture() {
		return particles.stream().filter(p -> p.getPlayer() == getCurrentPlayer())
				.filter(p -> !computeParticlesToCaptureBy(p).isEmpty()).collect(Collectors.toSet());
	}

	/**
	 * Determines all particles in line of sight of the given capturer that can
	 * actually be captured. A particle can be captured if the charge of its
	 * particles in line of sight sums up to 0, and at least two of the particles
	 * belong to the capturing player. The capturer particle should belong to the
	 * current player.
	 * 
	 * @param capturer
	 *            particle that should be checked for enemies to capture.
	 * @return all enemy particles the capturer is allowed to capture.
	 */
	public Set<Particle> computeParticlesToCaptureBy(Particle capturer) {
		if (capturer.getPlayer() != getCurrentPlayer()) {
			// TODO: What now? Do I care?
		}
		return computeParticlesInLineOfSight(capturer.getPosX(), capturer.getPosY()).stream()
				.filter(p -> p.getPlayer() != capturer.getPlayer()).filter(p -> {
					int charge = 0;
					int ownParticles = 0;
					for (Particle p2 : computeParticlesInLineOfSight(p.getPosX(), p.getPosY())) {
						charge += p2.getCharge();
						if (p2.getPlayer() == capturer.getPlayer()) {
							ownParticles++;
						}
					}
					return charge == 0 && ownParticles >= 2;
				}).collect(Collectors.toSet());
	}

	/**
	 * Determines all particles in line of sight of the given capturer that can
	 * actually be captured. A particle can be captured if the charge of its
	 * particles in line of sight sums up to 0, and at least two of the particles
	 * belong to the capturing player. The capturer particle should belong to the
	 * current player, and there has to be a particle at the given position.
	 * 
	 * @param posX
	 *            x-coordinate of the position of the capturer particle.
	 * @param posY
	 *            y-coordinate of the position of the capturer particle.
	 * @return all enemy particles the capturer is allowed to capture.
	 */
	public Set<Particle> computeParticlesToCaptureBy(int capturerPosX, int capturerPosY) {
		Optional<Particle> part = getParticle(capturerPosX, capturerPosY, particles);
		if (!part.isPresent()) {
			throw new PhwarBoardException("No particle at the given position found that could do the capture.");
		}
		return computeParticlesToCaptureBy(part.get());
	}

	/**
	 * Determines if the current player still needs to do his move. Also the only
	 * way to skip the move turn, if the player can't move anywhere.
	 * 
	 * @return true if the player still needs to move. False if he already has moved
	 *         or needs to skip his move.
	 */
	public boolean needToMove() {
		// cant move anywhere, has to skip
		if (particles.stream().filter(p -> p.getPlayer() == getCurrentPlayer())
				.noneMatch(p -> hasAtLeatOneCellToMove(p))) {
			state = State.MOVED;
		}
		return state == State.NOT_MOVED;
	}

	/**
	 * Checks if the given particle has at least one free cell directly around.
	 * 
	 * @param particle
	 *            particle to check for
	 * @return true if at least one free cell is in reach.
	 */
	private boolean hasAtLeatOneCellToMove(Particle particle) {
		for (int x = -1; x < 2; x++) {
			for (int y = -1; y < 2; y++) {
				if (x == -y || !isInsideBoard(particle.getPosX() + x, particle.getPosY() + y)) {
					continue;
				}
				if (!getParticle(particle.getPosX() + x, particle.getPosY() + y, particles).isPresent()) {
					return true;
				}
			}
		}
		return false;
	}

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
		// direction from target to start
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
		// missing corners: 5/-1, 5/-2,4/-2, 5/-3,4/-3,3/-3,
		// 5/-4,4/-4,3/-4,2/-4,5/-5,4/-5,3/-5,2/-5,1/-5
		// same for other: -5/1, -5/2,-4/2, ...
		for (int i = 1; i <= size; i++) {
			for (int j = size; j > size - i; j--) {
				if ((i == posX && -j == posY) || (-i == posX && j == posY)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @param startX
	 * @param startY
	 * @param targetX
	 * @param targetY
	 * @return true if move would cross center 0/0. Starting from or targeting
	 *         center returns false.
	 */
	private boolean isCrossingCenter(int startX, int startY, int targetX, int targetY) {
		return (startX == 0 && targetX == 0 && startY * targetY < 0)
				|| (startY == 0 && targetY == 0 && startX * targetX < 0)
				|| (startX == startY && targetX == targetY && startX * targetX < 0);
	}

	/**
	 * 
	 * @param startX
	 * @param startY
	 * @param targetX
	 * @param targetY
	 * @return true if you can move from start to target in a straight line.
	 */
	private boolean isStraightLine(int startX, int startY, int targetX, int targetY) {
		int diffX = startX - targetX;
		int diffY = startY - targetY;
		// x=0 --> on y-axis | y=0 --> on x-axis | y=x --> diagonal
		return diffX == 0 || diffY == 0 || diffX == diffY;
	}

	/**
	 * @return true if the game is already won.
	 */
	public boolean hasWon() {
		return state == State.WON;
	}

	/**
	 * Checks if the given player owns at least 1 electron, positron, and neutron.
	 * If not, remove the player and his particles from the game.
	 * 
	 * @param playerToCheck
	 *            player to be checked.
	 * @return true if removed the given player and particles, because he lost.
	 */
	private boolean checkParticleCount(int playerToCheck) {
		Map<Integer, Integer> counts = new HashMap<>();
		particles.stream().filter(p -> p.getPlayer() == playerToCheck)
				.forEach(p -> counts.merge(p.getCharge(), 1, (i1, i2) -> i1 + i2));
		if (counts.getOrDefault(0, 0) <= 0 || counts.getOrDefault(1, 0) <= 0 || counts.getOrDefault(-1, 0) <= 0) {
			// TODO: do it more fancy
			Set<Particle> particlesOfPlayer = particles.stream().filter(p -> p.getPlayer() != getCurrentPlayer())
					.collect(Collectors.toSet());
			particles.removeAll(particlesOfPlayer);
			return true;
		}
		return false;
	}

	public int getCurrentPlayer() {
		return playerQueue.peek();
	}

	public int getActivePlayerCount() {
		return playerQueue.size();
	}

	public void registerModelListener(ModelListener listener) {
		this.listener.add(listener);
	}

	private void informListener() {
		listener.forEach(l -> l.modelChanged());
	}

	public Optional<Particle> getParticle(int posX, int posY, Set<Particle> particles) {
		return particles.stream().filter(p -> p.getPosX() == posX && p.getPosY() == posY).findAny();
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
	private Set<Particle> computeParticlesInLineOfSight(int posX, int posY) {
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
		int startingPlayers = 2;
		size = 5;
		playerQueue = new ArrayBlockingQueue<>(startingPlayers);
		IntStream.range(0, startingPlayers).forEach(i -> playerQueue.add(i));
		state = State.NOT_MOVED;
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

		// capture, and after that another capture possible?
		// particles = new HashSet<>();
		// particles.add(new Particle(0, 0, -1, 4));
		// particles.add(new Particle(1, 0, -1, -4));
		// particles.add(new Particle(1, 1, -3, -2));
		// particles.add(new Particle(1, 1, -2, -1));
		// particles.add(new Particle(1, -1, -2, 0));
		// particles.add(new Particle(0, -1, 0, -2));
		// particles.add(new Particle(0, -1, 1, -1));
		// particles.add(new Particle(0, 1, 2, 2));

		// take away all valid capturer?
		// particles = new HashSet<>();
		// particles.add(new Particle(0, 0, 2, 5));
		// particles.add(new Particle(1, 0, -4, -5));
		// particles.add(new Particle(1, -1, -5, -4));
		// particles.add(new Particle(1, 1, -1, -1));
		// particles.add(new Particle(1, 1, -1, -1));
		// particles.add(new Particle(1, 1, 0, -2));
		// particles.add(new Particle(1, 1, -2, 0));
		// particles.add(new Particle(0, -1, 4, 0));
		// particles.add(new Particle(0, 1, -1, -2));
		// particles.add(new Particle(0, 1, -3, 0));

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
	NOT_MOVED, MOVED, CAPTURED, WON
}