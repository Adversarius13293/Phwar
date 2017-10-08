package adver.sarius.phwar.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import adver.sarius.phwar.view.ModelListener;

/**
 * The board model of the game Phase War. It handles all the game operations and
 * logic, and stores the board data.
 */
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
	/**
	 * Current state of the turn. This way you can tell if the player already has
	 * moved or the game is won.
	 */
	private State state;
	/** All particles on the board. */
	private Set<Particle> particles;
	/**
	 * All the registered ModelListener. They will be informed if there are any
	 * changes in the model.
	 */
	private Set<ModelListener> listener = new HashSet<>();
	/** Stores a description of all the actions that already happened. **/
	private List<String> log = new ArrayList<>();
	/**
	 * Current round of the game. Starts at 1 and increments every time when all
	 * players finished their turn once.
	 **/
	private int round;

	// TODO: LogHistory?!
	// TODO: isWon() method to be able to check it for predefined setups
	// TODO: "deconstruct" removes particles to avoid the use of their position?
	// TODO: nextPlayer() automatically?
	// TODO: make getParticle() static?
	// TODO: inform listener in logMessage()?
	// TODO: call unchecked methods by checked ones?

	public PhwarBoard() {
		resetDefaultBoard();
	}

	/**
	 * Copy constructor. Also copies the particle Objects. But does NOT copy or
	 * register any listener or logs.
	 * 
	 * @param board
	 *            the board to copy everything from, besides listener.
	 */
	public PhwarBoard(PhwarBoard board) {
		this.size = board.size;
		this.playerQueue = new LinkedList<>(board.playerQueue);
		this.state = board.state;
		this.particles = new HashSet<>();
		board.particles.forEach(p -> this.particles.add(new Particle(p)));
		// this.listener = new HashSet<>(board.listener);
		counter++;
	}

	public static long counter;

	/**
	 * Lets the current player move one of his particles. He can only move once at
	 * the start of each turn.
	 * 
	 * @param startX
	 *            the x coordinate of the own particle to move with.
	 * @param startY
	 *            the y coordinate of the own particle to move with.
	 * @param targetX
	 *            the x coordinate of the empty target position.
	 * @param targetY
	 *            the y coordinate of the empty target position.
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
		logMessage("moves " + pStart.getName() + " from " + startX + "/" + startY + " to " + targetX + "/" + targetY
				+ ".");
		state = (targetX == 0 && targetY == 0 && pStart.getCharge() == 0) ? State.WON : State.MOVED;
		if (state == State.WON) {
			logMessage("wins by moving his " + pStart.getName() + " on the center cell.");
		}
		informListener();
		return state == State.WON;
	}

	/**
	 * Lets the current player capture an enemy particle with his particle. He needs
	 * to move first if possible.
	 * 
	 * @param ownX
	 *            the x coordinate of the own particle to capture with.
	 * @param ownY
	 *            the y coordinate of the own particle to capture with.
	 * @param oppX
	 *            the x coordinate of the enemy particle to capture.
	 * @param oppY
	 *            the y coordinate of the enemy particle to capture.
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
		if (opp.isPresent() && opp.get().getPlayer() != getCurrentPlayer()) {
			Optional<Particle> own = getParticle(ownX, ownY, particles);
			if (own.isPresent() && own.get().getPlayer() == getCurrentPlayer()) {

				if (computeParticlesToCaptureBy(own.get()).contains(opp.get())) {
					particles.remove(opp.get());
					own.get().setPos(oppX, oppY);
					state = State.CAPTURED;
					logMessage("captures " + opp.get().getName() + " at " + oppX + "/" + oppY + " from player "
							+ opp.get().getPlayer() + " with " + own.get().getName() + " at " + ownX + "/" + ownY
							+ ".");

					if ((checkParticleCount(opp.get().getPlayer()) && playerQueue.size() == 1)
							|| (own.get().getCharge() == 0 && own.get().getPosX() == 0 && own.get().getPosY() == 0)) {
						state = State.WON;
						logMessage("wins by doing this capture.");
					}
					informListener();
				} else {
					throw new IllegalCaptureException("That particle is not in reach to be captured.");
				}
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
		logMessage("finishes the turn.");
		state = State.NOT_MOVED;
		int lastPlayer = getCurrentPlayer();
		playerQueue.add(playerQueue.poll());
		if (lastPlayer > getCurrentPlayer()) {
			round++;
		}
		// logMessage("starts the turn.");
		informListener();
		return getCurrentPlayer();
	}

	/**
	 * Does not check any rules or board conditions! You should always use
	 * {@link #move(int, int, int, int) move}, unless you need a fast execution and
	 * are sure, that the move is valid. No listener are informed, and most of the
	 * logs will be skipped.
	 * 
	 * @see #move(int, int, int, int)
	 * @param startX
	 *            the x coordinate of the own particle to move with.
	 * @param startY
	 *            the y coordinate of the own particle to move with.
	 * @param targetX
	 *            the x coordinate of the empty target position.
	 * @param targetY
	 *            the y coordinate of the empty target position.
	 * @return true if the current (moving) player has won the game with his move,
	 *         otherwise false.
	 */
	public boolean moveUnchecked(int startX, int startY, int targetX, int targetY) {
		Particle pStart = getParticle(startX, startY, particles).get();
		pStart.setPos(targetX, targetY);
		state = (targetX == 0 && targetY == 0 && pStart.getCharge() == 0) ? State.WON : State.MOVED;
		return state == State.WON;
	}

	/**
	 * Does not check any rules or board conditions! You should always use
	 * {@link #capture(int, int, int, int) capture}, unless you need a fast
	 * execution and are sure, that the capture is valid. No listener are informed,
	 * and most of the logs will be skipped.
	 * 
	 * @see #capture(int, int, int, int)
	 * @param ownX
	 *            the x coordinate of the own particle to capture with.
	 * @param ownY
	 *            the y coordinate of the own particle to capture with.
	 * @param oppX
	 *            the x coordinate of the enemy particle to capture.
	 * @param oppY
	 *            the y coordinate of the enemy particle to capture.
	 * @return true if the current (capturing) player has won the game with his
	 *         capture, otherwise false.
	 */
	public boolean captureUnchecked(int ownX, int ownY, int oppX, int oppY) {
		Particle own = getParticle(ownX, ownY, particles).get();
		Particle opp = getParticle(oppX, oppY, particles).get();
		particles.remove(opp);
		own.setPos(oppX, oppY);
		state = State.CAPTURED;
		if ((checkParticleCount(opp.getPlayer()) && playerQueue.size() == 1)
				|| (own.getCharge() == 0 && own.getPosX() == 0 && own.getPosY() == 0)) {
			state = State.WON;
		}
		return state == State.WON;
	}

	/**
	 * Does not check any rules or board conditions! You should always use
	 * {@link #nextPlayer()}, unless you need a fast execution and are sure, that
	 * the turn is valid. No listener are informed, and most of the logs will be
	 * skipped.
	 * 
	 * @see #nextPlayer()
	 * @return the new current player.
	 */
	public int nextPlayerUnchecked() {
		state = State.NOT_MOVED;
		int lastPlayer = getCurrentPlayer();
		playerQueue.add(playerQueue.poll());
		if (lastPlayer > getCurrentPlayer()) {
			round++;
		}
		return getCurrentPlayer();
	}

	/**
	 * 
	 * @return all particles of the current player that are able to capture an enemy
	 *         as the key, and all the enemy particles it can capture as the value.
	 */
	public Map<Particle, Set<Particle>> computeParticlesThatCanCapture() {
		Map<Particle, Set<Particle>> ret = new HashMap<>();
		particles.stream().filter(p -> p.getPlayer() == getCurrentPlayer()).forEach(p -> {
			Set<Particle> toCapture = computeParticlesToCaptureBy(p);
			if (!toCapture.isEmpty()) {
				ret.put(p, toCapture);
			}
		});
		return ret;
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
			System.out.println("computeParticlesToCaptureBy() wrong player?");
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
	 *            the x coordinate of the capturer particle.
	 * @param posY
	 *            the y coordinate of the capturer particle.
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
		// can't move anywhere, is allowed to skip
		if (state == State.NOT_MOVED && particles.stream().filter(p -> p.getPlayer() == getCurrentPlayer())
				.noneMatch(p -> hasAtLeatOneCellToMove(p))) {
			logMessage("has no particle to move with. Skipping the move.");
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
	 *            the x coordinate of the start position.
	 * @param startY
	 *            the y coordinate of the start position.
	 * @param targetX
	 *            the x coordinate of the target position.
	 * @param targetY
	 *            the y coordinate of the target position.
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
	 *            the x coordinate of the cell to check for.
	 * @param posY
	 *            the x coordinate of the cell to check for.
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
	 *            the x coordinate of the start position.
	 * @param startY
	 *            the y coordinate of the start position.
	 * @param targetX
	 *            the x coordinate of the target position.
	 * @param targetY
	 *            the y coordinate of the target position.
	 * @return true if move would cross center 0/0. Starting from or targeting
	 *         center returns false.
	 */
	public boolean isCrossingCenter(int startX, int startY, int targetX, int targetY) {
		return (startX == 0 && targetX == 0 && startY * targetY < 0)
				|| (startY == 0 && targetY == 0 && startX * targetX < 0)
				|| (startX == startY && targetX == targetY && startX * targetX < 0);
	}

	/**
	 * 
	 * @param startX
	 *            the x coordinate of the start position.
	 * @param startY
	 *            the y coordinate of the start position.
	 * @param targetX
	 *            the x coordinate of the target position.
	 * @param targetY
	 *            the y coordinate of the target position.
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
		if (particles.stream().filter(p -> p.getPlayer() == playerToCheck)
				.collect(() -> new HashSet<>(), (s, p) -> s.add(p.getCharge()), (s1, s2) -> s1.addAll(s2))
				.size() != 3) {
			particles.removeIf(p -> p.getPlayer() == playerToCheck);
			playerQueue.remove(playerToCheck);
			logMessage(
					"removed player " + playerToCheck + " from the game by capturing the last particle of one charge.");
			return true;
		}
		return false;
	}

	/**
	 * Searches the given set for any particle that matches the position.
	 * 
	 * @param posX
	 *            the x coordinate of the position to look for a particle.
	 * @param posY
	 *            the y coordinate of the position to look for a particle
	 * @param particles
	 *            set of particles to be searched in.
	 * @return possibly found particle at the given position.
	 */
	public Optional<Particle> getParticle(int posX, int posY, Set<Particle> particles) {
		return particles.stream().filter(p -> p.getPosX() == posX && p.getPosY() == posY).findAny();
	}

	/**
	 * Returns a set of all the first particles found in each of the 6 directions.
	 * Particle on starting position will be ignored.
	 * 
	 * @param posX
	 *            the x coordinate of the position to look for other particle in
	 *            line of sight.
	 * @param posY
	 *            the y coordinate of the position to look for other particle in
	 *            line of sight.
	 * 
	 * @return set containing 0 to 6 particles.
	 */
	private Set<Particle> computeParticlesInLineOfSight(int posX, int posY) {
		// TODO: Find better algorithm?
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

	/**
	 * @return Set containing all the particles that are currently in-play.
	 */
	public Set<Particle> getParticles() {
		return Collections.unmodifiableSet(particles);
	}

	/**
	 * @return the current player that has to do its turn.
	 */
	public int getCurrentPlayer() {
		return playerQueue.peek();
	}

	/**
	 * @return the amount of players that haven't lost yet.
	 */
	public int getActivePlayerCount() {
		return playerQueue.size();
	}

	/**
	 * @return the current round of the game. Starts at 1 and increments every time
	 *         when all players finished their turn once.
	 */
	public int getRound() {
		return this.round;
	}

	/**
	 * Returns the radius of the hexagon board. Size=0 means just the middle
	 * hexagon, size=1 means the middle hexagon with 6 surrounding ones.
	 * 
	 * @return size of the board.
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * @return unmodifiable view of all saved log entries.
	 */
	public List<String> getLogEntries() {
		return Collections.unmodifiableList(log);
	}

	/**
	 * Register a listener to receive updates on data changes.
	 * 
	 * @param listener
	 *            the listener which should be informed about changes.
	 */
	public void registerModelListener(ModelListener listener) {
		this.listener.add(listener);
	}

	/**
	 * Informs all registered listeners that something may have changed.
	 */
	private void informListener() {
		listener.forEach(l -> l.modelChanged());
	}

	/**
	 * Appends the message to the log with the current time and player as prefix.
	 * 
	 * @param message
	 *            message to log
	 */
	private void logMessage(String message) {
		log.add("[" + LocalTime.now() + "] Player " + getCurrentPlayer() + " " + message);
	}

	/**
	 * Creates a default board setup.
	 */
	private void resetDefaultBoard() {
		int startingPlayers = 2;
		size = 5;
		round = 1;
		log = new ArrayList<>();
		playerQueue = new LinkedList<>();
		IntStream.range(0, startingPlayers).forEach(i -> playerQueue.add(i));
		state = State.NOT_MOVED;
		particles = new HashSet<>();
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

		// can't move
		// particles = new HashSet<>();
		// particles.add(new Particle(0,0,0,-5));
		// particles.add(new Particle(0,-1,-1,-5));
		// particles.add(new Particle(0,1,-2,-5));
		// particles.add(new Particle(1,1,-3,-5));
		// particles.add(new Particle(1,1,-2,-4));
		// particles.add(new Particle(1,1,-1,-4));
		// particles.add(new Particle(1,1,0,-4));
		// particles.add(new Particle(1,1,1,-4));
		// particles.add(new Particle(1,-1,0,1));
		// particles.add(new Particle(1,0,0,2));
		// particles.add(new Particle(1,1,0,0));

		informListener();
	}

	/**
	 * Returns a character for the string representation depending on player and
	 * particle charge. Null returns the symbol for an empty cell.
	 * 
	 * @param particle
	 *            the particle to get the character for.
	 * @return character identifying the particle type of the owner, or empty cell.
	 */
	private char getCharForParticle(Particle particle) {
		if (particle == null) {
			return '*';
		}
		char[] symbols;
		switch (particle.getPlayer()) {
		case 0:
			symbols = new char[] { '-', '0', '+' };
			break;
		case 1:
			symbols = new char[] { 'e', 'n', 'p' };
			break;
		default:
			symbols = new char[] { (char) ('A' + particle.getPlayer()), (char) ('B' + particle.getPlayer()),
					(char) ('C' + particle.getPlayer()) };
		}
		return symbols[particle.getCharge() + 1];
	}

	/**
	 * Returns a character representation of the current board with its particles.
	 * Each player has its own characters for every type of particles.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		int width = 2;
		char[] maxInset = new char[size * width];

		int x = 0;
		int y = -size;
		for (int i = 0; i <= 4 * size; i++) {
			int plusOne = 0;
			int loops;
			if (i < size) {
				loops = i;
			} else if (i < 3 * size) {
				plusOne = (i + size % 2) % 2;
				loops = size - plusOne;
			} else {
				loops = 4 * size - i;
			}

			builder.append(maxInset, 0, (size + x) * width + plusOne * width);
			for (int j = 0; j <= loops; j++) {
				// builder.append(x+j*2+plusOne).append("/").append(y+j);
				builder.append(getCharForParticle(getParticle(x + j * 2 + plusOne, y + j, particles).orElse(null)));
				builder.append(maxInset, 0, 2 * width - 1);
			}
			builder.append(System.lineSeparator());

			if (i < size) {
				x--;
			} else if (i < 3 * size) {
				y += 1 - plusOne;
			} else {
				x++;
				y++;
			}
		}
		return builder.toString();
	}
}

enum State {
	NOT_MOVED, MOVED, CAPTURED, WON
}