package com.friya.wurmonline.client.hollywurm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.friya.spectator.communicator.InterestType;
import com.friya.spectator.communicator.MoveType;
import com.friya.spectator.communicator.ObjectOfInterest;
import com.friya.spectator.communicator.PointOfInterest;
import com.wurmonline.math.Vector3f;

class EventInformationScene
{
	private static final Logger logger = Logger.getLogger(EventInformationScene.class.getName());

	PointOfInterest poi;
	SceneDesigner designer;

	long startedAt;
	long runCount = 0;
	
	long currentSceneDuration = 0;
	
	EventInformationScene(PointOfInterest poi)
	{
		this.poi = poi;
		designer = new SceneDesigner(poi.getTitle(), "default-server_event");
		
	}

	// TODO: We might want to have a "last ran" variable so that we can avoid running the same repeatedly if we have very few idling-scenes
	boolean start(ScriptEventListener eventListener)
	{
		startedAt = System.currentTimeMillis();
		runCount++;
		currentSceneDuration = 0;

		recalculate(true);
		ScriptRunner.runServerEvent(eventListener, this);

		return true;
	}
	
	private boolean recalculate(boolean gotoStart)
	{
		if(runCount == 0 || gotoStart) {
			addGotoPointOfInterest(poi);
		}
		
		if(poi.getObjectsOfInterest().size() == 0) {
			// Then focus on this tile.
			addTileOfInterest(poi.getLocation());
		} else {
			addObjectsOfInterest(poi);
		}
		
		// TODO: Need to deal with objects of interest here and generate moves for those...
		logger.info("TODO recalculate(): Create more of EIS -- add as much randomness as possible too!");
		return true;
	}


	private boolean addTileOfInterest(Vector3f xyh) 
	{
		throw new RuntimeException("addTileOfInterest() We do not yet support POIs without OOIs");
	}
	
	
	private boolean addObjectsOfInterest(PointOfInterest poi)
	{
		// NOTES:
		// - priority on a OOI merely says how much time we should spend on it -- percentage of 
		//   total OOI's in the POI (so, if there are two events, one with prio 1 and other 
		//   with prio 2 -- 33% of time is spent on the former, and 66% on the latter)
		
		List<ObjectOfInterest> oois = poi.getObjectsOfInterest();
		
		if(oois.size() == 0) {
			return true;
		}
		
		Collections.shuffle(oois);
		
		int totalPriority = 0;
		for(ObjectOfInterest ooi : oois) {
			totalPriority += ooi.getPriority();
		}

		float msPerPriority = Math.max(0, (getMaxDuration() - currentSceneDuration) / totalPriority);
		
		List<SceneCommand> ooiCmds = new ArrayList<SceneCommand>();
		for(ObjectOfInterest ooi : oois) {
			// TODO: We MIGHT want to merge these into one tween to get a more fluid motion...
			
			// Add a few milliseconds to make sure we don't overlap with our /at commands. 
			currentSceneDuration += 20;

			getCommandsForOOI(msPerPriority, ooi, ooiCmds);
			for(SceneCommand sc : ooiCmds) {
				designer.addCommand(sc);
			}
			
			ooiCmds.clear();
		}
		
		return true;
	}
	

	private boolean getCommandsForOOI(float msPerPriority, ObjectOfInterest ooi, List<SceneCommand> cmds)
	{
		//List<SceneCommand> cmds = new ArrayList<SceneCommand>();
		
		// At the very least an object should get 5 seconds.
		int commandDuration = (int)Math.max(5000, msPerPriority * ooi.getPriority());
		int numArgs = 3;
		
		if(ooi.getType() == InterestType.MOVING_OBJECT) {
			numArgs = 4;
		}
		
		if(ooi.getMoveTypes().length == 0) {
			ooi.setMoveTypes(new MoveType[]{ MoveType.ANY });
		}

		// TODOs:
		if(ooi.getType() != InterestType.STATIC_OBJECT && ooi.getType() != InterestType.TILE) {
			throw new RuntimeException("getCommandsForOOI() TODO: not handling anything other than static object / tile atm");
		}
		
		// generate waypoints for this move... depending on MoveType(s)
		long moveLagMs = 1000;

		SceneCommand moveCmd = new SceneCommand("/camera move for " + (commandDuration - moveLagMs) + "ms", numArgs);
		moveCmd.startAfter(currentSceneDuration + moveLagMs);
		Paths.getInstance().createMovesInCmd(poi, ooi, moveCmd);
		cmds.add(moveCmd);
		
		int quickFocusDuration = 0;
		if(true) {
			SceneCommand quickFocusCmd = new SceneCommand("/camera focus for " + quickFocusDuration + "ms", numArgs);
			quickFocusCmd.series.add(new TweenPosition( ooi.getX(), ooi.getY(), ooi.getH() ));
			cmds.add(quickFocusCmd);
		}
		
		if(true) {
			SceneCommand holdFocusCmd = new SceneCommand("/camera focus for " + (commandDuration - quickFocusDuration) + "ms", numArgs);
			holdFocusCmd.series.add(new TweenPosition( ooi.getX(), ooi.getY(), ooi.getH() ));
			cmds.add(holdFocusCmd);
		}
		
		if(false) {
			SceneCommand focusCmd = new SceneCommand("/camera focus for " + (commandDuration - quickFocusDuration) + "ms", numArgs);
			focusCmd.startAfter(currentSceneDuration + quickFocusDuration);
			//Don't do this, create a new series: focusCmd.series = moveCmd.series;
			cmds.add(focusCmd);
		}

		
		// if using fourth argument (follow), remember to set objectId in TweenPosition
		// if pan around, make sure we do a quick focus (in 1/20th of move-time or so)
		// TODO: do lagged move/focus when moving (unless panning around)
		
		currentSceneDuration += commandDuration;

		return true;
	}
	
	
	private boolean addGotoPointOfInterest(PointOfInterest poi)
	{
		Vector3f currentPos;
		
		if(Mod.EARLY_TESTING) {
			currentPos = new Vector3f(
					987,
					1234,
					123
				);
		} else {
			currentPos = new Vector3f(
					WurmHelpers.getWorld().getPlayerCurrentTileX(), 
					WurmHelpers.getWorld().getPlayerCurrentTileY(), 
					WurmHelpers.getWorld().getPlayerPosH()
				);
		}

		
		Vector3f destination = poi.getLocation();
		Vector3f poiStart;
		
		int quickFocusDuration = 0;
		
		if(poi.getWaypoints() != null && poi.getWaypoints().size() > 0) {
			poiStart = poi.getWaypoints().get(0);

			long moveLagMs = 1000;
			long cmdDuration = poi.getWaypointsDuration();
			SceneCommand cmdMove = new SceneCommand("/camera move for " + (cmdDuration - moveLagMs) + "ms", 3);
			cmdMove.startAfter(currentSceneDuration + moveLagMs);

			SceneCommand cmdFocus = new SceneCommand("/camera focus for " + (cmdDuration - quickFocusDuration) + "ms", 3);
			cmdFocus.startAfter(currentSceneDuration + quickFocusDuration);
			
			for(Vector3f pos : poi.getWaypoints()) {
				cmdMove.series.add(new TweenPosition(pos.x, pos.y, pos.z));

				// Try to avoid looking at exact same position as we are at, as that induces very jerky camera.
				cmdFocus.series.add(new TweenPosition(pos.x, pos.y, pos.z));
			}
			
			// Don't move all the way...
			//cmdMove.series.add(new TweenPosition(destination.x, destination.y, destination.z));
			
			cmdFocus.series.add(new TweenPosition(destination.x, destination.y, destination.z));

			SceneCommand quickFocusCmd = new SceneCommand("/camera focus for " + quickFocusDuration + "ms", 3);
			quickFocusCmd.series.add(new TweenPosition( cmdFocus.series.get(0).x, cmdFocus.series.get(0).y, cmdFocus.series.get(0).h ));
			designer.addCommand(quickFocusCmd);
			
			designer.addCommand(cmdFocus);

			// The "move" adds an /at, we want the focus to start before the move, to get the "lagged look" feeling.
			designer.addCommand(cmdMove);
			currentSceneDuration += cmdDuration;
		} else {
			poiStart = poi.getLocation();
		}

		if(poiStart.distance(currentPos) > 40) {
			designer.setTeleportPoint(poiStart.x, poiStart.y, poiStart.z);
		}

		return true;
	}

	public String toString()
	{
		if(true) {
			// This is for testing only, to easily rerun scripts.
			WurmHelpers.tellCinematics("Debug notice: Saving script in EventInformationScene.toString()");
			designer.save(false);
			Actions.currentLoadedScript = designer.getFilename();
		}
		
		return designer.substitute();
	}

	long getPriority()
	{
		return poi.getPriority();
	}
	
	long getExpiration()
	{
		return poi.getExpires();
	}
	
	long getMinDuration()
	{
		return poi.getMinDuration();
	}
	
	long getMaxDuration()
	{
		return poi.getMaxDuration();
	}
	
	boolean canRunAgain()
	{
		return runCount < poi.getMaxRerunCount() || poi.getMaxRerunCount() < 0;
	}
	
}
