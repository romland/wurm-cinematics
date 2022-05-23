package com.friya.wurmonline.client.hollywurm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Purpose: Create simple scenes using in-game menus/commands.
 * 
 * For now: I do feel that scenes 'designed' in here are hit-and-run, you can Save them but 
 *          never load them again, use editor from then on.
 * 
 * @author Friyanouce
 */
public class SceneDesigner 
{
	private static Logger logger = Logger.getLogger(SceneDesigner.class.getName());

	private final static String subPrefix = "$$";
	
	private String name = null;
	private String templateName = null;
	private List<TweenPosition> cameraMoveWaypoints = new ArrayList<TweenPosition>();
	private List<TweenPosition> cameraFocusWaypoints = new ArrayList<TweenPosition>();
	private List<SceneCommand> sceneCommands = new ArrayList<SceneCommand>();
	
	private TweenPosition cameraMoveTarget = null;
	private TweenPosition cameraFocusTarget = null;
	private TweenPosition teleportPoint = null;
	private ArrayList<String> notes = new ArrayList<String>();
	
	private Map<String, String> vars = new HashMap<String, String>();

	private boolean fadingSetup = false;
	private String audioFilename = null;
	

	SceneDesigner(String name)
	{
		this(name, null);
	}


	boolean isEmpty()
	{
		return cameraMoveWaypoints.size() == 0 && cameraFocusWaypoints.size() == 0 && cameraMoveTarget == null && cameraFocusTarget == null;
	}


	void addVar(String key, String val)
	{
		vars.put(key, val);
	}


	SceneDesigner(String name, String templateName)
	{
		if(name.length() < 2) {
			throw new RuntimeException("A scene name must have at least two characters in its name");
		}

		this.name = name;
		
		if(templateName == null) {
			this.templateName = "default-scene";
		} else {
			this.templateName = templateName;
		}
	}

	boolean save(boolean silent)
	{
		String content = substitute();
		
		// This will simply overwrite whatever exists with that name.
		if(IO.save(getFilename(), content, null) == false) {
			WurmHelpers.tellCinematics("Failed to save \"" + getFilename() + "\"", MessageType.ERROR);
			return false;
		}

		if(!silent) {
			WurmHelpers.tellCinematics("Saved scene as '" + getFilename() + "\"");
		}
		
		return true;
	}
	
	String getFilename()
	{
		return IO.getSafeFilename(name);
	}

	protected String substitute()
	{
		// TODO: Change to other exceptions
		if(name == null) {
			throw new RuntimeException("A scene must have a name in order to be saved");
		}
		
		String templateNameSuffix = "";

		//
		// Pick the right template filename
		//
		if((cameraMoveWaypoints.size() > 0 || cameraMoveTarget != null) && (cameraFocusWaypoints.size() > 0 || cameraFocusTarget != null)) {
			templateNameSuffix = "_move_and_focus";

		} else if(cameraMoveWaypoints.size() > 0 || cameraMoveTarget != null) {
			templateNameSuffix = "_move";
			
		} else if(cameraMoveWaypoints.size() > 0 || cameraMoveTarget != null) {
			templateNameSuffix = "_focus";
		} else if(templateName.startsWith("default-scene")) {
			// Okay, let's just pick one then...
			templateNameSuffix = "_move";
		}
		
		String fullTemplateName = templateName + templateNameSuffix;
		
		if(!IO.exists("templates/" + fullTemplateName)) {
			throw new RuntimeException("The specified template does not exist: " + fullTemplateName);
		}

		String template = IO.load("templates/" + fullTemplateName);
		
		template = "# This cinematics script is based on: templates/" + fullTemplateName + "\r\n" 
				+ template;
		
		//
		// Do the substitutions
		//
		template = template.replace(subPrefix + "SCENE_NAME", name);
		
		//
		// Move.
		//
		boolean lastMoveWaypointIsTarget = false;
		boolean lastFocusWaypointIsTarget = false;

		if(cameraMoveTarget != null) {
			template = template.replace(subPrefix + "CAMERA_MOVE_TARGET", getTargetString(cameraMoveTarget));

		} else if(cameraMoveWaypoints.size() > 0) {
			// Let's be nice and convert the last waypoint to a target ... for now.
			template = template.replace(subPrefix + "CAMERA_MOVE_TARGET", getTargetString(cameraMoveWaypoints.get(cameraMoveWaypoints.size() - 1)));
			lastMoveWaypointIsTarget = true;
		} else {
			template = template.replace(subPrefix + "CAMERA_MOVE_TARGET", getTargetString(cameraMoveTarget));
		}

		//
		// Focus.
		//
		if(cameraFocusTarget != null) {
			template = template.replace(subPrefix + "CAMERA_FOCUS_TARGET", getTargetString(cameraFocusTarget));

		} else if(cameraFocusWaypoints.size() > 0) {
			// Let's be nice and convert the last waypoint to a target ... for now.
			template = template.replace(subPrefix + "CAMERA_FOCUS_TARGET", getTargetString(cameraFocusWaypoints.get(cameraFocusWaypoints.size() - 1)));
			lastFocusWaypointIsTarget = true;
		} else {
			template = template.replace(subPrefix + "CAMERA_FOCUS_TARGET", getTargetString(cameraFocusTarget));
		}

		template = template.replace(subPrefix + "CAMERA_MOVE_WAYPOINTS", getCameraMoveWaypointsString(lastMoveWaypointIsTarget));
		template = template.replace(subPrefix + "CAMERA_FOCUS_WAYPOINTS", getCameraFocusWaypointsString(lastFocusWaypointIsTarget));

		for (Map.Entry<String, String> entry : vars.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			template = template.replace(key, value);
		}
		
		//
		// Separate commands (mostly used by Server Side Events)
		//
		StringBuffer sceneCommandsBuf = new StringBuffer();
		for(SceneCommand sc : sceneCommands) {
			sceneCommandsBuf.append(sc.toString());
			sceneCommandsBuf.append("\r\n");
		}
		template = template.replace(subPrefix + "SCENE_COMMANDS", sceneCommandsBuf.toString());
		
		//
		// Setup Extras.
		//
		String setupExtras = "";

		if(audioFilename != null) {
			setupExtras += "audio play \"" + audioFilename + "\"\r\n";
		}

		if(teleportPoint != null) {
			setupExtras += "tell server \"#goto " + (int)teleportPoint.getX() + " " + (int)teleportPoint.getY() + " " + (int)teleportPoint.getH() + "\"\r\n"
					+ "\t\twait 3s\r\n";
		}
		
		if(fadingSetup) {
			setupExtras = "option fade on\r\n"
					+ setupExtras 
					+ "option fade off\r\n";
		}
		
		template = template.replace(subPrefix + "SETUP_EXTRAS", setupExtras);

		//
		// Add whatever we have in notes first in the script.
		//
		if(notes.size() > 0) {
			StringBuffer tmp = new StringBuffer();;
			for(String note : notes) {
				tmp.append("# ");
				tmp.append(note);
				tmp.append("\r\n");
			}
			template = tmp.toString() + template;
		}
		
		// This is no longer a template, though :)
		return template;
	}

	
	private String getWaypointString(TweenPosition tp)
	{
		if(tp == null) {
			return "# Missing method (waypoint) here! How come?";
		}
		
		return "waypoint " + tp.x + " " + tp.y + " " + tp.h;
	}

	private String getTargetString(TweenPosition tp)
	{
		if(tp == null) {
			return "# Missing method (target) here! How come?";
		}
		
		return "target " + tp.x + " " + tp.y + " " + tp.h;
	}

	private String getCameraMoveWaypointsString(boolean lastIsTarget)
	{
		return getWaypointsString(cameraMoveWaypoints, lastIsTarget);
	}
	
	private String getCameraFocusWaypointsString(boolean lastIsTarget)
	{
		return getWaypointsString(cameraFocusWaypoints, lastIsTarget);
	}

	private String getWaypointsString(List<TweenPosition> list, boolean lastIsTarget)
	{
		StringBuffer sb = new StringBuffer();
		
		TweenPosition tp;
		
		for(int i = 0; i < list.size(); i++) {
			if(i == (list.size() - 1)) {
				break;
			}

			tp = list.get(i);
			
			if(sb.length() > 0) {
				sb.append("\r\n\t\t");
			}
			sb.append(getWaypointString(tp));
		}
		
		return sb.toString();
	}
	
	boolean setTeleportPoint(float x, float y, float h)
	{
		teleportPoint = new TweenPosition(x, y, h);
		return true;
	}

	boolean addMoveWaypoint(float x, float y, float h)
	{
		return addWaypoint(cameraMoveWaypoints, x, y, h);
	}

	boolean addFocusWaypoint(float x, float y, float h)
	{
		return addWaypoint(cameraFocusWaypoints, x, y, h);
	}

	TweenPosition removeLastMoveWaypoint()
	{
		return removeLastWaypoint(cameraMoveWaypoints);
	}
	
	TweenPosition removeLastFocusWaypoint()
	{
		return removeLastWaypoint(cameraFocusWaypoints);
	}
	
	private boolean addWaypoint(List<TweenPosition> list, float x, float y, float h)
	{
		TweenPosition tp = new TweenPosition(x, y, h);
		return list.add(tp);
	}
	
	private TweenPosition removeLastWaypoint(List<TweenPosition> list)
	{
		if(list.size() == 0) {
			return null;
		}

		return list.remove(list.size() - 1);
	}
	
	boolean addObjectNote(long id, String name, float x, float y)
	{
		notes.add("Object note for ID " + id + ". " + name + " at " + x + " " + y);
		return true;
	}

	/**
	 * default is false
	 * 
	 * @return current status (true = fading)
	 */
	boolean toggleFadedSetup()
	{
		fadingSetup = !fadingSetup;
		return fadingSetup;
	}
	
	boolean addAudio(String fileName)
	{
		audioFilename = fileName;
		return true;
	}
	
	boolean addCommand(SceneCommand cmd)
	{
		sceneCommands.add(cmd);
		return true;
	}
}
