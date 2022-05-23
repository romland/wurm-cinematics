package com.friya.wurmonline.client.hollywurm;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.friya.spectator.communicator.MoveType;
import com.friya.wurmonline.client.hollywurm.paths.PathArgs;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.effects.CustomParticleEffect;
import com.wurmonline.math.Vector3f;
import com.wurmonline.shared.constants.CounterTypes;
import com.wurmonline.shared.constants.PlayerAction;

public class Actions 
{
	private static final Logger logger = Logger.getLogger(Actions.class.getName());
	
	private static SceneDesigner currentSceneDesigner = null;
	static String currentLoadedScript = null;
	private static String[] lastDir = null;				// need to keep track of indices for 'load script' dialog's response
	private static String[] lastExamplesDir = null;		// keeping track of this so we know which example is previous/next
	private static int currentLoadedExample = -1;
	
	static final int BML_DIALOG_LOAD_SCRIPT = -12345;
	
	static final short START_SCRIPT_ID = 30550;
	static final short STOP_SCRIPT_ID = 30551;
	static final short LOAD_SCRIPT_ID = 30552;
	static final short EDIT_SCRIPT_ID = 30553;
	
	static final short NEW_SCRIPT_ID = 30650;
	static final short NEW_WAYPOINT_ID = 30651;
	static final short REMOVE_LAST_WAYPOINT_ID = 30652;
	static final short NEW_FOCUS_ID = 30653;

	static final short TEST_ID = 30752;
	static final short TEST2_ID = 30753;
	static final short FOLLOW_ID = 30754;

	static final short HELP_ID = 30755;
	static final short GET_OBJECT_ID = 30756;
	static final short REMOVE_LAST_FOCUSPOINT_ID = 30757;
	static final short NEW_TELEPORT_ID = 30758;
	static final short NEW_OBJECT_NOTE_ID = 30759;
	static final short NEW_ORBIT_AROUND_ID = 30760;
	
	
	// Root-menu for cinematics
	static final short ROOT_MENU_ID = -30549;
	static final short SCENE_DESIGNER_MENU_ID = -30548;
	
	static final PlayerAction RootMenuAction = new PlayerAction(ROOT_MENU_ID, 65535, "Friya's Cinematics", true);
	static final PlayerAction SceneDesignerMenuAction = new PlayerAction(SCENE_DESIGNER_MENU_ID, 65535, "Scene Desginer", true);

	// Script actions.
	static final PlayerAction StartScriptAction = new PlayerAction(START_SCRIPT_ID, 65535, "Run script", true);
	static final PlayerAction StopScriptAction = new PlayerAction(STOP_SCRIPT_ID, 65535, "Stop script", true);
	static final PlayerAction LoadScriptAction = new PlayerAction(LOAD_SCRIPT_ID, 65535, "Load script", true);
	static final PlayerAction EditScriptAction = new PlayerAction(EDIT_SCRIPT_ID, 65535, "Edit current script", true);
	static final PlayerAction NewScriptAction = new PlayerAction(NEW_SCRIPT_ID, 65535, "New script", true);
	static final PlayerAction DocumentationAction = new PlayerAction(HELP_ID, 65535, "Help...", true);

	// Scene design actions.
	static final PlayerAction NewWaypointAction = new PlayerAction(NEW_WAYPOINT_ID, 65535, "Add waypoint", true);
	static final PlayerAction RemoveLastWaypointAction = new PlayerAction(REMOVE_LAST_WAYPOINT_ID, 65535, "Remove last waypoint", true);
	static final PlayerAction NewFocusPointAction = new PlayerAction(NEW_FOCUS_ID, 65535, "Add focus point", true);
	static final PlayerAction GetObjectIdAction = new PlayerAction(GET_OBJECT_ID, 65535, "Show object ID", true);
	static final PlayerAction RemoveLastFocuspointAction = new PlayerAction(REMOVE_LAST_FOCUSPOINT_ID, 65535, "Remove last focus point", true);
	static final PlayerAction NewTeleportPointAction = new PlayerAction(NEW_TELEPORT_ID, 65535, "Add teleport point", true);
	static final PlayerAction NewObjectNoteAction = new PlayerAction(NEW_OBJECT_NOTE_ID, 65535, "Add note for object", true);
	static final PlayerAction NewOrbitAroundAction = new PlayerAction(NEW_ORBIT_AROUND_ID, 65535, "Add orbit point", true);
	

	// public PlayerAction(final short aId, final int aTargetMask, final String aName, final boolean aInstant)
	public static PlayerAction[] getActions()
	{
		if(!Mod.cinematicsEnabled) {
			return new PlayerAction[] {};
		}
		
		return new PlayerAction[]{
				// ----------------------------
				RootMenuAction,
				
				NewScriptAction,
				LoadScriptAction,

				StartScriptAction,
				StopScriptAction,

				EditScriptAction,
				DocumentationAction,
				
				// ----------------------------
				SceneDesignerMenuAction,
				NewWaypointAction,
				RemoveLastWaypointAction,
				NewFocusPointAction,
				RemoveLastFocuspointAction,
				NewOrbitAroundAction,
				NewTeleportPointAction,
				NewObjectNoteAction,
				GetObjectIdAction
		};
	}

	public static boolean isOfInterest(short actionId)
	{
		PlayerAction[] acts = getActions();
		for(PlayerAction act : acts) {
			if(act.getId() == actionId) {
				return true;
			}
		}

		return false;
	}
	
	/**
	 * See com.wurmonline.shared.constants.CounterTypes
	 * vim wurmonline/shared/constants/CounterTypes.java
	 * 
	 * @param id
	 * @return
	 */
	static public final int getType(long id)
	{
		return (int)(id & 255);
	}
	
	static boolean handleChatInput(String cmd)
	{
		if(cmd.startsWith("/") == false) {
			return false;
		}

		String[] tokens = ScriptBuilder.tokenize(cmd);
		
		if(tokens.length == 0) {
			return false;
		}
		
		switch(tokens[0]) {
			case "/lo" :
			case "/loadscript" :
				if(tokens.length < 2) {
					WurmHelpers.tellCinematics("We need name of script to load here.", MessageType.ERROR);
					return false;
				}
				
				boolean res = cmdLoadScript(tokens[1]);
				if(res) {
					WurmHelpers.tellCinematics("Loaded script \"" + currentLoadedScript + "\".", MessageType.POSITIVE);
				} else {
					WurmHelpers.tellCinematics("No such script: \"" + tokens[1] + "\".", MessageType.ERROR);
				}
				return res;
			
			case "/example" :
				if(lastExamplesDir == null) {
					lastExamplesDir = IO.dir("examples");
				}
				
				if(tokens.length == 1) {
					if(currentLoadedExample < 0) {
						WurmHelpers.tellCinematics("No example script is currently loaded.", MessageType.ERROR);
					} else {
						WurmHelpers.tellCinematics("Currently example \"" + lastExamplesDir[currentLoadedExample] + "\" is loaded.", MessageType.POSITIVE);
					}
					return true;
				}
				
				if(tokens[1].startsWith("p")) {
					// /example previous
					if(currentLoadedExample > 0) {
						currentLoadedExample--;
					} else {
						currentLoadedExample = 0;
					}
					
				} else if(tokens[1].startsWith("n")) {
					// /example next
					if(currentLoadedExample < (lastExamplesDir.length - 1)) {
						currentLoadedExample++;
					} else {
						currentLoadedExample = lastExamplesDir.length - 1;
					}

				} else {
					WurmHelpers.tellCinematics("You want to use /example previous or /example next.", MessageType.ERROR);
					return false;
				}
				
				return handleChatInput("/loadscript \"examples" + File.separator + lastExamplesDir[currentLoadedExample]  + "\"");
		}
		
		return false;
	}

	
	/**
	 * This one's got a bit of a misleading name since it does not actually load anything. 
	 * Scripts are loaded when they are executed (so they are always fresh).
	 * 
	 * @param scriptFileName
	 * @return
	 */
	static private boolean cmdLoadScript(String scriptFileName)
	{
		if(IO.exists(scriptFileName) == false) {
			return false;
		}
		
		currentSceneDesigner = null;
		currentLoadedScript = scriptFileName;
		return true;
	}
	

	static boolean handleConsoleInput(String cmd, String[] args)
	{
		// Note, cmd is also in the args array, so first arg after command is [1].
		switch(cmd) {
			case "fx" :
				if(args.length > 1) {
					EffectBuilder.loadEffect(args[1]);
					return true;
				}
				break;
				
			default :
				// Forward all console commands we do not know to chat input and let that decide if it should be dealt with
				logger.info("unhandled console input: " + cmd + " " + Arrays.toString(args));
				if(args.length > 1) {
					return handleChatInput("/" + String.join(" ", args));
				} else {
					return handleChatInput("/" + cmd);
				}
		}
		
		return false;
	}
	
	private static void cmdStartScript()
	{
		try {
			
			//
			// The idea here is that a script being designed will take precedence over a loaded script. 
			// HOWEVER: If a script is loaded after something was being designed, that will clear the 
			// currentSceneDesigner. This should not be a problem, because that should be saved over
			// time it is changed -- so can always be reloaded at a later point.
			//
			// Logical? Didn't think so. We'll see how long it'll be before I rewrite this functionality.
			//
			
			if(currentSceneDesigner != null && currentSceneDesigner.isEmpty() == false) {
				currentLoadedScript = currentSceneDesigner.getFilename();
			}
			ScriptRunner.run(currentLoadedScript);

		} catch(CinematicBuilderException e) {
			WurmHelpers.tellCinematics(e.getMessage(), MessageType.ERROR);
			logger.log(Level.SEVERE, e.toString());
		}
	}
	
	private static void cmdStopScript()
	{
		try {
			ScriptRunner.stopScript();

		} catch(CinematicBuilderException e) {
			WurmHelpers.tellCinematics(e.getMessage(), MessageType.ERROR);
			logger.log(Level.SEVERE, e.toString());
		}
	}
	
	private static void cmdAddWaypoint(long[] sources)
	{
		if(currentSceneDesigner == null) {
			WurmHelpers.tellCinematics("You need to create a new script first.", MessageType.ERROR);
			return;
		}
		
		if(sources.length == 0) {
			WurmHelpers.tellCinematics("Huh. Shouldn't sources always contain something? Bug. Tell Friya.", MessageType.ERROR);
			return;
		}

		TileInfo ti = new TileInfo(sources[0]);
		int t = getType(sources[0]);

		Integer[] allowedTypes = new Integer[]{
			(int) CounterTypes.COUNTER_TYPE_TILES,
			(int) CounterTypes.COUNTER_TYPE_STRUCTURES,
			(int) CounterTypes.COUNTER_TYPE_WALLS,
			(int) CounterTypes.COUNTER_TYPE_FENCES,
			(int) CounterTypes.COUNTER_TYPE_TILEBORDER,
			(int) CounterTypes.COUNTER_TYPE_CAVETILES,
			(int) CounterTypes.COUNTER_TYPE_FLOORS,
			(int) CounterTypes.COUNTER_TYPE_TILECORNER,
			(int) CounterTypes.COUNTER_TYPE_BRIDGE_PARTS
		};

		if(ScriptBuilder.contains(allowedTypes, t) == false) {
			WurmHelpers.tellCinematics("You cannot add a waypoint to that. You need tiles, walls, floors, etc.", MessageType.ERROR);
			return;
		}
		
		// TODO: Note, this is just for 'move' now, question is do we want to add separate actions for move, focus (etc), or do we want to
		//       have some way of switching context?
		//currentSceneDesigner.addMoveWaypoint(ti.getTileX(), ti.getTileY(), ti.getHeight() * 10);	// dirts, not meters

		// Use height of player as opposed to height of tile.
		float h =  WurmHelpers.getPlayer().getPos().getH() * 10;
		currentSceneDesigner.addMoveWaypoint(ti.getTileX(), ti.getTileY(), h);	// dirts, not meters
		currentSceneDesigner.save(true);
		
		WurmHelpers.tellCinematics("Added waypoint " + ti.getTileX() + ", " + ti.getTileY() + " with height " + h + ".", MessageType.INFO);
	}


	private static void cmdAddFocusPoint(long[] sources)
	{
		if(currentSceneDesigner == null) {
			WurmHelpers.tellCinematics("You need to create a new script first.", MessageType.ERROR);
			return;
		}
		
		if(sources.length == 0) {
			WurmHelpers.tellCinematics("Huh. Shouldn't sources always contain something? Bug. Tell Friya.", MessageType.ERROR);
			return;
		}

		TileInfo ti = new TileInfo(sources[0]);
		int t = getType(sources[0]);

		Integer[] allowedTypes = new Integer[]{
			(int) CounterTypes.COUNTER_TYPE_TILES,
			(int) CounterTypes.COUNTER_TYPE_STRUCTURES,
			(int) CounterTypes.COUNTER_TYPE_WALLS,
			(int) CounterTypes.COUNTER_TYPE_FENCES,
			(int) CounterTypes.COUNTER_TYPE_TILEBORDER,
			(int) CounterTypes.COUNTER_TYPE_CAVETILES,
			(int) CounterTypes.COUNTER_TYPE_FLOORS,
			(int) CounterTypes.COUNTER_TYPE_TILECORNER,
			(int) CounterTypes.COUNTER_TYPE_BRIDGE_PARTS
		};

		if(ScriptBuilder.contains(allowedTypes, t) == false) {
			WurmHelpers.tellCinematics("You cannot add a waypoint to that. You need tiles, walls, floors, etc.", MessageType.ERROR);
			return;
		}

		// Use height of player as opposed to height of tile.
		float h =  WurmHelpers.getPlayer().getPos().getH() * 10;
		currentSceneDesigner.addFocusWaypoint(ti.getTileX(), ti.getTileY(), h);	// dirts, not meters
		currentSceneDesigner.save(true);
		
		WurmHelpers.tellCinematics("Added focus point " + ti.getTileX() + ", " + ti.getTileY() + " with height " + h + ".", MessageType.INFO);
	}
	

	private static void cmdFollow(long source)
	{
		if(source != CounterTypes.COUNTER_TYPE_PLAYERS && source != CounterTypes.COUNTER_TYPE_CREATURES) {
			WurmHelpers.tellCinematics("Cannot follow non-creatures/players.", MessageType.ERROR);
			return;
		}

		cmdStopScript();
		
		String scriptName = "Script follow " + System.currentTimeMillis();
		currentSceneDesigner = new SceneDesigner(scriptName, "follow");
		WurmHelpers.tellCinematics("The new script will be saved as: " + currentSceneDesigner.getFilename(), MessageType.IMPORTANT);

		currentLoadedScript = currentSceneDesigner.getFilename();
		
		currentSceneDesigner.addVar("$$CREATURE_ID", Long.toString(source) );

		currentSceneDesigner.save(true);
		cmdStartScript();
	}
	
	private static TileInfo getTileInfo(long source)
	{
		TileInfo ti = new TileInfo(source);
		int t = getType(source);

		Integer[] allowedTypes = new Integer[]{
			(int) CounterTypes.COUNTER_TYPE_TILES,
			(int) CounterTypes.COUNTER_TYPE_STRUCTURES,
			(int) CounterTypes.COUNTER_TYPE_WALLS,
			(int) CounterTypes.COUNTER_TYPE_FENCES,
			(int) CounterTypes.COUNTER_TYPE_TILEBORDER,
			(int) CounterTypes.COUNTER_TYPE_CAVETILES,
			(int) CounterTypes.COUNTER_TYPE_FLOORS,
			(int) CounterTypes.COUNTER_TYPE_TILECORNER,
			(int) CounterTypes.COUNTER_TYPE_BRIDGE_PARTS
		};

		if(ScriptBuilder.contains(allowedTypes, t) == false) {
			return null;
		}
		
		return ti;
	}
	
	private static void cmdEditCurrentScript()
	{
		if(currentLoadedScript == null) {
			WurmHelpers.tellCinematics("There is currently no loaded script. Note that a script that is currently being 'designed' in-game can not and should not be edited outside of game.", MessageType.INFO);
			return;
		}

		WurmHelpers.tellCinematics("Opening \"" + currentLoadedScript + "\" in editor. Any modifications to it will immediately take effect.", MessageType.IMPORTANT);
		try {
			Desktop.getDesktop().edit(IO.getFile(currentLoadedScript));
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "failed to open file for editing: " + e.toString(), e);
			WurmHelpers.tellCinematics("Failed to open file for editing: " + e.toString(), MessageType.ERROR);
		}
	}
	
	private static void cmdLoadScript()
	{
		int id = BML_DIALOG_LOAD_SCRIPT;
		String title = "Select a Cinematics script to load";
		
		lastDir = IO.dir();
		lastExamplesDir = IO.dir("examples");
		
		String bml = "border{"
					+ "	center{"
					+ "		text{"
					+ "			type='bold';"
					+ "			text=\"" + title + "...\""
					+ "		}"
					+ "	};null;"
					+ "	"
					+ "	scroll{"
					+ "		vertical=\"true\";"
					+ "		horizontal=\"false\";"
					+ "		varray{"
					+ "			rescale=\"true\";"
					+ "			passthrough{"
					+ "				id=\"id\";"
					+ "				text=\"-12345\""
					+ "			}"
					+ "			label{"
					+ "				text=' '"
					+ "			};"
					+ "			label{"
					+ "				text=' '"
					+ "			};"
					+ "			label{"
					+ "				text='Filename: '"
					+ "			};"
					+ "			dropdown{"
					+ "				id='fileid';"
					+ "				options=' ," + Arrays.toString(lastDir).replace("[", "").replace("]", "")  + "'"
					+ "			}"
					+ "			"
					+ "			harray {"
					+ "				button{"
					+ "					text='Load Script';"
					+ "					id='submit'"
					+ "				}"
					+ "			}"

					+ "			varray{"
					+ "				label{"
					+ "					text=' '"
					+ "				};"
					+ "				label{"
					+ "					text=' '"
					+ "				};"
					+ "				label{"
					+ "					text='Or load one of the examples: '"
					+ "				};"
					+ "				dropdown{"
					+ "					id='examplefileid';"
					+ "					options=' ," + Arrays.toString(lastExamplesDir).replace("[", "").replace("]", "")  + "'"
					+ "				}"
					+ "			}"

					+ "			"
					+ "			harray {"
					+ "				button{"
					+ "					text='Load Example';"
					+ "					id='example_submit'"
					+ "				}"
					+ "			}"

					+ "			varray{"
					+ "				label{"
					+ "					text=' '"
					+ "				};"
					+ "				label{"
					+ "					text=' '"
					+ "				};"
					+ "				label{"
					+ "					text='Note: You can also use /loadscript <filename> in a chat-tab to load a script.'"
					+ "				};"
					+ "			}"
					+ "		}"
					+ "	};null;null;"
					+ "}";

		WurmHelpers.getWorld().getHud().showBml((short)id, title, 400, 400, 0, 0, true, true, 100f, 100f, 100f, bml);
	}
	
	static boolean isBmlFormOfInterest(String id)
	{
		// only have one right now...
		return Integer.parseInt(id) == BML_DIALOG_LOAD_SCRIPT;
	}
	
	static boolean actOnBmlResponse(HashMap<String, String> options)
	{
		if(options.containsKey("id") == false) {
			return false;
		}
		
		int id = Integer.parseInt(options.get("id"));

		if(id == BML_DIALOG_LOAD_SCRIPT) {
			// The first entry is an empty string in the drop down.
			if(options.get("fileid").equals("0") && options.get("examplefileid").equals("0")) {
				WurmHelpers.tellCinematics("You did not select a script to load.", MessageType.ERROR);
				return true;
			}
			
			String fileName;
			boolean res = false;
			
			if(options.get("examplefileid").equals("0")) {
				fileName = lastDir[Integer.parseInt(options.get("fileid")) - 1];
				res = cmdLoadScript(fileName);
			} else {
				fileName = lastExamplesDir[Integer.parseInt(options.get("examplefileid")) - 1];
				res = cmdLoadScript("examples" + File.separator + fileName);
			}

			if(res) {
				WurmHelpers.tellCinematics("Loaded script \"" + currentLoadedScript + "\".", MessageType.POSITIVE);
			} else {
				WurmHelpers.tellCinematics("No such script: \"" + options.get("filename") + "\".", MessageType.ERROR);
			}
			
			return true;
		}

		return false;
	}
	
	public static boolean doMenuAction(long target, long[] sources, PlayerAction action)
	{
		String scriptName = null;
		
		TweenPosition tp;
		TileInfo ti;
		
		switch(action.getId()) {
			case Actions.START_SCRIPT_ID :
				cmdStartScript();
				break;

			case Actions.STOP_SCRIPT_ID :
				cmdStopScript();
				break;
				
			case Actions.EDIT_SCRIPT_ID :
				cmdEditCurrentScript();
				break;
				
			case Actions.NEW_WAYPOINT_ID :
				cmdAddWaypoint(sources);
				break;

			case Actions.REMOVE_LAST_WAYPOINT_ID :
				if(currentSceneDesigner == null) {
					WurmHelpers.tellCinematics("You need to create a new script first.", MessageType.ERROR);
					return true;
				}

				tp = currentSceneDesigner.removeLastMoveWaypoint();
				currentSceneDesigner.save(true);

				if(tp != null) {
					WurmHelpers.tellCinematics("Removed waypoint " + tp.toString() + ".", MessageType.INFO);
				}
				break;
				
			case Actions.REMOVE_LAST_FOCUSPOINT_ID :
				if(currentSceneDesigner == null) {
					WurmHelpers.tellCinematics("You need to create a new script first.", MessageType.ERROR);
					return true;
				}

				tp = currentSceneDesigner.removeLastFocusWaypoint();
				currentSceneDesigner.save(true);

				if(tp != null) {
					WurmHelpers.tellCinematics("Removed focus point " + tp.toString() + ".", MessageType.INFO);
				}
				break;
				
			case Actions.NEW_SCRIPT_ID :
				scriptName = "Script " + System.currentTimeMillis();
				currentSceneDesigner = new SceneDesigner(scriptName);
				WurmHelpers.tellCinematics("The new script will be saved as: " + currentSceneDesigner.getFilename(), MessageType.IMPORTANT);
				break;

			case Actions.TEST_ID :
				WurmHelpers.tellCinematics("Doing some test... Whatever Friya might be up to.");
				WurmHelpers.getWorld().setDrumrollEffectRequested(true);
				someTest();
				break;

			case Actions.TEST2_ID :
				WurmHelpers.tellCinematics("Doing some test 2... Whatever Friya might be up to.");
				someTest2();
				break;

			case Actions.FOLLOW_ID :
				cmdFollow(getType(sources[0]));
				break;
			
			case Actions.NEW_FOCUS_ID :
				cmdAddFocusPoint(sources);
				break;
			
			case Actions.GET_OBJECT_ID :
				WurmHelpers.getHud().textMessage(":Event", 1.0f, 1.0f, 1.0f, "Object ID: " + sources[0]);
				break;
			
			case Actions.LOAD_SCRIPT_ID :
				cmdLoadScript();
				break;
			
			case Actions.HELP_ID:
				try {
					Desktop.getDesktop().browse(new URI("http://www.filterbubbles.com/wurm-unlimited/cinematics/"));
				} catch (IOException | URISyntaxException e) {
					WurmHelpers.tellCinematics("Could not open help URL. :(", MessageType.ERROR);
				}
				break;
			
			case Actions.NEW_TELEPORT_ID :
				if(currentSceneDesigner == null) {
					WurmHelpers.tellCinematics("You need to create a new script first.", MessageType.ERROR);
					return true;
				}
				
				if((ti = getTileInfo(sources[0])) == null) {
					WurmHelpers.tellCinematics("You cannot set a teleport to that. You need tiles, floors, etc.", MessageType.ERROR);
					return true;
				}

				currentSceneDesigner.setTeleportPoint(ti.getTileX(), ti.getTileY(), (int)(WurmHelpers.getWorld().getPlayerPosH() * 10f));
				currentSceneDesigner.save(true);
				WurmHelpers.tellCinematics("Set teleport point for script to " + ti.getTileX() + " " + ti.getTileY(), MessageType.INFO);
				break;

			case Actions.NEW_OBJECT_NOTE_ID :
				addObjectNote(sources[0]);
				break;

			case Actions.NEW_ORBIT_AROUND_ID :
				if(currentSceneDesigner == null) {
					WurmHelpers.tellCinematics("You need to create a new script first.", MessageType.ERROR);
					return true;
				}
				
				if((ti = getTileInfo(sources[0])) == null) {
					WurmHelpers.tellCinematics("You cannot set a teleport to that. You need tiles, floors, etc.", MessageType.ERROR);
					return true;
				}

				// This is just hard-coded for now.
				int radius = 3;
				int res = cmdAddOrbitAround(ti.getHeight(), ti.getTileX(), ti.getTileY(), radius);

				if(res > 0) {
					WurmHelpers.tellCinematics("Added " + res + " waypoints around " + ti.getTileX() + " " + ti.getTileY(), MessageType.INFO);
					currentSceneDesigner.save(true);
				} else {
					WurmHelpers.tellCinematics("Failed to create an orbit around that.", MessageType.ERROR);
				}
				break;

			default :
				WurmHelpers.tellCinematics("This is not yet fully implemented. Let Friya know!");
				break;
		}

		return true;
	}
	

	private static int cmdAddOrbitAround(float height, int tileX, int tileY, int radius) 
	{
		int ret = 0;
		PathArgs args = new PathArgs();
		args.setXYHR(height, (float)tileX, (float)tileY, radius);
		ArrayList<TweenPosition> tps = Paths.getInstance().getPath(MoveType.CIRCLE, args);
		for(TweenPosition tp : tps) {
			currentSceneDesigner.addMoveWaypoint(tp.getX(), tp.getY(), tp.getH());
			ret++;
		}
		
		return ret;
	}
	

	private static boolean addObjectNote(long id)
	{
		String name; 
		float x;
		float y;

		int t = getType(id);

		if(t == CounterTypes.COUNTER_TYPE_PLAYERS || t == CounterTypes.COUNTER_TYPE_CREATURES) {
			TweenPosition loc = WurmHelpers.getCreaturePosition(id);
			if(loc == null) {
				WurmHelpers.tellCinematics("Failed to find creature/player with id " + id + ".", MessageType.ERROR);
				return false;
			}
			
			name = "TODO: creature name";
			x = loc.getX();
			y = loc.getY();
			
		} else if(t == CounterTypes.COUNTER_TYPE_ITEMS || t == CounterTypes.COUNTER_TYPE_BANKS) {
			
			GroundItemCellRenderable item = WurmHelpers.getGroundItems().get(id);
			if(item == null) {
				WurmHelpers.tellCinematics("Failed to find item with id " + id + ".", MessageType.ERROR);
				return false;
			}

			name = item.getHoverName();
			x = item.getXPos();
			y = item.getYPos();

		} else {
			WurmHelpers.tellCinematics("Can only add notes for creatures, players and items.", MessageType.ERROR);
			return false;
		}

		currentSceneDesigner.addObjectNote(id, name, x, y);
		currentSceneDesigner.save(true);
		
		WurmHelpers.tellCinematics("Added note for object " + id + ".", MessageType.INFO);
		return true;
	}
	
	private static void someTest2()
	{
		EffectBuilder.clearParticleEffects();
	}
	
	private static void someTest()
	{
		EffectBuilder.loadEffect("friya");

		String[] data = new String[]{ 
				"cmd-name-prolly", 
				"friya"					// name of particleEffect as named in  graphics.jar/xmls/particle.xml
		};
		
        if (data.length < 2) {
            return;
        }
		
        final float distance = 4.0f;
        final float rot = (float)((WurmHelpers.getWorld().getPlayerRotX() + 90.0f) * Math.PI / 180.0);
        final float tilt = (float)((WurmHelpers.getWorld().getPlayerRotY() + 180.0f) * Math.PI / 180.0);
        
        Vector3f targetPos = new Vector3f(
        		(float)(distance * Math.cos(rot) * Math.cos(tilt)), 
        		(float)(distance * Math.sin(rot) * Math.cos(tilt)), 
        		(float)(distance * Math.sin(tilt))
        );
        
        targetPos = new Vector3f(
        		WurmHelpers.getWorld().getPlayerPosX(), 
        		WurmHelpers.getWorld().getPlayerPosY(), 
        		WurmHelpers.getWorld().getPlayerPosH()
        ).add(targetPos);
        
        // addGenericParticle(final String effectName, final float xPos, final float yPos, final float zPos, final byte layer)
        final CustomParticleEffect effTest = WurmHelpers.getWorld().getServerConnection().getServerConnectionListener().addGenericParticle(
        		data[1], 
        		targetPos.x, 
        		targetPos.y, 
        		Math.max(WurmHelpers.getWorld().getPlayerPosH(), targetPos.z + WurmHelpers.getPlayer().getCameraHeightOffSet()), 
        		(byte)WurmHelpers.getWorld().getPlayerLayer()
        );
        
        EffectBuilder.addParticleEffect(effTest);
	}
	
	// final long source, final long[] targets, final PlayerAction action
	static boolean cmdIntercept(Object[] args)
	{
		PlayerAction action = (PlayerAction)args[2];
		
		if(Actions.isOfInterest(action.getId())) {
			long target = (long)args[0];
			long[] sources = (long[])args[1];
			return Actions.doMenuAction(target, sources, action);
		}

/*
		// examine
		if(action.getId() == 1) {
			long[] sources = (long[])args[1];

			if(sources.length > 0) {
				WurmHelpers.getHud().textMessage(":Event", 1.0f, 1.0f, 1.0f, "ID: " + sources[0]);
//				WurmHelpers.attachTextTo(sources[0]);
			}
			// even though we intercepted, let the action continue -- it's examine
			return false;
		}
*/

		return false;
	}
	
	@SuppressWarnings("unused")
	private static void inGameEditorTest()
	{
		logger.info("editorTest()");
		
		// TODO: Make sure return and enter keys work in the bloody form!

		String bml = ""
				+ "	border{"
				+ "		center{"
				+ "			text{"
				+ "				type='bold';"
				+ "				text=\"y, modifyzors ur scr1pt h3h3\""
				+ "			}"
				+ "		};null;"
				+ "		scroll{"
				+ "			vertical=\"true\";"
				+ "			horizontal=\"false\";"
				+ "			varray{"
				+ "				rescale=\"true\";"
				+ "				passthrough{"
				+ "					id=\"id\";"
				+ "					text=\"-1\""
				+ "				}"
				+ "				input{"										// wurmonline/client/renderer/gui/WurmInputField.java
//				+ "					onenter=\"true\";"						// false prevents enter altogether, as does true
				+ "					id=\"answer\";"
				+ "					maxchars=\"100\";"						// this influences the size of the edit window
				+ "					maxlines=\"-1\";"						// ...as does this
				+ "					bgcolor=\"200,200,200\";"
				+ "					color=\"0,0,0\";"
				+ "					text=\"yeah, this does not work :(\""
				+ "				}"
				+ "				harray {"
				+ "					button{"
				+ "						text='Send';"
				+ "						id='submit'"
				+ "					}"
				+ "				}"
				+ "			}"
				+ "		};null;null;"
				+ "	}";
		
		WurmHelpers.getWorld().getHud().showBml((short)-1, "Conjuring camera instructions", 200, 200, 0, 0, true, true, 100f, 100f, 100f, bml);
	}
}
