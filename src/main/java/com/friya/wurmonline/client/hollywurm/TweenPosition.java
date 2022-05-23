package com.friya.wurmonline.client.hollywurm;

import com.friya.wurmonline.client.hollywurm.TweenMeta.CoordinateType;
import com.wurmonline.client.game.PlayerPosition;

public class TweenPosition 
{

// handle all types of positions in here ... pass in original to create absolute from offset yada yada
	float x = 0;
	float y = 0;
	float h = 0;
	float xRot = 0;
	float yRot = 0;
	
	boolean initialized = false;
	
	int shortTargetId = 0;
	CoordinateType coordinateType = CoordinateType.UNKNOWN;
	
	public TweenPosition() 
	{
	}

	public TweenPosition(float xPos, float yPos, float hPos) 
	{
		x = xPos;
		y = yPos;
		h = hPos;
	}
	
	float getX() { return x; }
	float getY() { return y; }
	float getH() { return h; }
	float getRotX() { return xRot; }
	float getRotY() { return yRot; }

	void add(float otherXrot, float otherYrot)
	{
		xRot += otherXrot;
		yRot += otherYrot;
	}
	
	public String toString()
	{
		return "x: " + x + " y: " + y + " h: " + h + " xRot: " + xRot + " yRot: " + yRot;
	}
	
	void add(PlayerPosition other)
	{
		x += other.getX();
		y += other.getY();
		h += other.getH();
	}

	void add(TweenPosition other)
	{
		x += other.x;
		y += other.y;
		h += other.h;
		xRot += other.xRot;
		yRot += other.yRot;
	}
	
	public boolean setPos(String _x)
	{
		x = Float.parseFloat(_x);
		return true;
	}
	
	public boolean setPos(String _x, String _y)
	{
		x = Float.parseFloat(_x);
		y = Float.parseFloat(_y);
		return true;
	}

	public boolean setPos(String _x, String _y, String _h)
	{
		x = Float.parseFloat(_x);
		y = Float.parseFloat(_y);
		h = Float.parseFloat(_h);

		return true;
	}

	public boolean setRot(String _xRot, String _yRot)
	{
		xRot = Float.parseFloat(_xRot);
		yRot = Float.parseFloat(_yRot);
		return true;
	}

	public boolean setRot(String _xRot)
	{
		xRot = Float.parseFloat(_xRot);
		return true;
	}

	public boolean setXRot(float _xRot)
	{
		xRot = _xRot;
		return true;
	}

	public boolean setYRot(float _xRot)
	{
		xRot = _xRot;
		return true;
	}


	public void setPos(float x2, float y2, float h2, float rot2)
	{
		x = x2;
		y = y2;
		h = h2;
		xRot = rot2;
	}
	
	public void setPos(float x2, float y2, float h2)
	{
		x = x2;
		y = y2;
		h = h2;
	}
	
	public int getTileX()
	{
		return (int)(x / 4);
	}

	public int getTileY()
	{
		return (int)(y / 4);
	}

	public void setCoordinateType(CoordinateType ct)
	{
		coordinateType = ct;
	}
	
	public CoordinateType getCoordinateType()
	{
		return coordinateType;
	}
	
	public void setShortTargetId(int id)
	{
		shortTargetId = id;
	}

	public int getShortTargetId()
	{
		return shortTargetId;
	}

	public void setX(float v)
	{
		x = v;
	}
	public void setY(float v)
	{
		y = v;
	}
	public void setH(float v)
	{
		h = v;
	}

	public boolean isInitialized()
	{
		return initialized;
	}
	
	public void setInitialized(boolean v)
	{
		initialized = v;
	}
}
