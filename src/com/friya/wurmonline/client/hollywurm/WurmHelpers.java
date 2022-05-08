package com.friya.wurmonline.client.hollywurm;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modsupport.ModClient;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.debug.DInfo;
import com.wurmonline.client.debug.Debugs;
import com.wurmonline.client.game.GameProxy;
import com.wurmonline.client.game.PlayerDummy;
import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.game.World;
import com.wurmonline.client.options.Option;
import com.wurmonline.client.options.Options;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.cell.MovementData;
import com.wurmonline.client.renderer.gui.AbstractTab;
import com.wurmonline.client.renderer.gui.BenchmarkRenderer;
import com.wurmonline.client.renderer.gui.FriyasGuiProxy;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.HealthBar;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.renderer.gui.text.TextQuad;
import com.wurmonline.client.renderer.terrain.weather.Weather;
import com.wurmonline.math.Vector2f;
import com.wurmonline.math.Vector3f;
import com.wurmonline.shared.util.MovementChecker;
import com.wurmonline.shared.util.MulticolorLineSegment;

import aurelienribon.tweenengine.Tween;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public class WurmHelpers 
{
    private static Logger logger = Logger.getLogger(WurmHelpers.class.getName());

	private static boolean messagedOnStartup = false;


	static final String directions[] = {
		"north", "north-northeast", "northeast", "east-northeast", "east", "east-southeast", "southeast", "south-southeast",
		"south", "south-southwest", "southwest", "west-southwest", "west", "west-northwest", "northwest", "north-northwest",
		"north"
	};

	static final String directionsBrief[] = {
		"n", "nne", "ne", "ene", "e", "ese", "se", "sse", "s", "ssw", "sw", "wsw", "w", "wnw", "nw", "nnw", "n"
	};

	private static final Map<Long, TweenPosition> creatures = new HashMap<Long, TweenPosition>(); 
	private static MovementData movementData = null;
	private static MovementChecker movementChecker = null;
	private static BenchmarkRenderer benchmarkRenderer = null;
	private static Map<String, Option> wurmOptions = null;
	private static Map<Long, GroundItemCellRenderable> groundItems;
	
	private static StringBuffer lastFrameStats;
	
	
	/**
	 * This lives here so we don't get any unncessary class loads while doing early testing.
	 */
	static void initTweens()
	{
		Tween.setWaypointsLimit(150);
		Tween.setCombinedAttributesLimit(5);
		Tween.registerAccessor(PlayerObj.class, new PlayerAccessor());
		Tween.registerAccessor(PlayerDummy.class, new PlayerAccessor());
		Tween.registerAccessor(Weather.class, new WorldAccessor());
	}
	
	static void setupHooks()
	{
		if(Mod.cinematicsEnabled == false) {
			return;
		}
		
		try {
//			patchConsoleInput();
			patchChatInput();
			patchNameInHealthBar();
			
//			patchMenuItems();
			setupPopupDataInterception();
			
			if(Mod.debugWurm) {
				patchGameLoopDebug();
			}

			if(Mod.usePlayerTick) {
				patchGameTick();
			}
			
			if(Mod.useWorldRenderTick) {
				patchWorldRenderTick();
			}
			
			if(Mod.disableWurmRenderers) {
				patchRenderToggling();
			}

//			patchYRotCapRemoval();
			
//			patchWorldTick();

			if(false) {
				// This is a test to see if fly() interferes with automatic movement (it did not make it choppy! at least)
				patchFly();
			}
			
			if(Mod.disableAnyWeatherOverride) {
				patchWeather();
			}
			if(Mod.renderOwnBody == false) {
				patchNoBody();
			}
			setupActionInterception();
			setupStartupInterception();
			setupHealthBarRenderInterception();
			setupConsoleInputInterception();
			if(Mod.removeSureQuitDialog == true) {
				setupQuitDialogInterception();
			}

//			patchParticleEffectLoader();
			setupBmlInterception();
			setupBmlResponseInterception();
			setupHudRenderInterception();
			setupCloseTabInterception();
			
			setupCreatureTracking();
		} catch (NotFoundException | CannotCompileException e) {
			logger.log(Level.SEVERE, e.toString());
		}						
	}

	static boolean canFly()
	{
		return CellRenderable.world.getServerConnection().canFly();
	}

	static boolean classExists(String s)
	{
		try {
			ClassPool cp = HookManager.getInstance().getClassPool();
			@SuppressWarnings("unused")
			CtClass cls = cp.get(s);
			return true;
		} catch (NotFoundException e) {
			return false;
		}
	}

	static void tellConsole(String msg)
	{
		getConsole().handleInput(msg, false);
	}
	
	static void tellServer(String msg)
	{
		CellRenderable.world.getServerConnection().sendmessage5(":Local", msg);
	}
	
	static public void tellCinematics(String msg)
	{
		tellCinematics(msg, MessageType.INFO);
	}
	
	static void tellCinematics(String msg, MessageType mt)
	{
		float r = 0, g = 0, b = 0;
		
		switch(mt) {
			case INFO :
				r = 128f / 255f;
				g = 200f / 255f;
				b = 200f / 255f;
				break;

			case POSITIVE :
				r = 128f / 255f;
				g = 255f / 255f;
				b = 128f / 255f;
				break;

			case ERROR :
				r = 255f / 255f;
				g = 60f / 255f;
				b = 60f / 255f;
				break;

			case IMPORTANT :
				r = 140f / 255f;
				g = 140f / 255f;
				b = 240f / 255f;
				break;

			case WARNING :
				r = 160f / 255f;
				g = 128f / 255f;
				b = 0f / 255f;
				break;
		}
		
		if(getWorld() == null) {
			logger.info("tellCinematics: " + msg);
			return;
		}
		
		FriyasGuiProxy.activateTab(":Cinematics");
		
		getHud().textMessage(":" + "Cinematics", r, g, b, msg);
	}
	
	static Weather getWeather()
	{
		return Weather.getInstance();
	}
	
	static World getWorld()
	{
		return CellRenderable.world;
	}
	
	private static WurmConsole getConsole()
	{
		return ModClient.getClientInstance().getConsole();
	}
	
	public static HeadsUpDisplay getHud()
	{
		return getWorld().getHud();
	}
	
	static PlayerObj getPlayer()
	{
		if(CellRenderable.world == null) {
			throw new RuntimeException("no world yet, so no player");
		}
		
		return CellRenderable.world.getPlayer();
	}
	
	private static MovementData getMovementData()
	{
		if(movementData == null) {
			try {
				Field f = getPlayer().getPlayerBody().getClass().getDeclaredField("movementData");
				f.setAccessible(true);
				movementData = (MovementData)f.get(getPlayer().getPlayerBody());

			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("failed to get movementdata pointer");
			}
		}
		
		return movementData;
	}
	
	private static MovementChecker getMovementChecker()
	{
		if(movementChecker == null) {
			try {
				Field f = getPlayer().getClass().getDeclaredField("movementChecker");
				f.setAccessible(true);
				movementChecker = (MovementChecker)f.get(getPlayer());

			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("failed to get movementdata pointer");
			}
		}
		
		return movementChecker;
	}
	
	@SuppressWarnings("unchecked")
	static Map<Long, GroundItemCellRenderable> getGroundItems()
	{
		if(groundItems == null) {
			try {
				Field f = getWorld().getServerConnection().getServerConnectionListener().getClass().getDeclaredField("groundItems");
				f.setAccessible(true);
				groundItems = (HashMap<Long, GroundItemCellRenderable>)f.get(getWorld().getServerConnection().getServerConnectionListener());

			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("failed to get groundItems pointer");
			}
		}
		
		return groundItems;
	}
	
	
	/**
	 * TODO:
	 * 		It will render behind stencils (distant trees)
	 * 		It will not render when behind it
	 * 		It will not render if you are too close
	 * 		It is black
	 * 		It is "shrunk"
	 * 		If item is picked up and dropped, it loses the label (I'm okay with this)
	 * 		It is behind ALL trees (objects?)
	 * 
	 * Want options:
	 * 		Always face camera (for labels above players, etc)
	 * 		Some size setting
	 * 		Color
	 * 
	 * @param id
	 */
	@SuppressWarnings("unchecked")
	static void attachTextTo(long id)
	{
		GroundItemCellRenderable item = groundItems.get(id);
		if(item == null) {
			return;
		}

		// Need this to create ArrayList<TextQuad> in the item.
		// TOOD: This can possibly go wrong with signs (do we get duplicate text because of lack of cleanup or so?)
		item.createAttachedTexts();
		
		List<TextQuad> textQuads;
		
		try {
			Field f = item.getClass().getDeclaredField("attachedTextQuads");
			f.setAccessible(true);
			textQuads = (ArrayList<TextQuad>)f.get(item);

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("failed to attach text");
		}

		TextQuad textQuad = new TextQuad(
				"This is a test to label objects in the world!", 
				TextFont.getSignText(),
				new Vector2f(2.0f, 2.0f), 						// size
				new Vector3f(0.02f, 1.65f, 0.042f)				// offset
		);
		textQuads.add(textQuad);
		
		// This is done every time there's a game tick AND when it's updated()
		/*
		for (final TextQuad textQuad : this.attachedTextQuads) {
	        textQuad.setOrigin(
				this.x - GroundItemCellRenderable.world.getRenderOriginX(), 
				this.h, 
				this.y - GroundItemCellRenderable.world.getRenderOriginY(), 
				this.roll, 
				this.yaw, 
				this.pitch
			);
			textQuad.tick();
		}
		*/
		
		// We do not need gameticks -- we just need to call updated...
//		item.setWantsGameTicks(true);
		
		item.updated();
	}
	

	static BenchmarkRenderer getBenchmarkRenderer()
	{
		if(benchmarkRenderer == null) {
			benchmarkRenderer = new BenchmarkRenderer(getHud());
		}

		return benchmarkRenderer;
	}
	
	@SuppressWarnings("unchecked")
	static public Map<String, Option> getWurmOptions()
	{
		if(wurmOptions == null) {
			try {
				Field f = Options.class.getDeclaredField("allOptions");
				f.setAccessible(true);
				wurmOptions = (Map<String, Option>)f.get(null);

			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("failed to get movementdata pointer");
			}
		}
		
		return wurmOptions;
	}

	static void showUI()
	{
		if(Options.renderPicked.value() == false) {
			Options.renderPicked.set(true);
		}
		
		if(isHudVisible() == false) {
			getHud().setVisible(true);
		}
	}

	static void hideUI()
	{
		if(Options.renderPicked.value() == true) {
			Options.renderPicked.set(false);
		}
		
		if(isHudVisible() == true) {
			getHud().setVisible(false);
		}
	}
	
	/**
	 * Note! Do not call this in tick() or so, bad idea. Make 'render own body' configurable in the mod, instead. That way we just decide whether to inject or not.
	 * 
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private static boolean isHudVisible()
	{
		try {
			Field f = getHud().getClass().getDeclaredField("visible");
			f.setAccessible(true);
			return (boolean)f.get(getHud());

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return true;
	}


/*
	static private void patchParticleEffectLoader() throws NotFoundException, CannotCompileException
	{
        ClassPool classPool = HookManager.getInstance().getClassPool();

        CtClass theClass = classPool.get("com.wurmonline.client.console.WurmConsole");
        
		String str = "public void addParticleEffect(com.wurmonline.client.renderer.effects.CustomParticleEffect eff)"
        		+ "	{"
        		+ "		com.friya.wurmonline.client.hollywurm.WurmHelpers.tellCinematics(\"Adding effect...\");"
				+ "		if (testEffectList == null) {"
				+ "			testEffectList = new java.util.ArrayList();"
				+ "		}"
				+ "		testEffectList.add(eff);"
        		+ "		com.friya.wurmonline.client.hollywurm.WurmHelpers.tellCinematics(\"Added effect!\");"
        		+ "	}";
        CtMethod theMethod = CtNewMethod.make(str, theClass);
        theClass.addMethod(theMethod);
	}
*/
	static private void patchChatInput() throws NotFoundException, CannotCompileException
	{
		// wurmonline/client/renderer/gui/ChatManagerManager.java
		// 197     public void handleInput(final String message)

		HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.ChatManagerManager", "handleInput", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							if(Mod.cinematicsEnabled && Actions.handleChatInput((String)args[0])) {
								return null;
							}

							return method.invoke(proxy, args);
						}
                    };
                }
        	}
        );
		
	} // patchChatInput

	static private void setupQuitDialogInterception() throws NotFoundException, CannotCompileException
	{
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "showQuitConfirmationWindow", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							WurmClientBase.performShutdown();
							//return method.invoke(proxy, args);
							return null;
						}
                    };
                }
        	}
        );
        
	} // setupQuitDialogInterception

	static private void setupCloseTabInterception() throws NotFoundException, CannotCompileException
	{
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.ChatPanelComponent", "closeTab", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if( FriyasGuiProxy.getTabName((AbstractTab)args[0]).equals(":Cinematics")) {
								Mod.cinematicsEnabled = false;
								getHud().textMessage(":Event", 1f, 1f, 1f, "Completely disabled Friya's Cinematics. To re-enable it you have to restart your client.");
							}
							return method.invoke(proxy, args);
						}
                    };
                }
        	}
        );
        
	} // setupCloseTabInterception
	
	static private void setupConsoleInputInterception() throws NotFoundException, CannotCompileException
	{
//		ClassPool cp = HookManager.getInstance().getClassPool();
/*		
		String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
			CtPrimitiveType.longType,								// targets
			cp.get("java.lang.Long[]"),								// source
			cp.get("com.wurmonline.shared.constants.PlayerAction")	// action
			
			//HookManager.getInstance().getClassPool().get("java.lang.String")
			//HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
		});
*/		
        HookManager.getInstance().registerHook("com.wurmonline.client.console.WurmConsole", "handleDevInput", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							if(Actions.handleConsoleInput((String)args[0], (String[])args[1])) {
								return true;
							}
							return method.invoke(proxy, args);
						}
                    };
                }
        	}
        );
        
	} // setupConsoleInputInterception
	
/*
	static private void patchConsoleInput() throws NotFoundException, CannotCompileException
	{
		ClassPool classPool = HookManager.getInstance().getClassPool();
		
		CtClass ctWurmConsole = classPool.getCtClass("com.wurmonline.client.console.WurmConsole");
		ctWurmConsole.getMethod("handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z").insertBefore(
				  "if(com.friya.wurmonline.client.hollywurm.Mod.handleInput($1, $2)) {"
				+ "		return true;"
				+ "}"
		);
	}
*/
/*
	public static void debugActions(Object rootPopup, List<PlayerAction> actionList, String helpTopic, Map<String, Object> oldPopups, boolean grayedOut) 
	{
		if(actionList == null) {
			logger.info("actionList is null");
			return;
		}

		logger.info("---------------");
		for(PlayerAction a : actionList) {
			logger.info(
					  " " + a.getId()
					+ " " + a.getName()
					+ " " + a.getTargetMask()
					+ " " + a.isAtomic()
					+ " " + a.isInstant()
					+ " " + helpTopic
					+ " " + grayedOut
					+ " " + oldPopups.size()
			);
		}
	}
*/
	
	@SuppressWarnings("unused")
	static private void patchMenuItems() throws NotFoundException, CannotCompileException
	{
		//	$0 = this						$1									$2						$3										$4						$5
		// showPopupData(final WurmPopup rootPopup, final List<PlayerAction> actionList, final String helpTopic, final Map<String, WurmPopup> oldPopups, final boolean grayedOut)
		// EXAMINE = new PlayerAction((short)1, 65535, "Examine");
		// rootPopup.addButton(PlayerAction.EXAMINE, null, grayedOut);
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.client.renderer.gui.HeadsUpDisplay");
        CtMethod met = cls.getDeclaredMethod("showPopupData");
/*
        buildSubMenu(
	        java.util.Map,
	        java.lang.String,
	        com.wurmonline.shared.constants.PlayerAction,
	        int,
	        boolean
        ) not found in com.wurmonline.client.renderer.gui.HeadsUpDisplay
*/
        met.insertAfter(
       		  "{ "

        		+ "		$1.addSeparator();"
        		+ "		com.wurmonline.shared.constants.PlayerAction[] myActs = com.friya.wurmonline.client.hollywurm.Actions.getActions();"
        		+ "		for(int n = 0; n < myActs.length; n++) {"
        		+ "			$1.addButton(myActs[n], null, $5);"
        		+ "		}"

/*
        		+ "java.util.Iterator myIt = com.friya.wurmonline.client.hollywurm.Actions.getActionsIterator();	"
                + "while (myIt.hasNext()) {																				"
                + "    com.wurmonline.shared.constants.PlayerAction action = (com.wurmonline.shared.constants.PlayerAction)myIt.next();																"
                + "    com.wurmonline.client.renderer.gui.WurmPopup subMenu = null;																		"
                + "    if (action.getId() < 0) { 																		"
                + "        subMenu = this.buildSubMenu($4, action.getName(), myIt, -action.getId(), false);				"
                + "    }    																							"
                + "    $1.addButton(action, subMenu, false);														"
                + "}"
*/
       		+ "}");
/*
Iterator<PlayerAction> myIt = com.friya.wurmonline.client.hollywurm.Actions.getActionsIterator();	
while (myIt.hasNext()) {																				
PlayerAction action = myIt.next();																
WurmPopup subMenu = null;																		
if (action.getId() < 0) { 																		
subMenu = this.buildSubMenu($4, action.getName(), myIt, -action.getId(), false);				
}    									
rootPopup.addButton(action, subMenu, false);
*/	
	}
	
	static private void setupPopupDataInterception() throws NotFoundException, CannotCompileException
	{
		//									0									1						2										3						4
		// showPopupData(final WurmPopup rootPopup, final List<PlayerAction> actionList, final String helpTopic, final Map<String, WurmPopup> oldPopups, final boolean grayedOut)
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "showPopupData", null,
                new InvocationHandlerFactory() {
                    @Override
                    public InvocationHandler createInvocationHandler() {
                        return new InvocationHandler() {
    						@SuppressWarnings("unchecked")
							@Override
    						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    							method.invoke(proxy, args);
    							FriyasGuiProxy.addCinematicsSubMenu(getHud(), args[0], (Map<String, Object>)args[3], Actions.getActions());
    							return null;
    						}
                        };
                    }
        });
	}
	
	static private void patchNoBody() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.client.renderer.PlayerBodyRenderable");
        CtMethod met = cls.getDeclaredMethod("renderEquipment");
        met.insertBefore(
        		  "{"
        		+ "		if(true) { return; };"
        		+ "}");
	}

	static int worldTicks = 0;
	static public void worldTick(World w)
	{
		//logger.info("worldTick()");
		worldTicks++;

		if(Mod.useWorldTick) {
			ScriptRunner.gameTick();
		}
//		w.getCellRenderer().tick();
		w.getPlayer().gametick();
//		w.getHud().gameTick();
//		w.getGrassLeanBuffer().tick();
		
		if(worldTicks % 14400 == 0) {
			logger.info("SHOULD/will GC this tick!");
		}
		
		if(worldTicks % 1440 == 0) {
			logger.info("SHOULD/will do a Wurm TimeTick this tick!");
		}
	}
	static private void patchWorldTick() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.client.game.World");
        CtMethod met = cls.getDeclaredMethod("tick");
        met.insertBefore(
        		  "{"
        		+ "		if(true) {"
        		+ "			com.friya.wurmonline.client.hollywurm.WurmHelpers.worldTick(this);"
        		+ "			return;"
        		+ "		};"
        		+ "}");
	}

	static private void patchWorldRenderTick() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.client.renderer.WorldRender");
        CtMethod met = cls.getDeclaredMethod("render");
        met.insertBefore(
        		  "{"
        		+ "		com.friya.wurmonline.client.hollywurm.ScriptRunner.gameTick();"
        		+ "}");
	}
	
	
	static private void patchGameTick() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.client.game.PlayerObj");
        CtMethod met = cls.getDeclaredMethod("gametick");
        met.insertBefore(
        		  "{"
        		+ "		com.friya.wurmonline.client.hollywurm.ScriptRunner.gameTick();"
//        		+ "		if(true) {"
//        		+ "			this.movementChecker.fly(this.xPosUsed, this.yPosUsed, this.hPosUsed, this.xRotUsed, this.yRotUsed, (byte)0, this.layer);"
//        		+ "			this.setNewPlayerPosition(this.xPosUsed, this.yPosUsed, this.hPosUsed);"
//        		+ "			return 0.4f;"
//        		+ "		}"
        		+ "}");
        logger.info("Patched gametick()");
	}

	/**
	 * The goal is to kill the capping of Y rotation at 90/-90 when you look around
	 * 
	 * @throws NotFoundException
	 * @throws CannotCompileException
	 */
	static private void patchYRotCapRemoval() throws NotFoundException, CannotCompileException
	{

		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.client.game.PlayerObj");
        CtMethod met = cls.getDeclaredMethod("setNewPlayerPosition");
        
        met.instrument(new ExprEditor(){;
	        public void edit(FieldAccess f) throws CannotCompileException {
	        	//logger.info(f.getFieldName() + " " + f.isWriter() + " " + f.isReader());
	        	if(f.getFieldName().equals("yRotUsed") && f.isWriter()) {
	        		f.replace(""
	        				+ "yRotUsed = ($1 == -90 || $1 == 90 ? yRotUsed : $1);"
	        				//+ "yRotUsed = yRotUsed;"
	        			);
	        		logger.info("Replaced a yRotUsed...");
	        	}
	        	
	        }
	    });
        
        met = cls.getDeclaredMethod("turn");
        
        met.instrument(new ExprEditor(){;
	        public void edit(FieldAccess f) throws CannotCompileException {
	        	//logger.info(f.getFieldName() + " " + f.isWriter() + " " + f.isReader());
	        	if(f.getFieldName().equals("yRotUsed") && f.isWriter()) {
	        		f.replace(""
	        				+ "yRotUsed = ($1 == -90 || $1 == 90 ? yRotUsed : $1);"
	        				//+ "yRotUsed = yRotUsed;"
	        			);
	        		logger.info("Replaced a yRotUsed...");
	        	}
	        	
	        }
	    });
	}


	static private void patchRenderToggling() throws NotFoundException, CannotCompileException
	{
		Map<String, String[]> disableRenderers = new HashMap<String, String[]>() {
			private static final long serialVersionUID = 1L;
			{
				put("com/wurmonline/client/renderer/cell/CellRenderable.java",						new String[] { "render" });
				put("com/wurmonline/client/renderer/cell/SurfaceCell.java:", 						new String[] { "renderModelTrees", "renderTrees", "renderBillboardTrees", "renderSoftShadows"  });
				put("com/wurmonline/client/renderer/cell/TilesOverlay.java:", 						new String[] { "render" });
				put("com/wurmonline/client/renderer/cell/PlayerCellRenderable.java:", 				new String[] { "render" });
				put("com/wurmonline/client/renderer/cell/GroundItemCellRenderable.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/cell/LinkProtection.java:", 					new String[] { "render" });
				put("com/wurmonline/client/renderer/cell/MobileModelRenderable.java:", 				new String[] { "render" });
				put("com/wurmonline/client/renderer/cell/AttachedCellRenderable.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/cell/Cell.java:", 								new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/WaterRenderer.java:", 					new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/TerrainTexture.java:", 					new String[] { "renderOldTexture", "renderDirtyTextureTiles", "renderNewTexturePart", "renderNewTexture", "renderTransitionTiles", "renderNoTransitionTiles" });
				put("com/wurmonline/client/renderer/terrain/TerrainLod.java:", 						new String[] { "render" });
				// This enabled and we lose a "clear", so get trails... But, it still chops.
				//put("com/wurmonline/client/renderer/terrain/sky/SkyRenderer.java:", 				new String[] { "renderSkydome", "renderBurningSky", "renderRainbow", "renderSunHalo", "renderSun", "renderMoons", "renderGlare", "renderClouds" });
				put("com/wurmonline/client/renderer/terrain/sky/BasicCloudRenderer.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/weather/Weather.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/weather/RainOcclusion.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/weather/SnowRenderer.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/WaterTexture.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/TerrainRenderer.java:", 			new String[] { "renderTerrainTextures" });
				// Comment this out to get a bit of grass reference 
				put("com/wurmonline/client/renderer/terrain/decorator/DecorationChunk.java:", 			new String[] { "render" });
				// enable this and nothing will happen...
				//put("com/wurmonline/client/renderer/terrain/decorator/decorators/ModelDecorator.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/decorator/decorators/DirtDecorator.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/decorator/decorators/LawnGrassDecorator.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/decorator/decorators/LavaDecorator.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/decorator/decorators/PumpkinDecorator.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/decorator/decorators/CropDecorator.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/terrain/decorator/decorators/VegetationDecorator.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/cave/CaveRender.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/model/collada/StaticColladaModelData.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/model/collada/ColladaModelRender.java:", 			new String[] { "renderGPUSkinningTriangleList", "renderTriangleList", "renderModel", "renderSpecificMesh", "renderJoints" });
				put("com/wurmonline/client/renderer/model/ModelResourceWrapper.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/model/FailedModelData.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/model/dotxsi/DotXSIModelData.java:", 			new String[] { "render", "renderModel" });
				put("com/wurmonline/client/renderer/shadow/VarianceShadowMap.java:", 			new String[] { "renderShadowMap" });
				put("com/wurmonline/client/renderer/gui/TextureButton.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/gui/text/TextQuad.java:", 			new String[] { "render" });
				put("com/wurmonline/client/renderer/gui/PlonkComponent.java:", 			new String[] { "renderComponent" });
				put("com/wurmonline/client/renderer/gui/WurmImage.java:", 			new String[] { "renderComponent" });
				put("com/wurmonline/client/renderer/gui/PlonkLibraryWindow.java:", 			new String[] { "renderComponent" });
				put("com/wurmonline/client/renderer/gui/WurmComponent.java:", 			new String[] { "render", "renderComponent" });
				put("com/wurmonline/client/renderer/gui/Renderer.java:", 			new String[] { "renderSpyglassDistance", "renderCrosshair", "renderOnscreenMessageViewer", "renderHoverInfo", "renderPickData", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/gui/MissionBar.java:", 			new String[] { "renderComponent", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/ItemPlacer.java:", 			new String[] { "render", "renderOffscreen", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/FramePostProcessing.java:", 			new String[] { "renderVignette", "renderWorldTexture", "renderSSAO", "renderBloom", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/PlayerBodyRenderable.java:", 			new String[] { "renderNullEquipment", "renderPaperDoll", "renderHair", "renderEquipment", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/PostProcessRenderer.java:", 			new String[] { "renderBrightness", "renderBinocularsEffect", "renderDeadEffect", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
//xxx
				put("com/wurmonline/client/renderer/WorldRender.java:", 			new String[] { "renderPlayerShadow", "renderPlayer", "renderPlayerModel", "renderPlacingItem", "renderPickedItem", "renderReflection", "", "", "", "", "", "", "", "", "", "", "" });
//				put("com/wurmonline/client/renderer/mesh/MeshInstance.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
//				put("com/wurmonline/client/renderer/mesh/Mesh.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
//xxx
				put("com/wurmonline/client/renderer/effects/EffectRender.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/LightCorona.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/RiftSpawnEffect.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/RavineEffect.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/LightningBoltEffect.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/Effect.java:", 			new String[] { "render", "renderDistortion", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/TorchFlame.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/GlobalWarningEffect.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/IndentationEffect.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/LightBeamEffect.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/CustomParticleEffect.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/XmasLightsEffect.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/effects/WeaponTrailEffect.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/particlesystem/ParticleSystem.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/particles/Butterfly.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/particles/AlphaParticle.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/particles/Fish.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/particles/Leaf.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/particles/Bird.java:", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("com/wurmonline/client/renderer/particles/BatchParticleRenderer.java:", 			new String[] { "renderAlphaParticles", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });

				// untested below
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				put("", 			new String[] { "render", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" });
				
				
			}
		};
		
		ClassPool cp = HookManager.getInstance().getClassPool();
		for (Map.Entry<String, String[]> entry : disableRenderers.entrySet()) {
			String classStr = entry.getKey();
			
			if(classStr.length() == 0) {
				continue;
			}
			
			classStr = classStr.replace("/", ".").replace(".java", "").replace(":", "");
			
	        CtClass cls = cp.get(classStr);

	        for(String methodStr : entry.getValue()) {
	        	if(methodStr.length() == 0) {
	        		continue;
	        	}

	        	CtMethod met = cls.getDeclaredMethod(methodStr);
		        met.insertBefore(
		        		  "{"
		        		+ "		if(true) {"
		        		+ "			return;"
		        		+ "		}"
		        		+ "}");
	
		        logger.info("Disabled renderer: " + classStr + "." + methodStr);
	        }
		}

	}
	
	static private void patchFly() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.shared.util.MovementChecker");
        CtMethod met = cls.getDeclaredMethod("fly");
        met.insertBefore(
        		  "{"
        		+ "		if(true) {"
        		+ "			this.x = xTarget;"
        		+ "			this.y = yTarget;"
        		+ "			this.z = zTarget;"
        		+ "			this.layer = layerTarget;"
        		+ "			return"
        		+ "		}"
        		+ "}");
        logger.info("Patched fly()");
	}
	
	/**
	 * Remove rendering of character name in health bar if Cinematics is enabled.
	 * 
	 * @throws NotFoundException
	 * @throws CannotCompileException
	 */
	static private void patchNameInHealthBar() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.client.renderer.gui.HealthBarClassicRenderer");
        CtMethod met = cls.getDeclaredMethod("renderComponent");
        met.instrument(new ExprEditor(){;
	        public void edit(MethodCall m) throws CannotCompileException {
	            if (m.getMethodName().equals("isEmpty")) {
	                m.replace("$_ = com.friya.wurmonline.client.hollywurm.WurmHelpers.hideHealthBarTitle();");
	                return;
	            }

	            if (m.getMethodName().equals("getPlayerName")) {
	                m.replace("$_ = com.friya.wurmonline.client.hollywurm.WurmHelpers.getHealthBarPlayerName();");
	                return;
	            }
	        }
        });
        cls = cp.get("com.wurmonline.client.renderer.gui.HealthBarIronRenderer");
        met = cls.getDeclaredMethod("renderComponent");
        met.instrument(new ExprEditor(){;
	        public void edit(MethodCall m) throws CannotCompileException {
	            if (m.getMethodName().equals("isEmpty")) {
	                m.replace("$_ = com.friya.wurmonline.client.hollywurm.WurmHelpers.hideHealthBarTitle();");
	                return;
	            }
	
	            if (m.getMethodName().equals("getPlayerName")) {
	                m.replace("$_ = com.friya.wurmonline.client.hollywurm.WurmHelpers.getHealthBarPlayerName();");
	                return;
	            }
	        }
        });

/*
        cls = cp.get("com.wurmonline.client.renderer.gui.HealthBarIronRenderer");
        met = cls.getDeclaredMethod("renderComponent");
        met.instrument(new ExprEditor(){;
	        public void edit(FieldAccess m) throws CannotCompileException {
				if (m.getFieldName().equals("showName")) {
					m.replace("$_ = com.friya.wurmonline.client.hollywurm.WurmHelpers.showNameInHealthBar();");
					logger.info("Patched showName - iron");
				}
	        }
        });
*/
	}

	static public boolean hideHealthBarTitle()
	{
		return Mod.cinematicsEnabled;
	}
	
	static public String getHealthBarPlayerName()
	{
		return Mod.cinematicsEnabled ? "" : WurmHelpers.getPlayer().getPlayerName();
	}
	
	/**
	 * This is mostly to make sure no other mod is patching this, but am not sure it is needed!
	 * 
	 * @throws NotFoundException
	 * @throws CannotCompileException
	 */
	static private void patchWeather() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.client.renderer.terrain.weather.Weather");
        CtMethod met = cls.getDeclaredMethod("setWeather");
        met.instrument(new ExprEditor(){
        	
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("login")) {
                    m.replace("$_ = $proceed($1, $6, $3, $4, $5, $6);");
                    return;
                }
            }
        });
	}

	static private void setupHudRenderInterception() throws NotFoundException, CannotCompileException
	{
		//                            0                         1                      2                 3
		// beginRender(final float fraction, final boolean mouseAvailable, final int xMouse, final int yMouse)
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "beginRender", null,
                new InvocationHandlerFactory() {
                    @Override
                    public InvocationHandler createInvocationHandler() {
                        return new InvocationHandler() {
    						@Override
    						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    							if(Mod.useHudRendererTick) {
        							ScriptRunner.gameTick();
    							}
    							if((ScriptRunner.isRunning() && ScriptRunner.isBenchmarking()) || BenchmarkRenderer.isFading()) {
        							HeadsUpDisplay hud = (HeadsUpDisplay)proxy;
        							HeadsUpDisplay.scissor.reset(hud.getWidth(), hud.getHeight());
        							
        							getBenchmarkRenderer().beginRender((float)args[0], (boolean)args[1], (int)args[2], (int)args[3]);
        							return null;
    							}
    							
    							return method.invoke(proxy, args);
    						}
                        };
                    }
        });

        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "endRender", null,
                new InvocationHandlerFactory() {
                    @Override
                    public InvocationHandler createInvocationHandler() {
                        return new InvocationHandler() {
    						@Override
    						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

//    							if(ScriptRunner.isRunning() && ScriptRunner.isBenchmarking()) {
        							//HeadsUpDisplay hud = (HeadsUpDisplay)proxy;
        							getBenchmarkRenderer().endRender();
//       							return null;
///    							}
    							
    							return method.invoke(proxy, args);
    						}
                        };
                    }
        });
	}


	static private void setupHealthBarRenderInterception() throws NotFoundException, CannotCompileException
	{
//		ClassPool cp = HookManager.getInstance().getClassPool();
/*		
		String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
			CtPrimitiveType.longType,								// targets
			cp.get("java.lang.Long[]"),								// source
			cp.get("com.wurmonline.shared.constants.PlayerAction")	// action
			
			//HookManager.getInstance().getClassPool().get("java.lang.String")
			//HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
		});
*/		
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HealthBar", "renderComponent", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							if(Mod.cinematicsEnabled) {
								HealthBar healthBar = (HealthBar)proxy;
								Queue queue = (Queue)args[0];
						        int x = FriyasGuiProxy.getX(healthBar);
						        final int y = FriyasGuiProxy.getY(healthBar);
						        final TextFont text = FriyasGuiProxy.getText(healthBar);
						        
						        //x += text.getWidth(getPlayer().getPlayerName() + (FriyasGuiProxy.getNormalTitle(healthBar).isEmpty() ? "" : (" [" + FriyasGuiProxy.getNormalTitle(healthBar) + "]"))) + 8;
	
						        text.moveTo(x, y + text.getHeight());
						        
						        //text.paint(queue, healthBar.player.getPlayerName() + (healthBar.normalTitle.isEmpty() ? "" : (" [" + healthBar.normalTitle + "]")));
						        text.paint(queue, 
						        		"x:" + getPlayer().getPos().getTileX() 
						        		+ " y:" + getPlayer().getPos().getTileY() 
						        		+ " h:" + (int)(getPlayer().getPos().getH() * 10) 
						        		+ " " + getFacingDir(true).toUpperCase()
//						        		+ " v: " + getMovementData().getVelocity()
						        );
							}

							Object ret = method.invoke(proxy, args);
							return ret;
						}
                    };
                }
        	}
        );
        
	} // setupActionInterception

	static private String getFacingDir(boolean brief)
	{
		return formatBearing(getWorld().getPlayerRotX(), brief);
	}
	
	static private String formatBearing(double bearing, boolean brief)
	{
		if (bearing < 0 && bearing > -180) {
			bearing = 360.0 + bearing;
		}

		if (bearing > 360 || bearing < -180) {
			return "Unknown";
	    }

		return (brief ? directionsBrief : directions)[(int) Math.floor(((bearing + 11.25) % 360) / 22.5)];
	}
	

	static private void setupCreatureTracking() throws NotFoundException, CannotCompileException
	{
		// addTheCreature(
		//                  0                   1                     2                  3                    4              5
		//		final long id, final String modelName, final String lName, final byte materialId, final float x, final float y,
		//                  6               7                8                      9                 10                  11                  12
		//		final float h, final float rot, final byte layer, final boolean floating, final byte type, final boolean solid, final int soundSourceID,
		//                    13                      14                     15                 16                   17
		//		final byte kingdomId, final byte bloodKingdom, final byte rarity, final long bridgeId, final byte modtype
		// )
		//
		// WU 1.8:
		//                                                0                   1                     2                      3                    4
		// 200     public void addTheCreature(final long id, final String modelName, final String lName, final String hoverText, final byte materialId,
		//                                             5              6              7               8                9                     10
		// 201                             final float x, final float y, final float h, final float rot, final byte layer, final boolean floating,
		//                                             11                  12                   13                      14                     15
		// 202                             final byte type, final boolean solid, final int soundSourceID, final byte kingdomId, final byte bloodKingdom,
		//                                               16                 17                   18
		// 203                             final byte rarity, final long bridgeId, final byte modtype) {
		
        HookManager.getInstance().registerHook("com.wurmonline.client.comm.ServerConnectionListenerClass", "addTheCreature", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							float h;
							if((float)args[7] == -3000f) {
								h = getWorld().getNearTerrainBuffer().getHeight((int)((float)args[5] / 4f), (int)((float)args[6] / 4f));
							} else {
								h = (float)args[7];
							}
							setCreaturePosition((long)args[0], (float)args[5], (float)args[6], h, (float)args[8]);

							return method.invoke(proxy, args);
						}
                    };
                }
        	}
        );

        // wurmonline/client/renderer/cell/CreatureCellRenderable.java:
        //                                     0               1               2               3
        // public final void move(final float x1, final float y1, final float h1, final float rot1) 
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.cell.CreatureCellRenderable", "move", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Object ret = method.invoke(proxy, args);

							CreatureCellRenderable c = ((CreatureCellRenderable)proxy);

							float h;
							if((float)args[2] == -3000f) {
								h = getWorld().getNearTerrainBuffer().getHeight((int)(c.getXPos() / 4), (int)(c.getYPos() / 4));
							} else {
								h = c.getHPos();
							}

							setCreaturePosition(c.getId(), c.getXPos(), c.getYPos(), h, c.getRot());
							
							return ret;
						}
                    };
                }
        	}
        );

        // $0 = creature id
        HookManager.getInstance().registerHook("com.wurmonline.client.comm.ServerConnectionListenerClass", "deleteCreature", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							if(creatures.containsKey((long)args[0])) {
//								logger.info("Delete creature: "  + args[0]);
								creatures.remove((long)args[0]);
							}

							return method.invoke(proxy, args);
						}
                    };
                }
        	}
        );

	}
	

	static private void setCreaturePosition(long id, float x, float y, float h, float rot)
	{
		TweenPosition pp;

//		logger.info("Add/move creature: "  + id + " at " + x + " " + y + " " + h + " with rot " + rot);

		if(creatures.containsKey(id) == false) {
			pp = new TweenPosition();
			creatures.put(id, pp);
		} else {
			pp = creatures.get(id);
		}
		
		pp.setPos(x, y, h, rot);
	}
	

	static TweenPosition getCreaturePosition(long id)
	{
		if(Mod.EARLY_TESTING) {
			logger.info("EARLY_TESTING: Forcing creature position to hardcoded values");
			TweenPosition pp = new TweenPosition();

			pp.setPos(1200 * 4, 900 * 4, 40 / 10, 45f);
			return pp;
		}
		
		if(creatures.containsKey(id) == false) {
			return null;
		}
		
		return creatures.get(id);
	}
	
	
	static private void setupActionInterception() throws NotFoundException, CannotCompileException
	{
/*
		ClassPool cp = HookManager.getInstance().getClassPool();
		String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
			CtPrimitiveType.longType,								// targets
			cp.get("java.lang.Long[]"),								// source
			cp.get("com.wurmonline.shared.constants.PlayerAction")	// action
			
			//HookManager.getInstance().getClassPool().get("java.lang.String")
			//HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
		});
*/
		
        HookManager.getInstance().registerHook("com.wurmonline.client.comm.SimpleServerConnectionClass", "sendAction", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							if(Mod.cinematicsEnabled) {
								if(Actions.cmdIntercept(args) == true) {
									return null;
								}
							}
							
							Object ret = method.invoke(proxy, args);
							return ret;
						}
                    };
                }
        	}
        );
        
	} // setupActionInterception

/*
	1674 
	1675     private void reallyHandleCmdMessageMulticolored(final ByteBuffer bb) {
	1676         final String title = this.readStringByteLength(bb);
	1677         final int numSegments = bb.getShort();
	1678         final List<MulticolorLineSegment> segments = new ArrayList<MulticolorLineSegment>();
	1679         for (int i = 0; i < numSegments; ++i) {
	1680             segments.add(new MulticolorLineSegment(this.readStringShortLength(bb), (byte)(bb.get() & 0xFF)));
	1681         }
	1682         this.serverConnectionListener.textMessage(title, segments);
	1683     }
*/	

	
	static private void setupStartupInterception() throws NotFoundException, CannotCompileException
	{
/*
		ClassPool cp = HookManager.getInstance().getClassPool();
		String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
			HookManager.getInstance().getClassPool().get("java.lang.String")
		});
*/		
		HookManager.getInstance().registerHook("com.wurmonline.client.WurmClientBase", "startupMessage", null,
		    new InvocationHandlerFactory() {
		        @Override
		        public InvocationHandler createInvocationHandler() {
		            return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
							Object ret = method.invoke(proxy, args);
							
							if(!messagedOnStartup && Mod.cinematicsEnabled) {
								FriyasGuiProxy.fetchChatManagerAndPanel();
								
								messagedOnStartup = true;
								tellCinematics("Hello and welcome back to Friya's Cinematics!");
								tellCinematics("This tab is where you will get information about your scenes and scripts.");
								tellCinematics("If you don't need Cinematics right now, you can just close the tab using the little 'x' a slight bit to the right and up!");
		
								List<MulticolorLineSegment> segs = new ArrayList<MulticolorLineSegment>();
								segs.add(new MulticolorLineSegment("There may or may not be news at ", (byte)0));
								segs.add(new MulticolorLineSegment("http://filterbubbles.com/wurm-unlimited/cinematics/ ", (byte)8));
								segs.add(new MulticolorLineSegment("(you can right click this link to open it).", (byte)0));
								getHud().textMessage(":Cinematics", segs);
								tellCinematics("");

								if(Mod.autoLoadScript != null) {
									tellCinematics("Automatically loading script...", MessageType.IMPORTANT);
									Actions.handleChatInput("/loadscript \"" + Mod.autoLoadScript + "\"");
								}
								
								getMovementData();
								getMovementChecker();
								getBenchmarkRenderer();
								getGroundItems();
								Mod.outputRemoteWelcomeMessage();
								logger.info("Internal Wurm debug flags are: " + Options.debugsEnabled.value() + " " + Options.debugMode.value() + " " + Options.debugsEnabledThisFrame);
							}
							
							return ret;
						}
		            };
		        }
			}
		);
        
	} // setupStartupInterception


	static private void setupBmlInterception() throws NotFoundException, CannotCompileException
	{
/*
		ClassPool cp = HookManager.getInstance().getClassPool();
		String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
		//	HookManager.getInstance().getClassPool().get("java.lang.String")
		});
*/
		//                               0                  1                2                 3                     4                        5                   6              7              8               9
		// void showBMLForm(final short id, final String title, final int width, final int height, final boolean closeable, final boolean resizeable, final float r, final float g, final float b, final String bml)
        HookManager.getInstance().registerHook("com.wurmonline.client.comm.ServerConnectionListenerClass", "showBMLForm", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							if(((String)args[1]).equals("JSON:com.friya.wurmonline.client.hollywurm.WurmHelpers")) {
								//logger.info("This is JSON intended for us.");
								//logger.info("JSON in BML: " + args[9].toString());
								
								// Tell the server that we have capabilities to deal with Cinematics instructions.
								if(args[9].toString().equals("{ queryCinematicsCapable : 1 }")) {
									HashMap<String, String> opts = new HashMap<String, String>();
									opts.put("id", "-12346");
									opts.put("queryCinematicsCapable", "1");
									getWorld().getServerConnection().sendBmlResponse(opts, "none");
									logger.info("Telling server that we are capable of receiving cinematics instructions");
								} else {
									// Handle Cinematics instruction coming from server.
									Receiver.getInstance().handle(args[9].toString());
								}

								return null;
							} else {
								return method.invoke(proxy, args);
							}
						}
                    };
                }
        	}
        );
        
	} // setupStartupInterception
	
/*
Server will send us this if it is Cinematics capable:
[01:43:41 AM] INFO com.friya.wurmonline.client.hollywurm.WurmHelpers: This is JSON intended for us.
[01:43:41 AM] INFO com.friya.wurmonline.client.hollywurm.WurmHelpers: JSON in BML: { queryCinematicsCapable : 1 }
*/
	static private void setupBmlResponseInterception() throws NotFoundException, CannotCompileException
	{
/*
		ClassPool cp = HookManager.getInstance().getClassPool();
		String descriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] {
		//	HookManager.getInstance().getClassPool().get("java.lang.String")
		});
*/
		//                                                         0                    1
		// public void sendBmlResponse(final Map<String, String> values, final String buttonPressed)
        HookManager.getInstance().registerHook("com.wurmonline.client.comm.SimpleServerConnectionClass", "sendBmlResponse", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@SuppressWarnings("unchecked")
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							HashMap<String, String> values = (HashMap<String, String>)args[0];
							if(values.containsKey("id") && Actions.isBmlFormOfInterest(values.get("id"))) {
	
								logger.info("BML response (preventing send to server):");

								for (Map.Entry<String, String> entry : values.entrySet()) {
								    String key = entry.getKey();
								    String val = entry.getValue();
								    logger.info(key + " = " + val);
								}
								
								Actions.actOnBmlResponse(values);

								return null;
							} else {
								return method.invoke(proxy, args);
							}
							
						}
                    };
                }
        	}
        );
        
	} // setupStartupInterception

	private static void patchGameLoopDebug() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.client.WurmClientBase");
        CtMethod met = cls.getDeclaredMethod("runGameLoop");
        met.insertAfter(
        		  "{"
        		+ "		com.friya.wurmonline.client.hollywurm.WurmHelpers.fetchDebugTimers();"
        		+ "}"
        );
	}
	
	public static void fetchDebugTimers()
	{
		fetchDebugTimers(null, null);
	}

	static void fetchDebugTimers(List<DInfo> debugs, String indent)
	{
		if(indent == null) {
			indent = "";
			lastFrameStats = new StringBuffer("\nCurrent / Average / Maximum / Description\n");
		}

		if(debugs == null) {
			debugs = Debugs.getDebugs();
		}
		
		int i = 0;
        //final PriorityQueue<DInfo> queue = new PriorityQueue<DInfo>(debugs);
		while(i < debugs.size()) {
        //while (!queue.isEmpty()) {
            //final DInfo debug = queue.poll();
			DInfo debug = debugs.get(i);
			lastFrameStats.append(String.format("%s%4s  %4s  %4s  %s\n", indent, debug.getCurrent(), debug.getAverage(), debug.getMaximum(), debug.getDescription()));
			fetchDebugTimers(debug.getChildren(), indent + "\t");
            i++;
        }   
	}
	
	static void outputDebugTimers()
	{
		if(lastFrameStats != null) {
			logger.info(lastFrameStats.toString());
		} else {
			logger.info("No debug timers were set.");
		}
	}
}
