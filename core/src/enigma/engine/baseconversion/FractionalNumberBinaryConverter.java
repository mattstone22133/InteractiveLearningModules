package enigma.engine.baseconversion;


import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import enigma.engine.DrawableString;
import enigma.engine.Tools;
import enigma.engine.utilities.LERPSprite;

@SuppressWarnings("unused")
public class FractionalNumberBinaryConverter {
	private ArrayList<BinaryFractMultiply> components;
	private float number;
	private float x = 0;
	private float y = 0;
	private DrawableString exampleDS;
	private float scaleX = 1;
	private float scaleY = 1;
	private boolean complete = false;
	private float shrinkFactor = 0.45f;
	private int multLimit = Integer.MAX_VALUE;
	private boolean finalScaleResultScaled = false;
	
	public FractionalNumberBinaryConverter(float number) {
		components = new ArrayList<BinaryFractMultiply>();
		this.number = number;
		
		exampleDS = new DrawableString("0");
		components.add(new BinaryFractMultiply(number, 
				Gdx.graphics.getWidth() * 1.1f, Gdx.graphics.getHeight() * 0.5f, 
				false));
		
		positionElements();
	}

	private void positionElements() {
		scaleElements();
		
		float lastX = x; 
		float lastWidth = 0;
		float spacing = calculateSpacing();
		
		float yCorrection = 0;
		float xCorrection = 0;
		
		for(int i = 0; i < components.size(); ++i) {
			BinaryFractMultiply mult = components.get(i);
			float newX = lastX + lastWidth + ((i == 0) ? 0 : spacing);
			
			//check that x isn't going off screen.
			if(newX + mult.getWidth() > Gdx.graphics.getWidth()) {
				xCorrection = -Gdx.graphics.getWidth() * 0.15f;
				yCorrection = 4 * exampleDS.height();
			}
			
			
			if(i != components.size() -1 || i == 0 || (i == (multLimit - 1) && isDone())) {
				//mult.setPosition(newX, y);
				mult.setInterpolateToPoint(newX + xCorrection, y + yCorrection);
			} else {
				//mult.setPosition(newX + 0.5f * mult.getWidth(), y);
				mult.setInterpolateToPoint(newX + 0.5f * mult.getWidth() + xCorrection, y + yCorrection);
			}
			
			lastX = newX;
			lastWidth = mult.getWidth();
		}
	}
	
	private void scaleElements() {
		int size = components.size();
		for(int i = 0; i < size; ++i) {
			if(size - 1 != i) {
				//components.get(i).scale(shrinkFactor* scaleX, shrinkFactor* scaleY);
				components.get(i).setInterpolateToScale(shrinkFactor* scaleX, shrinkFactor* scaleY);
			} else {
				//go ahead and scale element if they're done.
				if (isDone() && components.get(i).isDone()) {
					//components.get(i).scale(shrinkFactor* scaleX, shrinkFactor* scaleY);
					components.get(i).setInterpolateToScale(shrinkFactor* scaleX, shrinkFactor* scaleY);
				}
			}
		}				
	}

	private float calculateSpacing() {
		//height doesn't change with length, so I use it a lot of a consistent, 
		//but scaled offset source.
		return exampleDS.height() * 0.5f;
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
		positionElements();
		
	}
	
	public void logic() {
		for(BinaryFractMultiply entity : components) {
			entity.logic();
		}
		
		//when the user is done, make sure the last component is correctly positioned and colored. 
		if(isDone()) {
			if(!finalScaleResultScaled) {
				positionElements();
				components.get(components.size() - 1).colorWholeDigit();
				finalScaleResultScaled = true;
			}
			return;
		}
		
		BinaryFractMultiply last = components.get(components.size() - 1);
		if(last.isDone()) {
			last.colorWholeDigit();
			float result = last.result();
			if(result >= 1) {
				result -= 1;
				result = Float.parseFloat(Tools.trimFloat(result , ("" + last.result()).length()));
			}
			if(result == 0 || shouldStopCreatingNewMultiplications()) {
				//binary conversion is done. 
				positionElements();
			} else {
				//there are still digits to be processed.
				BinaryFractMultiply newComp = new BinaryFractMultiply(result, x, y, true);
				components.add(newComp);
				newComp.setPosition(Gdx.graphics.getWidth() * 1.1f, newComp.getY());//move slightly to right
				positionElements();
			}
		}
//		if(!complete && components.size() > 0) {
//			if(last.isDone() && last.result() == 0) {
//				positionElements();
//				complete = true;
//			}
//		}
		
		
	}
	

	private boolean shouldStopCreatingNewMultiplications() {
		return components.size() >= multLimit;
	}

	public void draw(SpriteBatch batch) {
		for(BinaryFractMultiply entity : components) {
			entity.draw(batch);
		}		
	}

	public void IO() {
		for(BinaryFractMultiply entity : components) {
			if(!entity.isInterpolating()) {
				entity.IO();
			}
		}
	}
	
	public boolean isDone() {
		return complete 
				|| (components.size() == multLimit && components.get(components.size() -1).isDone());
	}

	public void setLimitMultiplications(int limitMultiplications) {
		this.multLimit = limitMultiplications;
	}

	public String getDigitsString() {
		if(!isDone())
			return null;
		
		String result = "";

		for(BinaryFractMultiply comp : components) {
			result += comp.getResultText().charAt(0);
		}
		
		return result;
	}

	public float getWholeResultLocX(int index) {
		BinaryFractMultiply comp= components.get(index);
		return comp.getWholeDS().getX();
	}

	public float getWholeResultLocY(int index) {
		BinaryFractMultiply comp= components.get(index);
		return comp.getWholeDS().getY();
	}
}
