package com.friya.wurmonline.client.hollywurm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.lwjgl.util.vector.Vector2f;

import aurelienribon.tweenengine.TweenEquation;
import aurelienribon.tweenengine.TweenPath;
import aurelienribon.tweenengine.TweenPaths;
import aurelienribon.tweenengine.TweenUtils;

public class ScriptBuilder
{
    private static Logger logger = Logger.getLogger(ScriptBuilder.class.getName());
    private static final String[] easingEquations = new String[]{ "back", "bounce", "circ", "cubic", "elastic", "expo", "linear", "quad", "quart", "quint", "sine" };
    private static final String[] easingEquationsOptions = new String[]{ "in", "out", "inout" };
    private static final String[] pathingEquations = new String[]{ "catmull-rom", "linear" };
    private static final String[] requireFunctions = new String[]{ "ability", "mod" };
    private static final String[] tellFunctions = new String[]{ "server", "console", "cinematics" };
    private static final String[] useFunctions = new String[]{ "smoothing", "easing", "pathing" };
    private static final String[] delayUnits = new String[]{ "ms", "s", "m", "h", "d" };
    private static final String[] cameraFunctions = new String[]{ "focus", "rotate", "move", "follow" };
    private static final String[] worldFunctions = new String[]{ "clouds", "rain", "fog", "time", "wind" };
    private static final String[] audioFunctions = new String[]{ "play", "stop" };
    private static final String[] relativeFocusDirections = new String[]{ "ahead" }; // TODO:  , "right", "behind", "left", degrees <0-360>
    private static final String[] dirStrings = new String[] {
		"north", "northnortheast", "northeast", "eastnortheast", "east", "eastsoutheast", "southeast", "southsoutheast",
		"south", "southsouthwest", "southwest", "westsouthwest", "west", "westnorthwest", "northwest", "northnorthwest",
		"north",
		
		"up", "down"
	};
    private static final String[] dirStringsAbbr = new String[] {
		"n", "nne", "ne", "ene", "e", "ese", "se", "sse", "s", "ssw", "sw", "wsw", "w", "wnw", "nw", "nnw",	
		
		"n",
		
		"u", "d"
	};

    private static final Map<String, String[]> blocks = new HashMap<String, String[]>() {
		private static final long serialVersionUID = 4874078113349013795L;
		{
			put("/at", new String[]{
			});

			put("/setup", new String[]{
				"require", "tell", "wait", "option", "audio"
			});
			
			put("/camera", new String[]{
				"waypoint", "target", "use", "wait", "repeat", "repeatyoyo", "poll"
			});

			put("/world", new String[]{
				"waypoint", "target", "use", "wait", "repeat", "repeatyoyo"
			});
		}
	};

	private static final Map<String, String[]> optionNames = new HashMap<String, String[]>() {
		private static final long serialVersionUID = 4874078113349013795L;
		{
			put("hud", new String[]{
				"on", "off"
			});
			put("fade", new String[]{
				"on", "off"
			});
			put("benchmarking", new String[]{
				"on", "off"
			});
			put("speed", new String[]{
				"$$d%"
			});
		}
	};
	
	private long currentTweenStartTime = 0;
    private String lastError = null;

	
	public ScriptBuilder()
	{
		// circ: https://greensock.com/docs/Easing/Circ
		// catmull-rom: https://en.wikipedia.org/wiki/Centripetal_Catmull%E2%80%93Rom_spline
	}
	

	ArrayList<Scene> parse(String fileName, String script)
	{
		ArrayList<Scene> scenes = new ArrayList<Scene>();
		Scene currentScene = null;
		
		currentScene = null;
		
		String[] lines = script.split("\n");
		
		String currentBlock = null;
		int currentBlockLine = -1;
		
		String instr[] = null;
		
		// This is a line based parser. Full block validation happens afterwards.
		for(int i = 0; i < lines.length; i++) {
			lastError = null;
			
			instr = tokenize(lines[i].trim());
			if(instr.length == 0) {
				continue;
			}
			
			//logger.info("Line " + (i + 1) + ": " + Arrays.toString(instr));
			
			//
			// Make sure we have a Scene, and create new scenes as we run into them.
			//

			if(currentScene == null && !isSceneStart(instr)) {
				continue;
			}

			if(isSceneStart(instr)) {
				currentTweenStartTime = 0;
				currentScene = new Scene(instr);
				continue;
			}

			if(isSceneEnd(instr)) {
				scenes.add(currentScene);
				
				// Debug.
				//logger.info("Scene.toString():\n" + currentScene.toString());

				currentScene = null;
				continue;
			}

			//
			// We have a Scene object, we are worrying about its contents (blocks/commands), aka camera/setup from here on.
			//
			
			if(isValidBlockStart(currentBlock, instr) == false) {
				// Not a valid block instruction (setup, camera, ...)
				throw new CinematicBuilderException((i + 1), (lastError == null ? instr[0] + " is not a valid command to start a block" : lastError), fileName);

			} else if(currentBlock == null || instr[0].startsWith("/")) {
				// Valid block instruction (this can be from null or a block switch)
				currentBlock = instr[0];
				currentBlockLine = i + 1;
				
				if(isValidCamera(instr)) {
					// add to scene (1/3)
					if(currentScene.addTweenCommand(instr, currentTweenStartTime) == false) {
						throw new CinematicBuilderException((i + 1), "failed to add camera to scene, you or Friya might know why", fileName);
					}
					
				} else if(isValidWorld(instr)) {
					// add to scene (2/3)
					if(currentScene.addTweenCommand(instr, currentTweenStartTime) == false) {
						throw new CinematicBuilderException((i + 1), "failed to add a world command to scene, you or Friya might know why", fileName);
					}
					
				} else if(isValidSetup(instr)) {
					// add to scene (2/3)
					if(currentScene.addSetupCommand(instr) == false) {
						throw new CinematicBuilderException((i + 1), "failed to add setup to scene, you or Friya might know why", fileName);
					}
				
				} else if(isValidAt(instr)) {
					// This specifies what timestamp the blocks after this command should start at. Makes it a lot easier to make
					// a bit bigger scenes without heaps of 'wait'.
					currentTweenStartTime = getDurationInMillis(instr[1]);

				} else {
					throw new CinematicBuilderException((i + 1), (lastError == null ? "failed to validate \"" + String.join(" ", instr[0]) + "\"" : lastError), fileName);
				}
				
				if(lastError != null) {
					throw new CinematicBuilderException((i + 1), lastError, fileName);
				}
				continue;
			}

			//
			// We have a block of a scene, we are worrying about the arguments (waypoints etc) from here on.
			//

			if(contains(blocks.get(currentBlock), instr[0]) == false) {
				// Invalid argument (i.e. waypoint, target...)
				throw new CinematicBuilderException((i + 1), instr[0] + " is not a valid argument for " + currentBlock, fileName);
			}
			
			//
			// We know we a valid context (scene/block), we can now start with the RELEVANT bits, the settings of each argument. Phew.
			//
			
			if(validate(currentScene, currentBlock, instr)) {
				
				// add to scene (3/3)
				if(currentScene.addArg(currentBlock, instr) == false) {
					if(currentScene.getLastError() == null) {
						throw new CinematicBuilderException((i + 1), "failed to add argument to scene, weird. You or Friya might know why", fileName);
					} else {
						throw new CinematicBuilderException((i + 1), currentScene.getLastError() + " (block starts at line " + currentBlockLine + ")", fileName);
					}
				}
				
			} else {
				throw new CinematicBuilderException((i + 1), (lastError == null ? "there is some issue on this line, god (or maybe Friya) knows what it is" : lastError), fileName);
			}

		}

		if(currentScene != null) {
			throw new CinematicBuilderException(0, "a scene must end with \"/scene end\"", fileName);
		}

		if(scenes.size() == 0) {
			throw new CinematicBuilderException(0, "a script must have at least one scene", fileName);
		}

		return scenes;
	}

	private boolean isSceneStart(String[] ins)
	{
		return ins.length > 1
				&& ins[0].equals("/scene")
				&& ins[1].equals("start");
	}
	
	private boolean isSceneEnd(String[] ins)
	{
		return ins.length > 1
				&& ins[0].equals("/scene")
				&& ins[1].equals("end");
	}
	
	
	private boolean isValidBlockStart(String currentBlock, String[] instr)
	{
		if((currentBlock == null || instr[0].startsWith("/")) && blocks.containsKey(instr[0]) == false ) {
			return false;
		}
		
		return true;
	}

	private boolean isValidSetup(String[] args)
	{
		return args[0].equals("/setup")
				&& requireNumArgs(args, 1, 1);
	}

	private boolean isValidCamera(String[] args)
	{
		return args[0].equals("/camera")
				&& requireNumArgs(args, 4, 5)
				&& requireArgumentInList(args[1], cameraFunctions)
				&& requireArgumentInList(args[2], new String[]{ "for" })
				&& requireDelay(args[3], 0, Integer.MAX_VALUE)
				&& (args.length == 4 || requireArgumentInList(args[4], new String[]{ "reversed" }))
		;
	}

	private boolean isValidWorld(String[] args)
	{
		return args[0].equals("/world")
				&& requireNumArgs(args, 4, 5)
				&& requireArgumentInList(args[1], worldFunctions)
				&& requireArgumentInList(args[2], new String[]{ "for" })
				&& requireDelay(args[3], 0, Integer.MAX_VALUE)
				&& (args.length == 4 || requireArgumentInList(args[4], new String[]{ "reversed" }))
		;
	}

	private boolean isValidAt(String[] args)
	{
		return requireNumArgs(args, 2, 2)
				&& requireDelay(args[1], 0, Integer.MAX_VALUE);
	}
	

	private boolean isValidWaypoint(String[] args)
	{
		// longest being: waypoint creature 1234567890 +3 -3 +20
		return requireNumArgs(args, 2, 6)
				&& requireCoordinatesOrOffsets(args);
	}

	private boolean isValidTarget(String[] args)
	{
		return requireNumArgs(args, 2, 6)
				&& requireCoordinatesOrOffsets(args);
	}

	private boolean isValidRequire(String[] args)
	{
		return requireNumArgs(args, 3, 3)
				&& requireArgumentInList(args[1], requireFunctions);
	}

	private boolean isValidTell(String[] args)
	{
		return requireNumArgs(args, 3, 3)
				&& requireArgumentInList(args[1], tellFunctions);
	}
	
	private boolean isValidUse(String[] args)
	{
		if(requireNumArgs(args, 3, 4) == false || requireArgumentInList(args[1], useFunctions) == false) {
			return false;
		}
		
		if((args[1].equals("smoothing") || args[1].equals("easing")) && requireArgumentInList(args[2], easingEquations) == false) {
			return false;
		} else if(args[1].equals("pathing") && requireArgumentInList(args[2], pathingEquations) == false) {
			return false;
		}
		
		if(args[1].equals("smoothing") || args[1].equals("easing")) {
			String endingType = (args.length == 4 ? args[3].toLowerCase() : "in");
			if(getTweenEquation(args[2], endingType) == null) {
				lastError = "\"" + endingType + "\" is not valid for for smoothing \"" + args[2] + "\"";
				return false;
			}
		}
		
		// Note: This is a bit tricky, I should probably 'rephrase' the if's above. But yeah, make sure *all*
		//       posible negative cases are covered or it will simply boil down to true and probably break
		//       things elsewhere.
		return true;
	}
	
	private boolean isValidWait(String[] args)
	{
		return requireNumArgs(args, 2, 2)
				&& requireDelay(args[1], 0, Integer.MAX_VALUE);
	}
	
	private boolean isValidRepeat(String[] args)
	{
		return requireNumArgs(args, 2, 3)
				&& isNumeric(args[1])
				&& (args.length == 2 || requireDelay(args[2], 0, Integer.MAX_VALUE));
	}

	private boolean isValidRepeatYoyo(String[] args)
	{
		return requireNumArgs(args, 2, 3)
				&& isNumeric(args[1])
				&& (args.length == 2 || requireDelay(args[2], 0, Integer.MAX_VALUE));
	}

	private boolean isValidOption(String[] args)
	{
		return requireNumArgs(args, 3, 3)
				&& requireKeyInMap(args[1], optionNames)
				&& requireArgumentInList(args[2], optionNames.get(args[1]));
	}

	private boolean isValidPoll(String[] args)
	{
		return requireNumArgs(args, 3, 3)
				&& requireArgumentInList(args[1], new String[]{ "interval", "once" })
				&& requireDelay(args[2], 0, Integer.MAX_VALUE);
	}
	
	private boolean isValidAudio(String[] args)
	{
		if(requireNumArgs(args, 2, 5) == false) {
			return false;
		}

		if(args.length < 4) {
			// audio play | audio stop | audio play "song.mp3"
			return requireArgumentInList(args[1], audioFunctions);
		} else if(args.length == 5) {
			// audio play "Tagirijus_-_Ascent.mp3" from 81s
			return requireArgumentInList(args[1], audioFunctions)
					&& requireFile("audio" + File.separator + args[2])
					&& requireArgumentInList(args[3], new String[]{ "from" })
					&& requireDelay(args[4], 0, Integer.MAX_VALUE);
		}
		
		return false;
	}

	
	private boolean requireFile(String fileName)
	{
		if(IO.exists(fileName) == false) {
			lastError = "file does not exist, " + IO.getCinematicsDir() + File.separator + fileName;
			return false;
		}
		return true;
	}

	private boolean requireKeyInMap(String keyName, Map<String, String[]> map)
	{
		if(map.containsKey(keyName) == false) {
			lastError = keyName + " is not valid here, should be one of " + Arrays.toString(map.keySet().toArray());
			return false;
		}
		
		return true;
	}
	
	private boolean requireArgumentInList(String arg, String[] args)
	{
		// TODO: Should just go with making regex definition valid here! I made it this way to be able to provide good errors, but meh...
		if(args.length == 1 && args[0].length() >= 3 && args[0].startsWith("$$")) {
			//logger.info("TODO HERE requireArgumentInList()");

			// verify e.g. $$d%
			String wildcard = args[0].substring(2, 3);
			String requiredRemainder = args[0].substring(3);
			//logger.info("wildcard: " + wildcard);
			//logger.info("requiredRemainder: " + requiredRemainder);
			
			String actualRemainder = arg.substring(arg.length() - requiredRemainder.length());
			//logger.info("actualRemainder: " + actualRemainder);
			
			if(actualRemainder.equals(requiredRemainder) == false) {
				lastError = arg + " must end with " + requiredRemainder;
				return false;
			}
			
			String actualArgument = arg.substring(0, arg.length() - actualRemainder.length());
			//logger.info("actualArgument: " + actualArgument);
			
			switch(wildcard) {
				case "d" :
					if(isNumeric(actualArgument) == false) {
						lastError = actualArgument + " must be a number";
						return false;
					}

					return true;

				default :
					lastError = "Invalid wildcard for required argument. This is a bug. Friya is adding something?";
					return false;
			}

//			lastError = "Unknown error checking valid arguments for \"" + arg + "\"";
//			return false;
		}
		
		if(!contains(args, arg)) {
			lastError = "argument \"" + arg + "\" should be one of: " + Arrays.toString(args);
			return false;
		}
		
		return true;
	}

	private boolean requireCoordinatesOrOffsets(String[] args)
	{
		if(args[1].equals("creature")) {
			// waypoint creature 1234567890 ...from e.g... waypoint creature 1234567890 +3 -3 +20
			String[] preArgs = Arrays.copyOfRange(args, 0, 3);

			// 1234567890 +3 -3 +20 ...from e.g... waypoint creature 1234567890 +3 -3 +20
			args = Arrays.copyOfRange(args, 2, args.length);

			if(isNumeric(preArgs[2]) == false) {
				lastError = "creature must be identified by a numeric id, you should be able to get a creature's id by examining them";
				return false;
			}
		}
		
		if(isRelativeFocusDirection(args[1])) {
			return true;
		}

		// deals with: waypoint 10 north 80 up
		if(args.length > 2
			&& (requireArgumentInList(args[2].replace("-", ""), dirStrings) 
			||  requireArgumentInList(args[2].replace("-", ""), dirStringsAbbr))
			) {

			if(isNumeric(args[1]) == false) {
				lastError = "the amount (" + args[1] + ") of relative movement is not correct, it needs to be a number";
				return false;
			}

			if(args.length > 4) {
				if(isNumeric(args[3]) == false
					|| (requireArgumentInList(args[4].replace("-", ""), dirStrings) == false
					&& requireArgumentInList(args[4].replace("-", ""), dirStringsAbbr) == false)) {
					return false;
				}
			}

			return true;
		}

		// x y h for absolute coordinate OR +x +y +h for relative coordinate (from current position)
		if(requireOffsets(Arrays.copyOfRange(args, 1, args.length)) || requireCoordinates(Arrays.copyOfRange(args, 1, args.length))) {
			lastError = null;
			return true;
		}

		lastError = "a waypoint (or target) must be a creature id (to follow), or coordinate for a tile and height above or offsets from starting position (offsets start with - or +), you cannot mix offsets with absolute coordinates";
		return false;
	}

	private boolean requireOffsets(String[] args)
	{
		for(String s : args) {
			if((s.startsWith("+") == false && s.startsWith("-") == false) || !isNumeric(s.replace("+", "").replace("-", ""))) {
				lastError = "an offset must consist of numbers starting with either a + or a -, \"" + s + "\" does not";
				return false;
			}
		}
		return true;
	}

	private boolean requireCoordinates(String[] args)
	{
		for(String s : args) {
			if(!isNumeric(s)) {
				lastError = "a coordinate must consist of numbers, \"" +s + "\" is not a number";
				return false;
			}
		}
		return true;
	}
	
	private boolean requireNumArgs(String[] args, int minNum, int maxNum)
	{
		if(args.length < minNum || args.length > maxNum) {
			if(minNum == maxNum) {
				lastError = args[0] + " wants " + (minNum-1) + " arguments";
			} else {
				lastError = args[0] + " wants between " + (minNum-1) + " and " + (maxNum-1) + " arguments";
			}
			return false;
		}
		return true;
	}
	
	
	private boolean requireDelay(String arg, int min, int max)
	{
		// This RE is a bit more complex than we actually need right now, but in the future I may want to support e.g. 1m13s.
		String[] tokens = arg.split("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");

		if(tokens.length != 2 || isNumeric(tokens[0]) == false) {
			lastError = "expecting a number and a time unit, such as \"15s\" for 15 seconds";
			return false;
		}

		if(!contains(delayUnits, tokens[1].toLowerCase())) {
			lastError = tokens[1] + " is not a valid time unit, expecting one of the following: " + Arrays.toString(delayUnits);
			return false;
		}

		return true;
	}
	
	
	private boolean validate(Scene scene, String block, String[] args)
	{
		boolean valid = false;
		
		switch(args[0]) {
		case "waypoint" :
			valid = isValidWaypoint(args);
			break;
			
		case "require" :
			valid = isValidRequire(args);
			break;
			
		case "tell" :
			valid = isValidTell(args);
			break;

		case "target" :
			valid = isValidTarget(args);
			break;

		case "use" :
			valid = isValidUse(args);
			break;
			
		case "wait" :
			valid = isValidWait(args);
			break;
		
		case "repeat" :
			valid = isValidRepeat(args);
			break;
			
		case "repeatyoyo" :
			valid = isValidRepeatYoyo(args);
			break;
		
		case "option" :
			valid = isValidOption(args);
			break;

		case "poll" :
			valid = isValidPoll(args);
			break;

		case "audio" :
			valid = isValidAudio(args);
			break;

		default :
			// This should really not happen as we should have verified this already.
			lastError = args[0] + " is not a valid command (should not happen here as it means the command is not implemented in validate())";
			break;
		}

		if(!valid && lastError == null) {
			// General purpose error in case validateXXX() did not set a lastError
			lastError = args[0] + " has a malformed or invalid argument";
		}

		return valid;
	}

	// useage: contains(haystack, needle)
	static <T> boolean contains(final T[] haystack, final T needle) {
	    if (needle == null) {
	        for (final T e : haystack)
	            if (e == null)
	                return true;
	    } else {
	        for (final T e : haystack)
	            if (e == needle || needle.equals(e))
	                return true;
	    }

	    return false;
	}
	
	static String[] tokenize(String line)
	{
		if (line == null || line.length() == 0) {
			return new String[0];
		}
		
		final int normal = 0;
		final int inQuote = 1;
		final int inDoubleQuote = 2;
		int state = normal;
		final StringTokenizer tok = new StringTokenizer(line, "\"\' \t", true);
		final ArrayList<String> result = new ArrayList<String>();
		final StringBuilder current = new StringBuilder();
		boolean lastTokenHasBeenQuoted = false;
		
		outerLoop:
		while (tok.hasMoreTokens()) {
			String nextTok = tok.nextToken();

			switch (state) {
				case inQuote:
					if ("\'".equals(nextTok)) {
						lastTokenHasBeenQuoted = true;
						state = normal;
					} else {
						current.append(nextTok);
					}
					break;

				case inDoubleQuote:
					if ("\"".equals(nextTok)) {
						lastTokenHasBeenQuoted = true;
						state = normal;
					} else {
						current.append(nextTok);
					}
			        break;
			        
				default:
					if ("\'".equals(nextTok)) {
						state = inQuote;
					} else if ("\"".equals(nextTok)) {
						state = inDoubleQuote;
					} else if (" ".equals(nextTok) || "\t".equals(nextTok)) {
						if (lastTokenHasBeenQuoted || current.length() != 0) {
							result.add(current.toString());
							current.setLength(0);
						}
					} else {
						if(nextTok.startsWith("#")) {
							break outerLoop;
						}
						current.append(nextTok);
					}
					lastTokenHasBeenQuoted = false;
					break;
			}
		}
		
		if (lastTokenHasBeenQuoted || current.length() != 0) {
			result.add(current.toString());
		}
		
		if (state == inQuote || state == inDoubleQuote) {
			throw new RuntimeException("missing or required quote in " + line);
		}
		
		return result.toArray(new String[result.size()]);
	}


	//match a number with optional '-' and decimal.
	private boolean isNumeric(String str)
	{
		return str.matches("-?\\d+(\\.\\d+)?"); 
	}

	static public boolean isRelativeFocusDirection(String arg)
	{
		return contains(relativeFocusDirections, arg);
	}

	static public long getDurationInMillis(String delayAndUnit)
	{
		String[] tokens = delayAndUnit.split("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");

		long amount = Integer.parseInt(tokens[0]);
		
		switch(tokens[1].toLowerCase()) {
			case "d" :
				return (amount * 24 * 60 * 60 * 1000);
			case "h" :
				return (amount * 60 * 60 * 1000);
			case "m":
				return (amount * 60 * 1000);
			case "s":
				return (amount * 1000);
			case "ms":
				return amount;
		}
		
		throw new CinematicBuilderException("failed to convert " + delayAndUnit + " to milliseconds");
	}


	static public boolean isOffset(String arg)
	{
		return arg.startsWith("+") || arg.startsWith("-");
	}
	
	
	// Fast overview: https://easings.net/
	static public TweenEquation getTweenEquation(String equation, String endsType)
	{
		equation = equation.toLowerCase();
		endsType = endsType.toLowerCase();
		
		if(contains(easingEquationsOptions, endsType) == false) {
			return null;
		}
		
		if(contains(easingEquations, equation.toLowerCase()) == false) {
			return null;
		}
		
		switch(endsType) {
			case "in" : 
				endsType = "IN"; 
				break;
				
			case "out" : 
				endsType = "OUT"; 
				break;
				
			case "inout" :
				endsType = "INOUT";
				break;
				
			default :
				return null;
		}
		
		equation = equation.substring(0,1).toUpperCase() + equation.substring(1).toLowerCase();
		
		return TweenUtils.parseEasing(equation + "." + endsType);
	}
	
	static public TweenPath getTweenPath(String fun)
	{
		fun = fun.toLowerCase();

		if(contains(pathingEquations, fun) == false) {
			logger.info("NO SUCH PATHING: " + fun);
			return null;
		}
		
		switch(fun) {
			case "linear" :
				return TweenPaths.linear;
	
			case "catmull-rom" :
				return TweenPaths.catmullRom;
			
			default :
				return null;
		}
	}
	

	/**
	 * Passed in argument must end with a % sign and must have a number before it, otherwise 1 (equivalent of 100%) is always returned.
	 * @param val
	 * @return
	 */
	static public float getMultiplierFromPerCent(String val)
	{
		if(val == null || val.length() <= 1 || val.endsWith("%") == false) {
			return 1f;
		}
		
		int percent = Integer.parseInt(val.substring(0, val.length() - 1));
		return (float)percent / 100f;
	}

    static public String abbreviatedDirStringToLongString(String dir)
    {
    	for(int i = 0; i < dirStringsAbbr.length; i++) {
    		if(dirStringsAbbr[i].equals(dir)) {
    			return dirStrings[i];
    		}
    	}
    	
    	// This was not abbreivated, we can just return what we got.
    	return dir;
    }

	// and after using this: convert distance and angle to a coordinate
	static float getAngleFromDirection(String dir)
	{
		dir = ScriptBuilder.abbreviatedDirStringToLongString(dir).replace("-", "");

		for(int i = 0; i < dirStrings.length; i++) {
			if(dirStrings[i].equals(dir)) {
				return ((i * (360f / (dirStrings.length - 3)))) % 360;			// -3 because we have up/down and an extra 'north' in the array
			}
		}
		
		throw new CinematicBuilderException("failed to translate direction (" + dir + ") to angle");
	}
	
	
	
	// https://stackoverflow.com/questions/42490604/getting-a-point-from-begginning-coordinates-angle-and-distance
	static Vector2f getPointFromDistAngle(float distance, float angle)
	{
		float rads = (float)Math.toRadians(angle);
		return new Vector2f(
			-(float)(distance * Math.cos(rads)),
			(float)(distance * Math.sin(rads))
		);
	}
}
