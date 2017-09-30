package adver.sarius.phwar.view;

import java.util.Arrays;

import adver.sarius.phwar.model.PhwarBoard;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class StatusView extends BorderPane implements ModelListener{
	
	private PhwarBoard board;
	private ParticleStackPane[] playerParticles;
	private Button buttonNext;
	
	public StatusView(PhwarBoard board) {
		super();
		this.board = board;
		board.registerModelListener(this);
		
		playerParticles = new ParticleStackPane[board.getPlayerCount()];
		Arrays.setAll(playerParticles, i -> new ParticleStackPane(i, 0, 30, -1));
		modelChanged();
		
		buttonNext = new Button("Start game");
		setBottom(buttonNext);
	}
	
	public Button getButtonNext() {
		return buttonNext;
	}
	
	
	@Override
	public void modelChanged() {
		setTop(playerParticles[board.getCurrentPlayer()]);
	}
	
	

}
