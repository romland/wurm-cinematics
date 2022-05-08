package com.friya.wurmonline.client.hollywurm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.friya.spectator.communicator.EventInformation;
import com.friya.spectator.communicator.PointOfInterest;

public class ServerEventHandler implements ScriptEventListener
{
	private static Logger logger = Logger.getLogger(ServerEventHandler.class.getName());

	private ArrayList<EventInformation> queuedEvents = new ArrayList<EventInformation>();
//	private ArrayList<EventInformation> idlingEvents = new ArrayList<EventInformation>();
	
	// Typically only one "event" is in at queuedScenes at any given point. Once an event made it in here, it is removed from queuedEvents.
	private LinkedList<EventInformationScene> queuedScenes = new LinkedList<EventInformationScene>();
	private LinkedList<EventInformationScene> idlingScenes = new LinkedList<EventInformationScene>();
	
	private EventInformationScene currentRunningScene = null;
	
	private static final int MAX_EVENTS = 100;
	private static ServerEventHandler instance = null;
	private static Timer timer;
	private static final long tickFrequency = 1000;
	private static boolean ticking;

	/*
	 * in the case of a "existing uniques event":
	 * 		they move
	 * 		but we want them as an idle event -- so they SHOULD be purged -- but not after having been ran just once
	 * 		do we always keep events around until they expire perhaps?
	 *  
	 *  
	 */
	
	// TODO: make sure to check an incoming event if we have any repeatable scenes or something without expiry (they might theoretically be purged before we get to them)

	// TODO: Basic show off deed -- locate some nice objects/buildings on deed and set those to OOIs
	
	// have more effects than just fade when switching long-distance scenes (fade to white, zoom into a Wurm logo then to brownish, ...) with a sound effect if no sounds are playing
	
	// TODO: need to calculate how much time we should spend on multiple waypoints (when going to location and between OOIs)
	
	// if no new events telling us "new audio", play audio until it's done?
	
	// - "completing house" event, calculate the size of it and pan around -- make it count for paste too, for easiness

	// design some default moves (like pan-around low etc)
	
	// between objects of interest, we should be able to jump between these randomly, provided they are within 40 tiles of eachother
	// ability to generate a new tween around an object of interest (but need a few random moves first)
	
	// be able to feed in a song with "key moments" which we'll cut scenes at (live music video -- kinda)
	
	// unique spawn event
	// existing uniques event (perfect idle event)
	
	/*
	 * generate tweens manually? 
	 * or use scene designer?
	 * or use designed templates?
	 * 
	 * TODO Communicator:
	 * - be able to specify which scene template to use for each scene?
	 * 
	 * moves:
	 * 		- low ground pan-around lookng upwards (4 waypoints around)
	 *		- only do #goto if we are > 40 tiles away from event -- otherwise generate a tween to cover distance
	 * 
	 * - each point-of-interest is a scene
	 * 		- each scene fades out and in
	 * 		- if there are waypoints in the POI, use that to get to 'location'
	 * 		- depending on max-duration, decide how much time is spent getting to destination (via waypoints)
	 * 		- if minCoord/maxCoord is decided, set up random coordinates within the area (waypoints) and keep focus on the location / objectsOfInterest
	 * 		- if objects of interest, use random moves around these, but always keep focus on one of them OR the main location -- switch between them
	 */

	// TODO: If a lot of tree tiles around an OOI, stay low
	// TODO: Tree avoidance when creating paths
	
	// Do we ever want to merge several scenes into one script? Perhaps to get rid of possible gaps?
	//String script = mergeScenes(true);

	// do we need to do anything when we get notification of scene/script end? Perhaps to minimize gaps? NOTE: We MUST NOT start right away in the event notice, but perhaps call tick() in say, 10-20ms.

	public static ServerEventHandler getInstance()
	{
		if(instance == null) {
			instance = new ServerEventHandler();
		}

		return instance; 
	}

	private void tick()
	{
		if(ticking) {
			return;
		}
		
		ticking = true;
		
		// TODO:
		// do we have a high priority scene in our queue? do we have a current running scene? Has it run its minimum duration?
		// - cancel it
		
		if(queuedEvents.size() > 0 && queuedScenes.size() == 0) {
			convertEventToScenes(getHighestPriorityEvent());
			Collections.shuffle(queuedScenes);
		}

		if(ScriptRunner.isRunning() == false && ScriptRunner.isSettingUp() == false) {
			logger.info("queuedEvents: " + queuedEvents.size() + " queuedScenes: " + queuedScenes.size() + " idlingScenes: " + idlingScenes.size());
			if(queuedScenes.size() > 0) {
	
				currentRunningScene = queuedScenes.get(0);

				currentRunningScene.start(this);

				if(currentRunningScene.canRunAgain() && idlingScenes.contains(currentRunningScene) == false) {
					idlingScenes.add(currentRunningScene);
				}
				queuedScenes.remove(currentRunningScene);

			} else if(idlingScenes.size() > 0) {
				// Fall back on scenes that can be rerun...
				Collections.shuffle(idlingScenes);
				currentRunningScene = idlingScenes.get(0);
				currentRunningScene.start(this);
			}
		}
		
		purgeExpiredPointsOfInterest();
		purgeExpiredScenes();
		
		ticking = false;
	}
	
	private void startEventPoller()
	{
		if(timer != null) {
			stopEventPoller();
		}
		
		ticking = false;
		timer = new Timer();
		
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				tick();
			}
		}, 0, tickFrequency);
	}

	private void stopEventPoller()
	{
		timer.cancel();
		timer = null;
	}
	
	private String mergeScenes()
	{
		StringBuffer sb = new StringBuffer();
		
		for(EventInformationScene eis : queuedScenes) {
			sb.append(eis);
			sb.append("\r\n\r\n");
		}

		return sb.toString();
	}

	private boolean convertEventToScenes(EventInformation ei)
	{
		ArrayList<PointOfInterest> pois = ei.getPois();
		
		for(PointOfInterest poi : pois) {
			EventInformationScene eiScene = new EventInformationScene(poi);
			
			if(eiScene.getExpiration() == 0) {
				idlingScenes.add(eiScene);
			} else {
				queuedScenes.add(eiScene);
			}
		}
		
		removeEvent(ei);
		return true;
	}
	
	// Remove any POIs we have sitting in queued server-events too.
	private int purgeExpiredPointsOfInterest()
	{
		int removed = 0;
		
		for(EventInformation ei : queuedEvents) {
			ArrayList<PointOfInterest> pois = ei.getPois();
			for(PointOfInterest poi : pois) {
				if(poi.getExpires() > System.currentTimeMillis()) {
					pois.remove(poi);
					removed++;
				}
			}
		}
		
		return removed;
	}

	// Remove any scenes we may have sitting waiting, in case we have not dealt with them yet
	private int purgeExpiredScenes()
	{
		int removed = 0;

		for(EventInformationScene eiScene : queuedScenes) {
			if(eiScene.getExpiration() > System.currentTimeMillis()) {
				queuedScenes.remove(eiScene);
				removed++;
			}
		}

		return removed;
	}

	public void addEvent(EventInformation ei)
	{
		if(queuedEvents.size() >= MAX_EVENTS) {
			removeLowestPriorityEvent();
		}
		
		localizeTimestamps(ei);
		queuedEvents.add(ei);
		startEventPoller();
		
		if(Mod.EARLY_TESTING) {
			tick();
		}
	}

	public void removeEvent(EventInformation ei)
	{
		queuedEvents.remove(ei);

		if(queuedEvents.size() == 0) {
			stopEventPoller();
		}
	}
	
	private void localizeTimestamps(EventInformation ei)
	{
		long ourTime = System.currentTimeMillis();

		for(PointOfInterest poi : ei.getPois()) {
			long expireDelta = poi.getExpires() - poi.getTimestamp();

			if(poi.getExpires() > 0) {
				poi.setExpires(ourTime + expireDelta);
			}

			poi.setTimestamp(ourTime);
		}
	}
	
	private EventInformation getHighestPriorityEvent()
	{
		EventInformation toReturn = null;
		
		if(queuedEvents.size() == 0) {
			return null;
		}
		
		int currentHighest = -10000;

		for(EventInformation ei : queuedEvents) {
			if(ei.getAveragePriority() > currentHighest) {
				currentHighest = ei.getAveragePriority();
				toReturn = ei;
			}
		}
		
		return toReturn;
	}
	
	private void removeLowestPriorityEvent()
	{
		EventInformation toRemove = null;
		
		if(queuedEvents.size() == 0) {
			return;
		}
		
		int lowestPriority = 999999;
		for(EventInformation ei : queuedEvents) {
			if(ei.getAveragePriority() < lowestPriority) {
				lowestPriority = ei.getAveragePriority();
				toRemove = ei;
			}
		}
		
		if(toRemove == null) {
			return;
		}
		
		removeEvent(toRemove);
	}

	@Override
	public void scriptEndedEvent(EventInformationScene scene) 
	{
		// TODO Auto-generated method stub
		logger.info("scriptEndedEvent() called");
	}

	@Override
	public void scriptStartedEvent(EventInformationScene scene) 
	{
		// TODO Auto-generated method stub
		logger.info("scriptStartedEvent() called");
	}

	@Override
	public void sceneEndedEvent(EventInformationScene scene) 
	{
		// TODO Auto-generated method stub
		logger.info("sceneEndedEvent() called");
	}

	@Override
	public void sceneStartedEvent(EventInformationScene scene) 
	{
		// TODO Auto-generated method stub
		logger.info("sceneStartedEvent() called");
	}

}
