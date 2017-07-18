package com.github.vinja.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FilenameUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Range;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithStatements;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.VoidType;
import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.compiler.CompilerContextManager;
import com.github.vinja.exception.LocationNotFoundException;
import com.github.vinja.omni.ClassInfo;
import com.github.vinja.omni.ClassInfoUtil;
import com.github.vinja.omni.ClassMetaInfoManager;
import com.github.vinja.util.DecompileUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@SuppressWarnings("all")
public class VinjaJavaSourceSearcher implements IJavaSourceSearcher {

	private static Cache<String, Future<VinjaJavaSourceSearcher>> cacheNew = CacheBuilder.newBuilder()
		    .maximumSize(2000)
		    .build(); 
	

	private static  PriorityBlockingQueue backQueue = new PriorityBlockingQueue();
	private static ExecutorService backEs = new ThreadPoolExecutor(20, 50, 30L, TimeUnit.SECONDS, backQueue);

	private static  PriorityBlockingQueue frontQueue = new PriorityBlockingQueue();
	private static ExecutorService frontEs = new ThreadPoolExecutor(4, 8, 5L, TimeUnit.SECONDS, frontQueue);


	public static final String NULL_TYPE = "NULL";

	private List<LocalVariableInfo> visibleVars = null;
	private CompilerContext ctx = null;
	private String currentFileName;
	private String curFullClassName;
	private boolean frontParse;
	
	private int classNameLine = 0;
	private int classNameCol = 0;

	private List<String> importedNames = new ArrayList<String>();
	private List<MemberInfo> memberInfos = new ArrayList<MemberInfo>();
	private List<String> staticImportedNames = new ArrayList<String>();
	private CompilationUnit compileUnit;
	
	public enum NameType {CLASS, FILE};

	private static CompilerContext findProperContext(String fileName, CompilerContext ctx) {

		if (fileName.startsWith("jar:")) {
			return ctx;
		}
		if (FilenameUtils.normalize(fileName).startsWith(ctx.getProjectRoot())) {
			return ctx;
		}
		CompilerContext tmp = ctx;
		String classPath = null;
		File file = new File(fileName);
		while (true) {
			File parentFile = file.getParentFile();
			if (parentFile == null)
				break;
			File classpathFile = new File(parentFile, ".classpath");
			if (classpathFile.exists()) {
				classPath = classpathFile.getAbsoluteFile().getAbsolutePath();
				break;
			}
			file = parentFile;
		}
		if (classPath != null) {
			tmp = CompilerContextManager.getInstnace().getCompilerContext(classPath);
		}

		return tmp;
	}
	
	public static Future<VinjaJavaSourceSearcher> loadJavaSourceSearcher(final String name, final CompilerContext ctx, final NameType nameType, boolean frontParse, VinjaSourceLoadPriority priority) {
		System.out.println("start loading " + name);
		String className = name;
		CompilerContext tempCtx = findProperContext(name, ctx);

		if (nameType == NameType.FILE ) {
			className = tempCtx.buildClassName(name);
		}

		Future<VinjaJavaSourceSearcher> f = cacheNew.getIfPresent(className);
		
        if (f == null) {
        	f = new VinjaSourceSearcherLoaderFutureTask(new VinjaSourceSearcherLoader(name,ctx,nameType,frontParse, priority));
        	if (frontParse) {
				frontEs.execute((FutureTask)f);
        	} else {
				backEs.execute((FutureTask)f);
        	}
			cacheNew.put(className, f);
        }
        return f;
	}
	
	
	public static VinjaJavaSourceSearcher createSearcher(String filename, CompilerContext ctx) {
		boolean frontParse = true;
		Future<VinjaJavaSourceSearcher> sourceSearcherFuture = loadJavaSourceSearcher(filename,ctx, NameType.FILE, frontParse, VinjaSourceLoadPriority.HIGH);
		try {
			return sourceSearcherFuture.get();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void clearSearcher(String filename, CompilerContext ctx) {
		CompilerContext tempCtx = findProperContext(filename, ctx);
		String className = tempCtx.buildClassName(filename);
		cacheNew.invalidate(className);
	}
	
	public VinjaJavaSourceSearcher(String filename, CompilerContext ctx) {
		this(filename,ctx, true);
	}

	public VinjaJavaSourceSearcher(String filename, CompilerContext ctx, boolean frontParse) {
		System.out.println("start parsing " + filename);
		this.ctx = findProperContext(filename, ctx);
		this.currentFileName = filename;
		this.curFullClassName = this.ctx.buildClassName(filename);
		this.frontParse = frontParse;

		// filename could be a jar entry path like below
		// jar://C:\Java\jdk1.6.0_29\src.zip!java/lang/String.java

		InputStream is = null;
		JarFile jarFile = null;
		try {

			if (filename.startsWith("jar:")) {
				String jarPath = filename.substring(6, filename.lastIndexOf("!"));
				jarFile = new JarFile(jarPath);
				String entryName = filename.substring(filename.lastIndexOf("!") + 1);
				entryName = entryName.replace("\\", "/");
				ZipEntry zipEntry = jarFile.getEntry(entryName);
				if (entryName.endsWith(".java")) {
					is = jarFile.getInputStream(zipEntry);
				} else {
					String classContent = DecompileUtil.decompile(jarPath, entryName);
					is = new ByteArrayInputStream(classContent.getBytes(StandardCharsets.UTF_8));
				}
			} else {
				File file = new File(filename);
				if (!file.exists()) {
					System.out.print(filename + " not exits");
					return;
				}
				is = new FileInputStream(filename);
			}
			readClassInfo(is);
		} catch (Exception e) {
			System.out.println("解析" + filename + "出错.");
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
			if (jarFile != null)
				try {
					jarFile.close();
				} catch (Exception e) {
				}

		}

	}

	private void readClassInfo(InputStream is) {
		this.compileUnit = JavaParser.parse(is);
		TypeDeclaration<?> firstClass = compileUnit.getTypes().get(0);
		
		Range classNameRange = firstClass.getName().getRange().get();
		classNameCol = classNameRange.begin.column;
		classNameLine = classNameRange.begin.line;
		
		NodeList<ImportDeclaration> imports = this.compileUnit.getImports();
		
		//cache the parse Result
		if (this.frontParse) {

			for (ImportDeclaration importDec : imports) {
				if (!importDec.isAsterisk()) {
					String importName = importDec.getNameAsString();
					boolean frontParse =false;
					VinjaJavaSourceSearcher.loadJavaSourceSearcher(importName, this.ctx, NameType.CLASS, frontParse, VinjaSourceLoadPriority.LOW);
				}
			}
		}
		if (firstClass instanceof AnnotationDeclaration) {	
			AnnotationDeclaration annoDeclare = (AnnotationDeclaration)firstClass;
			NodeList<BodyDeclaration<?>> members = annoDeclare.getMembers();
			for (BodyDeclaration member: members) {
				if (member instanceof AnnotationMemberDeclaration) {
					AnnotationMemberDeclaration annoMethod = (AnnotationMemberDeclaration) member;
					int line =  annoMethod.getRange().get().begin.line;
					MemberInfo info = new MemberInfo();

					List<String> modifierList = new ArrayList<String>();
					for (Iterator<Modifier> it = annoMethod.getModifiers().iterator(); it.hasNext();) {
						modifierList.add(it.next().asString());
					}
					info.setModifierList(modifierList);

					info.setName(annoMethod.getNameAsString());
					ClassLocInfo classLocInfo = this.findTypeSourceLocInfo(annoMethod.getType());
					info.setRtnType(classLocInfo.getClassName());
					info.setLineNum(annoMethod.getName().getRange().get().begin.line);
					info.setColumn(annoMethod.getName().getRange().get().begin.column);
					info.setMemberType(MemberType.METHOD);
					List<String[]> paramList = new ArrayList<String[]>();
					info.setParamList(paramList);
					memberInfos.add(info);
				}
			}
		}

		if (firstClass instanceof ClassOrInterfaceDeclaration) {
			ClassOrInterfaceDeclaration classDeclare = (ClassOrInterfaceDeclaration)firstClass;

			NodeList<TypeParameter> typeParameters = ((ClassOrInterfaceDeclaration)firstClass).getTypeParameters();
			for (TypeParameter pam : typeParameters ) {
			}
			
			List<ConstructorDeclaration> constructers = ((ClassOrInterfaceDeclaration)firstClass).getConstructors();
			for (ConstructorDeclaration consDeclare: constructers) {
				int line =  consDeclare.getRange().get().begin.line;
				MemberInfo info = new MemberInfo();

				List<String> modifierList = new ArrayList<String>();
				for (Iterator<Modifier> it = consDeclare.getModifiers().iterator(); it.hasNext();) {
					modifierList.add(it.next().asString());
				}
				info.setModifierList(modifierList);

				info.setName(consDeclare.getNameAsString());
				info.setRtnType(this.curFullClassName);
				info.setLineNum(consDeclare.getName().getRange().get().begin.line);
				info.setColumn(consDeclare.getName().getRange().get().begin.column);
				info.setMemberType(MemberType.CONSTRUCTOR);
				NodeList<Parameter> parameters = consDeclare.getParameters();
				List<String[]> paramList = new ArrayList<String[]>();
				for (Parameter param : parameters) {
					ClassLocInfo classInfo = this.findTypeSourceLocInfo(param.getType());
					if (classInfo == null) {
						System.out.println("parma type null" + param);
					}
					String paramJavaType = classInfo.getClassName();
					String paramName = param.getNameAsString();
					String[] methodParam = new String[] { paramJavaType, paramName };
					paramList.add(methodParam);
				}
				info.setParamList(paramList);
				memberInfos.add(info);
				
			}
			
		}
		
		
		List<MethodDeclaration> methods = firstClass.getMethods();
		for (MethodDeclaration methodDeclare : methods) {
			int line = methodDeclare.getRange().get().begin.line;

			MemberInfo info = new MemberInfo();

			List<String> modifierList = new ArrayList<String>();
			for (Iterator<Modifier> it = methodDeclare.getModifiers().iterator(); it.hasNext();) {
				modifierList.add(it.next().asString());
			}
			info.setModifierList(modifierList);

			info.setName(methodDeclare.getNameAsString());
			ClassLocInfo classLocInfo = this.findTypeSourceLocInfo(methodDeclare.getType());
			info.setRtnType(classLocInfo.getClassName());
			info.setLineNum(methodDeclare.getName().getRange().get().begin.line);
			info.setColumn(methodDeclare.getName().getRange().get().begin.column);
			info.setMemberType(MemberType.METHOD);
			NodeList<Parameter> parameters = methodDeclare.getParameters();
			List<String[]> paramList = new ArrayList<String[]>();
			for (Parameter param : parameters) {
				ClassLocInfo classInfo = this.findTypeSourceLocInfo(param.getType());
				if (classInfo == null) {
					System.out.println("parma type null" + param);
				}
				String paramJavaType = classInfo.getClassName();
				String paramName = param.getNameAsString();
				String[] methodParam = new String[] { paramJavaType, paramName };
				paramList.add(methodParam);
			}
			info.setParamList(paramList);
			memberInfos.add(info);
		}
		List<FieldDeclaration> fields = firstClass.getFields();
		for (FieldDeclaration field : fields) {
			NodeList<VariableDeclarator> variables = field.getVariables();
			for (VariableDeclarator var : variables) {
				MemberInfo info = new MemberInfo();

				List<String> modifierList = new ArrayList<String>();
				for (Iterator<Modifier> it = field.getModifiers().iterator(); it.hasNext();) {
					modifierList.add(it.next().asString());
				}
				info.setModifierList(modifierList);
				
				ClassLocInfo classLocInfo = this.findTypeSourceLocInfo(var.getType());
				info.setRtnType(classLocInfo.getClassName());

				info.setMemberType(MemberType.FIELD);
				info.setName(var.getNameAsString());
				info.setLineNum(var.getName().getRange().get().begin.line);
				info.setColumn(var.getName().getRange().get().begin.column);
				memberInfos.add(info);
			}
		}

		if (firstClass instanceof EnumDeclaration) {
			EnumDeclaration enumDec = (EnumDeclaration) firstClass;
			NodeList<EnumConstantDeclaration> entries = enumDec.getEntries();
			for (EnumConstantDeclaration entry : entries) {
				MemberInfo info = new MemberInfo();
				info.setMemberType(MemberType.FIELD);
				info.setName(entry.getNameAsString());
				info.setLineNum(entry.getName().getRange().get().begin.line);
				info.setRtnType(this.curFullClassName);
				info.setColumn(entry.getName().getRange().get().begin.column);
				memberInfos.add(info);
			}
		}
	}

	public Node searchDefLocation(Node node, int line, int col) {
		if (node.getChildNodes() == null || node.getChildNodes().size() == 0) {
			if (node.getRange().isPresent()) {
				Range range = node.getRange().get();
				if (range.begin.line == line && range.begin.column <= col && range.end.column >= col) {
					return node;
				}
			}
		} else {
			for (Node subNode : node.getChildNodes()) {
				Node tmp = searchDefLocation(subNode, line, col);
				if (tmp != null)
					return tmp;
			}
		}
		return null;
	}

	@Override
	public List<MemberInfo> getMemberInfos() {
		return this.memberInfos;
	}

	@Override
	public int getClassScopeLine() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	private static boolean isValidateModifier(boolean staticMember,boolean protectedMember,String mod) {
		if (staticMember && ! (mod.indexOf("static") > -1) ) return false;
		if (protectedMember ) {
			if ( mod.indexOf("protected") > -1 || mod.indexOf("public") > -1 ) return true;
		} else {
			if (mod.indexOf("public") > -1 ) return true;
		}
		return false;
	}

	@Override
	public ArrayList<com.github.vinja.omni.MemberInfo> getMemberInfo(Class aClass, boolean staticMember,
	        boolean protectedMember) {
		
		List<MemberInfo> tmpMemberInfo = this.memberInfos;
		if (aClass.getName().indexOf("$")> -1 ) { //inner class
			String innerClassName = aClass.getSimpleName();
			for (MemberInfo info : this.memberInfos) {
				if (innerClassName.equals(info.getName()) && info.getMemberType() == MemberType.SUBCLASS ) {
					tmpMemberInfo = info.getSubMemberList();
					break;
				}
			}
		}

		ArrayList<com.github.vinja.omni.MemberInfo> memberInfos = 
				new ArrayList<com.github.vinja.omni.MemberInfo>();
		for (MemberInfo info: tmpMemberInfo) {
			if (info.getMemberType() == MemberType.CONSTRUCTOR) continue;
			if (aClass.isEnum() || aClass.isInterface() || aClass.isAnnotation() || 
					isValidateModifier(staticMember, protectedMember, info.getModifierDesc())) {
				com.github.vinja.omni.MemberInfo memberInfo = new com.github.vinja.omni.MemberInfo();
				memberInfo.setModifiers(info.getModifierDesc());
				memberInfo.setMemberType(info.getMemeberTypeDesc());
				memberInfo.setName(info.getName());
				memberInfo.setReturnType(info.getShortRtnType());
				memberInfo.setExceptions("");
				memberInfo.setParams(info.formatParamList());
				memberInfos.add(memberInfo);
			}
		}
		return memberInfos;
	}

	@Override
	public ArrayList<com.github.vinja.omni.MemberInfo> getConstructorInfo() {
		ArrayList<com.github.vinja.omni.MemberInfo> memberInfos=new ArrayList<com.github.vinja.omni.MemberInfo>();
		for (MemberInfo info: this.memberInfos) {
			if (!isValidateModifier(false, true, info.getModifierDesc())) continue;
			if (! ( info.getMemberType() == MemberType.CONSTRUCTOR)) continue;
			
			com.github.vinja.omni.MemberInfo memberInfo=new com.github.vinja.omni.MemberInfo();
			memberInfo.setMemberType(info.getMemeberTypeDesc());
			memberInfo.setModifiers(info.getModifierDesc());
			memberInfo.setName(info.getName());
			memberInfo.setExceptions("");
			memberInfo.setParams(info.formatParamList());
			memberInfos.add(memberInfo);
		}
		return memberInfos;
	}

	@Override
	public int searchLoopOutLine(int currentLine) {
		System.out.println("searchLoopOutLine还未实现");
		return 0;
	}

	@Override
	public Set<String> searchNearByExps(int lineNum, boolean currentLine) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LocationInfo searchDefLocation(int line, int col, String sourceType) {
		Node node = this.searchDefLocation(this.compileUnit, line, col);
		return getLocationInfo(node, sourceType);
	}

	@SuppressWarnings("all")
	private String getClassFilePath(String className) {
		String path = ctx.findSourceOrBinPath(className);
		return path;
	}

	public ClassLocInfo findTypeSourceLocInfo(Type type) {
		String typeName = null;
		if (type instanceof PrimitiveType) {
			typeName = ((PrimitiveType) type).getType().asString();
		} else if (type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType)type;
			ClassLocInfo referencedClass = findTypeSourceLocInfo(type.getElementType());
			if (referencedClass != null) {
				String className = referencedClass.getClassName();
				for (int i=0; i<  arrayType.getArrayLevel(); i++) {
					className = className + "[]";
				}
				return new ClassLocInfo(className, referencedClass.getSourcePath());
			}
		} else if (type instanceof VoidType) {
			typeName = "void";
		}
		if (typeName != null) {
			return new ClassLocInfo(typeName, null);
		}

		if (type instanceof ClassOrInterfaceType) {
			ClassOrInterfaceType tmpType = (ClassOrInterfaceType) type;
			StringBuilder sb = new StringBuilder();
			if (tmpType.getScope().isPresent()) {
				sb.append("."  + tmpType.getNameAsString());
				while (tmpType.getScope().isPresent()) {
					tmpType = tmpType.getScope().get();
					sb.insert(0, "." + tmpType.getNameAsString());
				}
				sb.deleteCharAt(0);
				System.out.print(tmpType.asString());
			} else {
				sb.append(tmpType.getNameAsString());
			}
			return findReferencedClass(sb.toString());
		}

		System.out.println("找不到type的信息" + type.asString());
		return new ClassLocInfo(type.asString(), null);
	}

	public ClassLocInfo findReferencedClass(String className) {
		NodeList<ImportDeclaration> imports = this.compileUnit.getImports();
		
		for (ImportDeclaration importDec : imports) {
			String importName = importDec.getNameAsString();
			if (importDec.isAsterisk()) {
				String classCanonicalName = importDec.getNameAsString() + "." + className;
				String path = this.getClassFilePath(classCanonicalName);
				if (!path.equals("None"))
					return new ClassLocInfo(classCanonicalName, path);
			} else {
				String importedSimpleName = importName.indexOf(".") > 0
				        ? importName.substring(importName.lastIndexOf(".") + 1) : importName;
				if (importedSimpleName.equals(className)) {
					return new ClassLocInfo(importName, this.getClassFilePath(importName));
				}
			}
		}

		// same package class
		if (className.indexOf(".") < 0 && curFullClassName != null) {
			String classCanonicalName = className;
			if (curFullClassName.indexOf(".") > 0) {
				String packageName = curFullClassName.substring(0, curFullClassName.lastIndexOf("."));
				classCanonicalName = packageName + "." + className;
			}
			String path = this.getClassFilePath(classCanonicalName);
			if (!path.equals("None"))
				return new ClassLocInfo(classCanonicalName, path);
			
		}

		if (className.indexOf(".") < 0 ) {
			String classCanonicalName = "java.lang." + className;
			String path = this.getClassFilePath(classCanonicalName);
			if (!path.equals("None"))
				return new ClassLocInfo(classCanonicalName, path);
			
		}
		
		
		//try full qualified name
		if (className.indexOf(".") > 0 ) {
		String path = this.getClassFilePath(className);
			if (path != null) {
				return new ClassLocInfo(className, this.getClassFilePath(className));
			}
		}

		// try inner class
		String classCanonicalName = this.curFullClassName;
		/*while (true) {
			aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[] { classCanonicalName }, null);
			classCanonicalName = classCanonicalName + "$" + className;
			String path = this.getClassFilePath(classCanonicalName);
			if (!path.equals("None")) {
				return new ClassLocInfo(classCanonicalName, path);
			}
			if (aClass == null || aClass.equals("java.lang.Object"))
				break;
			aClass = aClass.getSuperclass();
			if (aClass == null || aClass.equals("java.lang.Object"))
				break;
			classCanonicalName = aClass.getCanonicalName();
		}*/

		return new ClassLocInfo(className, null);
	}

	private String findVarNameType(Node node, String name) {
		NodeWithType<?,Type> foundLocalNode = this.findVarNameDeclareExp(node, name);
		if (foundLocalNode != null) {
			return this.findTypeSourceLocInfo(foundLocalNode.getType()).getClassName();
		}
		
		LocationInfo locInfo = new LocationInfo();
		boolean found = this.searchMemberInHierachy(this.curFullClassName, MemberType.FIELD, name , null, locInfo);
		if (found) {
			return locInfo.getMemberInfo().getRtnType();
		}
		
		if (name.toUpperCase().substring(0,1).equals(name.substring(0,1))) {
			 ClassLocInfo referencedClass = this.findReferencedClass(name);
			 if (referencedClass != null) {
				 return referencedClass.getClassName();
			 }
		}
		return null;
	}
	
	private LocationInfo findVarNameLocation(Node node, String name) {
		NodeWithType<?,Type> foundLocalNode = this.findVarNameDeclareExp(node, name);
		LocationInfo locInfo = new LocationInfo();
		if (foundLocalNode != null) {
			Node tmpNode = (Node)foundLocalNode;
			int line = tmpNode.getRange().get().begin.line;
			int col = tmpNode.getRange().get().begin.column;
			locInfo.setCol(col);
			locInfo.setLine(line);
			locInfo.setFilePath(this.currentFileName);
			return locInfo;
		}
		
		if (name.toUpperCase().substring(0,1).equals(name.substring(0,1))) {
			 ClassLocInfo refClass = this.findReferencedClass(name);
			 if (refClass.getClassName() != null && refClass.getSourcePath() != null) {
					VinjaJavaSourceSearcher searcher = createSearcher(refClass.getSourcePath(), ctx);
					locInfo.setFilePath(refClass.getSourcePath());
					locInfo.setLine(searcher.classNameLine);
					locInfo.setCol(searcher.classNameCol);
					return locInfo;
				}
		}
		
		boolean found = this.searchMemberInHierachy(this.curFullClassName, MemberType.FIELD, name , null, locInfo);
		if (found) {
			return locInfo;
		}
		return null;
	}

	private NodeWithType<?, Type> findVarNameDeclareExp(Node node, String name) {

		Node parentNode = node;

		while (node.getParentNode().isPresent()) {
			parentNode = node.getParentNode().get();
			if (parentNode instanceof NodeWithStatements) {
				NodeWithStatements blockStmt = (NodeWithStatements) parentNode;
				int position = -1;
				for (int i = 0; i < blockStmt.getStatements().size(); i++) {
					if (blockStmt.getStatements().get(i).equals(node)) {
						position = i;
					}
				}
				if (position == -1) {
					throw new RuntimeException();
				}
				for (int i = position - 1; i >= 0; i--) {
					Node stmtNode = blockStmt.getStatements().get(i);
					if (stmtNode instanceof ExpressionStmt) {
						Expression expression = ((ExpressionStmt) stmtNode).getExpression();
						if (expression instanceof VariableDeclarationExpr) {
							NodeList<VariableDeclarator> variables = ((VariableDeclarationExpr) expression)
							        .getVariables();
							for (VariableDeclarator var : variables) {
								Type type = var.getType();
								String varName = var.getNameAsString();
								if (varName.equals(name)) {
									return var;
								}
							}
						}

					}
				}

			} else if (parentNode instanceof MethodDeclaration) {
				MethodDeclaration methodDec = (MethodDeclaration) parentNode;
				NodeList<Parameter> parameters = methodDec.getParameters();
				for (Parameter parameter : parameters) {
					if (parameter.getNameAsString().equals(name)) {
						return parameter;
					}
				}
			} else if (parentNode instanceof ConstructorDeclaration) {
				ConstructorDeclaration constructorDec = (ConstructorDeclaration) parentNode;
				NodeList<Parameter> parameters = constructorDec.getParameters();
				for (Parameter parameter : parameters) {
					if (parameter.getNameAsString().equals(name)) {
						return parameter;
					}
				}
			} else if (parentNode instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration classDec = (ClassOrInterfaceDeclaration) parentNode;
				NodeList<BodyDeclaration<?>> members = classDec.getMembers();
				for (BodyDeclaration bodyDecla : members) {
					if (bodyDecla instanceof FieldDeclaration) {
						FieldDeclaration field = (FieldDeclaration) bodyDecla;
						NodeList<VariableDeclarator> variables = field.getVariables();
						for (VariableDeclarator var : variables) {
							Type type = var.getType();
							String varName = var.getNameAsString();
							if (varName.equals(name)) {
								return var;
							}
						}
					}
				}
			} else if (parentNode instanceof ForeachStmt) {
				ForeachStmt foreachStmt = (ForeachStmt) parentNode;
				VariableDeclarationExpr varExp = foreachStmt.getVariable();
				NodeList<VariableDeclarator> variables = ((VariableDeclarationExpr) varExp) .getVariables();
				for (VariableDeclarator var : variables) {
					Type type = var.getType();
					String varName = var.getNameAsString();
					if (varName.equals(name)) {
						return var;
					}
				}
			} else if (parentNode instanceof CatchClause) {
				CatchClause catchExpr = (CatchClause) parentNode;
				Parameter parameter = catchExpr.getParameter();
				if (parameter.getNameAsString().equals(name)) {
					return parameter;
				}
			}
			node = parentNode;
		}
		return null;
	}
	
	private boolean tryFindMemberInStaticImport(CompilationUnit compileUnit, MemberType memberType, String memberName,  List<String> typenameList, LocationInfo info) {
		NodeList<ImportDeclaration> imports = compileUnit.getImports();
		
		for (ImportDeclaration importDec : imports) {
			String importName = importDec.getNameAsString();
			if (importDec.isStatic()) {

				if (!importDec.isAsterisk()) {
					String simpleName = importName.indexOf(".") > 0
							? importName.substring(importName.lastIndexOf(".") + 1) : importName;
					if (simpleName.equals(memberName)) {
						String className =importName.substring(0, importName.lastIndexOf("."));
						return searchMemberInClass(className, memberType, memberName, typenameList, info);
					}
				} else {
					String classCanonicalName = importDec.getNameAsString();
					boolean found = searchMemberInClass(classCanonicalName, memberType, memberName, typenameList, info);
					if (found) return true;
				}
			}
		}
		return false;
	}

	public boolean searchMemberInClass(String className, MemberType memberType, String memberName,
	        List<String> typenameList, LocationInfo info) {

		String classFilePath = getClassFilePath(className);
		if (classFilePath == null || classFilePath.equals("None"))
			throw new LocationNotFoundException();

		VinjaJavaSourceSearcher searcher = createSearcher(classFilePath, ctx);
		List<MemberInfo> leftClassMembers = searcher.getMemberInfos();

		// if classname not equals to filename, find member in subclass or inner
		// enum .
		if (!FilenameUtils.getBaseName(classFilePath).equals(className)) {
			if (className.indexOf("$") > 0)
				className = className.substring(className.indexOf("$") + 1);
			for (MemberInfo classMember : leftClassMembers) {
				if ((classMember.getMemberType() == MemberType.ENUM
				        || classMember.getMemberType() == MemberType.SUBCLASS)
				        && classMember.getName().equals(className)) {
					leftClassMembers = classMember.getSubMemberList();
					break;
				}

			}

		}

		if (!(memberType == MemberType.FIELD)) {
			MemberInfo memberInfo = findMatchedMethod(memberName, typenameList, leftClassMembers);
			if (memberInfo != null) {
				info.setLine(memberInfo.getLineNum());
				info.setCol(memberInfo.getColumn());
				info.setFilePath(classFilePath);
				info.setMemberInfo(memberInfo);
				return true;
			}
		} else {
			MemberInfo memberInfo = findMatchedField(memberName, leftClassMembers);
			if (memberInfo != null) {
				info.setLine(memberInfo.getLineNum());
				info.setCol(memberInfo.getColumn());
				info.setFilePath(classFilePath);
				info.setMemberInfo(memberInfo);
				return true;
			}
		}
		return tryFindMemberInStaticImport(searcher.compileUnit, memberType, memberName, typenameList, info);
	}

	public boolean searchMemberInHierachy(String className, MemberType memberType, String memberName,
	        List<String> typenameList, LocationInfo info) {

		boolean found = searchMemberInClass(className, memberType, memberName, typenameList, info);
		if (found)
			return true;

		// if can't find, search the super class
		Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[] { className }, null);
		if (!aClass.isInterface()) {
			if (aClass == null || aClass.getName().equals("java.lang.Object")) {
				return false;
			}
			aClass = aClass.getSuperclass();
			if (aClass == null || aClass.getName().equals("java.lang.Object")) {
				return false;
			}
			String superClassName = aClass.getCanonicalName();
			return searchMemberInHierachy(superClassName, memberType, memberName, typenameList, info);
		} else {
			Class[] classes = aClass.getInterfaces();
			for (Class clazz : classes) {
				String itfName = clazz.getCanonicalName();
				found = searchMemberInHierachy(itfName, memberType, memberName, typenameList, info);
				if (found)
					return true;
			}
		}
		return false;

	}

	private String getNodeJavaType(Node node) {
		if (node instanceof StringLiteralExpr) {
			return "String";
		} else if (node instanceof IntegerLiteralExpr) {
			return "integer";
		} else if (node instanceof LongLiteralExpr) {
			return "long";
		} else if (node instanceof DoubleLiteralExpr) {
			return "double";
		} else if (node instanceof CharLiteralExpr) {
			return "char";
		} else if (node instanceof NullLiteralExpr) {
			return "null";
		} else if (node instanceof BooleanLiteralExpr) {
			return "boolean";
		} else if (node instanceof MethodCallExpr) {
			MemberInfo methodInfo = this.findMethodInfo((MethodCallExpr) node);
			if (methodInfo != null)
				return methodInfo.getRtnType();
		} else if (node instanceof NameExpr) {
			return this.findVarNameType(node, ((NameExpr) node).getNameAsString());
		} else if (node instanceof BinaryExpr) {
			return getNodeJavaType(((BinaryExpr) node).getLeft());
		} else if (node instanceof UnaryExpr) {
			return getNodeJavaType(((UnaryExpr) node).getExpression());
		} else if (node instanceof FieldAccessExpr) {
			MemberInfo methodInfo = this.findFieldInfo((FieldAccessExpr) node);
			if (methodInfo != null)
				return methodInfo.getRtnType();
		} else if (node instanceof ObjectCreationExpr) {
			ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr)node;
			ClassLocInfo referencedClass = this.findReferencedClass(objectCreationExpr.getType().getNameAsString());
			if (referencedClass != null) {
				return referencedClass.getClassName();
			}
		} else if (node instanceof CastExpr) {
			CastExpr castExpr = (CastExpr)node;
			ClassLocInfo referencedClass = this.findReferencedClass(castExpr.getType().asString());
			if (referencedClass != null) {
				return referencedClass.getClassName();
			}
		} else if (node instanceof ArrayCreationExpr) {
			ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr)node;
			ClassLocInfo referencedClass = this.findReferencedClass(arrayCreationExpr.getElementType().asString());
			if (referencedClass != null) {
				String className = referencedClass.getClassName();
				for (ArrayCreationLevel level: arrayCreationExpr.getLevels()) {
					className = className + "[]";
				}
				return className;
			}
		} else if (node instanceof EnclosedExpr) {
			return getNodeJavaType(((EnclosedExpr) node).getInner().get());
		} else if (node instanceof ClassExpr) {
			return "java.lang.Class";
		} else if (node instanceof ThisExpr) {
			return this.curFullClassName;
		} else if (node instanceof SuperExpr) {
			Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[] { curFullClassName }, null);
			return aClass.getSuperclass().getCanonicalName();
		} else if (node instanceof ArrayAccessExpr) {
			ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr) node;
			return getNodeJavaType(arrayAccessExpr.getName());
		} else if (node instanceof ConditionalExpr) {
			ConditionalExpr condExpr = (ConditionalExpr) node;
			return getNodeJavaType(condExpr.getThenExpr());
		}

		return null;
	}

	private MemberInfo findMatchedField(String fieldName, List<MemberInfo> memberInfos) {
		for (MemberInfo member : memberInfos) {
			if (MemberType.FIELD == member.getMemberType() && member.getName().equals(fieldName)) {
				return member;
			}
		}
		return null;
	}

	private MemberInfo findMatchedMethod(String methodName, List<String> argTypes, List<MemberInfo> memberInfos) {

		List<MemberInfo> paramCountMatchedList = new ArrayList<MemberInfo>();

		for (MemberInfo member : memberInfos) {
			if ( (member.getMemberType() == MemberType.METHOD  || member.getMemberType() == MemberType.CONSTRUCTOR)
					&&  member.getName().equals(methodName)) {

				List<String[]> memberParamList = member.getParamList();
				if ((argTypes == null || argTypes.size() == 0)
				        && (memberParamList == null || memberParamList.size() == 0)) {
					return member;
				}

				int matchedCount = memberParamList.size();

				boolean hasVarArg = false;
				String lastDefType = null;
				if (memberParamList.size() > 0) {
					lastDefType = memberParamList.get(memberParamList.size() - 1)[0];
					// vararg
					if (lastDefType.endsWith("...")) {
						matchedCount = memberParamList.size() - 1;
						hasVarArg = true;
						lastDefType = lastDefType.substring(0, lastDefType.length() - 3);
					}
				}
				if (!hasVarArg) {
					// 如果没有变参,同时参数个数又不匹配,则匹配不成功
					if (matchedCount != argTypes.size()) {
						continue;
					} else {
						paramCountMatchedList.add(member);
					}
				}

				boolean noMatch = false;
				int i;
				for (i = 0; i < matchedCount; i++) {
					String actTypeName = argTypes.get(i);
					String defTypeName = memberParamList.get(i)[0];
					if (!arguMatch(defTypeName, actTypeName)) {
						noMatch = true;
						break;
					}
				}
				if (hasVarArg) {
					while (true) {
						if (i >= argTypes.size())
							break;
						String actTypeName = argTypes.get(i);
						if (!arguMatch(lastDefType, actTypeName)) {
							noMatch = true;
							break;
						}
						i = i + 1;
					}
				}
				if (!noMatch)
					return member;
			}
		}
		if (paramCountMatchedList.size() > 0) {
			return paramCountMatchedList.get(0);
		}
		return null;
	}

	private boolean arguMatch(String defTypeName, String actTypeName) {
		if (defTypeName == null || defTypeName.equals("java.lang.Object"))
			return true;
		if (actTypeName.equals("String") && defTypeName.equals("java.lang.String"))
			return true;
		if (defTypeName.equals(actTypeName))
			return true;
		if (actTypeName.equals(NULL_TYPE))
			return true;
		if (defTypeName.length() == 1)  //generic type
			return true;
		return false;
	}

	private MemberInfo findMethodInfo(MethodCallExpr callExpr) {

		MemberInfo info = new MemberInfo();
		Optional<Expression> scope = callExpr.getScope();
		
		String methodName = callExpr.getNameAsString();

		NodeList<Expression> arguments = callExpr.getArguments();
		List<String> argumentTypes = new ArrayList<String>();
		for (Expression argu : arguments) {
			String javaType = this.getNodeJavaType(argu);
			argumentTypes.add(javaType);
		}

		List<MemberInfo> tempMemberInfo = null;
		String className = "";
		if (scope.isPresent()) {
			className = this.getNodeJavaType(scope.get());
		} else {
			className = this.curFullClassName;
		}
		LocationInfo locInfo = new LocationInfo();
		boolean found = this.searchMemberInHierachy(className, MemberType.METHOD, methodName, argumentTypes, locInfo);
		if (found) {
			return locInfo.getMemberInfo();
		}
		return null;
	}
	
	private MemberInfo findFieldInfo(FieldAccessExpr fieldExpr) {

		MemberInfo info = new MemberInfo();
		Expression scope = fieldExpr.getScope();
		String fieldName = fieldExpr.getNameAsString();

		List<MemberInfo> tempMemberInfo = null;

		String javaType = this.getNodeJavaType(scope);
		if (javaType.endsWith("[]") && fieldName.equals("length")) {
			MemberInfo memberInfo = new MemberInfo();
			memberInfo.setRtnType("int");
			return memberInfo;
		} 
		String path = this.getClassFilePath(javaType);
		VinjaJavaSourceSearcher searcher = VinjaJavaSourceSearcher.createSearcher(path, ctx);
		tempMemberInfo = searcher.getMemberInfos();
		
		MemberInfo memberInfo = this.findMatchedField(fieldName, tempMemberInfo);
		return memberInfo;
	}
	
	
	
	

	private LocationInfo getLocationInfo(Node node, String sourceType) {
		if ((node instanceof Name) && node.getParentNode().get() instanceof AnnotationExpr) {
			LocationInfo info = new LocationInfo();
			AnnotationExpr annoExpr = (AnnotationExpr)node.getParentNode().get();
			ClassLocInfo refClass = findReferencedClass(annoExpr.getNameAsString());
			if (refClass.getClassName() != null && refClass.getSourcePath() != null) {
				VinjaJavaSourceSearcher searcher = createSearcher(refClass.getSourcePath(), ctx);
				info.setFilePath(refClass.getSourcePath());
				info.setLine(searcher.classNameLine);
				info.setCol(searcher.classNameCol);
				return info;
			}
		}
		
		
		if (!(node instanceof SimpleName)) {
			System.out.println("not simplename");
			return null;
		}
		Node parentNode = node.getParentNode().get();
		// parseAllVisibleVar(parentNode);

		LocationInfo info = new LocationInfo();

		if (parentNode instanceof NameExpr) {
			NameExpr nameExpr = (NameExpr) parentNode;
			String nodeName = nameExpr.getNameAsString();
			return this.findVarNameLocation(node, nameExpr.getNameAsString());
		} else if (parentNode instanceof ClassOrInterfaceType) {
			Node pparentNode = parentNode.getParentNode().get();
			if (pparentNode instanceof ObjectCreationExpr) {
				ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr)pparentNode;
				String constructorName = objectCreationExpr.getType().getNameAsString();
				String className = this.findTypeSourceLocInfo(objectCreationExpr.getType()).getClassName();
				
				NodeList<Expression> arguments = objectCreationExpr.getArguments();
				List<String> argumentTypes = new ArrayList<String>();
				for (Expression argu : arguments) {
					String javaType = this.getNodeJavaType(argu);
					argumentTypes.add(javaType);
				}
				boolean suc = this.searchMemberInHierachy(className, MemberType.CONSTRUCTOR, constructorName, argumentTypes, info);
				if (suc )  {
					return info;
				}
			} 
			//find location of class if can't find constructor  or it's just not a constructor call
			ClassLocInfo refClass = findReferencedClass(((ClassOrInterfaceType)parentNode).getNameAsString());
			if (refClass.getClassName() != null && refClass.getSourcePath() != null) {
				VinjaJavaSourceSearcher searcher = createSearcher(refClass.getSourcePath(), ctx);
				info.setFilePath(refClass.getSourcePath());
				info.setLine(searcher.classNameLine);
				info.setCol(searcher.classNameCol);
				return info;
			}

		} else if (parentNode instanceof FieldAccessExpr) {
			FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) parentNode;
			String scopeJavaClass = this.getNodeJavaType(fieldAccessExpr.getScope());
			this.searchMemberInHierachy(scopeJavaClass, MemberType.FIELD, fieldAccessExpr.getNameAsString(), null, info);
			return info;
		} else if (parentNode instanceof MethodCallExpr) {
			MethodCallExpr callExpr = (MethodCallExpr) parentNode;
			Optional<Expression> scope = callExpr.getScope();
			if (callExpr.getName().equals(node)) {
				NodeList<Expression> arguments = callExpr.getArguments();
				List<String> argumentTypes = new ArrayList<String>();
				for (Expression argu : arguments) {
					String javaType = this.getNodeJavaType(argu);
					argumentTypes.add(javaType);
				}

				List<MemberInfo> tempMemberInfo = null;
				String scopeJavaClass = this.curFullClassName;
				if (scope.isPresent()) {
					scopeJavaClass = this.getNodeJavaType(scope.get());
					String path = this.getClassFilePath(scopeJavaClass);
					info.setFilePath(path);

					if (sourceType != null && sourceType.equals("impl")) {
						ClassMetaInfoManager cmm = this.ctx.getClassMetaInfoManager();
						ClassInfo classInfo = cmm.getMetaInfo(scopeJavaClass);
						if (classInfo != null) {
							Set<String> subNames = classInfo.getSubNames();
							if (subNames.size() == 1) {
								scopeJavaClass = subNames.toArray(new String[] {})[0];
							}
						}
					}
				}

				this.searchMemberInHierachy(scopeJavaClass, MemberType.METHOD, callExpr.getNameAsString(),
				        argumentTypes, info);
				return info;
			}
		}
		return null;
	}

	public static void main(String[] args) {
		CompilerContext ctx = CompilerContext.load("/Users/wangsn/work/TuyaSmartServer/smart-service/.classpath");
		String source = "/Users/wangsn/work/TuyaSmartServer/smart-service/src/main/java/com/tuya/smart/service/device/impl/GatewayService.java";
		VinjaJavaSourceSearcher searcher = VinjaJavaSourceSearcher.createSearcher(source, ctx);
		Object result = searcher.searchDefLocation(204, 44, "impl");
		System.out.println(result);
	}

}

class ClassLocInfo {

	private String className;
	private String sourcePath;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public ClassLocInfo(String className, String sourcePath) {
		this.className = className;
		this.sourcePath = sourcePath;
	}

}
