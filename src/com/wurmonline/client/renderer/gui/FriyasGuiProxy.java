package com.wurmonline.client.renderer.gui;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import com.friya.wurmonline.client.hollywurm.WurmHelpers;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.shared.constants.PlayerAction;

public class FriyasGuiProxy 
{
//    @SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(FriyasGuiProxy.class.getName());

	private static ChatPanelComponent chatPanel;
	private static ChatManagerManager chatManager;

	public static int getX(WurmComponent c)
	{
		return c.x;
	}

	public static int getY(HealthBar healthBar) 
	{
		return healthBar.y;
	}

	public static TextFont getText(HealthBar healthBar) 
	{
		return healthBar.text;
	}

	public static String getNormalTitle(HealthBar healthBar) 
	{
		return healthBar.normalTitle;
	}
	
	public static String getTabName(AbstractTab tab)
	{
		return tab.name;
	}
	
	public static void fetchChatManagerAndPanel()
	{
		try {
			Field f = WurmHelpers.getHud().getClass().getDeclaredField("chatManager");
			f.setAccessible(true);
			chatManager = (ChatManagerManager)f.get(WurmHelpers.getHud());

			f = chatManager.getClass().getDeclaredField("chatChatPanel");
			f.setAccessible(true);
			chatPanel = (ChatPanelComponent)f.get(chatManager);

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("this will crash things later on (1)", e);
		}
	}
	
	public static void activateTab(String tabName)
	{
/*
		logger.info("------------ debug");
		logger.info("activateTab called()!");
		logger.info("chat panel name: " + chatPanel.getName());
		logger.info("active tab: " + chatPanel.getActiveTab().name);
		logger.info("cinematics tab: " + chatPanel.getTab(tabName));
*/
		if(chatPanel.getActiveTab() != null && chatPanel.getActiveTab().name.equals(tabName) == false && chatPanel.getTab(tabName) != null) {
			logger.info("Active tab was not cinematics,  activating it...");
			chatPanel.setActiveTab(chatPanel.getTab(tabName), true);
		}
	}

	public static void addCinematicsSubMenu(HeadsUpDisplay hud, Object rootPopupObject, Map<String, Object> oldPopups, PlayerAction[] actions)
	{
		if(oldPopups == null) {
			return;
		}
		
		WurmPopup rootPopup = (WurmPopup)rootPopupObject;
//		Map<String, WurmPopup> oldPopups = (Map<String, WurmPopup>)oldPopupsObject;
		
		Iterator<PlayerAction> it = new ArrayList<>(Arrays.asList(actions)).iterator();

		//final Iterator<PlayerAction> it = actionList.iterator();
        while (it.hasNext()) {
            final PlayerAction action = it.next();
            WurmPopup subMenu = null;
            if (action.getId() < 0) {
                subMenu = buildSubMenu(hud, oldPopups, action.getName(), it, -action.getId(), false);
            }
            rootPopup.addButton(action, subMenu, false);
        }

	}

    static private WurmPopup buildSubMenu(HeadsUpDisplay hud, final Map<String, Object> oldPopups, final String preString, final Iterator<PlayerAction> it, final int count, final boolean grayedOut)
    {
        final WurmPopup popup = new WurmPopup(preString);
        final WurmPopup oldPopup = (WurmPopup)oldPopups.get(popup.id);
        
        if (oldPopup != null) {
            hud.showPopupComponent(popup);
            popup.x = oldPopup.x;
            popup.y = oldPopup.y;
        }
        
        for (int i = 0; i < count; ++i) {
            final PlayerAction action = it.hasNext() ? it.next() : null;
            if (action != null) {
                WurmPopup subMenu = null;
                if (action.getId() < 0) {
                    subMenu = buildSubMenu(hud, oldPopups, preString + "->" + action.getName(), it, -action.getId(), grayedOut);
                }
                popup.addButton(action, subMenu, grayedOut);
            }
        }
        
        return popup;
    }
	
}
