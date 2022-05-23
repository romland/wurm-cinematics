package com.friya.wurmonline.client.hollywurm;

/**
 * Stored in a tween when it's created, used to grab meta information about it's source data.
 * 
 * @author J.Romland
 */
public class TweenMeta 
{
	enum CoordinateType {
		UNKNOWN(0),
		RELATIVE_NUMBER(1),
		RELATIVE_STRING(2),
		ABSOLUTE(3);
		
	    private final int value;
	    
	    private CoordinateType(int value) 
	    {
	        this.value = value;
	    }

	    public int getValue()
	    {
	        return value;
	    }

	    public static CoordinateType fromInt(int x) {
	        switch(x) {
	        case 0:
	            return UNKNOWN;
	        case 1:
	            return RELATIVE_NUMBER;
	        case 2:
	            return RELATIVE_STRING;
	        case 3:
	            return ABSOLUTE;
	        }
	        return null;
	    }
	};
	
	// anything
	private String name = "";
	
	// e.g. focus
	private String cameraType = "";
	
	// e.g. /camera
	private String commandType = "";
	
	// timestamp when we should start the tween. Future support for /camera focus for 20s after 15s .... delayed start
	private long startAt = 0;
	
	CoordinateType coordinateType = CoordinateType.UNKNOWN;
	
	TweenPosition focusPoint = null;
	TweenPosition followPoint = null;
	

	public TweenMeta(String _name, String _cameraType, String _commandType) 
	{
		this(_name, _cameraType, _commandType, CoordinateType.UNKNOWN, 0);
	}

	public TweenMeta(String _name, String _cameraType, String _commandType, CoordinateType _coordinateType) 
	{
		this(_name, _cameraType, _commandType, _coordinateType, 0);
	}
	
	public TweenMeta(String _name, String _cameraType, String _commandType, CoordinateType _coordinateType, long startAtMs) 
	{
		this.name = _name;
		this.cameraType = _cameraType;
		this.commandType = _commandType;
		this.coordinateType = _coordinateType;
		this.startAt = startAtMs;
	}

	public String toString()
	{
		return name + ", " + commandType + ", type: " + cameraType + ", start after: " + startAt + "ms, coordinate type: " + coordinateType.toString();
	}
	
	void setStartTime(long startAtMs)
	{
		this.startAt = startAtMs;
	}
	

	long getStartTime()
	{
		return startAt;
	}
	
	void setCoordinateType(CoordinateType ct)
	{
		coordinateType = ct;
	}
}
