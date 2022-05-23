package com.friya.wurmonline.client.hollywurm;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

class Scene
{
    @SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(Scene.class.getName());

    private String name = null;
	private String lastError = null;
	
	// Kek, an array list of array lists containing an array -- this will get terrible
	private List<ValidatedTween> tweensData = new ArrayList<ValidatedTween>();
	private ValidatedSetup setupData = null;
	
	Scene(String[] args) 
	{
		if(args.length > 2) {
			name = args[2];
		} else {
			name = "New Scene";
		}
	}
	
	String getName()
	{
		return name;
	}

	// /setup *
	boolean addSetupCommand(String[] action)
	{
		// Only allow one setup per scene.
		if(setupData != null) {
			return false;
		}
		
		setupData = new ValidatedSetup(action);
		return true;
	}
	
	// /camera *
	boolean addTweenCommand(String[] action, long startAtMs)
	{
		return tweensData.add(new ValidatedTween(action, startAtMs));
	}

	private boolean addSetupArg(String[] action)
	{
		return setupData.addArg(action);
	}

	private boolean addTweenArg(String[] action)
	{
		boolean res = lastTween().addArg(action);

		// 
		// TODO: Move to Builder -- keep track of last waypoint/target we added and compare number of arguments when we run into the next.
		//
		// This is a *bit* out of place, but it's the only thing that needs to be verified on a 'block' basis (as opposed to per line)
		//
		if((action[0].equals("waypoint") || action[0].equals("target")) && hasConsistentWaypointTargetArgs(lastTween()) == false) {
			setLastError("within one 'block', the target and all waypoints must have the same number of arguments (i.e. one waypoint cannot say x, y, height and another just x and y)");
			return false;
		}
		
		return res;
	}
	
	private boolean hasConsistentWaypointTargetArgs(ValidatedTween vt)
	{
		int prevArgCount = -1;
		
		List<String[]> vtArgs = vt.getData();
		
		for(String[] args : vtArgs) {
			if(args[0].equals("waypoint") || args[0].equals("target")) {
				if(prevArgCount == -1) {
					prevArgCount = args.length;
				} else {
					if(prevArgCount != args.length) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	boolean addArg(String context, String[] action)
	{
		switch(context) {
			case "/camera" :
				return addTweenArg(action);

			case "/world" :
				return addTweenArg(action);

			case "/setup" :
				return addSetupArg(action);

			default :
				lastError = "could not add an argument to command "+ context + " in scene. This is likely a bug, adding something new Friya?";
				return false;
		}
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("/scene start \"" + name + "\"\n");

		sb.append(setupData.toString());
		for(int j = 0; j < tweensData.size(); j++) {
			sb.append(tweensData.get(j).toString());
		}
		
		sb.append("/scene end\n");

		return sb.toString();
	}

	private ValidatedTween lastTween()
	{
		return tweensData.get(tweensData.size()-1);
	}

	List<ValidatedTween> getTweensData() {
		return tweensData;
	}

	ValidatedSetup getSetupData() 
	{
		if(setupData == null) {
			setupData = new ValidatedSetup();
		}
		
		return setupData;
	}

	String getLastError() 
	{
		return lastError;
	}

	void setLastError(String lastError) 
	{
		this.lastError = lastError;
	}
}
