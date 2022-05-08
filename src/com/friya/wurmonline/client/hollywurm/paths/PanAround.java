package com.friya.wurmonline.client.hollywurm.paths;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import com.friya.spectator.communicator.MoveType;
import com.friya.wurmonline.client.hollywurm.TweenPosition;

public class PanAround implements MovePath
{
	@Override
	public MoveType getType()
	{
		return MoveType.PAN_AROUND;
	}
	
	@Override
	public ArrayList<TweenPosition> getPath(PathArgs args) throws PathArgException
	{
		if(args.hasXYHR() == false) {
			throw new PathArgException("method expects x,y,h,radius");
		}
		
		float x = args.x;
		float y = args.y;
		float h = args.h;
		float radius = args.radius;
		
		ArrayList<TweenPosition> ret = new ArrayList<TweenPosition>();
		
		ret.add(new TweenPosition(
				x - radius,
				y,
				h + ThreadLocalRandom.current().nextInt(40)
			));

		ret.add(new TweenPosition(
				x,
				y - radius,
				h + ThreadLocalRandom.current().nextInt(40)
			));
		
		ret.add(new TweenPosition(
				x + radius,
				y,
				h + ThreadLocalRandom.current().nextInt(40)
			));

		ret.add(new TweenPosition(
				x,
				y + radius,
				h + ThreadLocalRandom.current().nextInt(10)
			));
		
		return ret;
	}
}
