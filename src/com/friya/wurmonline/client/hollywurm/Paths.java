package com.friya.wurmonline.client.hollywurm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import com.friya.spectator.communicator.MoveType;
import com.friya.spectator.communicator.ObjectOfInterest;
import com.friya.spectator.communicator.PointOfInterest;
import com.friya.wurmonline.client.hollywurm.paths.*;

public class Paths 
{
	private static Logger logger = Logger.getLogger(Paths.class.getName());
	private static Paths instance = null;
	
	private static HashMap<MoveType, MovePath> moves = new HashMap<MoveType, MovePath>(); 
	
	static public Paths getInstance()
	{
		if(instance == null) {
			instance = new Paths();
		}

		return instance;
	}
	
	public Paths()
	{
		moves.put(MoveType.PAN_AROUND, new PanAround());
	}
	
	private MovePath getPathObject(MoveType moveType)
	{
		if(moves.containsKey(moveType) == false) {
			throw new RuntimeException("MoveType " + moveType + " is not added to 'moves' collection (in ctor) so cannot do this move");
		}
		
		return moves.get(moveType);
	}
	
	
	ArrayList<TweenPosition> getPath(MoveType moveType, PathArgs args)
	{
		try {
			switch(moveType) {
				case PAN_AROUND :
					return getPathObject(moveType).getPath(args);
				default :
					throw new RuntimeException("No implementation for MoveType " + moveType + " in place yet -- this would be a TODO");
			}
		} catch(PathArgException e) {
			throw new RuntimeException(e);
		}
	}

/*
	ArrayList<TweenPosition> getCircle(float atHeight, float centerX, float centerY, float radius)
	{
		return Circle.getPath(atHeight, centerX, centerY, radius);
	}

	static ArrayList<TweenPosition> getPanAround(float x, float y, float h, float radius)
	{
		return PanAround.getPath(x, y, h, radius);
	}
*/
	MoveType getRandomMove(MoveType[] moves)
	{
		return moves[ThreadLocalRandom.current().nextInt(0, moves.length)];
	}

	boolean createMovesInCmd(PointOfInterest poi, ObjectOfInterest ooi, SceneCommand cmd)
	{
		// TODO: Each of these movetypes should have several different moves and we pick a random one...
		MoveType mt = getRandomMove(ooi.getMoveTypes());
		
		float x = ooi.getX();
		float y = ooi.getY();
		float h = ooi.getH();
		
		if(x == 0 && y == 0) {
			x = poi.getLocation().x;
			y = poi.getLocation().y;
		}
		
		PathArgs args = new PathArgs();
		
		switch(mt) {
			case PAN_AROUND :
				args.setXYHR(x, y, h, 1.5f);
				break;
	
			case FOLLOW :
			case HOVER :
			case ANY :
			case NONE :
			default :
				throw new RuntimeException("setMoveWaypoints() TODO: " + mt.toString());
		}

		cmd.series.addAll( 
			getPath(mt, args)
		);
		return true;
	}

}
