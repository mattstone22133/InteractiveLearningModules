package enigma.engine.sorting;

import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import enigma.engine.Draggable;
import enigma.engine.Positionable;
import enigma.engine.TextureLookup;
import enigma.engine.Tools;
import enigma.engine.Touchable;
import enigma.engine.utilities.LERPSprite;

/**
 * A visual representation of an array that allows swapping of elements. 
 * 
 * @author matts
 * @version 1/21/2018
 */
public class SortableArray extends Positionable implements Touchable {
	public static final float MAX_HEIGHT = 300f;
	
	protected Random rng;
	
	protected ShapeRenderer sr;
	protected Rectangle boundBox;
	protected float elementWidth;
	protected float spacingWidth;
	protected Vector2 touchOffset = new Vector2();
	protected ArrayList<VisualColumn> elements;
	protected ArrayList<VisualColumn> originalOrderingElements;
	
	//Marks the current iteration
	protected boolean drawIterationMarker = false;
	protected boolean drawStepMarker = false;
	protected LERPSprite iterationMarker;
	protected LERPSprite stepMarker;
	protected int iterationIndex = 0;
	protected int stepIndex = 0;
	
	//draw by order of height, this means that smaller blocks will draw in front and can be seen
	protected ArrayList<Vector2> arrayIndexPositions;
	protected int minimumElementValue = 1;

	// utility for converting touches
	private Vector3 convertedTouchVect = new Vector3(0, 0, 0);
	private Draggable dragTarget = null;

	private int maxElementValue;

	private int seed;

	/**
	 * Create a visual representation of an array that allows swapping of elements. 
	 * 
	 * @param x
	 * @param y
	 * @param elementWidth
	 * @param numElements
	 * @param maxElementValue
	 */
	public SortableArray(float x, float y, float elementWidth, int numElements, int maxElementValue, int seed) {
		float initWidth = elementWidth * numElements;
		this.elementWidth = elementWidth;
		this.spacingWidth = elementWidth;
		
		this.seed = seed;
		rng = new Random(seed);

		sr = TextureLookup.shapeRenderer;
		boundBox = new Rectangle(x, y, initWidth, SortableArray.MAX_HEIGHT);
		
		iterationMarker = configureMarker(Color.DARK_GRAY);
		stepMarker = configureMarker(Color.YELLOW);

		this.maxElementValue = maxElementValue;
		generateElements(numElements, maxElementValue);
		
		//positional setup
		setMarkerToPosition(iterationMarker, 0);
		setPosition(x, y);
	}
	
	public SortableArray(SortableArray toClone) {
		//passing same seed should cause same element generation. 
		this(toClone.getX(), toClone.getY(), toClone.elementWidth, toClone.elements.size(), toClone.maxElementValue, toClone.seed);
		centerOnPoint(getX(), getY());
	}

	private LERPSprite configureMarker(Color color) {
		LERPSprite sprite = new LERPSprite(TextureLookup.arrowUpSmall);
		sprite.setColor(color);
		sprite.setSize(elementWidth, sprite.getHeight());
		return sprite;
	}

	private void generateElements(int numElements, int maxElementValue) {
		elements = new ArrayList<VisualColumn>(numElements);
		originalOrderingElements = new ArrayList<VisualColumn>(numElements);
		arrayIndexPositions = new ArrayList<Vector2>(numElements);

		for (int idx = 0; idx < numElements; ++idx) {
			int elementValue = rng.nextInt(maxElementValue + 1 - minimumElementValue) + minimumElementValue;
			VisualColumn vc = new VisualColumn(0, 0, elementValue, maxElementValue, MAX_HEIGHT, elementWidth);
			vc.setPosition(getX() + idx * (elementWidth + spacingWidth), getY());
			elements.add(vc);
			originalOrderingElements.add(vc);

			Vector2 arraySlot = new Vector2(vc.getX(), vc.getY());
			arrayIndexPositions.add(arraySlot);
		}
	}

	@Override
	public void setPosition(float x, float y) {
		float translateX = x - getX();
		float translateY = y - getY();

		for (VisualColumn element : elements) {
			element.translate(translateX, translateY);
		}

		for (Vector2 loc : arrayIndexPositions) {
			float oldX = loc.x;
			float oldY = loc.y;

			loc.x = oldX + translateX;
			loc.y = oldY + translateY;
		}
		boundBox.setPosition(x, y);
		setMarkerToPosition(iterationMarker, iterationIndex);
		setMarkerToPosition(stepMarker, stepIndex);
	}

	@Override
	public void translate(float transX, float transY) {
		boundBox.setPosition(boundBox.getX() + transX, boundBox.getY() + transY);
		for (VisualColumn element : elements) {
			element.translate(transX, transY);
		}
		for (Vector2 loc : arrayIndexPositions) {
			float oldX = loc.x;
			float oldY = loc.y;

			loc.x = oldX + transX;
			loc.y = oldY + transY;
		}
	}

	@Override
	public float getX() {
		return boundBox.getX();
	}

	@Override
	public float getY() {
		return boundBox.getY();
	}

	private static ArrayList<VisualColumn> interpolating = new ArrayList<VisualColumn>();
	@Override
	public void draw(SpriteBatch batch) {
		interpolating.clear();
		for (VisualColumn element : elements) {
			element.draw(batch);
			if (element.isInterpolating()) {
				interpolating.add(element);
			}
		}
		
		//draw interpolating elements overtop of the others.
		for (VisualColumn element : interpolating) {
			element.draw(batch);
		}
		
		//if the target is being dragged, then draw overtop of it to ensure that it is always seen regardless of height (ie draw order)
		if(dragTarget != null) {
			Color cachedColor = sr.getColor();
			float r = cachedColor.r;
			float g = cachedColor.g;
			float b = cachedColor.b;
			float a = cachedColor.a;

			sr.setColor(TextureLookup.getRedColor());
			dragTarget.draw(batch);
			sr.setColor(r, g, b, a);
		}
	}
	
	public void drawPreSprites(SpriteBatch batch) {
		if(drawStepMarker)
			stepMarker.draw(batch);
		
		if(drawIterationMarker)
			iterationMarker.draw(batch);
	}

	@Override
	public void logic() {
		for (VisualColumn element : elements) {
			element.logic();
		}
		stepMarker.logic();
		iterationMarker.logic();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void centerOnPoint(float x, float y) {
		setPosition(x, y);
		float width = elements.size() * (elementWidth + spacingWidth);
		this.translate(-width * 0.5f, 0);
		setMarkerToPosition(iterationMarker, iterationIndex);
		setMarkerToPosition(stepMarker, stepIndex);
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button, OrthographicCamera camera) {
		boolean handled = false;
		Tools.convertMousePointsIntoGameCoordinates(camera, convertedTouchVect);

		for (VisualColumn element : elements) {
			if (element.contains(convertedTouchVect.x, convertedTouchVect.y)) {
				dragTarget = element;
				dragTarget.startedDragging(convertedTouchVect.x, convertedTouchVect.y);
				handled = true;
				break;
			}
		}

		return handled;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button, OrthographicCamera camera) {
		boolean handled = false;
		Tools.convertMousePointsIntoGameCoordinates(camera, convertedTouchVect);

		if (dragTarget != null) {
			dragTarget.endedDragging(convertedTouchVect.x, convertedTouchVect.y);

			VisualColumn swappedWith = attemptSwapWithOtherElement(dragTarget);
			int idx = getIndex(dragTarget);
			if (idx != -1) {
				setLERPToPosition(idx, dragTarget, TextureLookup.getRedColor());
			}
			if (swappedWith != null) {
				idx = getIndex(swappedWith);
				if (idx != -1) {
					setLERPToPosition(idx, swappedWith, TextureLookup.getBlueColor());
				}
			}
			handled = true;
		}
		dragTarget = null;

		return handled;
	}

	protected void setLERPToPosition(int idxOfPosition, Draggable interpolatingItem, Color color) {
		Vector2 loc = arrayIndexPositions.get(idxOfPosition);
		interpolatingItem.setInterpolatePoint(loc.x, loc.y);
		VisualColumn casted = (VisualColumn) interpolatingItem;
		if(casted != null) {
			casted.setOverrideColor(color);
		}
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer, OrthographicCamera camera) {
		boolean handled = false;

		Tools.convertMousePointsIntoGameCoordinates(camera, convertedTouchVect);
		if (dragTarget != null) {
			dragTarget.draggedToPoint(convertedTouchVect.x, convertedTouchVect.y);
		}

		return handled;
	}

	public VisualColumn attemptSwapWithOtherElement(Draggable draggedElement) {
		VisualColumn dragged = (VisualColumn) draggedElement;

		if (dragged == null) {
			// cast failed, wrong type
			return null;
		}

		for (int idx = 0; idx < elements.size(); ++idx) {
			VisualColumn currEle = elements.get(idx);
			if (dragged != currEle) {
				if (dragged.colidingWith(currEle)) {
					VisualColumn swappedElement = forceSwap(dragged, currEle);
					return swappedElement;
//					int draggedIndex = getIndex(dragged);
//					int elementIndex = getIndex(currEle);
//					if (draggedIndex != -1 && elementIndex != -1) {
//						VisualColumn temp = elements.get(draggedIndex);
//						elements.set(draggedIndex, currEle);
//						elements.set(elementIndex, temp);
//						return currEle;
//					}
				}
			}
		}

		return null;
	}
	
	protected VisualColumn forceSwap(VisualColumn movingElement, VisualColumn displacedElement) {
		int fromIndex = getIndex(movingElement);
		int toIndex = getIndex(displacedElement);
		if (fromIndex != -1 && toIndex != -1) {
			VisualColumn temp = elements.get(fromIndex);
			elements.set(fromIndex, displacedElement);
			elements.set(toIndex, temp);
			return displacedElement;
		}
		return null;
	}

	private int getIndex(Draggable element) {
		for (int idx = 0; idx < elements.size(); ++idx) {
			VisualColumn current = elements.get(idx);
			if (current == element) {
				return idx;
			}
		}
		return -1;
	}
	
	//----------------------------- Solving Methods -------------------
	protected void setMarkerToPosition(LERPSprite marker, int idx) {
		if(iterationIndex < elements.size()) {
			Vector2 loc = arrayIndexPositions.get(idx);
			marker.setPosition(loc.x, loc.y - marker.getHeight() * 1.5f);
		}
	}
	
	protected void setMarkerLERPToPosition(LERPSprite marker,int idx) {
		if(iterationIndex < elements.size()) {
			Vector2 loc = arrayIndexPositions.get(idx);
			marker.setInterpolatePoint(loc.x, loc.y - marker.getHeight() * 1.5f);
		}
	}
	
	public void nextSolveStep() {
		if(stepIndexComplete()) {
			incrementIteration();
		} 
		
		if(!solveCompleted()) {
			
		}
	}

	private void incrementIteration() {
		iterationIndex += 1;
		stepIndex = iterationIndex;
		
		setMarkerLERPToPosition(iterationMarker, iterationIndex);
		setMarkerLERPToPosition(stepMarker, iterationIndex);
	}

	private boolean solveCompleted() {
		return false;
	}

	protected boolean stepIndexComplete() {
		return false;
	}
	
	//----------------------------- Solving Methods -------------------
}
