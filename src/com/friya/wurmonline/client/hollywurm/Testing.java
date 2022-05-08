package com.friya.wurmonline.client.hollywurm;
/*
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
*/
public class Testing 
{
/*
	private static Logger logger = Logger.getLogger(Testing.class.getName());

	// wurmclient_source_from_cfr/class/FZOk5L6Gfy.java
    static byte[] f1()
    {
    	logger.info("TESTING!");

    	ByteArrayOutputStream byteArrayOutputStream;
        byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        List list = new ArrayList();
        getClassLoaders(list, Testing.class.getClassLoader());
        Iterator it = list.iterator();
        while (it.hasNext()) {
            ClassLoader clsLoader = (ClassLoader)it.next();
            try {
                logger.info("cls: " + clsLoader.getClass().getName());
                
                zipOutputStream.putNextEntry(new ZipEntry(clsLoader.getClass().getName()));
                Field object2 = ClassLoader.class.getDeclaredField("classes");
                object2.setAccessible(true);
                list = (List)object2.get(object2);
                Iterator it2 = list.iterator();
                while (it2.hasNext()) {
                    String cls = it2.next().toString();

                    if (cls.contains("com.wurmonline.client"))
                    	continue;
                    
                    zipOutputStream.write(cls.getBytes());
                    zipOutputStream.write(";".getBytes());
                }   
                zipOutputStream.closeEntry();
            }   
            catch (Exception exception) {}
        }   
        try {
            zipOutputStream.close();
            byteArrayOutputStream.close();
        }   
        catch (IOException iOException) {}
        return byteArrayOutputStream.toByteArray();
    }

    static private void getClassLoaders(List list, ClassLoader classLoader)
    {
        do {
            list.add(classLoader);
            classLoader = classLoader.getParent();
        } while (classLoader != null);
    }
*/
}
