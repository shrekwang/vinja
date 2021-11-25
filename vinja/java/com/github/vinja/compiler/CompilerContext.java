package com.github.vinja.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

import com.github.vinja.debug.BreakpointManager;
import com.github.vinja.debug.Debugger;
import com.github.vinja.omni.ClassInfo;
import com.github.vinja.omni.ClassMetaInfoManager;
import com.github.vinja.omni.PackageInfo;
import com.github.vinja.util.JdeLogger;
import com.github.vinja.util.Preference;
import com.github.vinja.util.UserLibConfig;
import com.github.vinja.util.VjdeUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class CompilerContext {
	private static JdeLogger log = JdeLogger.getLogger("CompilerContext");
	private String encoding = null;
	private String srcVM = null;
	private String dstVM = null;
	private String outputDir;
	private String projectRoot;
	private ReflectAbleClassLoader loader;
	private List<String> srcLocations=new ArrayList<String>();
	private List<String> libSrcLocations = new CopyOnWriteArrayList<String>();
	
	private List<String> extSrcLocations=new ArrayList<String>();
	private List<String> extOutputDirs = new ArrayList<String>();

	private List<ClassPathEntry> classPathEntries = null ;
	
	private List<URL> classPathUrls = new ArrayList<URL>();
	private List<String> fsClassPathUrls = new ArrayList<String>();
	private Preference pref = Preference.getInstance();
	private PackageInfo packageInfo = new PackageInfo();
	private ClassMetaInfoManager classMetaInfoManager = null;
	
	private String lastSearchedRtlName = "";
	private String lastSearchResult = "";
	
    private boolean mvnProject = false;
	private boolean flatProject = false;
	private boolean classInfoCached = false;
	
	private Map<String,String> reactorProjectMap = new HashMap<String,String>();

    private DecompileHorse decompileHorse = DecompileHorse.getInstance();
	
	private enum ResourceType { Java, Other };
	
	Cache<String, String> clsSourcePathCache = CacheBuilder.newBuilder().maximumSize(3000).build(); 
	Cache<String, String> clsBinPathCache =    CacheBuilder.newBuilder().maximumSize(5000).build(); 
	
	private Map<String,String> testSrcLocationMap = new HashMap<String,String>();
	private Map<String,String> dstToSrcMap = new HashMap<String,String>();
	
	private static ExecutorService backJobs =  new ThreadPoolExecutor(8, 16,
                      10, TimeUnit.SECONDS,
                      new LinkedBlockingQueue<Runnable>(),
                      new ThreadFactoryBuilder().setNameFormat("compileCtx-thread-%d").build());
	
	public static CompilerContext load(String classPathXml) {
		return new CompilerContext(classPathXml);
	}
	
	public CompilerContext(String classPathXml) {
		this(classPathXml, true);
	}
	
	public CompilerContext(String classPathXml, boolean cacheClassInfo) {
		
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
			addToClassPaths(file, -1);
			String jdkSrc = FilenameUtils.concat(System.getenv("JAVA_HOME") , "src.zip");
			if (jdkSrc != null ) {
				 File jdkSrcFile = new File(jdkSrc);
				 if (jdkSrcFile.exists()) {
					 libSrcLocations.add(jdkSrc);
				 }
			}
		}
		this.classMetaInfoManager = new ClassMetaInfoManager(this);
		
		URL urlsA[] = new URL[classPathUrls.size()];
		classPathUrls.toArray(urlsA);
		loader = new ReflectAbleClassLoader(this.projectRoot,urlsA, this.getClass().getClassLoader());
		
		if (cacheClassInfo) {
			cacheClassInfo();
		}
	}
	
	public void cacheClassInfo() {
		if (classInfoCached) return;
		
		if (!this.isFlatProject()) {
			cachePackageInfo();
		} else {
			cacheFlatProjectPackageInfo();
		}
		classInfoCached = true;
	}
	
	public String[] getAllSourceFiles() {
		return getAllSourceFilesByType(ResourceType.Java);
	}
	
	public String[] getAllResourceFiles() {
		return getAllSourceFilesByType(ResourceType.Other);
	}
	
	public String[] getAllSourceFilesByType(ResourceType type) {
		List<String> names = new ArrayList<String>();
		for (String srcRoot : srcLocations ) {
            File dir = new File(srcRoot);
            if (dir.isFile()) continue;
            appendSrcFileToList(names, dir,type);
		}
		String[] result = new String[names.size()];
		for (int i=0; i<names.size(); i++) {
			result[i] = names.get(i);
		}
		return result;
	}
	
	private void appendSrcFileToList(List<String> names, File dir,ResourceType type) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				appendSrcFileToList(names, file, type);
			} else {
				String fileName = file.getName();
				if (type == ResourceType.Java && fileName.endsWith(".java")){
					names.add(file.getAbsolutePath());
				}
				if (type == ResourceType.Other && isResourceFile(fileName)) {
					names.add(file.getAbsolutePath());
				}
			}
		}

	}
	private boolean isResourceFile(String fileName) {
		if (fileName.endsWith(".java")
				|| fileName.endsWith(".class")) {
			return false;
		}
		return true;
	}
	
	public void refreshClassInfo(List<String> classNames) {
		
		if (loader == null) return;
		URL[] urls = loader.getURLs();
		loader = new ReflectAbleClassLoader(this.projectRoot,urls, this.getClass().getClassLoader());
		for (Debugger debugger : Debugger.getInstances()) {
			BreakpointManager bpmgr = debugger.getBreakpointManager();
			for (String className : classNames ) {
				packageInfo.addClassNameToCache(className,"src path");
				packageInfo.addClasstoDstClass(className);
				//String classPath = getOutputDir() + "/" + className.replace('.', '/') + ".class";
				//File outFile = new File(classPath);
				
				String resourceName = className.replace('.', '/') + ".class";
				this.getClassLoader().clearResourceByteCache(this.projectRoot,resourceName);
				
				this.clsBinPathCache.invalidate(className);
				this.clsSourcePathCache.invalidate(className);

				String classAsPath = className.replace('.', '/') + ".class";
				InputStream stream = this.getClassLoader().getResourceAsStream(classAsPath);
				classMetaInfoManager.loadSingleMetaInfo(stream);
				bpmgr.verifyBreakpoint(className);
			}
		}
		
		classMetaInfoManager.constructAllSubNames();
		
		//refresh class info in other CompilerContext
		List<CompilerContext> ctxs = CompilerContextManager.getInstnace().getAllLoadedContext();
		for (CompilerContext ctx : ctxs) {
			if (ctx.getExtOutputDirs().contains(this.getOutputDir())) {
				ctx.refreshClassInfo(classNames);
			}
		}	}
	
	
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
		String reactorRoot = prop.get("reactorRoot");
		if (reactorRoot != null ) {
			reactorProjectMap = ProjectLocationConf.loadProjectConfig(new File(reactorRoot, ".jde_module"));
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
				addToClassPaths(libFile,-1);
			}
		}
	}
	
	private void initClassPath(String classPathXml) {
		classPathEntries=parseClassPathXmlFile(classPathXml);
		
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
				addToClassPaths(libFile, -1);
				if (entry.sourcepath !=null) {
					libSrcLocations.add(entry.sourcepath);
				} else {
                    //decompileHorse.decompileJar(entryAbsPath, this);
                }
			} else if (entry.kind.equals("src")) {
				//if path startswith "/", it's another eclipse project 
				if (entry.path.startsWith("/")) {
					parseDependProjectClassXml(entry.path.substring(1));
				} else {
					srcLocations.add(entryAbsPath);
					if (entry.output != null && !entry.output.trim().equals("")) {
						String outputAbsPath = FilenameUtils.concat(projectRoot, entry.output);
						File libFile = new File(outputAbsPath);
						addToClassPaths(libFile, 0);
						testSrcLocationMap.put(entryAbsPath, outputAbsPath);
					}
				}
			} else if (entry.kind.equals("output")) {
				File libFile = new File(entryAbsPath);
				//output path should be searched first in classpath
				addToClassPaths(libFile, 0);
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
				dstToSrcMap.put(entryPath, sourcePath);
				addToClassPaths(libFile, -1);
				if (sourcePath !=null && !sourcePath.equals("")) {
					libSrcLocations.add(sourcePath);
				} else {
                    //decompileHorse.decompileJar(entryPath, this);
                }
			}
		}
		String extraSrcLocs =Preference.getInstance().getValue(Preference.EXTRA_SRC_LOCS);
		if (extraSrcLocs !=null && extraSrcLocs.length() > 0) {
			String [] paths = extraSrcLocs.split(",");
			for (String path : paths) {
				extSrcLocations.add(path);
			}
		}
		
	} 

    public void addDecompiledLibSrcLocation(String jarPath, String sourcePath) {
    	dstToSrcMap.put(jarPath, sourcePath);
        libSrcLocations.add(sourcePath);
    }
	
	private void parseDependProjectClassXml(String projectName) {
		String extProjectPath = reactorProjectMap.get(projectName);
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
				addToClassPaths(libFile, 1);
				extOutputDirs.add(libFile.getAbsolutePath());
			} 
		}
	}
	
	private void cacheFlatProjectPackageInfo() {
		packageInfo.cacheSystemRtJar();
		packageInfo.cacheClassNameInDist(outputDir,false);
	}
	
	private void cachePackageInfo() {

		List<Future> futures = new ArrayList<Future>();
		futures.add( backJobs.submit(() -> packageInfo.cacheSystemRtJar()));

		for (String path : fsClassPathUrls) {
			if (path.endsWith(".jar")) {
				futures.add( backJobs.submit(() -> packageInfo.cacheClassNameInJar(path)));
			}
		}
		
		futures.add( backJobs.submit(() -> packageInfo.cacheClassNameInDist(outputDir,true)));
		futures.add( backJobs.submit(() -> classMetaInfoManager.cacheAllInfo(outputDir)));


		for (String testOutDir :testSrcLocationMap.values()) {
			futures.add( backJobs.submit(() -> packageInfo.cacheClassNameInDist(testOutDir,true)));
			futures.add( backJobs.submit(() -> classMetaInfoManager.cacheAllInfo(testOutDir)));
		}
		
		for (String dirPath : extOutputDirs) {
			futures.add( backJobs.submit(() -> packageInfo.cacheClassNameInDist(dirPath,true)));
			futures.add( backJobs.submit(() -> classMetaInfoManager.cacheAllInfo(dirPath)));
		}
		
		try {
			for(Future f: futures) { 
				f.get(); 
			}
		} catch (Exception e) {
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
					entry.output = el.getAttribute("output");
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
		String locatedOutputDir = testSrcLocationMap.get(locatedSrcRoot);
		if (locatedOutputDir == null) {
			locatedOutputDir = outputDir;
		}
		
		String relativePath = source.substring(locatedSrcRoot.length()+1);
		
		String dstPath = FilenameUtils.concat(locatedOutputDir, relativePath);
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
	
	public List<String> getFsClassPathUrls() {
	    return fsClassPathUrls;
	}

	public ReflectAbleClassLoader getClassLoader() {
		return loader;
	}
	
	class ClassPathEntry {
		
		public String  kind;
		public String  path;
		public String  sourcepath;
		public String output;

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
	
	public String findSourceOrBinPath(String className) {
		String path = findSourceClass(className);
		if ("None".equals(path)) path = findClassBinPath(className);
		return path;
	}

	public String findSourceClass(final String className) {
		try {
			String result = clsSourcePathCache.get(className, new Callable<String>() {
				@Override
				public String call() {
					if (className == null)
						return "None";
					ClassInfo classInfo = classMetaInfoManager.getMetaInfo(className);
					
					String rtlPathName = className.replace(".", "/") + ".java";
					if (classInfo != null && classInfo.getSourceName() != null) {
						int dotIndex = className.lastIndexOf(".");
						String packagePath = "";
						if (dotIndex > -1) {
							packagePath = className.substring(0, dotIndex + 1).replace(".", "/");
						}
						rtlPathName = packagePath + classInfo.getSourceName();
					}
					String classFileLocation = packageInfo.findClassLocation(className);
					if (classFileLocation != null) {
						String srcLocation = dstToSrcMap.get(classFileLocation);
						if (srcLocation != null && ! srcLocation.trim().equals(""))  {
							if (!specialPackageCase(srcLocation,rtlPathName)) {
								String tmpPath = "jar://" + srcLocation + "!" + rtlPathName;
								return tmpPath;
							}
						}
					}

					String result =  findSourceFile(rtlPathName);
					//TODO 这里需要考虑inner class
					if (result.equals("None") && className.indexOf(".")> 0 ) {
						rtlPathName = className.substring(0, className.lastIndexOf(".")).replace(".", "/") + ".java";
						result =  findSourceFile(rtlPathName);
					}
					return result;
				}
			});
			return result;
		} catch (ExecutionException e) {
			e.printStackTrace();
			return "None";
		}
	}

	public boolean specialPackageCase(String srcLocation, String rtlPathName) {
		//spring packaged cglib in binary jar , but not in source jar
		if (srcLocation.indexOf("spring-core") > -1
				&& rtlPathName.startsWith("org/springframework/cglib")) {
			return true;
		}
		return false;
	}
	
	public String findClassBinPath(String className) {
		try {
			String result = clsBinPathCache.get(className, new Callable<String>() {
				@Override
				public String call() {
					if (className == null) return "None";
					String rtlPathName = className.replace(".", "/") + ".class";
					
					String classDstPath = packageInfo.findClassLocation(className);
					if (classDstPath != null) {
						if (classDstPath.endsWith("jar")) {
							classDstPath = "jar://" + classDstPath + "!" +rtlPathName;
						}
						return classDstPath;
					}
					
					for (String path : fsClassPathUrls) {
						if (path.endsWith(".jar")) {
							if ( hasEntry(path, rtlPathName)) {
								//extractContentToTemp(libSrcLoc,rtlPathName,tmpPath);
								String tmpPath = "jar://" + path + "!" +rtlPathName;
								return tmpPath;
							}
						}
					}
					return "None";
				}
			});
			return result;
		} catch (ExecutionException e) {
			e.printStackTrace();
			return "None";
		}
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
		//System.out.println("hasEntry: " + zipFileName + " "+ rtlPath);
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
	
	public String findProperOutputDir(String srcFile) {
		for (String srcPath : testSrcLocationMap.keySet() ) {
			srcFile = FilenameUtils.normalize(srcFile);
			srcPath = FilenameUtils.normalize(srcPath);
			if (srcFile.indexOf(srcPath) == 0) return testSrcLocationMap.get(srcPath);
		}
		return outputDir;
	}
	private void addToClassPaths(File libFile, int index) {
		try { 
			URL url = null;
			if (libFile.getName().endsWith(".jar")) {
				url = new URL("jar", "", -1, libFile.toURI().toString() + "!/");
				//jdk bug hacks, disable caching
				//URLConnection uConn = new URLConnection(url) {
                //   @Override
                //   public void connect() throws IOException {
                         // NOOP
                //  }
                //};
				//uConn.setDefaultUseCaches(false);
			} else {
			    url = libFile.toURI().toURL();
			}
			if (index == -1) {
			    classPathUrls.add(url); 
			} else {
			    classPathUrls.add(index,url); 
			}
			fsClassPathUrls.add(libFile.getAbsolutePath());
        } catch (Exception e) {
            String errorMsg = VjdeUtil.getExceptionValue(e);
            log.warn(errorMsg);
		}
	}
	
	//close jar file opened by classloader etc.
	public void clean() {
		try {
			this.loader.close();
		} catch (Exception e) {
			e.printStackTrace();
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
