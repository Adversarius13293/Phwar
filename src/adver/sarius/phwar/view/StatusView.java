package adver.sarius.phwar.view;

import java.util.Arrays;

import adver.sarius.phwar.model.PhwarBoard;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class StatusView extends BorderPane implements ModelListener {

	private PhwarBoard board;
	private VBox[] playerDisplays;
	private Button buttonNext;

	public StatusView(PhwarBoard board) {
		super();
		this.board = board;
		board.registerModelListener(this);

		playerDisplays = new VBox[board.getActivePlayerCount()]; // TODO: player-ids have to be starting continuous from 0... 
		Arrays.setAll(playerDisplays, i -> new VBox(new Label("Current player:"), new ParticleStackPane(i, 0, 30, -1)));
		modelChanged();

		buttonNext = new Button("Start game");
		setBottom(buttonNext);

		setPadding(new Insets(5));
		setStyle("-fx-border-color: black");
	}

	public Button getButtonNext() {
		return buttonNext;
	}

	@Override
	public void modelChanged() {
		setTop(playerDisplays[board.getCurrentPlayer()]);
	}
}