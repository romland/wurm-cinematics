package com.friya.wurmonline.client.hollywurm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

public class UpdateChecker 
{
	private static Logger logger = Logger.getLogger(UpdateChecker.class.getName());

	public UpdateChecker() 
	{
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					getUrl("http://www.filterbubbles.com/wurm-unlimited/cinematics/latest_version.txt");

				} catch (IOException e) {
					logger.severe("failed to check for updates");
				} 
			}
		});

		t.start();
	}

	private boolean getUrl(String path) throws IOException
	{
		URL url = new URL(path);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		String inputLine;

		while ((inputLine = in.readLine()) != null) {
			String[] parts = inputLine.split("=");
			if(parts.length != 2) {
				continue;
			}
			
			Mod.softwareUpdateInfo.put(parts[0], parts[1]);
		}

		return true;
	}
}
