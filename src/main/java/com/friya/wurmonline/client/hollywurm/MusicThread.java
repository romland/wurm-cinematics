package com.friya.wurmonline.client.hollywurm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;


class MusicThread extends Thread
{
	static AdvancedPlayer player = null;
	static private String fileName = null;
	static private int startAtMillisecond = 0;
	
	public MusicThread(String fileName, int startAtMillisecond)
	{
		MusicThread.fileName = fileName;
		MusicThread.startAtMillisecond = startAtMillisecond;
	}
	
    public void run()
    {
		try {
			FileInputStream fileInputStream = new FileInputStream(IO.getCinematicsDir() + File.separator + "audio" + File.separator + fileName);
			player = new AdvancedPlayer(fileInputStream);

			// A frame lasts 26millisecs. 38.4615 frames / second
			//int startFrame = (int)(38.4615f * seconds);
			int startFrame = (int)(startAtMillisecond / 26f);
			player.play(startFrame, Integer.MAX_VALUE);

		} catch(FileNotFoundException e) {
		    e.printStackTrace();
		} catch(JavaLayerException e) {
		    e.printStackTrace();
		}
    } 
    
	public void terminate()
	{
		player.close();
	}
}
