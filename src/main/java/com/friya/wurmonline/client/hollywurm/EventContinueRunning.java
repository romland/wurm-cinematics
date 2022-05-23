package com.friya.wurmonline.client.hollywurm;

//import java.util.logging.Logger;

public class EventContinueRunning extends EventOnce
{
//	private static Logger logger = Logger.getLogger(EventContinueRunning.class.getName());

    private Scene scene;
    private SceneRunner runner;
    
	public EventContinueRunning(long fromNow, Unit unit, Scene _scene, SceneRunner _runner)
	{
		super(fromNow, unit);
		scene = _scene;
		runner = _runner;
	}


	@Override
	public boolean invoke() 
	{
		try {
			runner.continueRunning(scene);
		} catch(CinematicBuilderException e) {
			WurmHelpers.tellCinematics(e.getMessage(), MessageType.ERROR);
		}
		
		
		return true;
	}
}
