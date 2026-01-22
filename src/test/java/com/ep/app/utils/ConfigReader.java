package com.ep.app.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
	
	 public static String getConfigValue(String key) {
	        try (FileInputStream fis = new FileInputStream("src/test/resources/runConfig.properties")) {
	            Properties props = new Properties();
	            props.load(fis);
	            return props.getProperty(key);
	        } catch (IOException e) {
	            System.err.println("Could not read config file, using default: ");
	           
	        }
			return key;
	    }

}