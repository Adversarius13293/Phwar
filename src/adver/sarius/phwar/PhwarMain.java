package adver.sarius.phwar;

import java.io.IOException;

import adver.sarius.phwar.model.PhwarBoard;
import adver.sarius.phwar.view.PhwarBoardView;
import adver.sarius.phwar.view.SideBarPane;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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

		double hexaSize = 40;
		
		PhwarBoard board = new PhwarBoard();

		SideBarPane sideBar = new SideBarPane(board);
		layout.setLeft(sideBar);
		
		PhwarBoardController controller = new PhwarBoardController(board, sideBar.getButtonNext());
		PhwarBoardView view = new PhwarBoardView(board, hexaSize, controller::handleHexagonClick);
		controller.initGetHexagonFunc(view::getHexagon);
		layout.setCenter(view);
		
		Label feedback = new Label(controller.getFeedbackProperty().get());
		controller.getFeedbackProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				feedback.setText(newValue);
			}
		});
		layout.setBottom(feedback);
		
		primaryStage.sizeToScene();
		primaryStage.centerOnScreen();
	}
	
	@Override
	public void stop() {
//		System.out.println("stoped");
	}

	public static void main(String[] args) {
		launch(args);
	}
}