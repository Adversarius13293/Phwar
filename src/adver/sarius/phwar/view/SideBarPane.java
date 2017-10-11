package adver.sarius.phwar.view;

import java.util.Arrays;

import adver.sarius.phwar.model.PhwarBoard;
import adver.sarius.util.javafx.DragResizer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SideBarPane extends BorderPane implements ModelListener {

	private PhwarBoard board;
	private HBox[] playerDisplays;
	private Button buttonNext;

	public SideBarPane(PhwarBoard board) {
		super();
		this.board = board;
		board.registerModelListener(this);
		// TODO: player-ids have to be starting continuous from 0...
		playerDisplays = new HBox[board.getActivePlayerCount()]; 
		Arrays.setAll(playerDisplays, i -> {
			HBox box = new HBox(new Label("Current player: "), new ParticleStackPane(i, 0, 30, -1));
			box.setAlignment(Pos.CENTER_LEFT);
			return box;
		});

		VBox statusView = new VBox();
		labelRound = new Label();
		statusView.getChildren().add(labelRound);

		ListView<String> logView = new ListView<>();
		logEntries = FXCollections.observableArrayList();
		logView.setItems(logEntries);
		logView.setMinHeight(60);
		logView.setPrefWidth(328);
		DragResizer.makeResizable(logView, DragResizer.EAST+DragResizer.SOUTH);

		statusView.getChildren().add(logView);

		setCenter(statusView);

		buttonNext = new Button("Start game");
		setBottom(buttonNext);

		setPadding(new Insets(5));
		setStyle("-fx-border-color: black");
		modelChanged();
	}

	private Label labelRound;
	private ObservableList<String> logEntries;

	public Button getButtonNext() {
		return buttonNext;
	}

	@Override
	public void modelChanged() {
		setTop(playerDisplays[board.getCurrentPlayer()]);
		labelRound.setText("Round: " + board.getRound());
		// TODO: find better way than always deleting and adding every entry.
		logEntries.clear();
		logEntries.addAll(board.getLogEntries());
	}
}