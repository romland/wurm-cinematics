package com.wurmonline.client.game;

public class PlayerDummy
{
	private static PlayerDummy instance;

	public static PlayerDummy getInstance()
	{
		if(instance == null) {
			instance = new PlayerDummy();
		}

		return instance; 
	}
}
