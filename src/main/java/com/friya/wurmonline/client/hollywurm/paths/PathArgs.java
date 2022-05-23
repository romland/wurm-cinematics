package com.friya.wurmonline.client.hollywurm.paths;

public class PathArgs
{
	public float x, y, h, radius;
	
	public void setXYHR(float _x, float _y, float _h, float _radius)
	{
		x = _x;
		y = _y;
		h = _h;
		radius = _radius;
	}
	
	public boolean hasXYHR()
	{
		return (x > 0 || y > 0 || h > 0) && radius != 0;
	}
}
