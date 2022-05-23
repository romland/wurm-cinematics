package com.friya.wurmonline.client.hollywurm;

import java.util.logging.Logger;

import com.friya.spectator.communicator.EventInformation;
import com.friya.spectator.communicator.JsonReceiver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Receiver implements JsonReceiver 
{
	private static Logger logger = Logger.getLogger(Receiver.class.getName());
	private static Receiver instance = null;

	public static Receiver getInstance()
	{
		if(instance == null) {
			instance = new Receiver();
		}

		return instance; 
	}

	// com.friya.wurmonline.client.hollywurm.spectator.Receiver
	public boolean handle(String json)
	{
		if(Mod.acceptServerEvents == false) {
			logger.info("Ignoring event, acceptServerEvents is false.");
			return false;
		}

		Gson gson = new GsonBuilder().create();
        
        EventInformation e = gson.fromJson(json, EventInformation.class);

        logger.info("Event: " + e.toString());

        ServerEventHandler.getInstance().addEvent(e);
        return true;
	}
}
