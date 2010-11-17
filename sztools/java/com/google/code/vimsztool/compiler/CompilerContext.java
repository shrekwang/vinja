package com.google.code.vimsztool.compiler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.code.vimsztool.omni.PackageInfo;
import com.google.code.vimsztool.util.UserLibConfig;

public class CompilerContext {

	private String encoding = "utf-8";
	private String srcVM = "1.6";
	private String dstVM = "1.6";
	private String outputDir;
	private String projectRoot;
	private ReflectAbleClassLoader loader;
	private List<String> srcLocations=new ArrayList<String>();
	private List<URL> classPathUrls = new ArrayList<URL>();
	
	public CompilerContext(String classPathXml) {
		
		File file=new File(classPathXml);
		String abpath = file.getAbsolutePath();
		if (file.isFile()) {
			this.projectRoot=new File(abpath).getParent();
			String jdeXmlPath = FilenameUtils.concat(projectRoot, ".jde");
			initJdeProperty(jdeXmlPath);
			initClassPath(classPathXml);
		} else {
			this.projectRoot = abpath;
			this.outputDir = abpath;
			this.srcLocations.add(abpath);
			try { classPathUrls.add(file.toURL()); } catch (Exception e) {}
		}
		initClassLoader();
	}
	
	private void initJdeProperty(String jdeXmlPath) {
		Map<String,String> prop = parseJdeXmlFile(jdeXmlPath);
		if (prop == null ) return;
		String target = prop.get("target");
		if (target != null && ! target.trim().equals("") )  {
			this.dstVM = target;
		}
		String source = prop.get("source");
		if (source != null && ! source.trim().equals("")) {
			this.srcVM = source;
		}
		String encoding = prop.get("utf-8");
		if (encoding != null && ! source.trim().equals("")) {
			this.encoding = encoding;
		}
			
	}
	
	private void addUserLib(String entryPath) {
		int userLibIndex = entryPath.indexOf("USER_LIBRARY");
		if (userLibIndex > -1 ) {
			String userLibName = entryPath.split("/")[1];
			List<String> jarPaths = UserLibConfig.getUsrLibArchives(userLibName);
			if (jarPaths == null ) return;
			for (String path : jarPaths) {
				File libFile = new File(path);
				try { classPathUrls.add(libFile.toURL()); } catch (Exception e) {}
			}
		}
	}
	
	private void initClassPath(String classPathXml) {
		List<ClassPathEntry> classPathEntries=parseClassPathXmlFile(classPathXml);

		for (ClassPathEntry entry : classPathEntries) {
			String entryAbsPath = FilenameUtils.concat(projectRoot, entry.path);
			if (entry.kind.equals("lib")) {
				File libFile = new File(entryAbsPath);
				try { classPathUrls.add(libFile.toURL()); } catch (Exception e) {}
			} else if (entry.kind.equals("src")) {
				srcLocations.add(entryAbsPath);
			} else if (entry.kind.equals("output")) {
				File libFile = new File(entryAbsPath);
				try { classPathUrls.add(libFile.toURL()); } catch (Exception e) {}
				outputDir=entryAbsPath;
			} else if (entry.kind.equals("con")) {
				addUserLib(entry.path);
			}
		}
		
	} 
	private void initClassLoader() {
		URL urlsA[] = new URL[classPathUrls.size()];
		classPathUrls.toArray(urlsA);
		loader = new ReflectAbleClassLoader(urlsA, this.getClass().getClassLoader());
		cachePackageInfo(urlsA,outputDir);
	}
	
	private void cachePackageInfo(URL[] urls,String outputDir) {
		PackageInfo.cacheSystemRtJar();
		for (URL url : urls) {
			String path = url.getPath();
			if (path.endsWith(".jar")) {
				PackageInfo.cacheClassNameInJar(path);
			}
		}
		PackageInfo.cacheClassNameInDist(outputDir);
	}
	
	private Map<String,String> parseJdeXmlFile(String jdeXmlPath ) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			Map<String,String> prop = new HashMap<String,String>();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(jdeXmlPath);
			Element docEle = dom.getDocumentElement();

			NodeList nl = docEle.getElementsByTagName("property");
			if(nl != null && nl.getLength() > 0) {
				for(int i = 0 ; i < nl.getLength();i++) {
					//get the employee element
					Element el = (Element)nl.item(i);
					String name = el.getAttribute("name");
					String value=el.getAttribute("value");
					prop.put(name, value);
				}
			}
			return prop;
		}catch(Exception e) { }
		return null;
	}
	
	private List<ClassPathEntry> parseClassPathXmlFile(String classPathXml){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			List<ClassPathEntry> classPathEntries=new ArrayList<ClassPathEntry>();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(classPathXml);
			Element docEle = dom.getDocumentElement();

			//get a nodelist of elements
			NodeList nl = docEle.getElementsByTagName("classpathentry");
			if(nl != null && nl.getLength() > 0) {
				for(int i = 0 ; i < nl.getLength();i++) {
					//get the employee element
					Element el = (Element)nl.item(i);
					ClassPathEntry entry=new ClassPathEntry();
					entry.kind=el.getAttribute("kind");
					entry.path=el.getAttribute("path");
					classPathEntries.add(entry);
				}
			}
			return classPathEntries;
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}
	
	public String getResourceDistFile(String source) {
		String locatedSrcRoot = null;
		for (String srcRoot : srcLocations ) {
			if (source.indexOf(srcRoot) > -1 ) {
				locatedSrcRoot=srcRoot;
				break;
			}
		}
		if (locatedSrcRoot == null) return null;
		String relativePath = source.substring(locatedSrcRoot.length()+1);
		String dstPath = FilenameUtils.concat(outputDir, relativePath);
		return dstPath;
	}


    public String locateSourcePath(String binClassName) {
        String relativePath = binClassName.replace(".",File.separator)+".java";
		for (String srcRoot : srcLocations ) {
            String absSrcPath = FilenameUtils.concat(srcRoot, relativePath);
            File file = new File(absSrcPath);
            if ( file.exists()) {
                return absSrcPath;
            }
		}
        return "";
    }
	public String buildClassName(String source) {
		
		String locatedSrcRoot = null;
		for (String srcRoot : srcLocations ) {
			if (source.indexOf(srcRoot) > -1 ) {
				locatedSrcRoot=srcRoot;
				break;
			}
		}
		if (locatedSrcRoot == null) return null;
		String packageName = source.substring(locatedSrcRoot.length()+1);
		String[] parts = packageName.split(Pattern.quote(File.separator));
		StringBuilder fullQualifiedName=new StringBuilder();
		for (int i=0; i<parts.length; i++) {
			if (i == parts.length-1) {
				String className=parts[i].substring(0, parts[i].indexOf("."));
				fullQualifiedName.append(className);
			} else {
				fullQualifiedName.append(parts[i]).append(".");
			}
		}
			
		return fullQualifiedName.toString();
	}
	
	public List<URL> getClassPathUrls() {
		return this.classPathUrls;
	}

	public ReflectAbleClassLoader getClassLoader() {
		return loader;
	}
	
	class ClassPathEntry {
		
		public String 	kind;
		public String  path;

	}
	
	
	/* =================================================================== */
	/* ====================== boring getter setter ======================= */
	/* =================================================================== */
	
	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getSrcVM() {
		return srcVM;
	}

	public void setSrcVM(String srcVM) {
		this.srcVM = srcVM;
	}

	public String getDstVM() {
		return dstVM;
	}

	public void setDstVM(String dstVM) {
		this.dstVM = dstVM;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public String getProjectRoot() {
		return projectRoot;
	}


}
