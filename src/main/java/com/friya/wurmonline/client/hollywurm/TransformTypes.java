package com.friya.wurmonline.client.hollywurm;

public class TransformTypes
{
	public static final int UNKNOWN_TRANSFORM = -1;
	
	public static final int ROT_X = 1;
	public static final int ROT_Y = 2;
	public static final int ROT_XY = 3;

	public static final int MOVE_X = 4;
	public static final int MOVE_Y = 5;
	public static final int MOVE_H = 6;
	public static final int MOVE_XY = 7;
	public static final int MOVE_XYH = 8;

	public static final int FOCUS_XY = 9;
	public static final int FOCUS_XYH = 10;
	public static final int FOCUS_RELATIVE_TO_VELOCITY = 11;
	
	public static final int FOLLOW_XY = 12;
	public static final int FOLLOW_XYH = 13;

	public static final int FOLLOW_FOCUS = 14;
	
	public static final int WEATHER_WIND = 15;
	public static final int WEATHER_CLOUDS = 16;
	public static final int WEATHER_RAIN = 17;
	public static final int WEATHER_FOG = 18;
	public static final int WORLD_TIME = 19;
	
	// NOTE: for debugging, it's not DRY at all
	private static final String[] types = new String[]{
		"", "ROT_X", "ROT_Y", "ROT_XY", "MOVE_X", "MOVE_Y", "MOVE_H", "MOVE_XY", "MOVE_XYH", "FOCUS_XY", "FOCUS_XYH", "FOCUS_RELATIVE_TO_VELOCITY", "FOLLOW_XY", "FOLLOW_XYH", "FOLLOW_FOCUS",
		"WEATHER_WIND", "WEATHER_CLOUDS", "WEATHER_RAIN", "WEATHER_FOG", "WORLD_TIME"
	};
	
	static String getTypeAsString(int type)
	{
		return types[type];
	}
	
	static int getWorldChangeType(String type, int numArgs)
	{
		switch(type) {
			case "wind" :
				return WEATHER_WIND;

			case "clouds" :
				return WEATHER_CLOUDS;

			case "rain" :
				return WEATHER_RAIN;

			case "fog" :
				return WEATHER_FOG;

			case "time" :
				return WORLD_TIME;

			default :
				return UNKNOWN_TRANSFORM;
		}
	}
	
	
	static int getCameraType(String type, int numArgs, boolean targetIsMoving)
	{
		switch(type) {
			case "focus" :
				return getCameraFocusType(numArgs, targetIsMoving);
			case "rotate" :
				return getCameraRotateType(numArgs);
			case "move" :
				return getCameraMoveType(numArgs);
			case "follow" :
				return getCameraFollowType(numArgs);
			default :
				return UNKNOWN_TRANSFORM;
		}
	}

	static private int getCameraFocusType(int numArgs, boolean targetIsMoving)
	{
		switch(numArgs) {
		case 1 : 
			return FOCUS_RELATIVE_TO_VELOCITY;
		case 2:
			if(targetIsMoving) {
				return FOLLOW_FOCUS;
			} else {
				return FOCUS_XY;
			}
		case 3:
			return FOCUS_XYH;
		case 4:
			// e.g. 200 north 50 up
			return FOCUS_XYH;
		case 5:
			return FOLLOW_FOCUS;
		default :
			return UNKNOWN_TRANSFORM;
		}
	}

	static private int getCameraRotateType(int numArgs)
	{
		switch(numArgs) {
		case 1:
			return ROT_X;
		case 2:
			return ROT_XY;
		default :
			return UNKNOWN_TRANSFORM;
		}
	}

	static private int getCameraMoveType(int numArgs)
	{
		switch(numArgs) {
		case 1:
			return MOVE_X;
		case 2:
			return MOVE_XY;
		case 3:
			return MOVE_XYH;
		case 4 :
			// this covers case "5 north 20 up"
			return MOVE_XYH;
		default :
			return UNKNOWN_TRANSFORM;
		}
	}

	static private int getCameraFollowType(int numArgs)
	{
		switch(numArgs) {
		case 4:
			return FOLLOW_XY;
		case 5:
			return FOLLOW_XYH;
		default :
			return UNKNOWN_TRANSFORM;
		}
	}
	
}
