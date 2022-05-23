package com.friya.spectator.communicator;

import java.util.ArrayList;
import com.wurmonline.math.Vector3f;

public class PointOfInterest
{
	private String title;
	private long timestamp;
	private long expires = 0;					// 0 = it will never expire. This POI can be used as a fallback when we have nothing else to do.
	private String message = "";
	private ArrayList<Vector3f> waypoints;
	private long waypointsDuration = 10000;
	private Vector3f location;
	private String audio = null;
	private String image = null;
	private InterestType type;
	private String spectator = "*";
	private int priority = 1;
	private long minDuration = 5000;
	private long maxDuration = 60000;
	private int maxRerunCount = 0;				// Says that this POI cannot be run more than once.
	private Vector3f minXYH;
	private Vector3f maxXYH;
	private ArrayList<ObjectOfInterest> objectsOfInterest = new ArrayList<ObjectOfInterest>();

	public PointOfInterest copyTo(PointOfInterest copy)
	{
		copy.title = title;
		copy.timestamp = timestamp;
		copy.expires = expires;
		copy.message = message;
		if(waypoints != null) {
			copy.waypoints = new ArrayList<Vector3f>();
			for(Vector3f v : waypoints) {
				copy.waypoints.add(
					new Vector3f(v.x, v.y, v.z)
				);
			}
		}
		copy.waypointsDuration = waypointsDuration;
		if(location != null) {
			copy.location = new Vector3f(location.x, location.y, location.z);
		}
		copy.audio = audio;
		copy.image = image;
		copy.type = type;
		copy.spectator = spectator;
		copy.priority = priority;
		copy.minDuration = minDuration;
		copy.maxDuration = maxDuration;
		copy.maxRerunCount = maxRerunCount;
		if(minXYH != null) {
			copy.minXYH = new Vector3f(minXYH.x, minXYH.y, minXYH.z);
		}
		if(maxXYH != null) {
			copy.maxXYH = new Vector3f(maxXYH.x, maxXYH.y, maxXYH.z);
		}
		if(objectsOfInterest != null) {
			for(ObjectOfInterest ooi : objectsOfInterest) {
				copy.objectsOfInterest.add(ooi.copyTo(new ObjectOfInterest()));
			}
		}
		return copy;
	}
	
	public PointOfInterest()
	{
	}
	
	public PointOfInterest(String title, float tileX, float tileY)
	{
		if(location == null) {
			location = new Vector3f();
		}
		
		this.title = title;
		this.location.x = tileX;
		this.location.y = tileY;
	}	

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		
		if(waypoints != null && waypoints.size() > 0) {
			sb.append("Waypoints:\n");
			for(Vector3f tp : waypoints) {
				sb.append("\t");
				sb.append(tp);
				sb.append("\n");
			}
		} else {
			sb.append("No waypoints");
		}

		if(objectsOfInterest!= null && objectsOfInterest.size() > 0) {
			sb.append("Objects of interest:\n");
			for(ObjectOfInterest ooi : objectsOfInterest) {
				sb.append("\t");
				sb.append(ooi);
				sb.append("\n");
			}
		} else {
			sb.append("No objects of interest");
		}

		return "title:" + getTitle() + " ts:" + getTimestamp() + " exp:" + getExpires() + " type:" + getType() + "\n" + sb.toString();
	}


	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}


	public long getTimestamp() {
		return timestamp;
	}


	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}


	public long getExpires() {
		return expires;
	}


	public void setExpires(long expires) {
		this.expires = expires;
	}


	public String getMessage() {
		return message;
	}


	public void setMessage(String message) {
		this.message = message;
	}


	public ArrayList<Vector3f> getWaypoints() {
		return waypoints;
	}


	public void addWaypoint(Vector3f waypoint) {
		this.waypoints.add(waypoint);
	}


	public Vector3f getLocation() {
		return location;
	}


	public void setLocation(Vector3f location) {
		this.location = location;
	}


	public String getAudio() {
		return audio;
	}


	public void setAudio(String audio) {
		this.audio = audio;
	}


	public String getImage() {
		return image;
	}


	public void setImage(String image) {
		this.image = image;
	}


	public InterestType getType() {
		return type;
	}


	public void setType(InterestType type) {
		this.type = type;
	}


	public String getSpectator() {
		return spectator;
	}


	public void setSpectator(String spectator) {
		this.spectator = spectator;
	}


	public int getPriority() {
		return priority;
	}


	public void setPriority(int priority) {
		this.priority = priority;
	}


	public long getMinDuration() {
		return minDuration;
	}


	public void setMinDuration(long minDuration) {
		this.minDuration = minDuration;
	}


	public long getMaxDuration() {
		return maxDuration;
	}


	public void setMaxDuration(long maxDuration) {
		this.maxDuration = maxDuration;
	}


	public Vector3f getMinXYH() {
		return minXYH;
	}

	
	public void setMinXYH(float minX, float minY, float minH) {
		this.minXYH.x = minX;
		this.minXYH.y = minY;
		this.minXYH.z = minH;
	}

	
	public Vector3f getMaxXYH() {
		return maxXYH;
	}

	
	public void setMaxXYH(float maxX, float maxY, float maxH) {
		this.maxXYH.x = maxX;
		this.maxXYH.y = maxY;
		this.maxXYH.z = maxH;
	}

	
	public ArrayList<ObjectOfInterest> getObjectsOfInterest() {
		return objectsOfInterest;
	}


	public void addObjectOfInterest(ObjectOfInterest objectOfInterest) {
		this.objectsOfInterest.add(objectOfInterest);
	}

	public long getWaypointsDuration() {
		return waypointsDuration;
	}

	public void setWaypointsDuration(long waypointsDuration) {
		this.waypointsDuration = waypointsDuration;
	}

	public int getMaxRerunCount() {
		return maxRerunCount;
	}

	public void setMaxRerunCount(int maxRerunCount) {
		this.maxRerunCount = maxRerunCount;
	}
}
