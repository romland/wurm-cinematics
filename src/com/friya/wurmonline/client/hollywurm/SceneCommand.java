package com.friya.wurmonline.client.hollywurm;

import java.util.ArrayList;

class SceneCommand 
{
	long startAfter = 0;
	int numArgs = 0;
	String command = null;
	String easing = null;
	String pathing = null;
	ArrayList<TweenPosition> series = new ArrayList<TweenPosition>();

	public SceneCommand(String cmd, int numArgs)
	{
		this.command = cmd;
		this.numArgs = numArgs;
	}


	public String toString()
	{
		if(series.size() == 0) {
			return "";
		}
		
		StringBuffer sb = new StringBuffer();

		// Only add this if it's set. We want to tag along on previous /at if it's not.
		if(startAfter > 0) {
			sb.append("\t/at ");
			sb.append(startAfter);
			sb.append("ms\r\n");
		}
		
		sb.append("\t\t");
		sb.append(command);
		sb.append("\r\n");
		for(int i = 0; i < series.size(); i++) {
			sb.append("\t\t\t");
			if(i == (series.size() - 1)) {
				sb.append("target");
			} else {
				sb.append("waypoint");
			}
			if(numArgs > 3) sb.append(" " + series.get(i).shortTargetId);
			if(numArgs > 0) sb.append(" " + series.get(i).x);
			if(numArgs > 1) sb.append(" " + series.get(i).y);
			if(numArgs > 2) sb.append(" " + series.get(i).h);
			sb.append("\r\n");
		}
		
		if(easing != null) {
			sb.append("\t\t\tuse easing ");
			sb.append(easing);
			sb.append("\r\n");
		}

		if(pathing != null) {
			sb.append("\t\t\tuse pathing ");
			sb.append(pathing);
			sb.append("\r\n");
		}
		
		return sb.toString();
	}

	void startAfter(long i) 
	{
		startAfter = i;
	}
}
