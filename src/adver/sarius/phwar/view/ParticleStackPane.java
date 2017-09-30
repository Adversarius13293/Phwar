package adver.sarius.phwar.view;

import java.util.Random;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class ParticleStackPane extends StackPane {
	public ParticleStackPane(int player, int charge, double radius, double rectangleSize) {
		super();
		setPickOnBounds(false);

		Circle circle = new Circle();
		circle.setMouseTransparent(true);
		circle.setRadius(radius);
		circle.setFill(getColorForCircle(player));
		circle.setStroke(Color.BLACK);
		circle.setStrokeWidth(radius / 20);
		getChildren().add(circle);
		if (charge != 0) {
			Rectangle rec = new Rectangle(rectangleSize, rectangleSize / 8, getColorForRect(player));
			rec.setMouseTransparent(true);
			getChildren().add(rec);
			if (charge > 0) {
				rec = new Rectangle(rectangleSize / 8, rectangleSize, getColorForRect(player));
				rec.setMouseTransparent(true);
				getChildren().add(rec);
			}
		}
	}

	private Paint getColorForCircle(int player) {
		Paint ret;
		switch (player) {
		case 0:
			ret = Color.WHITE;
			break;
		case 1:
			ret = Color.BLACK;
			break;
		case 2:
			ret = Color.LIGHTGRAY;
			break;
		default: // TODO: Test
			Random random = new Random(player);
			ret = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
		}
		return ret;
	}

	private Paint getColorForRect(int player) {
		Paint ret;
		switch (player) {
		case 1:
			ret = Color.WHITE;
			break;
		default:
			ret = Color.BLACK;
		}
		return ret;
	}

}