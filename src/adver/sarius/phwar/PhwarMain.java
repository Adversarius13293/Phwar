package adver.sarius.phwar;

import java.io.IOException;

import javax.swing.RootPaneContainer;

import adver.sarius.phwar.model.PhwarBoard;
import adver.sarius.phwar.view.PhwarBoardView;
import adver.sarius.phwar.view.StatusView;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class PhwarMain extends Application {

	// stage and scene
	// stage: top-level container
	// scene: content container

	@Override
	public void start(Stage primaryStage) {
		Scene scene=null;
		BorderPane layout = null;
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(PhwarMain.class.getResource("view/RootLayout.fxml"));
			layout = (BorderPane) loader.load();
			scene = new Scene(layout);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		primaryStage.setTitle("Phase War");
		primaryStage.setScene(scene);
		primaryStage.show();

		int hexaSize = 40;
		
		PhwarBoard board = new PhwarBoard();
		board.resetDefaultBoard();

		StatusView status = new StatusView(board);
		layout.setLeft(status);
		
		PhwarBoardController controller = new PhwarBoardController(board, status.getButtonNext());
		PhwarBoardView view = new PhwarBoardView(board, hexaSize, controller::handleHexagonClick);
		controller.initGetHexagonFunc(view::getHexagon);
		layout.setCenter(view);
		
		// TODO: Sizes
		primaryStage.setWidth(view.getPrefWidth());
	}
	
	@Override
	public void stop() {
//		System.out.println("stoped");
	}

	public static void main(String[] args) {
		launch(args);
	}
}