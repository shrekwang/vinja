package com.google.code.vimsztool.util;

import java.io.FileInputStream;
import java.util.Properties;


public class Preference {
	
	private static Preference instance  = new Preference();
	private Properties prop  = null;
	
	private Preference() {}
	
	public static Preference getInstance() {
		return instance;
	}
	
	public void init(String cfgPath) {
		prop  = new Properties();
		try {
			prop.load(new FileInputStream(""));
		} catch (Exception e) { 
		}
	}
	
	public String getConfigValue(String key) {
		return prop.getProperty(key);
	}

}
