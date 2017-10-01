package adver.sarius.phwar.view;

import java.util.ListIterator;
import java.util.stream.IntStream;

import adver.sarius.phwar.model.Particle;
import adver.sarius.phwar.model.PhwarBoard;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class PhwarBoardView extends GridPane implements ModelListener {

	private PhwarBoard board;
	private double hexaSize;
	// I dont like it, but since the controller is part of the view, I cant keep them completely separated.
	private EventHandler<MouseEvent> mouseClicked; 

	public PhwarBoardView(PhwarBoard board, double hexaSize, EventHandler<MouseEvent> mouseClicked) {
		this.board = board;
		this.hexaSize = hexaSize;
		this.mouseClicked = mouseClicked;

		constructView();

		board.registerModelListener(this);
//		setGridLinesVisible(true);
	}

	// TODO: should/can I just rebuild the whole board instead?
	private void redoParticles() {
		double gridHeight = Math.cos(Math.toRadians(30)) * hexaSize;
		for (Node node : getChildren()) {
			if (node instanceof StackPane) {
				StackPane hexaPane = (StackPane) node;
				for (ListIterator<Node> iter =hexaPane.getChildren().listIterator();iter.hasNext();) {
					Node n = iter.next();
					if (n instanceof ParticleStackPane) {
						iter.remove();
					} else if (n instanceof Hexagon) {
						Hexagon hexa = (Hexagon) n;
						Particle particle = board.getParticle(hexa.getPosX(), hexa.getPosY(), board.getParticles())
								.orElse(null);
						if (particle != null) {
							
							iter.add(new ParticleStackPane(particle.getPlayer(), particle.getCharge(),
									gridHeight * 0.91, hexaSize));
						}
					} else {
						if(n instanceof Label) {
							// do nothing
						} else {
							System.out.println("redoParticles ex");
						}
					}
				}
			} else {
				System.out.println("redoParticles ex2");
				// TODO: Exception?
			}
		}
	}

	private void constructView() {
		int boardSize = board.getSize();

		double gridWidth = Math.cos(Math.toRadians(60)) * hexaSize + hexaSize;
		double gridHeight = Math.cos(Math.toRadians(30)) * hexaSize;
		
		int centerCol = boardSize;
		int centerRow = 2 * boardSize;

		for (int col = 0; col < boardSize * 2 + 1; col++) {
			// only use every second row, alternating depending on the column. Also alternating start position depending on board size.
			for (int row = (col + boardSize % 2) % 2; row < boardSize * 4 + 1; row += 2) { 
				int boardX = col - centerCol;
				int boardY = (int) ((row - centerRow) / 2. + (col - centerCol) / 2.); // TODO: compute via method?
				if (!board.isInsideBoard(boardX, boardY)) {
					// should already skip them while iterating. But this way I keep the logic for the board in one place.
					continue; 
				}

				StackPane hexaPane = new StackPane();
				hexaPane.setPickOnBounds(false);
				add(hexaPane, col, row);

				Hexagon hexa = new Hexagon(hexaSize, boardX, boardY);
				hexaPane.getChildren().add(hexa);
				hexa.setOnMouseClicked(mouseClicked);
				
				boolean showCoords = true;
				if(showCoords) {
					Label coords = new Label(boardX+"/"+boardY);
					coords.setMouseTransparent(true);
					hexaPane.getChildren().add(coords);
					coords.setTextFill(Color.ORANGERED);
					StackPane.setAlignment(coords, Pos.TOP_CENTER);
				}
			}
			ColumnConstraints cc = new ColumnConstraints(gridWidth);
			cc.setHalignment(HPos.LEFT);
			getColumnConstraints().add(cc);
		}
		// one additional empty row for overlapping hexagon 
		IntStream.range(0, boardSize * 4 + 2).forEach(i -> {
			RowConstraints rc = new RowConstraints(gridHeight);
			rc.setValignment(VPos.TOP);
			getRowConstraints().add(rc);
		});
		redoParticles();

		this.setPrefWidth(gridWidth*(boardSize*2+2));
		this.setMaxWidth(gridWidth*(boardSize*2+2));
		this.setMinWidth(gridWidth*(boardSize*2+2));
	}
	
	public Hexagon getHexagon(int boardPosX, int boardPosY) {
		for(Node node : getChildren()) {
			if(node instanceof StackPane) {
				for(Node n : ((StackPane)node).getChildren()) {
					if(n instanceof Hexagon) {
						Hexagon hexa = (Hexagon)n;
						if(hexa.getPosX() == boardPosX && hexa.getPosY() == boardPosY) {
							return hexa;
						}
					}
				}
			} else {
				// TODO: Exception
			}
		}
		System.out.println(this.getClass() + "getHexagon() returned null");
		return null; // TODO: Exception?
	}
	
	@Override
	public void modelChanged() {
		redoParticles();
	}
}