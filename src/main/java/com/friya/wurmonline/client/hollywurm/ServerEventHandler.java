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

	// Typically only one "event" is in at queuedScenes at any given point. Once an event made it in here, it is removed from queuedEvents.
	private LinkedList<EventInformationScene> queuedScenes = new LinkedList<EventInformationScene>();
	private LinkedList<EventInformationScene> idlingScenes = new LinkedList<EventInformationScene>();
	
	private EventInformationScene currentRunningScene = null;
	
	private static final int MAX_EVENTS = 100;
	private static ServerEventHandler instance = null;
	private static Timer timer;
	private static final long tickFrequency = 1000;
	private static boolean ticking;

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
