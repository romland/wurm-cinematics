package com.friya.wurmonline.client.hollywurm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.wurmonline.client.debug.Debugs;
import com.wurmonline.client.options.Options;
import com.wurmonline.client.renderer.gui.BenchmarkRenderer;
import com.wurmonline.client.stats.Stats;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.TweenManager;

public class ScriptRunner
{
	private static Logger logger = Logger.getLogger(SceneRunner.class.getName());
	private static List<Scene> scenes = new ArrayList<Scene>();

	private static Timer timer;
	private static final long tickFrequency = 10;
	private static boolean ticking;
	private static long lastTick = 0;
	
	private static TweenManager tweenMgr;
	
	private static String lastScript = null;
	
	private static final int MAX_FPS_SNAPSHOTS = 1024;
	private static int[] fps = new int[MAX_FPS_SNAPSHOTS];
	private static long skippedTicks = 0;
	private static int fpsSnapshots = 0;
	private static int fpsSnapshotFrequency = 500;		// Cannot be more than 1000 (1 second)
	private static long lastFpsSnapshot = 0;
	private static boolean benchmarking = false;
	private static long startedMyTickAt = 0;
	
	private static long scriptStartedAt = 0;
	private static long sceneStartedAt = 0;
	private static int pendingTweenCount = 0;
	
	private static long lastStutter = 0;
	
	private static ScriptEventListener eventListener = null;
	private static EventInformationScene eventListenerArg = null;

	private static MusicThread audio = null;
	

	static public void gameTick()
	{
		tick();
	}
	
	static private void tick()
	{
		startedMyTickAt = System.currentTimeMillis();
		if(ticking) {
			skippedTicks++;
			return;
		}
		
		if(tweenMgr == null) {
			return;
		}
		
		ticking = true;

		EventDispatcher.poll();

		if(tweenMgr.getRunningTweensCount() > 0) {
			// Deal with paused tweens (these are paused due to /at command in script)
			if(getSceneElapsedTimeExcludingSetup() > 0) {
				// Resume any paused tweens (these are pending a start signal because of /at command)
				// -5ms is okay to start at -- that ensures it will always start within +/- 5 ms of requested time (instead of 0-10ms later)
				List<BaseTween<?>> objects = tweenMgr.getObjects();
				pendingTweenCount = 0;
				for(BaseTween<?> t : objects) {
					if(t.isPaused()) {
						long startAt = ((TweenMeta)t.getUserData()).getStartTime();
						if(startAt <= (System.currentTimeMillis() - SceneRunner.getInstance().getSetupDoneAt() - (tickFrequency / 2))) {
							WurmHelpers.tellCinematics("        Starting " + ((TweenMeta)t.getUserData()).toString());
							t.resume();
							logger.info("Running a waiting tween, scheduled for " + startAt + "ms, actually started at " + (System.currentTimeMillis() - SceneRunner.getInstance().getSetupDoneAt()) + "ms");
						} else {
							pendingTweenCount++;
						}
					}
				}
			}

			//tweenMgr.update(tickFrequency * SceneRunner.getInstance().getSceneSpeed());
			//tweenMgr.update((System.currentTimeMillis() - lastTick) * SceneRunner.getInstance().getSceneSpeed());
			//tweenMgr.update(tickFrequency);
			tweenMgr.update((System.currentTimeMillis() - lastTick));
			
			addFpsSnapshotMaybe();
			
		} else if(SceneRunner.getInstance().isRunning()) {
			showUI();
			WurmHelpers.tellCinematics("Scene finished running (run time: " + (System.currentTimeMillis() - sceneStartedAt) + "ms)", MessageType.IMPORTANT);
			outputFpsSummary();
			SceneRunner.getInstance().setRunning(false);
			startNextScene();
		}
		
		long tickLen = (System.currentTimeMillis() - lastTick);
		if((Options.fpsLimitEnabled.value() == false && tickLen > 30) || (Options.fpsLimitEnabled.value() && tickLen > ((1000.0 / Options.fpsLimit.value()) + 2))) {
			long currTime = System.currentTimeMillis();
			if(Mod.debugWurm) {
				logger.info("Stuttering frame. Debug timers of it were:");
				WurmHelpers.outputDebugTimers();
			}
			logger.info("stutter (debug timers above): " + tickLen + " ms (of which ~" + (currTime - startedMyTickAt) + " ms is Cinematics). Time since last stutter: " + (currTime - lastStutter) + " ms");
			lastStutter = currTime;
		}
		
		lastTick = System.currentTimeMillis();
		ticking = false;
	}
	
	static void showUI()
	{
		// Don't muck about with hud unless user said it should be off.
		if(SceneRunner.getInstance().wantHiddenHud() == false) {
			return;
		}

		if(Mod.EARLY_TESTING) {
			return;
		}

		WurmHelpers.showUI();
	}

	static void hideUI()
	{
		if(Mod.EARLY_TESTING) {
			return;
		}
		
		WurmHelpers.hideUI();
	}


	static private void runInit()
	{
		if(tweenMgr == null) {
			// Initialize settings of tweens, this should really only happen once. XXX: this is looking for a better home.
			WurmHelpers.initTweens();
			tweenMgr = new TweenManager();
		}

		if(timer != null) {
			timer.cancel();
			timer = null;
		}
		
		if(    Mod.usePlayerTick == false
			&& Mod.useHudRendererTick == false 
			&& Mod.useWorldTick == false 
			&& Mod.useWorldRenderTick == false 
			&& timer == null) {
			ticking = false;
			lastTick = System.currentTimeMillis() - (tickFrequency + 1);
			timer = new Timer();
			
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					tick();
				}
			}, 0, tickFrequency);
		}

		fps = new int[MAX_FPS_SNAPSHOTS];
		fpsSnapshots = 0;
		skippedTicks = 0;
		lastFpsSnapshot = 0;
		setBenchmarking(false);
		scriptStartedAt = System.currentTimeMillis();
		sceneStartedAt = 0;
		eventListener = null;
		eventListenerArg = null;
		
		scenes.clear();
	}
	

	/**
	 * 
	 * @param fileName
	 */
	static void run(String fileName)
	{
		if(fileName == null) {
			throw new CinematicBuilderException("No script has been loaded yet.");
		}
		
		if(IO.exists(fileName) == false) {
			throw new CinematicBuilderException("Script \"" + fileName + "\" does not exist.");
		}
		
		if(SceneRunner.getInstance().isRunning() == true) {
			throw new CinematicBuilderException("A scene is already running, stop that one first.");	
		}

		runInit();
		lastScript = fileName;
		
		ScriptBuilder builder = new ScriptBuilder();
		String script = IO.load(fileName);

		WurmHelpers.tellCinematics("Starting script \"" + fileName + "\"...", MessageType.POSITIVE);
		scenes.addAll(
				builder.parse(fileName, script)
		);

		startNextScene();
	}
	

	static boolean runServerEvent(ScriptEventListener eventHandler, EventInformationScene eiScene)
	{
		if(eiScene == null) {
			return false;
		}

		if(SceneRunner.getInstance().isRunning() == true) {
			return false;	
		}

		String script = eiScene.toString();
		String fileName = "Server Event";

		eventListener = eventHandler;
		eventListenerArg = eiScene;
		runInit();
		lastScript = fileName;

		ScriptBuilder builder = new ScriptBuilder();
		scenes.addAll(
				builder.parse(fileName, script)
		);

		notifyScriptStarted();
		startNextScene();

		return true;
	}

	static private boolean startNextScene()
	{
		if(scenes.size() == 0) {
			stopAudio();
			notifySceneEnded();
			notifyScriptEnded();
			return false;
		}
		
		sceneStartedAt = System.currentTimeMillis();
		SceneRunner.getInstance().start(scenes.get(0));
		notifySceneStarted();
		scenes.remove(0);

		return true;
	}

	static boolean startAudio(String fileName, int startAt)
	{
		stopAudio();

		audio = new MusicThread(fileName, startAt);
        audio.start();
        return true;
	}

	static boolean stopAudio()
	{
		if(audio != null) {
			audio.terminate();
			audio = null;
		}

		return true;
	}
	
	static void stopScript()
	{
		if(Mod.debugWurm) {
			logger.info("*Hopefully* a normal frame as reference:");
			WurmHelpers.outputDebugTimers();
		}

		scenes.clear();

		stopAudio();

		scriptStartedAt = 0;
		sceneStartedAt = 0;

		if(BenchmarkRenderer.isFading()) {
			BenchmarkRenderer.fadeToShow();
		}

		//if(SceneRunner.getInstance().isRunning()) {
			showUI();
			SceneRunner.getInstance().stop();
		//}
		notifySceneEnded();
		notifyScriptEnded();
	}
	
	static TweenManager getTweenManager()
	{
		return tweenMgr;
	}

	static private void addFpsSnapshotMaybe()
	{
		long ts = System.currentTimeMillis();

		if(ts > (lastFpsSnapshot + fpsSnapshotFrequency)) {
			fps[fpsSnapshots % MAX_FPS_SNAPSHOTS] = Stats.fps.get();
			lastFpsSnapshot = ts;
			//logger.info(ts + " FPS snapshot " + fpsSnapshots + " (at index " + (fpsSnapshots % MAX_FPS_SNAPSHOTS) + "): " + fps[fpsSnapshots % MAX_FPS_SNAPSHOTS]);
			fpsSnapshots++;
		}
	}
	
	static private void outputFpsSummary()
	{
		int[] tmpFps;

		if(fpsSnapshots == 0) {
			return;
		}

		if(fpsSnapshots > MAX_FPS_SNAPSHOTS) {
			tmpFps = fps;
		} else {
			tmpFps = Arrays.copyOfRange(fps, 0, fpsSnapshots);
		}
		
		Arrays.sort(tmpFps);
		
		double median;

		if (tmpFps.length % 2 == 0) {
		    median = ((double)tmpFps[tmpFps.length/2] + (double)tmpFps[tmpFps.length/2 - 1])/2;
		} else {
		    median = (double) tmpFps[tmpFps.length/2];
		}

		long sum = 0;
		int low = Integer.MAX_VALUE;
		int high = 0;
		int below60 = 0;
		for(int d : tmpFps) {
			if(d < low)
				low = d;
			
			if(d > high)
				high = d;
			
			if(d < 60) {
				below60++;
			}
			
			sum += d;
		}
		
		double average = (sum / tmpFps.length);
		
		int duration = tmpFps.length / (1000 / fpsSnapshotFrequency);
		
		// % below 60fps
		double ratioBelow = 0;

		if(below60 > 0) {
			ratioBelow = (float)below60 / (float)tmpFps.length * 100f;
		}
	
		WurmHelpers.tellCinematics(String.format("Ran \"%s\" for %d seconds. FPS: average %.0f, median %.0f, low %d, high %d. Below 60 FPS %.0f%% of the time.", lastScript, duration, average, median, low, high, ratioBelow), MessageType.INFO);

		if(skippedTicks > 0) {
			WurmHelpers.tellCinematics(String.format("Skipped ticks: %d (performance is not great, consider reducing settings or closing programs)", skippedTicks), MessageType.WARNING);
		}
	}
	
	static public void setBenchmarking(boolean v)
	{
		benchmarking = v;
	}
	
	static public long getSceneElapsedTime()
	{
		if(sceneStartedAt == 0) {
			return 0;
		}
		
		return System.currentTimeMillis() - sceneStartedAt;
	}
	
	static public long getSceneElapsedTimeExcludingSetup()
	{
		if(SceneRunner.getInstance().getSetupDoneAt() == 0) {
			return 0;
		}
		
		return System.currentTimeMillis() - SceneRunner.getInstance().getSetupDoneAt();
	}
	

	static public long getScriptElapsedTime()
	{
		if(scriptStartedAt == 0) {
			return 0;
		}

		return System.currentTimeMillis() - scriptStartedAt;
	}
	
	static public long getPendingTweenCount()
	{
		return pendingTweenCount;
	}

	static public boolean isRunning()
	{
		return SceneRunner.getInstance().isRunning();
	}
	
	static public boolean isSettingUp()
	{
		return SceneRunner.getInstance().isSetupDone() == false;
	}
	
	static public boolean isBenchmarking()
	{
		return isRunning() && benchmarking;
	}

	static private void notifyScriptStarted()
	{
		if(eventListener != null) {
			eventListener.scriptStartedEvent(eventListenerArg);
		}
	}

	static private void notifyScriptEnded()
	{
		if(eventListener != null) {
			eventListener.scriptEndedEvent(eventListenerArg);
		}
	}

	static private void notifySceneStarted()
	{
		if(eventListener != null) {
			eventListener.sceneStartedEvent(eventListenerArg);
		}
	}

	static private void notifySceneEnded()
	{
		if(eventListener != null) {
			eventListener.sceneEndedEvent(eventListenerArg);
		}
	}

}
