package com.friya.wurmonline.client.hollywurm;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector2f;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.game.PlayerPosition;
import com.wurmonline.math.Vector3f;
import com.friya.wurmonline.client.hollywurm.TransformTypes;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenAccessor;

public class PlayerAccessor implements TweenAccessor<PlayerObj>
{
    private static Logger logger = Logger.getLogger(PlayerAccessor.class.getName());

    private static PlayerAccessor instance = null;

    private static TweenPosition currentFocusPoint = null;
    private static TweenPosition currentFollowPoint = null;

	public static PlayerAccessor getInstance()
	{
		if(instance == null) {
			instance = new PlayerAccessor();
		}

		return instance; 
	}
    
    public int getValues(Tween tween, PlayerObj target, int tweenType, float[] returnValues)
	{
		PlayerPosition pos;
//		TweenPosition tp;
		TweenMeta meta = ((TweenMeta)tween.getUserData());
		
		switch (tweenType) {
			case TransformTypes.ROT_X:
				returnValues[0] = getXrot(target);
				return 1;
			case TransformTypes.ROT_Y:
				returnValues[0] = getYrot(target);
				return 1;
			case TransformTypes.ROT_XY:
				returnValues[0] = getXrot(target);
				returnValues[1] = getYrot(target);
				return 2;
			
			case TransformTypes.MOVE_XY:
				pos = target.getPos();
				returnValues[0] = pos.getX();
				returnValues[1] = pos.getY();
				return 2;
				
			case TransformTypes.MOVE_XYH:
				pos = target.getPos();
				returnValues[0] = pos.getX();
				returnValues[1] = pos.getY();
				returnValues[2] = pos.getH();
				return 3;
				
			case TransformTypes.FOCUS_RELATIVE_TO_VELOCITY :
				pos = target.getPos();
				returnValues[0] = pos.getX();
				returnValues[1] = pos.getY();
				returnValues[2] = pos.getH();
				return 3;

			case TransformTypes.FOCUS_XY:
				if(meta.focusPoint == null) {
					meta.focusPoint = initFocusPoint();
				}
				returnValues[0] = meta.focusPoint.getX();
				returnValues[1] = meta.focusPoint.getY();
				return 2;

			case TransformTypes.FOCUS_XYH:
				if(meta.focusPoint == null) {
					meta.focusPoint = initFocusPoint();
				}
				returnValues[0] = meta.focusPoint.getX();
				returnValues[1] = meta.focusPoint.getY();
				returnValues[2] = meta.focusPoint.getH();
				return 3;

			case TransformTypes.FOLLOW_FOCUS :
				if(meta.focusPoint == null) {
					meta.focusPoint = initFocusPoint();
				}
				returnValues[0] = meta.focusPoint.getX();
				returnValues[1] = meta.focusPoint.getY();
				returnValues[2] = meta.focusPoint.getH();
				return 3;

			case TransformTypes.FOLLOW_XY :
				throw new RuntimeException("TODO: FOLLOW_XY");
				//return 4;

			case TransformTypes.FOLLOW_XYH :
//				TweenPosition targetCreaturePos = WurmHelpers.getCreaturePosition(SceneRunner.getInstance().getShortCreatureId(SceneRunner.followPoint.getShortTargetId()));
				if(meta.followPoint == null) {
					meta.followPoint = initFollowPoint();
				}

				pos = target.getPos();

				returnValues[0] = meta.followPoint.getCoordinateType().getValue();
				returnValues[1] = meta.followPoint.getShortTargetId();

				returnValues[2] = meta.followPoint.getX();
				returnValues[3] = meta.followPoint.getY();
				returnValues[4] = meta.followPoint.getH();

				return 5;

			default:
				assert false;
				return 0;
		}
	}

	private TweenPosition initFollowPoint()
	{
		if(currentFollowPoint == null) {
			currentFollowPoint = new TweenPosition();
			Vector2f xy = ScriptBuilder.getPointFromDistAngle(10000, WurmHelpers.getWorld().getPlayerRotY());
			currentFollowPoint.setX(WurmHelpers.getWorld().getPlayerPosX() - xy.x);
			currentFollowPoint.setY(WurmHelpers.getWorld().getPlayerPosY() - xy.y);
		}
		return currentFollowPoint;
	}

    private TweenPosition initFocusPoint()
	{
		// get current bearing to a tile position -- and put that in focusPoint
		if(currentFocusPoint == null) {
			currentFocusPoint = new TweenPosition();
			Vector2f xy = ScriptBuilder.getPointFromDistAngle(10000, WurmHelpers.getWorld().getPlayerRotY());
			currentFocusPoint.setX(WurmHelpers.getWorld().getPlayerPosX() - xy.x);
			currentFocusPoint.setY(WurmHelpers.getWorld().getPlayerPosY() - xy.y);
		}
		return currentFocusPoint;
	}
	
	public void setValues(Tween tween, PlayerObj target, int tweenType, float[] newValues)
	{
		switch (tweenType ) {
			case TransformTypes.ROT_X:
				setXrot(target, newValues[0]);
				break;
			case TransformTypes.ROT_Y:
				setYrot(target, newValues[1]);
				break;
			case TransformTypes.ROT_XY:
				setXYrot(target, newValues[0], newValues[1]);
				break;
			
			case TransformTypes.MOVE_XY:
				setXYPos(target, newValues[0], newValues[1]);
				break;

			case TransformTypes.MOVE_XYH:
				setXYHPos(tween, target, newValues[0], newValues[1], newValues[2]);
				break;

			case TransformTypes.FOCUS_RELATIVE_TO_VELOCITY :
				faceForward(tween, target, newValues[0], newValues[1], newValues[2]);
				break;
				
			case TransformTypes.FOCUS_XY:
				setXYFocus(tween, target, newValues[0], newValues[1]);
				break;

			case TransformTypes.FOCUS_XYH:
				setXYHFocus(tween, target, newValues[0], newValues[1], newValues[2]);
				break;

			case TransformTypes.FOLLOW_FOCUS :
				setFollowFocus(tween, target, (int)newValues[0], (int)newValues[1], newValues[2], newValues[3], newValues[4]);
				break;

			case TransformTypes.FOLLOW_XY :
				// what we get in is:
				// .waypoint(coordType.getValue(), shortTargetCreatureId, x, y);
				setXYFollowPos(tween, target, (int)newValues[0], (int)newValues[1], newValues[2], newValues[3]);
				break;

			case TransformTypes.FOLLOW_XYH :
				// what we get in is:
				// .waypoint(coordType.getValue(), shortTargetCreatureId, x, y, h);
				setXYHFollowPos(tween, target, (int)newValues[0], (int)newValues[1], newValues[2], newValues[3], newValues[4]);
				break;
				
			default:
				assert false;
				break;
		}
	}


	private void faceForward(Tween tween, PlayerObj t, float x, float y, float h)
	{
		// TODO: This is slow as a motherfucker, rewrite and refactor (use temp vectors, or simply don't use vectors)
		TweenMeta meta = ((TweenMeta)tween.getUserData());
		
		x = meta.focusPoint.getX();
		y = meta.focusPoint.getY();
		h = meta.focusPoint.getH();
		
		float lerp = 0.25f;
		float deltaTime = 0.2f;		// TODO
		double angleXY = getXYAngle(0, 0, WurmHelpers.getWorld().getPlayerPosX() - x, WurmHelpers.getWorld().getPlayerPosY() - y);

		float dist = new Vector3f(x, y, h).distance(new Vector3f(WurmHelpers.getWorld().getPlayerPosX(), WurmHelpers.getWorld().getPlayerPosY(), WurmHelpers.getWorld().getPlayerPosH()));

		if(dist > 0.01) {
			double xy = WurmHelpers.getWorld().getPlayerRotX() + ((((((angleXY - WurmHelpers.getWorld().getPlayerRotX()) % 360) + 540) % 360) - 180) * lerp * deltaTime);
			
			if(Math.abs(xy - WurmHelpers.getWorld().getPlayerRotX()) < 5) {
				setXrot(t, (float)xy);
			}
		}

		// store it so we can get velocity next time around
		meta.focusPoint.setX(WurmHelpers.getWorld().getPlayerPosX());
		meta.focusPoint.setY(WurmHelpers.getWorld().getPlayerPosY());
		meta.focusPoint.setH(WurmHelpers.getWorld().getPlayerPosH());
	}


	/*
	 * - what we get passed in here is the offset from the target,
	 * - I think: the move of source (player) to the moving target (creature) is something that should be handled outside of the tween (with a lerp)
	 * - this means ... we just have a semi-fixed speed for covering distance, and another one to pan around target to the right 'near' position
	 *
	 * Follow does not look great yet. In fact, I think this is where I stopped development.
	 */
	private void setXYHFollowPos(Tween tween, PlayerObj t, int coordType, int shortCreatureId, float x, float y, float h)
	{
		TweenMeta meta = ((TweenMeta)tween.getUserData());

		// REFACTOR: Class envy
		TweenPosition targetCreaturePos = WurmHelpers.getCreaturePosition(SceneRunner.getInstance().getShortCreatureId(shortCreatureId));
		
		if(targetCreaturePos == null) {
			tween.kill();
			WurmHelpers.tellCinematics("Lost track of creature " + SceneRunner.getInstance().getShortCreatureId(shortCreatureId) + "...", MessageType.ERROR);
			return;
		}

		boolean skipH = ((Float)h).equals(Float.MIN_VALUE);

		meta.followPoint.setPos(x, y, h, 0);

		float lerp = 0.1f;
		float deltaTime = 0.2f;		// TODO

		//
		// we want to both repel (very close) and close gap (far away)
		//
		float distX = Math.abs(targetCreaturePos.getX() - t.getPos().getX());
		float distY = Math.abs(targetCreaturePos.getY() - t.getPos().getY());
		float minDist = Math.min(distX, distY);

		
		// X:	creature at 1200
		//		      me at 1199  -- 1 tile north of creature
		//		= creature - me = 1200 - 1199 = +1 = move north
		//
		// if I am south of
		//		= creature - me = 1199 - 1200 = -1 = move south

		// This will just repel in one axis
		float xRepel = 0;
		float yRepel = 0;

		// 2 = half a tile
		if(minDist < 2) {
			if(distY < distX) {
				// The shortest distance is along x (east->west)
				if(targetCreaturePos.getX() <= t.getPos().getX()) {
					// target is west of us, if it comes towards us, move east
					xRepel += 2f;
				} else {
					// target is east of us, move west
					xRepel -= 2f;
				}
			} else {
				// The shortest distance is along x (west->east)
				
				if(targetCreaturePos.getY() <= t.getPos().getY()) {
					// target is north of us, if it comes towards us, move south
					yRepel += 2f;
				} else {
					// target is south of us, move north
					yRepel -= 2f;
				}
			}
		}
		
		x = t.getPos().getX() + (((targetCreaturePos.getX() + x + xRepel) - t.getPos().getX()) * lerp * deltaTime);
		y = t.getPos().getY() + (((targetCreaturePos.getY() + y + yRepel) - t.getPos().getY()) * lerp * deltaTime);
		h = t.getPos().getH() + (((targetCreaturePos.getH() + h         ) - t.getPos().getH()) * lerp * deltaTime);
		
		if(skipH) {
			setXYPos(t, x, y);
		} else {
			setXYHPos(tween, t, x, y, h);
		}
	}
	
	private void setXYFollowPos(Tween tween, PlayerObj t, int coordType, int shortCreatureId, float x, float y)
	{
		setXYHFollowPos(tween, t, coordType, shortCreatureId, x, y, Float.MIN_VALUE);
	}


	private void setFollowFocus(Tween tween, PlayerObj t, int coordType, int shortCreatureId, float x, float y, float h)
	{
		TweenMeta meta = ((TweenMeta)tween.getUserData());

		TweenPosition targetCreaturePos = WurmHelpers.getCreaturePosition(SceneRunner.getInstance().getShortCreatureId(shortCreatureId));

		if(targetCreaturePos == null) {
			tween.kill();
			WurmHelpers.tellCinematics("lost track of creature " + SceneRunner.getInstance().getShortCreatureId(shortCreatureId) + "...", MessageType.ERROR);
			return;
		}

		meta.focusPoint.setX(x);
		meta.focusPoint.setY(y);
		meta.focusPoint.setH(h);

		double angleXY = getXYAngle(t.getPos(), targetCreaturePos);
		double angleUP = getZAngle(t.getPos(), targetCreaturePos);

		float lerp = 0.25f;
		float deltaTime = 0.2f;		// TODO

		// whoa
	    double xy = WurmHelpers.getWorld().getPlayerRotX() + ((((((angleXY - WurmHelpers.getWorld().getPlayerRotX()) % 360) + 540) % 360) - 180) * lerp * deltaTime);

	    // TODO: Need same lerping for this:
		double up = WurmHelpers.getWorld().getPlayerRotY() + ((angleUP - WurmHelpers.getWorld().getPlayerRotY()) * lerp * deltaTime);

		setXrot(t, (float)xy);
		setYrot(t, (float)up);
	}

/*
	// Sadly, this one does not update when we automatically move. Not sure where in the calling-chain this gets set. Need something else.
	static private float getPlayersFacingAngleFromVelocity()
	{
		double theta = Math.atan2(
				// Y first!
				UtilProxy.getMcYa(WurmHelpers.getMovementChecker()), UtilProxy.getMcXa(WurmHelpers.getMovementChecker())
		);
		theta += Math.PI / 2.0;
		double angle = Math.toDegrees(theta);
		if (angle < 0) {
			angle += 360;
		}
		return (float) angle;
	}
*/
	
	float getXrot(PlayerObj t)
	{
		return (float)getInstanceValue(t, "xRotUsed");
	}
	
	private void setXrot(PlayerObj t, float x)
	{
		setInstanceValue(t, "xRotUsed", x);
	}
	
	float getYrot(PlayerObj t)
	{
		return (float)getInstanceValue(t, "yRotUsed");
	}
	
	private void setYrot(PlayerObj t, float y)
	{
		setInstanceValue(t, "yRotUsed", y);
	}
	
	private void setXYrot(PlayerObj t, float x, float y)
	{
		setXrot(t, x);
		setYrot(t, y);
	}

	private void setXYPos(PlayerObj t, float x, float y)
	{
		setInstanceValue(t, "xPosUsed", x);
		setInstanceValue(t, "yPosUsed", y);
	}
	
	
//	private float[] ourLastSet = new float[] { 0, 0, 0 };
	
	private void setXYHPos(Tween tween, PlayerObj t, float x, float y, float h)
	{
		setInstanceValue(t, "xPosUsed", x);
		setInstanceValue(t, "yPosUsed", y);
		setInstanceValue(t, "hPosUsed", h);
	}
	
	private void setXYFocus(Tween tween, PlayerObj player, float x, float y)
	{
		TweenMeta meta = ((TweenMeta)tween.getUserData());

		double angle = getXYAngle(player.getPos(), meta.focusPoint);
		setXrot(player, (float)angle);

		meta.focusPoint.setX(x);
		meta.focusPoint.setY(y);
	}

	private void setXYHFocus(Tween tween, PlayerObj player, float x, float y, float h)
	{
		TweenMeta meta = ((TweenMeta)tween.getUserData());

		double angleXY = getXYAngle(player.getPos(), meta.focusPoint);
		double angleUP = getZAngle(player.getPos(), meta.focusPoint);

		float dist = new Vector3f(x, y, h).distance(new Vector3f(WurmHelpers.getWorld().getPlayerPosX(), WurmHelpers.getWorld().getPlayerPosY(), WurmHelpers.getWorld().getPlayerPosH()));
		if(Mod.closeByFocusFix && dist < 0.2f && (Math.abs(getXYAngle(player.getPos(), meta.focusPoint) - getXrot(player)) > 2 || Math.abs(getZAngle(player.getPos(), meta.focusPoint) - getYrot(player)) > 2)) {
			return;
		}
		
		setXYrot(player, (float)angleXY, (float)angleUP);

		meta.focusPoint.setX(x);
		meta.focusPoint.setY(y);
		meta.focusPoint.setH(h);
	}
	
	private double getZAngle(PlayerPosition source, TweenPosition destination)
	{
	    double deltaX = source.getX() - destination.getX();
	    double deltaY = source.getY() - destination.getY();
	    double deltaZ = source.getH() - destination.getH();
	    double angle = Math.asin(deltaZ / Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ));

	    return Math.toDegrees(angle);
	}
	
	private double getXYAngle(PlayerPosition source, TweenPosition destination)
	{
		return getXYAngle(source.getX(), source.getY(), destination.getX(), destination.getY());
	}

	private double getXYAngle(double srcX, double srcY, double dstX, double dstY)
	{
		double theta = Math.atan2(dstY - srcY, dstX - srcX);
		theta += Math.PI / 2.0;
		double angle = Math.toDegrees(theta);
		if (angle < 0) {
			angle += 360;
		}
		
		return (double) angle;
	}

	/**
	 * Returns an object containing the value of any field of an object instance (even private).
	 * 
	 * @param classInstance
	 *            An Object instance.
	 * @param fieldName
	 *            The name of a field in the class instantiated by classInstance
	 * @return An Object containing the field value.
	 */
	Object getInstanceValue(final Object classInstance, final String fieldName) 
	{
		try {
			final Field field = classInstance.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(classInstance);
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.toString());
			return null;
		}
	}

	/**
	 * Use reflection to change value of any instance field.
	 * 
	 * @param classInstance
	 *            An Object instance.
	 * @param fieldName
	 *            The name of a field in the class instantiated by
	 *            classInstancee
	 * @param newValue
	 *            The value you want the field to be set to.
	 */
	void setInstanceValue(final Object classInstance, final String fieldName, final Object newValue) 
	{
		try {
			final Field field = classInstance.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(classInstance, newValue);
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.toString());
		}
	}
    
}
