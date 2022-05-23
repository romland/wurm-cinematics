package com.friya.spectator.communicator;

import java.util.ArrayList;

public class EventInformation
{
	private EventHook hook = null;

	private final long id; 
	private final String serverName; 
	private final String serverIP;
	private final int mapSize;
	private ArrayList<PointOfInterest> pointsOfInterest;

	public void copyTo(EventInformation copy, boolean copyHook)
	{
		if(copyHook) {
			copy.hook = hook.copyTo(new EventHook());
		} else {
			copy.hook = null;
		}
		
		// These are assigned in ctor and do not need copying. 
		/*
		copy.id = id;
		copy.serverName = serverName;
		copy.serverIP = serverIP;
		copy.mapSize = mapSize;
		*/

		copy.pointsOfInterest = new ArrayList<PointOfInterest>();
		for(PointOfInterest poi : pointsOfInterest) {
			copy.pointsOfInterest.add(poi.copyTo(new PointOfInterest()));
		}
	}

	public EventInformation(long _id, String _serverName, String _serverIP, int _mapSize)
	{
		id = _id;
		serverName = _serverName;
		serverIP = _serverIP;
		mapSize = _mapSize;
		pointsOfInterest = new ArrayList<PointOfInterest>();
	}


	public void addPoi(PointOfInterest poi)
	{
		getPointsOfInterest().add(poi);
	}
	
	public ArrayList<PointOfInterest> getPois()
	{
		return getPointsOfInterest();
	}

	public int getAveragePriority()
	{
		int totPriority = 0;
		
		for(PointOfInterest poi : getPointsOfInterest()) {
			totPriority += poi.getPriority();
		}
		
		if(totPriority == 0) {
			return 0;
		}
		
		return (totPriority / getPointsOfInterest().size());
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		
		if(getPointsOfInterest().size() > 0) {
			sb.append("pointsOfInterest:\n");
			for(PointOfInterest tp : getPointsOfInterest()) {
				sb.append("\t");
				sb.append(tp);
				sb.append("\n");
			}
		} else {
			sb.append("No pointsOfInterest");
		}

		return getId() + " " + getServerName() + " " + getServerIP() + " " + getMapSize() + "\n" + sb.toString();
	}


	public long getId() {
		return id;
	}


	public String getServerName() {
		return serverName;
	}


	public String getServerIP() {
		return serverIP;
	}


	public int getMapSize() {
		return mapSize;
	}

	public ArrayList<PointOfInterest> getPointsOfInterest() {
		return pointsOfInterest;
	}

	public EventHook getHook() {
		return hook;
	}
}
