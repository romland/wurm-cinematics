package com.friya.wurmonline.client.hollywurm.paths;

import java.util.ArrayList;

import com.friya.spectator.communicator.MoveType;
import com.friya.wurmonline.client.hollywurm.TweenPosition;

public class Circle 
{
	static public MoveType getType()
	{
		return MoveType.CIRCLE;
	}
	
	static public ArrayList<TweenPosition> getPath(float atHeight, float centerX, float centerY, float radius)
	{
		ArrayList<TweenPosition> ret = new ArrayList<TweenPosition>();

		float step = (float) (2.0 * Math.PI / 20.0);
		float theta;

		TweenPosition tp;
		for(theta = 0f; theta < (2.0 * Math.PI); theta += step) {
			tp = new TweenPosition(
				(float) (centerX + radius * Math.cos(theta)),
				(float) (centerY - radius * Math.sin(theta)),
				atHeight
			);
			ret.add(tp);
			//logger.info("" + tp);
		}

		return ret;
	}
}
