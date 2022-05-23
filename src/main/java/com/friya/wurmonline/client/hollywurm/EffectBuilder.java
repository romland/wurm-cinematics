package com.friya.wurmonline.client.hollywurm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.client.renderer.effects.CustomParticleEffect;
import com.wurmonline.client.renderer.effects.CustomParticleEffectInfo;
import com.wurmonline.client.renderer.effects.CustomParticleEffectXml;
import com.wurmonline.shared.xml.XmlNode;
import com.wurmonline.shared.xml.XmlParser;

public class EffectBuilder 
{
	private static Logger logger = Logger.getLogger(EffectBuilder.class.getName());
	
    private static ArrayList<CustomParticleEffect> effectList = new ArrayList<CustomParticleEffect>();

    // TODO:
	// 		allow inerting of models too: ModelResourceLoader
	// 		and textures: ResourceTextureLoader
	//
	//		...then allow server to transfer these things to clients
	//

	static void loadEffect(String effectName) 
    {
        Map<String, CustomParticleEffectInfo> customParticleEffecList = getCPEXml();

        XmlNode rootNode = null;
        try {
			//final InputStream xmlStream = WurmClientBase.getResourceManager().getResourceAsStream("particle.xml");
			final InputStream xmlStream = load("effects/" + effectName);
			rootNode = XmlParser.parse(xmlStream);
            
        } catch (Exception e) {
        	logger.severe(e.toString());
        	throw new RuntimeException("Failed to load (1) " + effectName);
        }

		try {
	        if (rootNode != null) {
	            final List<XmlNode> particleEffects = rootNode.getChildren();
	            for (final XmlNode particleEffect : particleEffects) {
	                final String name = particleEffect.getName();
	                CustomParticleEffectInfo customParticleEffect;
					customParticleEffect = loadEffectProperties(particleEffect);
	                customParticleEffecList.put(name, customParticleEffect);
	            }   
	        }   
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.severe(e.toString());
			logger.log(Level.SEVERE, "Failed to load (2) " + effectName, e);
		}
    }   


	private static InputStream load(String fn) throws FileNotFoundException
	{
		if(fn.endsWith(".txt") == false) {
			fn += ".txt";
		}

		if(!IO.isSecure(fn)) {
			return null;
		}

		File f = new File(IO.getNormalizedFilePath(fn));
		logger.info(fn);
		logger.info(f.toString());
		return new FileInputStream(f);
	}

	static CustomParticleEffectInfo loadEffectProperties(XmlNode node) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Method m = CustomParticleEffectXml.class.getDeclaredMethod("loadEffectProperties", XmlNode.class);
		m.setAccessible(true); //if security settings allow this
		Object o = m.invoke(null, node); //use null if the method is static

		return (CustomParticleEffectInfo)o;
	}
	
	static Map<String, CustomParticleEffectInfo> getCPEXml()
	{
		return CustomParticleEffectXml.getCustomParticleList();
	}

	static void addParticleEffect(CustomParticleEffect eff)
	{
	    if (effectList == null) {
	        effectList = new ArrayList<CustomParticleEffect>();
	    }
	    
	    effectList.add(eff);
	}

	static void clearParticleEffects()
	{
		for(CustomParticleEffect e : effectList) {
			if(e != null) {
				e.removeEffectInNextGameTick();
				//e.delete();
			}
		}
		effectList.clear();
	}
	
}
