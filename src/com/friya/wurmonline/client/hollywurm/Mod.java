package com.friya.wurmonline.client.hollywurm;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

// cd "D:\Program Files (x86)\Steam\steamapps\common\Wurm Unlimited\WurmLauncher"
// Get-Content .\client.log -wait


// /load Friya-Deed-Edgehedge-1491701433098.friyaclip

// spiders: #goto 1230 1661 
// /loadscript Creature_Spawn_test.txt

public class Mod implements WurmClientMod, Initable, PreInitable, Configurable
{
	// NOTE: For fast and easy testing, set this to true and the entire flow of parsing/building/running is executed right at startup of Wurm.
	static final boolean EARLY_TESTING = false;

	public static String currentVersion = "1.0.0";

	public static boolean removeSureQuitDialog = true;
	public static boolean disableAnyWeatherOverride = false;
	public static boolean showScriptInfoWhenBenchmarking = true;
	public static boolean renderOwnBody = false;
	public static boolean cinematicsEnabled = true;
	public static boolean acceptServerEvents = true;
	public static boolean disableWurmRenderers = false;
	
	// Ticker, only ONE (or none to use our internal clock) of these must be active at any given point...
	public static boolean usePlayerTick = false;			// Set for testing only	(this is a bad tick -- frequently 40ms+)
	public static boolean useWorldTick = false;				// Set for testing only (but NOT together with any of the two above)
	public static boolean useHudRendererTick = false;		// Set for testing only (but NOT together with the above)
	public static boolean useWorldRenderTick = true;		// recommended?
	
	// Set to null to not load anything.
	//static String autoLoadScript = "test38";
	static String autoLoadScript = null;
	//static String autoLoadScript = "examples/0410 - Synchronizing with music";
	//static String autoLoadScript = "born-to-be-wild";
	//static String autoLoadScript = "examples\\0250 - Follow the mosquito.txt";
	
	public static boolean debugWurm = false;					// Set this to true to collect timer stats on frames. We'll save a snapshot of last frame, each frame. Costly.
	public static boolean closeByFocusFix = true;
	
	
	
	static HashMap<String, String> softwareUpdateInfo = new HashMap<String, String>();
	
	private static Logger logger = Logger.getLogger(Mod.class.getName());

    public enum DearReader {
        BORROWING("   If you are going to borrow code, at last give some credit to the author. Authors like that. It makes it easier to spend hundreds of hours creating things."),
        RENAMING("    If you HAVE TO release this code as your own or from your own VCS, at least have the decency to make it clear that it's a fork so I don't have to support your shit, "),
        LESSONS("     lessons were learned."),
        IDIOT("       So, on this note (and others over the years) ... that lad called Sindusk on Wurm Forums is a retard. Whatever you are paying your shrink, Sindusk, it's clearly too much."),
    	NEW_WURM_DEV("Oh. That was a surprise (him becoming Wurm dev). The guy's got a severe lack of integrity, a very hard time telling right from wrong and can only tweak other people's code."
    			+ " Not to mention that if he lets out a fart, he will make sure *everyone* knows that it smelled like rose petals and was a tremendous scientific breakthrough."),
        LICENSE("     You may use this program for whatever purpose, provided that the credits and this enum (DearReader) and its contents is present in the program or parts thereof."),
        ME("                     -- (c)Friya, 2018"),
        FUTURE("      When I consider this mod finished, I will remove these idiotic license terms."),
    	EMPTY("       ");
        private DearReader(String s) {}
    }


    @Override
	public void configure(Properties p)
	{
    	IO.makeDirs();
    	
    	logger.info("\n"
			+ "                                 ,A\n"
			+ "                                g@@\n"
			+ "            A.                 d@V@\n"
			+ "            i@b               iA` @                    .A\n"
			+ "            i@V@s            dA`  @                   ,@@i\n"
			+ "            i@ '*@Ws______mW@f    @@                  ]@@@\n"
			+ "             @]  '^********f`     @@@                g@!l@\n"
			+ "             @b           .  ,g   @@@W             ,W@[ l@\n"
			+ "             ]@   Mmmmmf  ~**f^   @@^@@Wm_.      _g@@`  l@\n"
			+ "              M[    ^^      ^^    @@  ^*@@@@@@@@@@@f`   l@\n"
			+ "              ]@i  '[]`  . '[]`   @@      ~~~~~~~`      l@\n"
			+ "              '@W       |[        @@               _.   l@\n"
			+ "               Y@i      'Ns    .  @@   _m@@m,    g*~VWi l@\n"
			+ "                M@. ,.       g@` ]@@  gP    V,  '  m- ` l@\n"
			+ "                '@W  Vmmmmmm@f   @@@. ~  m-    g   '    l@\n"
			+ "                 '@b  'V***f    g@A@W    '     @        l@\n"
			+ "                  '*W_         g@A`M@b       g M_.      l@\n"
			+ "                    'VMms___gW@@f   @@.      '- ~`      W@\n"
			+ "                       ~*****f~`     @@.     ,_m@Ws__   @@\n"
			+ "                                     '@@.   gA~`   ~~* ]@P\n"
			+ "                                       M@s.'`         g@A\n"
			+ "                                        V@@Ws.    __m@@A\n"
			+ "                                          ~*M@@@@@@@@*`\n"
			+ "\n"
			+ "                          Friya's Cinematics                 \n"
			+ "                          ``````````````````                 \n"
			+ "                Java tweening library by Aurelien Ribon      \n"
			+ "            MP3 decoder by javazoom.net/javalayer/about.html \n"
			+ "                Example song Tagirijus by Manuel Senfft      \n"
			+ "                    JSON encoder/decoder by Google           \n"
			+ "               The drama masks ASCII art by David Laundra    \n"
			+ "\n"
			+ "               Everything else by Friya  (aka Friyanouce)    \n"
			+ "\n"
			+ "            You can find me and some of my other projects at \n"
			+ "                       http://filterbubbles.com              \n"
			+ "\n"
			+ "              You can also find me on Discord as Friya#7934  \n"
			+ "\n"
			+ "                               Enjoy!                        \n"
			+ "\n"
			+ "Your folder for cinematics scripts is: " + IO.getCinematicsDir() + "\\"
			+ "\n"
		);
    	
    	removeSureQuitDialog = Boolean.valueOf(p.getProperty("removeSureQuitDialog", String.valueOf(removeSureQuitDialog))).booleanValue();
    	disableAnyWeatherOverride = Boolean.valueOf(p.getProperty("disableAnyWeatherOverride", String.valueOf(disableAnyWeatherOverride))).booleanValue();
    	showScriptInfoWhenBenchmarking = Boolean.valueOf(p.getProperty("showScriptInfoWhenBenchmarking", String.valueOf(showScriptInfoWhenBenchmarking))).booleanValue();
    	renderOwnBody = Boolean.valueOf(p.getProperty("renderOwnBody", String.valueOf(renderOwnBody))).booleanValue();
    	cinematicsEnabled = Boolean.valueOf(p.getProperty("cinematicsEnabled", String.valueOf(cinematicsEnabled))).booleanValue();
    	acceptServerEvents = Boolean.valueOf(p.getProperty("acceptServerEvents", String.valueOf(acceptServerEvents))).booleanValue();
    	if(!usePlayerTick && !useWorldTick && !useHudRendererTick) {
    		useWorldRenderTick = Boolean.valueOf(p.getProperty("useWorldRenderTick", String.valueOf(useWorldRenderTick))).booleanValue();
    	} else {
    		logger.info("Ignoring useWorldRenderTick setting because it's overridden in code. Sorry!");
    	}
    	disableWurmRenderers = Boolean.valueOf(p.getProperty("disableWurmRenderers", String.valueOf(disableWurmRenderers))).booleanValue();
    	
    	autoLoadScript = String.valueOf(p.getProperty("autoLoadScript", String.valueOf(autoLoadScript)));
    	logger.info("Loaded configuration...");
	}

    public void preLoad()
    {
    	if(autoLoadScript != null && autoLoadScript.length() > 0) {
    		Actions.handleChatInput("/loadscript " + autoLoadScript);
    	}
    }

    @Override
	public void preInit()
	{
	}
	

    private void test()
	{
    	Receiver r = new Receiver();
    	r.handle(IO.load("_spectate-test-01.txt"));


//    	PathMaker.getCircle(200f, 1200f, 1200f, 5);
    	
//		Testing.f1();
//    	int x = 1/0;logger.info(""+x);
    	
//    	ScriptRunner.startMusic();
/*
    	try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/
//    	outputRemoteWelcomeMessage();
    	
    	
//    	logger.info( Arrays.toString(IO.dir("examples")) );
  
//    	ScriptRunner.run(autoLoadScript);
    	
		//ScriptRunner.runScript("Script_1528054707586");

		// It's an early test, so bail out early as well.
    	logger.info("Triggering a fatal exception...");
		int x = 1/0;logger.info(""+x);
	}


	/**
	 * Typically this map contains:
	 *		latestVersion=1.0.0
	 *		downloadAt=http://www.filterbubbles.com/wurm-unlimited/cinematics/download.html
	 *		downloadMessage=First public release.
	 *		message=
	 */
	static void outputRemoteWelcomeMessage()
	{
		if(softwareUpdateInfo.containsKey("message") && softwareUpdateInfo.get("message").trim().length() > 0) {
			WurmHelpers.tellCinematics("");
			WurmHelpers.tellCinematics(softwareUpdateInfo.get("message"), MessageType.IMPORTANT);
			WurmHelpers.tellCinematics("");
		}

		if(softwareUpdateInfo.containsKey("latestVersion") && softwareUpdateInfo.get("latestVersion").equals(currentVersion) == false) {
			WurmHelpers.tellCinematics("");
			WurmHelpers.tellCinematics("There is a new version of Friya's Cinematics available at " + softwareUpdateInfo.get("downloadAt"), MessageType.IMPORTANT);
			WurmHelpers.tellCinematics(softwareUpdateInfo.get("downloadMessage"), MessageType.IMPORTANT);
			WurmHelpers.tellCinematics("");
		}
	}
    
	private void openGettingStartedOnFirstLaunch()
	{
		try {
			File file = new File(System.getProperty("user.dir") + File.separator + "mods" + File.separator + "friyas-cinematics" + File.separator + "showed-getting-started.txt");

			if (file.createNewFile()){
				logger.info("File is now created -- will not open on launch again.");
				Desktop.getDesktop().browse(new URI("http://www.filterbubbles.com/wurm-unlimited/cinematics/"));
			} else {
				//logger.info("File existed.");
			}
		} catch (IOException | URISyntaxException e) {
			logger.info("Unable to open Cinematics site for the first time");
		}

	}

    @Override
	public void init()
	{
    	if(Mod.EARLY_TESTING) {
			test();
		}

    	new UpdateChecker();
    	openGettingStartedOnFirstLaunch();

    	WurmHelpers.setupHooks();

    	//logger.info("FORCING DIV BY 0"); int x = 1/0;
	}
}
