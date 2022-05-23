package com.friya.spectator.communicator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ObjectOfInterest 
{
	private long id = -10;
	private InterestType type = InterestType.STATIC_OBJECT;
	private MoveType[] moveTypes;
	private int priority = 1;									// a misnomer; time spent in comparison to sibling OOIs (total priority of myself and all siblings / allowed time * this priority)
	private float x = 0;
	private float y = 0;
	private float h = 0;
	
	public ObjectOfInterest copyTo(ObjectOfInterest copy)
	{
		copy.id = id;
		copy.type = type;
		if(moveTypes != null) {
			copy.moveTypes = Arrays.copyOf(moveTypes, moveTypes.length);
		}
		copy.priority = priority;
		copy.x = x;
		copy.y = y;
		copy.h = h;
		return copy;
	}

	public String toString()
	{
		return getId() + " " + getType() + " " + Arrays.toString(getMoveTypes());
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public InterestType getType() {
		return type;
	}

	public void setType(InterestType type) {
		this.type = type;
	}

	public MoveType[] getMoveTypes() {
		return moveTypes;
	}

	public void setMoveTypes(MoveType[] moveTypes) {
		this.moveTypes = moveTypes;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}
	
	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}
	
	public float getH() {
		return h;
	}

	public void setH(float h) {
		this.h = h;
	}
}
