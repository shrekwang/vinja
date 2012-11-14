package com.google.code.vimsztool.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.code.vimsztool.debug.BreakpointManager;
import com.google.code.vimsztool.omni.ClassInfo;
import com.google.code.vimsztool.omni.ClassMetaInfoManager;
import com.google.code.vimsztool.omni.PackageInfo;
import com.google.code.vimsztool.util.JdeLogger;
import com.google.code.vimsztool.util.Preference;
import com.google.code.vimsztool.util.UserLibConfig;
import com.google.code.vimsztool.util.VjdeUtil;

public class CompilerContext {
	private static JdeLogger log = JdeLogger.getLogger("CompilerContext");
	private String encoding = null;
	private String srcVM = null;
	private String dstVM = null;
	private String outputDir;
	private String projectRoot;
	private ReflectAbleClassLoader loader;
	private List<String> srcLocations=new ArrayList<String>();
	private List<String> libSrcLocations = new ArrayList<String>();
	
	private List<String> extSrcLocations=new ArrayList<String>();
	private List<String> extOutputDirs = new ArrayList<String>();
	
	private List<URL> classPathUrls = new ArrayList<URL>();
	private Preference pref = Preference.getInstance();
	private PackageInfo packageInfo = new PackageInfo();
	private ClassMetaInfoManager classMetaInfoManager = null;
	
	private String lastSearchedRtlName = "";
	private String lastSearchResult = "";
	
	private boolean flatProject = false;
	
	public static CompilerContext load(String classPathXml) {
		return new CompilerContext(classPathXml);
	}
	
	public CompilerContext(String classPathXml) {
		
		encoding = pref.getValue(Preference.JDE_COMPILE_ENCODING);
		srcVM = pref.getValue(Preference.JDE_SRC_VM);
		dstVM = pref.getValue(Preference.JDE_DST_VM);
		
		File file=new File(classPathXml);
		String abpath = file.getAbsolutePath();
		if (file.isFile()) {
			this.projectRoot=new File(abpath).getParent();
			String jdeXmlPath = FilenameUtils.concat(projectRoot, ".jde");
			File jdeXmlFile = new File(jdeXmlPath);
			if (jdeXmlFile.exists() && jdeXmlFile.canRead()) {
				initJdeProperty(jdeXmlPath);
			}
			initClassPath(classPathXml);
		} else {
			this.setFlatProject(true);
			this.projectRoot = abpath;
			this.outputDir = abpath;
			this.srcLocations.add(abpath);
			try { classPathUrls.add(file.toURI().toURL()); } catch (Exception e) {}
		}
		this.classMetaInfoManager = new ClassMetaInfoManager(this);
		initClassLoader();
	}
	
	public String[] getAllSourceFiles() {
		List<String> names = new ArrayList<String>();
		for (String srcRoot : srcLocations ) {
            File dir = new File(srcRoot);
            if (dir.isFile()) continue;
            appendSrcFileToList(names, dir);
		}
		String[] result = new String[names.size()];
		for (int i=0; i<names.size(); i++) {
			result[i] = names.get(i);
		}
		return result;
	}
	
	private void appendSrcFileToList(List<String> names, File dir) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				appendSrcFileToList(names, file);
			} else {
				if (!file.getName().endsWith(".java"))
					continue;
				names.add(file.getAbsolutePath());
			}
		}

	}
	
	public void refreshClassInfo(List<String> classNames) {
		
		if (loader == null) return;
		URL[] urls = loader.getURLs();
		loader = new ReflectAbleClassLoader(urls, this.getClass().getClassLoader());
		BreakpointManager bpmgr = BreakpointManager.getInstance();
		
		for (String className : classNames ) {
			packageInfo.addClassNameToCache(className);
			packageInfo.addClasstoDstClass(className);
			String classPath = getOutputDir() + "/" + className.replace('.', '/') + ".class";
			File outFile = new File(classPath);
			classMetaInfoManager.loadSingleMetaInfo(outFile);
			bpmgr.verifyBreakpoint(className);
		}
		classMetaInfoManager.constructAllSubNames();
		
		//refresh class info in other CompilerContext
		List<CompilerContext> ctxs = CompilerContextManager.getInstnace().getAllLoadedContext();
		for (CompilerContext ctx : ctxs) {
			if (ctx.getExtOutputDirs().contains(this.getOutputDir())) {
				ctx.refreshClassInfo(classNames);
			}
		}	}
	
	
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
		String encoding = prop.get("encoding");
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
				try { classPathUrls.add(libFile.toURI().toURL()); } catch (Exception e) {
					String errorMsg = VjdeUtil.getExceptionValue(e);
		    		log.warn(errorMsg);
				}
			}
		}
	}
	
	private void initClassPath(String classPathXml) {
		List<ClassPathEntry> classPathEntries=parseClassPathXmlFile(classPathXml);
		
		 String jdkSrc = FilenameUtils.concat(System.getenv("JAVA_HOME") , "src.zip");
		 if (jdkSrc != null ) {
			 File jdkSrcFile = new File(jdkSrc);
			 if (jdkSrcFile.exists()) {
				 libSrcLocations.add(jdkSrc);
			 }
		 }

		for (ClassPathEntry entry : classPathEntries) {
			String entryAbsPath = FilenameUtils.concat(projectRoot, entry.path);
			if (entry.kind.equals("lib")) {
				File libFile = new File(entryAbsPath);
				try { classPathUrls.add(libFile.toURI().toURL()); } catch (Exception e) {}
				if (entry.sourcepath !=null) {
					libSrcLocations.add(entry.sourcepath);
				}
			} else if (entry.kind.equals("src")) {
				//if path startswith "/", it's another eclipse project 
				if (entry.path.startsWith("/")) {
					parseDependProjectClassXml(entry.path.substring(1));
				} else {
					srcLocations.add(entryAbsPath);
				}
			} else if (entry.kind.equals("output")) {
				File libFile = new File(entryAbsPath);
				//output path should be searched first in classpath
				try { classPathUrls.add(0, libFile.toURI().toURL()); } catch (Exception e) {}
				if (entryAbsPath.endsWith("/") || entryAbsPath.endsWith("\\")) {
					entryAbsPath = entryAbsPath.substring(0, entryAbsPath.length()-1);
				}
				outputDir=entryAbsPath;
			} else if (entry.kind.equals("con")) {
				addUserLib(entry.path);
			} else if (entry.kind.equals("var")) {
				String entryPath = entry.path;
				String sourcePath = entry.sourcepath;
				String libVarName = entryPath ;
				String srcVarName = sourcePath;
				if (entryPath.indexOf("/") > 0) {
					libVarName = entryPath.substring(0, entryPath.indexOf("/"));
				}
				if (sourcePath.startsWith("/")) {
					sourcePath = sourcePath.substring(1);
				}
				if (sourcePath.indexOf("/")>0) {
					srcVarName = sourcePath.substring(0, sourcePath.indexOf("/"));
				}
				String varValue = VarsConfiger.getVarValue(libVarName);
				String srcVarValue = VarsConfiger.getVarValue(srcVarName);
				if (varValue == null  ) {
					log.info(libVarName + " is not properly configured in vars.txt");
					continue;
				}
				if (varValue !=null) entryPath = entryPath.replace(libVarName, varValue);
				if (srcVarValue !=null) sourcePath = sourcePath.replace(srcVarName, srcVarValue);
				
				File libFile = new File(entryPath);
				if (!libFile.exists())  {
					log.info(entryPath + " in classpath not exists.");
					continue;
				}
				try { classPathUrls.add(libFile.toURI().toURL()); } catch (Exception e) {}
				if (sourcePath !=null && !sourcePath.equals("")) {
					libSrcLocations.add(sourcePath);
				}
			}
		}
	} 
	
	private void parseDependProjectClassXml(String projectName) {
		String extProjectPath = ProjectLocationConf.getProjectLocation(projectName);
		if (extProjectPath == null ) {
			log.warn("the file path configured for project " + projectName + " not exits!. please check project.cfg");
			return;
		}
		
		String classPathXml = FilenameUtils.concat(extProjectPath, ".classpath");
		File file = new File(classPathXml);
		if (! file.exists()) {
			log.warn("the .classpath file configured for project " + projectName + " not exits!. please check project.cfg");
			return;
		}
		
		List<ClassPathEntry> classPathEntries=parseClassPathXmlFile(classPathXml);

		for (ClassPathEntry entry : classPathEntries) {
			String entryAbsPath = FilenameUtils.concat(extProjectPath, entry.path);
			if (entry.kind.equals("src")) {
				if (! entry.path.startsWith("/")) {
					extSrcLocations.add(entryAbsPath);
				}
			} else if (entry.kind.equals("output")) {
				File libFile = new File(entryAbsPath);
				//add external project output dir to second search place in class path
				try { classPathUrls.add(1, libFile.toURI().toURL()); } catch (Exception e) {}
				extOutputDirs.add(libFile.getAbsolutePath());
			} 
		}
	}
	
	private void initClassLoader() {
		URL urlsA[] = new URL[classPathUrls.size()];
		classPathUrls.toArray(urlsA);
		loader = new ReflectAbleClassLoader(urlsA, this.getClass().getClassLoader());
		if (!this.isFlatProject()) {
			cachePackageInfo(urlsA,outputDir);
			classMetaInfoManager.cacheAllInfo(outputDir);
		} else {
			cacheFlatProjectPackageInfo(outputDir);
		}
	}
	
	private void cacheFlatProjectPackageInfo(String dir) {
		packageInfo.cacheSystemRtJar();
		packageInfo.cacheClassNameInDist(outputDir,false);
	}
	
	private void cachePackageInfo(URL[] urls,String outputDir) {
		packageInfo.cacheSystemRtJar();
		for (URL url : urls) {
			String path = url.getPath();
			if (path.endsWith(".jar")) {
				packageInfo.cacheClassNameInJar(path);
			}
		}
		packageInfo.cacheClassNameInDist(outputDir,true);
		for (String dirPath : extOutputDirs) {
			packageInfo.cacheClassNameInDist(dirPath,true);
		}
	}
	
	private Map<String,String> parseJdeXmlFile(String jdeXmlPath ) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			Map<String,String> prop = new HashMap<String,String>();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(new File(jdeXmlPath));
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
		}catch(Throwable e) {
			String errorMsg = VjdeUtil.getExceptionValue(e);
    		log.warn(errorMsg);
		}
		return null;
	}
	
	private List<ClassPathEntry> parseClassPathXmlFile(String classPathXml){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			List<ClassPathEntry> classPathEntries=new ArrayList<ClassPathEntry>();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(new File(classPathXml));
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
					entry.sourcepath =el.getAttribute("sourcepath");
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
		
		if (source == null) return null;
		
		String packageName = "";
		if (source.startsWith("jar:")) {
			packageName = source.substring(source.lastIndexOf("!")+1);
		} else {
			String locatedSrcRoot = null;
			for (String srcRoot : srcLocations ) {
				if (source.indexOf(srcRoot) > -1 ) {
					locatedSrcRoot=srcRoot;
					break;
				}
			}
			if (locatedSrcRoot == null) return null;
			packageName = source.substring(locatedSrcRoot.length()+1);
		}
		
		String[] parts = packageName.split("\\\\|/");
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
		public String  sourcepath;

	}
	
	/**
	 * only search in source dir of current project
	 * @param rtlPathName
	 * @return
	 */
	public String findSourceFileInSrcPath(String rtlPathName) {
		rtlPathName = rtlPathName.replace("\\", "/");
		for (String srcLoc : srcLocations) {
			String absPath = FilenameUtils.concat(srcLoc, rtlPathName);
			File file = new File(absPath) ;
			if (file.exists()) {
				return absPath;
			}
		}
		return null;
	}
	
	public String findSourceClass(String className) {
		if (className == null) return "None";
		ClassInfo classInfo = classMetaInfoManager.getMetaInfo(className);
		String rtlPathName = className.replace(".", "/") + ".java";
		if (classInfo!=null && classInfo.getSourceName() !=null ) {
			int dotIndex = className.lastIndexOf(".");
			String packagePath = "";
			if (dotIndex > -1) {
				packagePath = className.substring(0,dotIndex+1).replace(".", "/");  
			} 
			rtlPathName = packagePath + classInfo.getSourceName();
		}
		return findSourceFile(rtlPathName);
	}
	
	/**
	 * search in source dir and jar file and other locations .
	 * @param rtlPathName
	 * @return
	 */
	public String findSourceFile(String rtlPathName) {
		
		rtlPathName = rtlPathName.replace("\\", "/");
		if (lastSearchedRtlName.equals(rtlPathName)) {
			return lastSearchResult;
		}
		
		lastSearchedRtlName = rtlPathName;
		
		String className = FilenameUtils.getBaseName(rtlPathName);
		String tmpDirPath =FilenameUtils.concat(projectRoot,".tmp");
		File tmpDirFile = new File(tmpDirPath);
		if (!tmpDirFile.exists()) {
			boolean suc = tmpDirFile.mkdirs();
			if (!suc) return "None";
		}
		
		String tmpPath =FilenameUtils.concat(tmpDirPath,className+".class");
		
		for (String srcLoc : srcLocations) {
			String absPath = FilenameUtils.concat(srcLoc, rtlPathName);
			File file = new File(absPath) ;
			if (file.exists()) {
				lastSearchResult = absPath;
				return absPath;
			}
		}
		
		//also search in dependent project's source location
		for (String srcLoc : extSrcLocations ) {
			String absPath = FilenameUtils.concat(srcLoc, rtlPathName);
			File file = new File(absPath) ;
			if (file.exists()) {
				lastSearchResult = absPath;
				return absPath;
			}
		}
		if (rtlPathName.indexOf("/") < 0) {
			rtlPathName = "java/lang/"+rtlPathName;
		}
		for (String libSrcLoc : libSrcLocations) {
			if (libSrcLoc.endsWith(".jar") || libSrcLoc.endsWith(".zip")) {
				if ( hasEntry(libSrcLoc, rtlPathName)) {
					//extractContentToTemp(libSrcLoc,rtlPathName,tmpPath);
					tmpPath = "jar://" + libSrcLoc + "!" +rtlPathName;
					lastSearchResult = tmpPath;
					return tmpPath;
	            }
			} else {
				String absPath = FilenameUtils.concat(libSrcLoc, rtlPathName);
				File file = new File(absPath) ;
				if (file.exists()) {
					lastSearchResult = absPath;
					return absPath;
				}
			}
		}
		
		lastSearchResult = "None";
		return "None";
	}
	
	public static boolean hasEntry(String zipFileName, String rtlPath) {
		try {
			ZipFile zipFile = null;
			zipFile = new ZipFile(zipFileName);
			ZipEntry zipEntry = zipFile.getEntry(rtlPath);
			zipFile.close();
			if (zipEntry == null)
				return false;
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static void extractContentToTemp(String zipFileName, String rtlPath,String tmpPath) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(zipFileName);
			ZipEntry zipEntry = zipFile.getEntry(rtlPath);
			File f = new File(tmpPath);
			f.createNewFile();
			InputStream in = zipFile.getInputStream(zipEntry);
			FileOutputStream out = new FileOutputStream(f);

			byte[] by = new byte[1024];
			int c;
			while ((c = in.read(by)) != -1) {
				out.write(by, 0, c);
			}
			out.close();
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (zipFile != null) {
				try { zipFile.close(); } catch (Exception e) {}
			}
		}

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

	public PackageInfo getPackageInfo() {
		return packageInfo;
	}
	
	public ClassMetaInfoManager getClassMetaInfoManager() {
		return classMetaInfoManager;
	}

	public boolean isFlatProject() {
		return flatProject;
	}

	public void setFlatProject(boolean flatProject) {
		this.flatProject = flatProject;
	}

	public List<String> getExtOutputDirs() {
		return extOutputDirs;
	}

	public void setExtOutputDirs(List<String> extOutputDirs) {
		this.extOutputDirs = extOutputDirs;
	}


}
