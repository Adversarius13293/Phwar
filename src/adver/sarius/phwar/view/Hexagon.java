package adver.sarius.phwar.view;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class Hexagon extends Polygon {
	private int posX;
	private int posY;

	public Hexagon(double size) {
		super();
		for (int i = 0; i < 6; i++) {
			double rad = Math.toRadians(60 * i);
			getPoints().addAll(Math.cos(rad) * size, Math.sin(rad) * size);
		}
//		Color color = new Color(Math.random(), Math.random(), Math.random(), 0.1);
//		setFill(color);
//		setStroke(new Color(color.getRed(), color.getGreen(), color.getBlue(), 1));
		setStroke(Color.BLACK);
		render();
	}

	public Hexagon(double size, int posX, int posY) {
		this(size);
		this.posX = posX;
		this.posY = posY;
	}

	private void render() {
		if (clicked) {
			this.setStrokeWidth(3);
		} else {
			this.setStrokeWidth(1);
		}
		
		if(!capturer || !toCapture) {
			setFill(Color.WHITE);
		}
		// override above
		if (capturer) {
			setFill(Color.GREEN);
		}
		// override above. both at once should never be possible... TODO:
		if(toCapture) {
			setFill(Color.RED);
		}
	}

	private boolean clicked = false;
	private boolean capturer = false;
	private boolean toCapture = false;

	public void setClicked(boolean clicked) {
		if (this.clicked != clicked) {
			this.clicked = clicked;
			render();
		}
	}

	public void setCapturer(boolean capturer) {
		if (this.capturer != capturer) {
			this.capturer = capturer;
			render();
		}
	}
	
	public void setToCapture(boolean toCapture) {
		if (this.toCapture != toCapture) {
			this.toCapture = toCapture;
			render();
		}
	}

	public int getPosX() {
		return this.posX;
	}

	public int getPosY() {
		return this.posY;
	}
}