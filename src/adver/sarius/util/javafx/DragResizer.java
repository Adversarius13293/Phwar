package adver.sarius.util.javafx;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * BORROWED FROM:
 * http://andrewtill.blogspot.com/2012/12/dragging-to-resize-javafx-region.html
 * and https://gist.github.com/hitchcock9307/b8d40576f11794c08cae783610771ea8
 * </br>
 * Modified to support all side resizing, depending on parameter (no edges) Also
 * only setting the preferred size now.
 *
 * <p>
 * <p>
 * {@link DragResizer} can be used to add mouse listeners to a {@link Region}
 * and make it resizable by the user by clicking and dragging the border in the
 * same way as a window.
 * <p>
 * Usage:
 * 
 * <pre>
 * DragResizer.makeResizable(myAnchorPane, DragResizer.EAST + DragResizer.SOUTH);
 * </pre>
 *
 * @author atill, hitchcock9307, JohnnyAW, Adversarius13293
 */
public class DragResizer {

	/**
	 * The margin around the control that a user can click in to start resizing the
	 * region.
	 */
	private static final int RESIZE_MARGIN = 2;

	private final Region region;
	private short dragging = 0;
	private int allowedDirections = 0;
	private boolean initPref = false;

	public static final short NOTDRAGGING = 0;
	public static final short NORTH = 1;
	public static final short SOUTH = 2;
	public static final short EAST = 4;
	public static final short WEST = 8;

	private DragResizer(Region aRegion, int allowedDirections) {
		region = aRegion;
		this.allowedDirections = allowedDirections;
	}

	public static void makeResizable(Region region) {
		makeResizable(region, NORTH + SOUTH + EAST + WEST);
	}

	public static void makeResizable(Region region, int allowedDirections) {
		final DragResizer resizer = new DragResizer(region, allowedDirections);

		region.setOnMousePressed((event) -> resizer.mousePressed(event));
		region.setOnMouseDragged((event) -> resizer.mouseDragged(event));
		region.setOnMouseMoved((event) -> resizer.mouseOver(event));
		region.setOnMouseReleased((event) -> resizer.mouseReleased(event));
	}

	protected void mouseReleased(MouseEvent event) {
		dragging = NOTDRAGGING;
		region.setCursor(Cursor.DEFAULT);
	}

	protected void mouseOver(MouseEvent event) {
		if (isInDraggableZoneS(event) || dragging == SOUTH) {
			region.setCursor(Cursor.S_RESIZE);
		} else if (isInDraggableZoneE(event) || dragging == EAST) {
			region.setCursor(Cursor.E_RESIZE);
		} else if (isInDraggableZoneN(event) || dragging == NORTH) {
			region.setCursor(Cursor.N_RESIZE);
		} else if (isInDraggableZoneW(event) || dragging == WEST) {
			region.setCursor(Cursor.W_RESIZE);
		} else {
			region.setCursor(Cursor.DEFAULT);
		}
	}

	private boolean isInDraggableZoneN(MouseEvent event) {
		return (allowedDirections & NORTH) > 0 && event.getY() < RESIZE_MARGIN;
	}

	private boolean isInDraggableZoneW(MouseEvent event) {
		return (allowedDirections & WEST) > 0 && event.getX() < RESIZE_MARGIN;
	}

	private boolean isInDraggableZoneS(MouseEvent event) {
		return (allowedDirections & SOUTH) > 0 && event.getY() > (region.getHeight() - RESIZE_MARGIN);
	}

	private boolean isInDraggableZoneE(MouseEvent event) {
		return (allowedDirections & EAST) > 0 && event.getX() > (region.getWidth() - RESIZE_MARGIN);
	}

	private double getValueWithinBounds(double lowerBound, double value, double upperBound) {
		// bounds below 0 are ignored
		double upper = upperBound < 0 ? value : Math.min(upperBound, value);
		return Math.max(lowerBound, upper);
	}

	private void mouseDragged(MouseEvent event) {
		if (dragging == SOUTH) {
			region.setPrefHeight(event.getY());
		} else if (dragging == EAST) {
			region.setPrefWidth(event.getX());
		} else if (dragging == NORTH) {
			// getHeight() isn't always in sync with getPrefHeight() when mouse is a bit faster.
			double oldHeight = getValueWithinBounds(region.getMinHeight(), region.getPrefHeight(), region.getMaxHeight());
			region.setPrefHeight(oldHeight - event.getY());
			double newHeight = getValueWithinBounds(region.getMinHeight(), region.getPrefHeight(), region.getMaxHeight());
			region.setTranslateY(region.getTranslateY() + oldHeight - newHeight);
		} else if (dragging == WEST) {
			double oldWidth = getValueWithinBounds(region.getMinWidth(), region.getPrefWidth(), region.getMaxWidth());
			region.setPrefWidth(oldWidth - event.getX());
			double newWidth = getValueWithinBounds(region.getMinWidth(), region.getPrefWidth(), region.getMaxWidth());
			region.setTranslateX(region.getTranslateX() + oldWidth - newWidth);
		}
	}

	private void mousePressed(MouseEvent event) {
		// ignore clicks outside of the draggable margin
		if (isInDraggableZoneE(event)) {
			dragging = EAST;
		} else if (isInDraggableZoneS(event)) {
			dragging = SOUTH;
		} else if (isInDraggableZoneN(event)) {
			dragging = NORTH;
		} else if (isInDraggableZoneW(event)) {
			dragging = WEST;
		} else {
			return;
		}

		// init the prefSize in case it is not set.
		if (!initPref) {
			region.setPrefHeight(region.getHeight());
			region.setPrefWidth(region.getWidth());
			initPref = true;
		}
	}
}