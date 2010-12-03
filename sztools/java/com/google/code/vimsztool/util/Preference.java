package com.google.code.vimsztool.util;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;


public class Preference {
	
	private static Preference instance  = new Preference();
	private Properties prop  = null;
	private String sztoolHome = null;
	
	private Preference() {}
	
	public static final String JDE_COMPILE_ENCODING = "jde_compile_encoding" ; 
	public static final String JDE_COMPILE_IGNORE_WARING = "jde_compile_ignore_waring" ; 
	public static final String JDE_SRC_VM = "jde_src_vm" ; 
	public static final String JDE_DST_VM = "jde_dst_vm" ; 
	public static final String JDE_SERVER_PORT = "jde_server_port" ; 
	public static final String JDE_ECLIPSE_CONXML_PATH = "jde_eclipse_conxml_path" ; 
	public static final String DEFAULT_EXCLUDE_PATTERN = "default_exclude_pattern";

	
	public static Preference getInstance() {
		return instance;
	}
	
	public void init(String sztoolHome) {
		this.sztoolHome = sztoolHome;
		prop  = new Properties();
		String cfgPath = FilenameUtils.concat(sztoolHome, "share/conf/sztools.cfg");
		try {
			prop.load(new FileInputStream(cfgPath));
		} catch (Exception e) { 
		}
	}
	
	public String getSztoolHome() {
		return sztoolHome;
	}
	
	public String getValue(String key) {
		String value= prop.getProperty(key);
		return value == null ? "" : value;
	}
	
}
