package com.google.code.vimsztool.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class UserLibConfig {
	
	private static Map<String,List<String>> usrLibCache=new HashMap<String,List<String>>();
	
	public static List<String> getUsrLibArchives(String name) {
		return usrLibCache.get(name);
	}
	
	public static void init(String xmlPath){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(xmlPath);
			Element docEle = dom.getDocumentElement();

			NodeList libNodeList = docEle.getElementsByTagName("library");
			
			if(libNodeList != null && libNodeList.getLength() > 0) {
				for(int i = 0 ; i < libNodeList.getLength();i++) {
					
					Element libNode = (Element)libNodeList.item(i);
					String libName = libNode.getAttribute("name");
					NodeList archiveNodeList = libNode.getElementsByTagName("archive");
					
					if(archiveNodeList != null && archiveNodeList.getLength() > 0) {
						List<String> urls =new ArrayList<String>();
						for(int j = 0 ; j < archiveNodeList.getLength();j++) {
							Element arvhiveNode = (Element)archiveNodeList.item(j);
							urls.add(arvhiveNode.getAttribute("path"));
						}
						usrLibCache.put(libName, urls);
					}
				}
			}
		}catch(Exception e) {
		}
	}
}
