package com.friya.wurmonline.client.hollywurm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

class IO
{
	private static Logger logger = Logger.getLogger(IO.class.getName());
	
	static private final String saveDir = System.getProperty("user.dir") + File.separator + "mods" + File.separator + "friyas-cinematics" + File.separator + "Cinematics";

	static private final String fileExt = ".txt";

	static boolean isSecure(String fileName)
	{
		String normalizedSaveDir = Paths.get(saveDir).normalize().toString();
		String pathAndFileName = getNormalizedFilePath(fileName);

		if(pathAndFileName.startsWith(normalizedSaveDir)) {
			return true;
		} else {
			return false;
		}
	}


	static String getNormalizedFilePath(String fileName)
	{
		return Paths.get(saveDir + File.separator + fileName).normalize().toString();
	}


	static boolean exists(String fileName)
	{
		if(fileName == null) {
			return false;
		}
		
		if(fileName.endsWith(fileExt) == false && fileName.endsWith(".mp3") == false) {
			fileName += fileExt;
		}

		if(!isSecure(fileName)) {
			return false;
		}
		
		if((new File(getNormalizedFilePath(fileName)).exists()) == false) {
			return false;
		}
		
		return true;
	}
	
	static String[] dir()
	{
		return dir(null);
	}

	static String[] dir(String subFolder)
	{
		if(subFolder != null) {
			subFolder = File.separator + subFolder;
		} else {
			subFolder = "";
		}
		
		ArrayList<String> tmp = new ArrayList<String>();
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(saveDir + subFolder), "*{" + fileExt + "}")) {
		    for (Path entry: stream) {
		    	tmp.add(entry.getFileName().toString());
		    }
		} catch (IOException x) {
		    System.err.println(x);
		    return null;
		}
		
		return tmp.toArray(new String[tmp.size()]);
	}

	static boolean makeDirs()
	{
		// Root dir
		if((new File(saveDir).exists()) == false) {
			if(!(new File(saveDir).mkdir())) {
				logger.log(Level.SEVERE, "Failed to create folder " + saveDir);
				return false;
			}
		}

		String newDir = saveDir + File.separator + "effects";
		if((new File(newDir).exists()) == false) {
			if(!(new File(newDir).mkdir())) {
				logger.log(Level.SEVERE, "Failed to create folder " + newDir);
				return false;
			}
		}

		newDir = saveDir + File.separator + "templates";
		if((new File(newDir).exists()) == false) {
			if(!(new File(newDir).mkdir())) {
				logger.log(Level.SEVERE, "Failed to create folder " + newDir);
				return false;
			}
		}

		return true;
	}

	static String getSafeFilename(String fn)
	{
		return fn.replaceAll("[^A-Za-z0-9]", "_");
	}
	
	static boolean save(String name, String contents, String subFolder)
	{
		if((new File(saveDir).exists()) == false) {
			if(!makeDirs()) {
				return false;
			}
		}
		
		if(name.endsWith(fileExt)) {
			name = name.substring(0, name.length() - fileExt.length());
		}

		String safeFileName = getSafeFilename(name);

		try (Writer writer = new BufferedWriter(
				new OutputStreamWriter(
					new FileOutputStream(saveDir + (subFolder == null ? "" : File.separator + subFolder) + File.separator + safeFileName  /*+ "-" + System.currentTimeMillis()*/ + fileExt), "utf-8")
				)
			) {
			writer.write(contents);

		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to save file " + name, e);
			return false;
		}

		return true;
	}

	static File getFile(String fileName)
	{
		if(fileName.endsWith(fileExt) == false && fileName.endsWith(".ogg") == false) {
			fileName += fileExt;
		}

		if(!isSecure(fileName)) {
			return null;
		}

		return new File(getNormalizedFilePath(fileName));
	}

	static String load(String fileName)
	{
		File file = getFile(fileName);
		
		StringBuffer sb = new StringBuffer();

		try (FileInputStream fis = new FileInputStream(file)) {
			int content;
			
			while ((content = fis.read()) != -1) {
				sb.append((char)content);
			}

		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to load " + fileName, e);
			return null;
		}
		
		if(sb.length() == 0) {
			return null;
		}

		return sb.toString();
	}


	static String getCinematicsDir()
	{
		return saveDir;
	}
}
