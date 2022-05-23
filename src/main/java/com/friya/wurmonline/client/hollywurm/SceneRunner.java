package com.friya.wurmonline.client.hollywurm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector2f;

import com.friya.wurmonline.client.hollywurm.EventOnce.Unit;
import com.friya.wurmonline.client.hollywurm.TweenMeta.CoordinateType;
import com.wurmonline.client.game.PlayerDummy;
import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.game.PlayerPosition;
import com.wurmonline.client.renderer.gui.BenchmarkRenderer;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenEquation;
import aurelienribon.tweenengine.TweenPath;


/***
 * 
 * @author Friya
 */
public class SceneRunner // because Ceno is drawing blanks... again :(
{
	private static final Logger logger = Logger.getLogger(SceneRunner.class.getName());
    private static SceneRunner instance;
	
	private static boolean sceneRunning = false;
	private static long setupDoneAt = -1;

	private boolean wantHiddenHud = false;
	private float sceneSpeed = 1f;					// 1 = normal
	
	private String lastError = null;
	private boolean setupPaused = false;
	private List<String[]> remainingSetupActions = new ArrayList<String[]>();

	private Map<Integer, Long> shortCreatureIds = new HashMap<Integer, Long>();

	/**
	 * 
	 * @return
	 */
	public static SceneRunner getInstance()
	{
		if(instance == null) {
			instance = new SceneRunner();
		}

		return instance; 
	}
	
	SceneRunner()
	{
	}


	/***
	 * 
	 * @param scene
	 */
	void start(Scene scene)
	{
		if(Mod.EARLY_TESTING) {
			logger.info("EARLY_TESTING enabled -- we don't care if we load a lot of Wurm classes");
		}
		
		if(sceneRunning == true) {
			throw new CinematicBuilderException("There's already a scene running, wait for it to finish or stop it...");
		}
		
		cleanUp();

		lastError = null;		// ARGH! This was the fucker -- ONLY clear this one at start of a scene!
		
		continueRunning(scene);
	}
	

	/***
	 * 
	 * @param scene
	 */
	void continueRunning(Scene scene)
	{
		if(!run(scene)) {
			cleanUp();
			
			if(wantHiddenHud) {
				// Not sure if I want this here, but it was somewhat confusing to not get any feedback when failing to start a scene.
				ScriptRunner.showUI();
			}
			
			ScriptRunner.stopAudio();

			if(lastError != null) {
				throw new CinematicBuilderException("could not run scene; " + lastError);
			} else {
				throw new CinematicBuilderException("could not run scene; verify your script. That said, this should probably not happen at this point, let Friya know");
			}
		}
	}


	void stop()
	{
		WurmHelpers.tellCinematics("Stopping scene.", MessageType.IMPORTANT);
		cleanUp();
	}
	
	private void cleanUp()
	{
		sceneRunning = false;
		setupDoneAt = -1;

		if(ScriptRunner.getTweenManager() != null) {
			ScriptRunner.getTweenManager().killAll();
		}

		EventDispatcher.cancelAll();
		
		setupPaused = false;
		remainingSetupActions.clear();
		
		shortCreatureIds.clear();
	}
	
	boolean isRunning()
	{
		return sceneRunning;
	}
	
	boolean isSetupDone()
	{
		return setupDoneAt > 0 || setupDoneAt == -1;
	}
	
	long getSetupDoneAt()
	{
		return setupDoneAt;
	}
	
	void setRunning(boolean status)
	{
		sceneRunning = status;
	}


	/**
	 * This method can be called with a partial /setup-block. This happens if there was a 'wait' for instance,
	 * make sure to populate remainingSetupActions if it is a 'split' task.
	 * 
	 * This is a bad name, after its run all of /setup, it will continue with the rest. Sooo, not just setup.
	 * 
	 * @param scene
	 * @return
	 */
	private boolean run(Scene scene)
	{
		String[] act;
		boolean res;
		List<String[]> setupData;

		// If there is a 'wait' last in /setup, this list can be empty -- this is okay -- we will continue to 'after setup' stage.
		if(setupPaused == false && remainingSetupActions.size() == 0) {
			setupData = scene.getSetupData().getData();
		} else {
			// Grab a copy so we don't destroy original data.
			setupData = new ArrayList<>(remainingSetupActions);
		}

		setupPaused = false;
		setupDoneAt = 0;

		//
		// This only deals with commands in /setup.
		// For /camera commands, see runTween()
		//
		for(int i = 0; i < setupData.size(); i++) {
			act = setupData.get(i);
			res = false;
			
			logger.info("setup script: " + Arrays.toString(act));

			// There are no settings in /setup command (for now)
			if(act[0].equals("/setup")) {
				continue;
			}
			
			switch(act[0]) {
				case "tell" :
					switch(act[1]) {
						case "console" :
							res = doTellConsole(act[2]);
							break;
							
						case "server" : 
							res = doTellServer(act[2]); 
							break;
						
						case "cinematics" : 
							res = doTellCinematics(act[2]); 
							break;
							
						default : 
							lastError = "invalid tell"; 
							return false;
					}
					break;
				
				case "require" :
					switch(act[1]) {
						case "ability" :
							res = doRequireAbility(act[2]); 
							break;
							
						case "mod" : 
							res = doRequireMod(act[2]); 
							break;
							
						default : 
							lastError = "invalid require"; 
							return false;
					}
					break;
					
				case "option" :
					res = doSetupOption(act[1], act[2]); 
					break;

				case "wait" :
					setupPaused = true;
					remainingSetupActions = new ArrayList<String[]>(setupData.subList(i + 1, setupData.size()));
					long duration = ScriptBuilder.getDurationInMillis(act[1]);

					EventContinueRunning ev = new EventContinueRunning(duration, Unit.MILLISECONDS, scene, this);
					EventDispatcher.add(ev);

					if(Mod.EARLY_TESTING) {
						if(ev.cancel() == false) {
							logger.info("Failed to cancel event!");
						} else {
							logger.info("Cancelled continue event, doing IMMEDIATE call to invoke() in it instead...");
							ev.invoke();
						}
					}
					
					// "wait" should always return true, we will come back in here via the event above. 
					return true;

				case "audio" :
					switch(act.length) {
						case 2:
							if(act[1].equals("stop")) {
								res = doAudioStop();
							} else if(act[1].equals("play")) {
								res = doAudioResume();
							}
							break;
						case 3:
							res = doAudioPlay(act[2]);
							break;
						case 5:
							res = doAudioPlayFrom(act[2], act[4]);
							break;
						default :
							lastError = "invalid number of arguments to audio action (" + act[1] + ") -- adding new functionality, Friya?";
							return false;
					}
					break;

				default :
					lastError = "unhandled command in SceneRunner: " + act[0] + " -- TODO?";
					return false;
			}

			if(!res) {
				if(lastError == null) {
					lastError = "failed to " + act[0] + " " + act[1];
				}
				return false;
			}
		}

		if(runTweens(scene) == false) {
			if(lastError == null) {
				lastError = "failed to run tweens";
			}
			return false;
		}
		
		return true;
	}

	
	/***
	 * 
	 * @return
	 */
	private boolean runTweens(Scene scene)
	{
		List<ValidatedTween> tweensData = scene.getTweensData();
		List<Tween> sceneTweens = new ArrayList<Tween>();
		
		// These tweens are running in parallel (this scene)
		for(ValidatedTween tweenData : tweensData) {
			if(!createTween(tweenData, sceneTweens)) {
				lastError = (lastError == null ? "failed to run: " : lastError + ": ") + tweenData.toString();
				return false;
			}
		}
		
		TweenMeta meta = null;
		
		setupDoneAt = System.currentTimeMillis();

		// Big moment and pointless comment coming up: Start all tweens for this scene.
		for(Tween t : sceneTweens) {
			logger.info("Running tween, " + t.getUserData().toString() + ", type: " + TransformTypes.getTypeAsString(t.getType()));
			
			meta = (TweenMeta)t.getUserData();
			
			if(meta.coordinateType == CoordinateType.UNKNOWN) {
				lastError = "will not start a tween with unknown coordinate type. This is definitely a bug; silly Friya.";
				return false;
			}
			
			t.setCallback(new TweenCallback() {
				@Override
				public void onEvent(int type, BaseTween<?> source) {
					WurmHelpers.tellCinematics("        Finished " + ((TweenMeta)source.getUserData()).toString());
				}
			});

			if(Mod.EARLY_TESTING) {
				logger.info("EARLY_TESTING -- NOT starting above tween");
				continue;
			}

			t.start(ScriptRunner.getTweenManager());
			
			// If tween has delayed start, pause it. They will be resumed at time in tick(), ScriptRunner.
			if(meta.getStartTime() > 0) {
				t.pause();
			} else {
				WurmHelpers.tellCinematics("        Starting " + meta.toString());
			}
		}

		WurmHelpers.tellCinematics("Running scene \"" + scene.getName() + "\"...", MessageType.IMPORTANT);
		sceneRunning = true;
		
		return true;
	}
	
	private boolean createTween(ValidatedTween validTween, List<Tween> sceneTweens)
	{
		List<String[]> tweenData = validTween.getData();

		if(tweenData.isEmpty() || tweenData.get(0).length == 0 || (tweenData.get(0)[0].equals("/camera") == false && tweenData.get(0)[0].equals("/world") == false)) {
			lastError = "invalid command, empty block or missing argument";
			return false;
		}
		
		String[] tweenHeader = tweenData.get(0);
		
		boolean targetIsMoving = false;

		int numWaypointTargetArgs = 0;
		
		// The checks below should probably be in Builder:
		// 		x	Make sure there is always a 'target' after waypoints (and nowhere else)
		// 		x	Make sure there IS always a target
		boolean foundTarget = false;
		for(String[] tweenArg : tweenData) {
			if(tweenArg[0].equals("waypoint") || tweenArg[0].equals("target")) {
				if(foundTarget) {
					lastError = "a target must be after all waypoints";
					return false;
				}
				numWaypointTargetArgs = tweenArg.length - 1;	// -1 because we don't count the keyword ("waypoint" or "target")

				if(tweenArg[1].equals("creature")) {
					targetIsMoving = true;
				} else {
					if(targetIsMoving == true && tweenArg[1].equals("creature") == false) {
						lastError = "all or no waypoints and target must follow a creature";
						return false;
					}
				}
			}

			if(tweenArg[0].equals("target")) {
				foundTarget = true;
			}
		}
		
		if(!foundTarget) {
			lastError = "a /camera or /world must always have exactly one target (but can have several waypoints)";
			return false;
		}
		
		if(numWaypointTargetArgs < 1) {
			lastError = "an instruction had no arguments";
			return false;
		}

		int transformType = TransformTypes.UNKNOWN_TRANSFORM;
		
		if(tweenHeader[0].equals("/camera")) {
			transformType = TransformTypes.getCameraType(tweenHeader[1], numWaypointTargetArgs, targetIsMoving);

		} else if(tweenHeader[0].equals("/world")) {
			transformType = TransformTypes.getWorldChangeType(tweenHeader[1], numWaypointTargetArgs);
		}
		
		if(transformType == TransformTypes.UNKNOWN_TRANSFORM) {
			lastError = "unable to determine which transform this (" + tweenHeader[1] + ") is, perhaps wrong number of arguments to waypoint or target?";
			return false;
		}
		
		String commandType = tweenHeader[1];

		Tween tween;
		try {
			// Reminder: This is the duration of a single block within in a scene (a scene itself does not have any time limit ... at least not yet) 
			long duration = ScriptBuilder.getDurationInMillis(tweenHeader[3]);

			if(tweenHeader[0].equals("/camera")) {
				if(tweenHeader.length == 5 && tweenHeader[4].equals("reversed")) {
					tween = Tween.from(Mod.EARLY_TESTING ? PlayerDummy.getInstance() : WurmHelpers.getPlayer(), transformType, (float)duration);
				} else {
					tween = Tween.to(Mod.EARLY_TESTING ? PlayerDummy.getInstance() : WurmHelpers.getPlayer(), transformType, (float)duration);
				}
			} else if(tweenHeader[0].equals("/world")) {
				if(tweenHeader.length == 5 && tweenHeader[4].equals("reversed")) {
					tween = Tween.from(WurmHelpers.getWeather(), transformType, (float)duration);
				} else {
					tween = Tween.to(WurmHelpers.getWeather(), transformType, (float)duration);
				}
			
			} else {
				lastError = "unknown header: " + tweenHeader[0] + ". This is a bug, adding something new, Friya?";
				return false;
			}

			TweenMeta meta = new TweenMeta("command " + (sceneTweens.size() + 1), commandType, tweenHeader[0]);
			meta.setStartTime(validTween.getStartTime());
			tween.setUserData(meta);
			
			boolean setArgResult = false;
	
			// Start with second item, first is the 'header' (i.e. /camera ...)
			for(int i = 1; i < tweenData.size(); i++) {
				setArgResult = addTweenMethod(tween, commandType, transformType, numWaypointTargetArgs, tweenData.get(i));
				
				if(setArgResult == false) {
					if(lastError == null) {
						lastError = "unable to set arguments " + Arrays.toString(tweenData.get(i));
					}
					return false;
				}
			}
			
			// Throw the tweens into a collection. runTweens() will start them.
			sceneTweens.add(tween);
			
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.toString(), e);
			lastError = (lastError == null ? "" : lastError + ", additionally, ") + "exception while creating tween: " + e.toString();

			WurmHelpers.tellCinematics("Exception while adding tween, check client/console log for more information", MessageType.ERROR);
			return false;
		}

		return true;
	}
	
	private boolean addTweenMethod(Tween tween, String cameraType, int transformType, int numWaypointArgs, String[] args)
	{
		//
		// NOTE FOR FUTURE: When adding new methods to tweens, add to Builder.blocks under the entry
		// for "/camera" -- then there should be enough hints on how to go on adding the method...
		//
		
		switch(args[0]) {
			case "target" :
				// Yes, fall-through.
				
			case "waypoint" :
				if(setTweenCoordinate(tween, cameraType, transformType, numWaypointArgs, args) == false) {
					if(lastError == null) {
						lastError = "set tween arg: unknown error setting tween coordinate";
					}
					return false;
				}
				break;
			
			case "use" :
				if(setTweenEquation(tween, cameraType, transformType, numWaypointArgs, args) == false) {
					if(lastError == null) {
						lastError = "set tween use: unknown error";
					}
					return false;
				}
				break;
			
			case "wait" :
				if(setTweenWait(tween, cameraType, transformType, numWaypointArgs, args) == false) {
					if(lastError == null) {
						lastError = "set tween wait is invalid";
					}
					return false;
				}
				break;

			case "repeat" :
				if(setTweenRepeat(tween, cameraType, transformType, numWaypointArgs, args) == false) {
					if(lastError == null) {
						lastError = "set tween repeat is invalid";
					}
					return false;
				}
				break;
				
			case "repeatyoyo" :
				if(setTweenRepeatYoyo(tween, cameraType, transformType, numWaypointArgs, args) == false) {
					if(lastError == null) {
						lastError = "set tween repeatyoyo is invalid";
					}
					return false;
				}
				break;
				
			default :
				lastError = "add tween method: " + args[0] + " is not yet implemented, adding something new?";
				return false;
		}
		
		return true;
	}

	/**
	 * Called for both waypoint and target.
	 * 
	 * @param tween
	 * @param commandType
	 * @param transformType
	 * @param numWaypointArgs
	 * @param args
	 * @return
	 */
	private boolean setTweenCoordinate(Tween tween, String commandType, int transformType, int numWaypointArgs, String[] args)
	{
		PlayerObj player = null;

		if(tween.getTarget() instanceof PlayerObj) {
			player = ((PlayerObj)(tween.getTarget()));
		}
		
		String method = args[0];	// can be target, waypoint, ... (possibly more in the future)
		
		boolean targetIsMoving = false;
		TweenPosition targetCreaturePosition = null;
		long targetCreatureId = -10;
		int shortTargetCreatureId = -1; 
		CoordinateType coordType;


		TweenMeta meta = ((TweenMeta)tween.getUserData());
		
		float angle;
		float x, y;
		float h;
		TweenPosition tp;
		switch(commandType) {
			// -----------------------------------------------------------------------------------------------
			//
			// WORLD: various 
			//
			// -----------------------------------------------------------------------------------------------
			case "rain" :
			case "fog" :
			case "clouds" :
				coordType = CoordinateType.ABSOLUTE;

				meta.setCoordinateType(coordType);

				if(numWaypointArgs != 1) {
					lastError = "arguments for " + method + " in " + commandType + ": invalid number of args -- " + numWaypointArgs;
					return false;
				}

				if(method.equals("target")) {
					tween.target(Integer.parseInt(args[1]));

				} else if(method.equals("waypoint")) {
					tween.waypoint(Integer.parseInt(args[1]));

				} else {
					lastError = "set tween target for " + method + " in " + commandType + ": unknown method \"" + method + "\"";
					return false;
				}
				break;

			case "wind" :
				if(numWaypointArgs != 2) {
					lastError = "required number of arguments is 2 for \"" + method + "\" in command type \"" + commandType + "\"";
					return false;
				}

				coordType = CoordinateType.ABSOLUTE;
				meta.setCoordinateType(coordType);

				angle = ScriptBuilder.getAngleFromDirection(args[2]);
				
				if(method.equals("target")) {
					tween.target(Integer.parseInt(args[1]), angle);

				} else if(method.equals("waypoint")) {
					tween.waypoint(Integer.parseInt(args[1]), angle);

				} else {
					lastError = "set tween target for " + method + " in " + commandType + ": unknown method \"" + method + "\"";
					return false;
				}
				break;

			case "time" :
				if(numWaypointArgs != 2) {
					lastError = "required number of arguments is 2 for \"" + method + "\" in command type \"" + commandType + "\"";
					return false;
				}

				coordType = CoordinateType.ABSOLUTE;
				meta.setCoordinateType(coordType);

				// We want to "animate" in seconds. Would be nice if we could both get and set in seconds.

				long seconds = (Integer.parseInt(args[1]) * 60 * 60) + (Integer.parseInt(args[1]) * 60);
				
				if(!Mod.EARLY_TESTING) {
					// This gets the 'midnight' timestamp and adds it to the seconds we wish to animate to.
					seconds += WurmHelpers.getWorld().getWurmTime() - (WurmHelpers.getWorld().getWurmTime() % (24*60*60));

					logger.info("DEBUG: current time is: " + WurmHelpers.getWorld().getWurmTime() + " target seconds is: " + seconds);
				}
				
				if(method.equals("target")) {
					tween.target(seconds);

				} else if(method.equals("waypoint")) {
					tween.waypoint(seconds);

				} else {
					lastError = "set tween target for " + method + " in " + commandType + ": unknown method \"" + method + "\"";
					return false;
				}

				break;

			// -----------------------------------------------------------------------------------------------
			//
			// CAMERA: Follow creature/player camera 
			//
			// -----------------------------------------------------------------------------------------------
			case "follow" :
				targetIsMoving = true;
				targetCreatureId = Long.parseLong(args[2]);

				if((targetCreaturePosition = WurmHelpers.getCreaturePosition(targetCreatureId)) == null) {
					lastError = "creature with id " + targetCreatureId + " not found";
					return false;
				}
				
				shortTargetCreatureId = addShortCreatureId(targetCreatureId);

				tp = new TweenPosition();

				switch(numWaypointArgs) {
					case 4:
						// creature 1234567890 +3 -3 (no height)
						tp.setPos(args[3], args[4]);
						break;
						
					case 5:
						// creature 1234567890 +3 -3 +20
						tp.setPos(args[3], args[4], args[5]);
						break;
						
					default :
						lastError = "set tween coordinate for " + method + " in " + commandType + ": could not convert " + Arrays.toString(args) + " to a location relative to a creature/player";
						return false;
				}

				if(ScriptBuilder.isOffset(args[3]) && ScriptBuilder.isOffset(args[4])) {
					// This is the 'offset' case -- add target creature's coordinate to the offsets.
					// Let's be lazy and just convert them to absolute.
					coordType = CoordinateType.RELATIVE_NUMBER;
					
				} else {
					logger.info("TODO: Need to deal with string direction (to separate from offsets)");
					coordType = CoordinateType.ABSOLUTE;
				}
				meta.setCoordinateType(coordType);
				
				// We want to convert FROM tilex/y and height-in-dirts TO position and meters
				x = (tp.x * 4f) + 2f;
				y = (tp.y * 4f) + 2f;
				h = (tp.h / 10f);

				// creatureId will get truncated since it's passed in as a float
				if(method.equals("target")) {
					switch(numWaypointArgs) {
						case 4:
							tween.target(coordType.getValue(), shortTargetCreatureId, x, y);
							break;
	
						case 5:
							tween.target(coordType.getValue(), shortTargetCreatureId, x, y, h);
							break;
	
						default :
							lastError = "set tween coordinate for " + method + " in " + commandType + ": invalid number of args -- " + numWaypointArgs;
							return false;
					}
				} else if(method.equals("waypoint")) {
					switch(numWaypointArgs) {
						case 4:
							tween.waypoint(coordType.getValue(), shortTargetCreatureId, x, y);
							break;
	
						case 5:
							tween.waypoint(coordType.getValue(), shortTargetCreatureId, x, y, h);
							break;
	
						default :
							lastError = "set tween coordinate for " + method + " in " + commandType + ": invalid number of args -- " + numWaypointArgs;
							return false;
					}
				} else {
					lastError = "set tween coordinate for " + method + " in " + commandType + ": unknown method \"" + method + "\"";
					return false;
				}

				// We want to keep reference, not a copy. This however means that this may turn null at any
				// given point in a tween (or more likely: never move again).
				//logger.info("TODO: meta.followPoint should be initialized in getValue() -- not when creating the tween!");
				//meta.followPoint = new TweenPosition();
				break;

			// -----------------------------------------------------------------------------------------------
			//
			// CAMERA: Focus camera on coordinate
			//
			// -----------------------------------------------------------------------------------------------
			case "focus" :
				if(args[1].equals("creature")) {
					targetIsMoving = true;
					targetCreatureId = Long.parseLong(args[2]);
	
					if((targetCreaturePosition = WurmHelpers.getCreaturePosition(targetCreatureId)) == null) {
						lastError = "creature with id " + targetCreatureId + " not found";
						return false;
					}
					
					shortTargetCreatureId = addShortCreatureId(targetCreatureId);
				}
				
				if(ScriptBuilder.isRelativeFocusDirection(args[1])) {
					meta.setCoordinateType(CoordinateType.RELATIVE_STRING);
					return true;
				}
				
				// Yes, fall-through! We have dealt with the special "Focus" cases above.

			// -----------------------------------------------------------------------------------------------
			//
			// CAMERA: Move camera
			//
			// -----------------------------------------------------------------------------------------------
			case "move" :
				tp = new TweenPosition();
				switch(numWaypointArgs) {
					case 1:
						tp.setPos(args[1]);
						break;
						
					case 2:
						if(targetIsMoving) {
							tp.setPos(targetCreaturePosition.getTileX(), targetCreaturePosition.getTileY(), targetCreaturePosition.getH() / 10);
						} else {
							tp.setPos(args[1], args[2]);
						}
						break;
						
					case 3:
						tp.setPos(args[1], args[2], args[3]);
						break;
						
					case 4 :
						// e.g. waypoint 5 north 20 up  (this is always relative to current position)

						angle = 0;
						float distance = 0;
						int heightChange = 0;

						// need to figure out which comes first, the x/y or the up/down
						if(args[2].equals("up") || args[2].equals("down") || args[2].equals("u") || args[2].equals("d")) {
							if(args[2].startsWith("u")) {
								heightChange += Integer.parseInt(args[1]);
							} else {
								heightChange -= Integer.parseInt(args[1]);
							}
							
							angle = ScriptBuilder.getAngleFromDirection(args[4]);
							distance = Integer.parseInt(args[3]);

						} else {
							if(args[4].startsWith("u")) {
								heightChange += Integer.parseInt(args[3]);
							} else {
								heightChange -= Integer.parseInt(args[3]);
							}

							angle = ScriptBuilder.getAngleFromDirection(args[2]);
							distance = Integer.parseInt(args[1]);
						}

						Vector2f loc = ScriptBuilder.getPointFromDistAngle(distance, angle);
/*						
						logger.info("distance/dir: " + args[1] + " " + args[2]);
						logger.info("angle: " + angle);
						logger.info("string dir to point: " + loc.getX() + " " + loc.getY());
						logger.info("string dir to point: " + ((int)Math.round(loc.getX())) + " " + ((int)Math.round(loc.getY())));
						logger.info("string to height change: " + Math.round(heightChange));
*/						

						// HACK: This is just to fool the check for relative position after this ugly-ass case statement.
						args[1] = "-0";
						
						tp.setPos(loc.getX(), loc.getY(), heightChange);
						break;
						
					default :
						lastError = "set tween coordinate for " + method + " in " + commandType + ": could not convert " + Arrays.toString(args) + " to coordinate";
						return false;
				}

				// NOTE: will never go in here if target is a mover since args[1] is not an offset (it's the string "creature")
				if(ScriptBuilder.isOffset(args[1])) {
					// This is the 'offset' case -- add players' coordinate to the offsets
					// offset coordinates -- let's be lazy and just convert them to absolute

					logger.info("relative coord: " + tp.toString());
					
					coordType = CoordinateType.RELATIVE_NUMBER;

					if(Mod.EARLY_TESTING) {
						// skip this...
						logger.info("EARLY_TESTING says, skip adding offset... need to be tested 'live'");
					} else {
						// was: tp.add(player.getPos(), true);
						//tp.add(player.getPos());
						
						PlayerPosition orgPos = player.getPos();
						
						tp.x += orgPos.getTileX();
						tp.y += orgPos.getTileY();
						tp.h += (int)(orgPos.getH() * 10);			// meters to dirt
					}
				} else {
					// this is absolute coordinates... I don't need to do anything, I think?
					coordType = CoordinateType.ABSOLUTE;
				}

				meta.setCoordinateType(coordType);

				// We want to convert FROM tilex/y and height-in-dirts TO position and meters
				x = (tp.x * 4f) + 2f;
				y = (tp.y * 4f) + 2f;
				h = (tp.h / 10f);

				// Positions are (tile * 4) + 2 for x and y (positions are more precise than a tile in the game)
				// Height should be divided by 10 to get it in 'dirts' instead of meters.
				if(method.equals("target")) {
					switch(numWaypointArgs) {
						case 1:
							tween.target(x);
							break;
	
						case 2:
							if(targetIsMoving) {
								tween.target(coordType.getValue(), shortTargetCreatureId, x, y, h);
							} else {
								tween.target(x, y);
							}
							break;
	
						case 3:
						case 4:		// covers 10 northwest 20 up
							tween.target(x, y, h);
							break;
	
						default :
							lastError = "set tween coordinate for " + method + " in " + commandType + ": invalid number of args -- " + numWaypointArgs;
							return false;
					}
				} else if(method.equals("waypoint")) {
					switch(numWaypointArgs) {
						case 1:
							tween.waypoint(x);
							break;
	
						case 2:
							tween.waypoint(x, y);
							break;
	
						case 3:
						case 4:		// covers 10 northwest 20 up
							tween.waypoint(x, y, h);
							break;
	
						default :
							lastError = "set tween coordinate for " + method + " in " + commandType + ": invalid number of args -- " + numWaypointArgs;
							return false;
					}
				} else {
					lastError = "set tween coordinate for " + method + " in " + commandType + ": unknown method \"" + method + "\"";
					return false;
				}
				
				//logger.info("final pos: " + x + " " + y + " " + h);
				break;

			// -----------------------------------------------------------------------------------------------
			//
			// CAMERA: Rotate camera to angle(s)
			//
			// -----------------------------------------------------------------------------------------------
			case "rotate" :
				tp = new TweenPosition();
				switch(numWaypointArgs) {
					case 1:
						tp.setRot(args[1]);
						break;
						
					case 2:
						tp.setRot(args[1], args[2]);
						break;
						
					default :
						lastError = "set tween rotation for " + method + " in " + commandType + ": could not convert " + Arrays.toString(args) + " to angle(s)";
						return false;
				}

				if(ScriptBuilder.isOffset(args[1])) {
					// This is the 'offset' case -- add players' coordinate to the offsets
					// offset coordinates -- let's be lazy and just convert them to absolute
					meta.setCoordinateType(CoordinateType.RELATIVE_NUMBER);
					if(Mod.EARLY_TESTING) {
						// skip this...
						logger.info("EARLY_TESTING says, skip adding offset... need to be tested 'live'");
					} else {
						tp.add(PlayerAccessor.getInstance().getXrot(player), PlayerAccessor.getInstance().getYrot(player));
					}
				} else {
					// this is absolute coordinates... I don't need to do anything, I think?
					meta.setCoordinateType(CoordinateType.ABSOLUTE);
				}

				if(method.equals("target")) {
					switch(numWaypointArgs) {
						case 1:
							tween.target((tp.xRot));
							break;
	
						case 2:
							tween.target((tp.xRot), (tp.yRot));
							break;
	
						default :
							lastError = "set tween rotation for " + method + " in " + commandType + ": invalid number of args -- " + numWaypointArgs;
							return false;
					}
				} else if(method.equals("waypoint")) {
					switch(numWaypointArgs) {
						case 1:
							tween.waypoint((tp.xRot));
							break;
	
						case 2:
							tween.waypoint((tp.xRot), (tp.yRot));
							break;

						default :
							lastError = "set tween rotation for " + method + " in " + commandType + ": invalid number of args -- " + numWaypointArgs;
							return false;
					}
				} else {
					lastError = "set tween rotation for " + method + " in " + commandType + ": unknown method \"" + method + "\"";
					return false;
				}
				
				break;
			
			default :
				lastError = "command type \"" + commandType + "\" is not yet implemented, adding something new?";
				return false;
		}
		
		return true;
	}

	long getShortCreatureId(int shortId)
	{
		if(shortCreatureIds.containsKey(shortId) == false) {
			return -10; 
		}
		
		return shortCreatureIds.get(shortId);
	}
	
	private int addShortCreatureId(long longId)
	{
		int ret = shortCreatureIds.size();
		shortCreatureIds.put(ret, longId);
		return ret;
	}
	
	/**
	 * 
	 * Deal with 'use' in /camera (or ease() / path() on a tween)
	 * 
	 * @param tween
	 * @param cameraType
	 * @param transformType
	 * @param numWaypointArgs
	 * @param args
	 * @return
	 */
	private boolean setTweenEquation(Tween tween, String cameraType, int transformType, int numWaypointArgs, String[] args)
	{
		String equationType = args[1];
		
		switch(equationType) {
			case "easing" :
				// fall-through -- same thing
			case "smoothing" :
				String endsType;
				
				if(args.length < 4) {
					logger.info("NOTE: adding default ends-type, in-and-out");
					endsType = "inout";
				} else {
					endsType = args[3];
				}
				
				TweenEquation eq = ScriptBuilder.getTweenEquation(args[2], endsType);
				if(eq == null) {
					lastError = "no such smoothing function " + args[2] + "." + endsType;
					return false;
				}
				
				tween.ease(eq);
				break;

			case "pathing" :
				TweenPath tp = ScriptBuilder.getTweenPath(args[2]);
				
				if(tp == null) {
					lastError = "no such pathing function " + args[2];
					return false;
				}

				tween.path(tp);
				break;

			default :
				lastError = "unknown equation type: " + equationType;
				return false;
		}
		
		
		
		return true;
	}

	private boolean setTweenWait(Tween tween, String cameraType, int transformType, int numWaypointArgs, String[] args)
	{
		long delayDuration = ScriptBuilder.getDurationInMillis(args[1]);

		tween.delay(delayDuration);
		return true;
	}


	private boolean setTweenRepeat(Tween tween, String cameraType, int transformType, int numWaypointArgs, String[] args)
	{
		long duration = 0;
		
		if(args.length == 3) {
			duration = ScriptBuilder.getDurationInMillis(args[2]);
		}
		
		tween.repeat(Integer.parseInt(args[1]), duration);
		return true;
	}
	
	private boolean setTweenRepeatYoyo(Tween tween, String cameraType, int transformType, int numWaypointArgs, String[] args)
	{
		long duration = 0;
		
		if(args.length == 3) {
			duration = ScriptBuilder.getDurationInMillis(args[2]);
		}
		
		tween.repeatYoyo(Integer.parseInt(args[1]), duration);
		return true;
	}
	
	/***
	 * 
	 * @param s
	 * @return
	 */
	private boolean doRequireAbility(String s)
	{
		if(Mod.EARLY_TESTING) {
			return true;
		}

		switch(s) {
			case "flight" :
				if(WurmHelpers.canFly() == false) {
					lastError = "requiring flight failed, it is not available on your character, try with a GM or remove the requirement";
					return false;
				}
				
				return true;

			default :
				lastError = "unknown ability: " + s;
				return false;
		}
	}
	
	private boolean doSetupOption(String opt, String val)
	{
		opt = opt.toLowerCase();
		
		switch(opt) {
			case "hud" :
				switch(val) {
					case "on" :
						wantHiddenHud = false;
						break;
						
					case "off" :
						wantHiddenHud = true;
						ScriptRunner.hideUI();
						break;
						
					default :
						lastError = "the " + opt + " option cannot be set to " + val;
						return false;
				}
				break;

			case "speed" :
				// 100% is default, 50% half speed, 200% double speed
				sceneSpeed = ScriptBuilder.getMultiplierFromPerCent(val);
				break;

			case "fade" :
				switch(val) {
					case "on" :
						BenchmarkRenderer.fadeToBlack();
						break;
					case "off" :
						BenchmarkRenderer.fadeToShow();
						break;
					default :
						lastError = "the " + opt + " option cannot be set to " + val;
						return false;
				}
				break;

			case "benchmarking" :
				switch(val) {
					case "on" :
						ScriptRunner.setBenchmarking(true);
						break;
					case "off" :
						ScriptRunner.setBenchmarking(false);
						break;
					default :
						lastError = "the " + opt + " option cannot be set to " + val;
						return false;
				}
				break;

			default :
				lastError = "no such option: " + opt;
				return false;
		}

		return true;
	}

	/***
	 * 
	 * @param s
	 * @return
	 */
	private boolean doRequireMod(String s)
	{
		if(Mod.EARLY_TESTING) {
			return true;
		}

		if(WurmHelpers.classExists(s)) {
			return true;
		} else {
			lastError = "required mod \"" + s + "\" was not found, you can solve this by removing the requirement";
			return false;
		}
	}

	/**
	 * Send a command to the client using the console. These commands will not hit server (by default).
	 * 
	 * Used for e.g. "flight" or "timelock 17"
	 * 
	 * @param msg
	 * @return
	 */
	private boolean doTellConsole(String msg)
	{
		if(Mod.EARLY_TESTING) {
			return true;
		}

		// XXX: This is a hack to not toggle it off!
		if(msg.equals("flight") && WurmHelpers.getPlayer().isFlying()) {
			return true;
		}

		//ModClient.getClientInstance().getConsole().handleInput(msg, false);
		WurmHelpers.tellConsole(msg);
		return true;
	}
	
	/**
	 * Send a message using "Local" to the server. It's up to the server to echo the message or not.
	 * 
	 * Used for e.g. #goto x y
	 * 
	 * @param msg
	 * @return
	 */
	private boolean doTellServer(String msg)
	{
		if(Mod.EARLY_TESTING) {
			return true;
		}

		//world.getServerConnection().sendmessage5(":Local", msg);
		WurmHelpers.tellServer(msg);
		return true;
	}
	
	/**
	 * Send message to the "Cinematics" tab, this will never hit the server.
	 * 
	 * Used for printing informational messages to the user.
	 * 
	 * @param msg
	 * @return
	 */
	private boolean doTellCinematics(String msg)
	{
		if(Mod.EARLY_TESTING) {
			return true;
		}

		//world.getHud().textMessage(":" + "Cinematics", 128, 200, 200, msg);
		WurmHelpers.tellCinematics(msg);
		return true;
	}
	

	private boolean doAudioStop()
	{
		return ScriptRunner.stopAudio();
	}

	private boolean doAudioResume()
	{
		lastError = "audio resume is not yet implemented";
		return false;
	}
	
	private boolean doAudioPlay(String fileName)
	{
		// Each scene can clear this field, but the control of stopping the audio automatically is left to ScriptRunner.
		// Explicit stopping can be done by a scene, however.
		return ScriptRunner.startAudio(fileName, 0);
	}
	
	private boolean doAudioPlayFrom(String fileName, String offset)
	{
		int n = (int)ScriptBuilder.getDurationInMillis(offset);
		return ScriptRunner.startAudio(fileName, n);
	}

	boolean wantHiddenHud()
	{
		return wantHiddenHud;
	}

	float getSceneSpeed()
	{
		return sceneSpeed;
	}
}
