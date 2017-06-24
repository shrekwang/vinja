package com.github.vinja.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.io.FilenameUtils;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.compiler.CompilerContextManager;
import com.github.vinja.exception.LocationNotFoundException;
import com.github.vinja.omni.ClassInfo;
import com.github.vinja.omni.ClassInfoUtil;
import com.github.vinja.omni.ClassMetaInfoManager;
import com.github.vinja.omni.JavaExpUtil;
import com.github.vinja.util.DecompileUtil;
import com.github.vinja.util.LRUCache;
import com.github.vinja.util.ModifierFilter;


public class JavaSourceSearcher implements IJavaSourceSearcher {
    
    public static final String NULL_TYPE = "NULL";

    private List<LocalVariableInfo> visibleVars = null;
    private ParseResult parseResult = null;
    private CompilerContext ctx  = null;
    private String currentFileName;
    private String curFullClassName ;
    
    private List<String> importedNames = new ArrayList<String>();
    private List<MemberInfo> memberInfos = new ArrayList<MemberInfo>();
    private List<String> staticImportedNames = new ArrayList<String>();
    public static LRUCache<String, JavaSourceSearcher> cache = new LRUCache<String,JavaSourceSearcher>(500);
    
    private int classScopeLine = 1;
    
    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#getMemberInfos()
	 */
    @Override
	public List<MemberInfo> getMemberInfos() {
    	return this.memberInfos;
    }
    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#getImportedNames()
	 */
	public List<String> getImportedNames() {
    	return this.importedNames;
    }
    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#getClassScopeLine()
	 */
    @Override
	public int getClassScopeLine() {
    	return classScopeLine;
    }
    
    public static JavaSourceSearcher createSearcher(String filename, CompilerContext ctx) {
    	JavaSourceSearcher result = cache.get(filename);
		if (result == null) {
			result = new JavaSourceSearcher(filename,ctx);
			cache.put(filename, result);
		}
		return result;
    }
    
    public static void clearSearcher(String filename) {
    	cache.remove(filename);
    }

	private JavaSourceSearcher(String filename, CompilerContext ctx) {
		this.ctx = findProperContext(filename,ctx);
		this.currentFileName = filename;
		this.curFullClassName = this.ctx.buildClassName(filename);

		//filename could be a jar entry path like below
		// jar://C:\Java\jdk1.6.0_29\src.zip!java/lang/String.java
		if (filename.startsWith("jar:")) {
			JarFile jarFile = null;
			try {
				String jarPath = filename.substring(6,filename.lastIndexOf("!"));
				jarFile = new JarFile(jarPath);
				String entryName = filename .substring(filename.lastIndexOf("!") + 1);
				entryName = entryName.replace("\\", "/");
				ZipEntry zipEntry = jarFile.getEntry(entryName);
                InputStream is = null;
				if (entryName.endsWith(".java")) {
                    is = jarFile.getInputStream(zipEntry);
				} else {
					String classContent = DecompileUtil.decompile(jarPath, entryName);
					is = new ByteArrayInputStream(classContent.getBytes(StandardCharsets.UTF_8));
				}
				parseResult = AstTreeFactory.getJavaSourceAst(is, this.ctx.getEncoding());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (jarFile != null) try {jarFile.close(); } catch (Exception e) {}
			}
		} else {
			parseResult = AstTreeFactory.getJavaSourceAst(filename);
		}
		if (!parseResult.hasError()) {
			CommonTree tree = parseResult.getTree();
			readClassInfo(tree,this.memberInfos);
		}

	}
	
	private CompilerContext findProperContext(String fileName, CompilerContext ctx) {
		
		if (fileName.startsWith("jar:")) {
			return ctx;
		}
		if (FilenameUtils.normalize(fileName).startsWith(ctx.getProjectRoot())) {
			return ctx;
		}
		CompilerContext tmp = ctx;
		String classPath = null;
		File file=new File(fileName);
		while (true) {
			File parentFile =file.getParentFile();
			if (parentFile == null ) break;
			File classpathFile = new File(parentFile,".classpath");
			if (classpathFile.exists()) {
				classPath = classpathFile.getAbsoluteFile().getAbsolutePath();
				break;
			}
			file = parentFile;
		}
		if (classPath !=null) {
			tmp = CompilerContextManager.getInstnace().getCompilerContext(classPath);
		}
		
		return tmp;
	}
	
	/* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#getMemberInfo(java.lang.Class, boolean, boolean)
	 */
	@Override
	@SuppressWarnings("all")
	public ArrayList<com.github.vinja.omni.MemberInfo> getMemberInfo(Class aClass,
			boolean staticMember, boolean protectedMember) {
		
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
	
	/* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#getConstructorInfo()
	 */
	@Override
	@SuppressWarnings("all")
	public  ArrayList<com.github.vinja.omni.MemberInfo> getConstructorInfo() {
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
	
	public static boolean isValidateModifier(boolean staticMember,boolean protectedMember,String mod) {
		if (staticMember && ! (mod.indexOf("static") > -1) ) return false;
		if (protectedMember ) {
			if ( mod.indexOf("protected") > -1 || mod.indexOf("public") > -1 ) return true;
		} else {
			if (mod.indexOf("public") > -1 ) return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchLoopOutLine(int)
	 */
	@Override
	public int searchLoopOutLine(int currentLine) {
		CommonTree tree = parseResult.getTree();
		LinkedList<CommonTree> currentLineNodes = new LinkedList<CommonTree>();
		
		searchAllNodeAtLine(tree, currentLineNodes, currentLine); 
		CommonTree parentNode = currentLineNodes.get(0);
		CommonTree childNode = null;
		boolean foundOut = false;
		while (true) {
			int nodeType = parentNode.getType();
			if (nodeType == JavaParser.WHILE || 
					nodeType == JavaParser.FOR || nodeType == JavaParser.FOR_EACH) {
				foundOut = true;
				break;
			}
			if (parentNode.getParent() == null) break;
			childNode = parentNode;
			parentNode = (CommonTree)parentNode.getParent();
				
		}
		if (!foundOut) return -1;
		
		while (childNode.getChildIndex() == parentNode.getChildCount()-1) {
			childNode = parentNode;
			parentNode = (CommonTree)parentNode.getParent();
		}
		CommonTree nextNode = (CommonTree)parentNode.getChild(childNode.getChildIndex()+1);
		return getNodeLine(nextNode);
	}
	
	private int getNodeLine(CommonTree node) {
		if (node.getLine() > 0) return node.getLine();
		for (int i=0; i<node.getChildCount(); i++) {
			CommonTree childNode = (CommonTree)node.getChild(i);
			int childNodeLine = getNodeLine(childNode);
			if (childNodeLine > 0) return childNodeLine;
		}
		return -1;
	}
	

	/* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchNearByExps(int, boolean)
	 */
	@Override
	public Set<String> searchNearByExps(int lineNum, boolean currentLine) {
		CommonTree tree = parseResult.getTree();
		
		LinkedList<CommonTree> nodes =  new LinkedList<CommonTree>();
		Set<String> exps = new LinkedHashSet<String>();
		
		searchAllNodeAtLine(tree,nodes,lineNum);
		for (CommonTree node : nodes) {
			searchNearByExps(exps, node,currentLine,lineNum);
		}
		return exps;
	}
	
	private void searchNearByExps(Set<String> exps,CommonTree node, boolean currentLine,int lineNum) {
		
		CommonTree tmpNode = null;
		if (node.getType() == JavaParser.ASSIGN) {
			
			if (currentLine ) {
				tmpNode = (CommonTree)node.getChild(1);
				if (tmpNode.getType() != JavaParser.METHOD_CALL) {
					exps.addAll(getNodeText(tmpNode,false));
				}
			} else {
				tmpNode = (CommonTree)node.getChild(0);
				if (tmpNode.getType() != JavaParser.METHOD_CALL) {
					exps.addAll(getNodeText(tmpNode,false));
				}
			}
		} else if (node.getType() == JavaParser.METHOD_CALL) {
			
			//invoke obj
		    /* do not invoke method when doing a quick eval
			tmpNode = (CommonTree)node.getChild(0);
			if (tmpNode.getType() == JavaParser.DOT) {
				tmpNode = (CommonTree)tmpNode.getChild(0);
				exps.addAll(getNodeText(tmpNode,false));
			}
			*/
			//arg list
			tmpNode = (CommonTree)node.getChild(1);
			exps.addAll(getNodeText(tmpNode,false));
		} else if (node.getType() == JavaParser.RETURN) {
			tmpNode = (CommonTree)node.getChild(0);
			if (tmpNode.getType() != JavaParser.METHOD_CALL) {
				exps.addAll(getNodeText(tmpNode));
			}
		} else if (node.getType() == JavaParser.VAR_DECLARATOR) {
			
			if (currentLine ) {
				tmpNode = (CommonTree)node.getChild(1);
				CommonTree tmpChildNode = (CommonTree)tmpNode.getChild(0);
				if (tmpChildNode !=null && tmpChildNode.getType() != JavaParser.CLASS_CONSTRUCTOR_CALL) {
                    exps.addAll(getNodeText(tmpNode,false));
				}
			} else {
				tmpNode = (CommonTree)node.getChild(0);
				if (tmpNode.getType() != JavaParser.METHOD_CALL) {
					exps.addAll(getNodeText(tmpNode,false));
				}
			}
			
		} else if (node.getType() ==  JavaParser.NOT_EQUAL
				|| node.getType()== JavaParser.EQUAL
				|| node.getType() == JavaParser.GREATER_THAN
				|| node.getType() == JavaParser.GREATER_OR_EQUAL
				|| node.getType() == JavaParser.LESS_THAN
				|| node.getType() == JavaParser.LESS_OR_EQUAL) {
			
			tmpNode = (CommonTree)node.getChild(0);
			if (tmpNode.getType() != JavaParser.METHOD_CALL) {
				exps.addAll(getNodeText(tmpNode));
			}
			
			if (tmpNode.getType() != JavaParser.METHOD_CALL) {
				tmpNode = (CommonTree)node.getChild(1);
			}
			exps.addAll(getNodeText(tmpNode,false));
		}
		for (int i=0; i<node.getChildCount(); i++) {
			CommonTree subNode = (CommonTree) node.getChild(i);
			if (subNode.getLine() == lineNum) {
				searchNearByExps(exps,subNode,currentLine,lineNum );
			}
		}
	}
	private List<String> getNodeText(CommonTree node) {
		return getNodeText(node,true);
	}
	
	private List<String> getNodeText(CommonTree node,boolean allNode) {
		List<String> result = new ArrayList<String>();
		if (node == null ) return result;
		if (node.getType() == JavaParser.ARGUMENT_LIST) {
			for (int i=0; i<node.getChildCount(); i++) {
				CommonTree subNode = (CommonTree)node.getChild(i);
				result.addAll(getNodeText(subNode,allNode));
			}
		} else if (node.getType() == JavaParser.METHOD_CALL){
			StringBuilder sb = new StringBuilder();
			CommonTree dotNode = (CommonTree) node.getChild(0);
			CommonTree argNode = (CommonTree) node.getChild(1);
			String p1 = getNodeText(dotNode).get(0);
			List<String> p2 = getNodeText(argNode);
			sb.append(p1);
			sb.append("(");
			int count = 0;
			for (String t: p2) {
				count++;
				if (t ==null || t.trim().equals("")) continue;
				sb.append(t);
				if (count < p2.size()-1) { sb.append(","); }
			}
			sb.append(")");
			result.add(sb.toString());
		} else if (node.getType() == JavaParser.ARRAY_ELEMENT_ACCESS) {
			StringBuilder sb = new StringBuilder();
			CommonTree objNode = (CommonTree) node.getChild(0);
			CommonTree eleNode = (CommonTree) node.getChild(1);
			String p1 = getNodeText(objNode).get(0);
			String p2 = getNodeText(eleNode).get(0);
			sb.append(p1);
			sb.append("[");
			sb.append(p2);
			sb.append("]");
			result.add(sb.toString());
		} else if (node.getType() == JavaParser.DOT ) {
			CommonTree objNode = (CommonTree) node.getChild(0);
			CommonTree memberNode = (CommonTree) node.getChild(1);
			String p1 = getNodeText(objNode).get(0);
			String p2 = getNodeText(memberNode).get(0);
			result.add(p1+"."+p2);
		} else if (node.getType() == JavaParser.EXPR) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<node.getChildCount(); i++) {
				CommonTree subNode = (CommonTree) node.getChild(i);
				List<String> subTexts = getNodeText(subNode,allNode);
				result.addAll(subTexts);
				result.add(sb.toString());
			}
		} else if (
				node.getType() == JavaParser.PLUS
				|| node.getType() == JavaParser.MINUS
				|| node.getType() == JavaParser.PLUS
				|| node.getType() == JavaParser.MINUS
				|| node.getType() == JavaParser.STAR
				|| node.getType() == JavaParser.DIV
				|| node.getType() == JavaParser.MOD
				|| node.getType()== JavaParser.NOT_EQUAL
				|| node.getType()== JavaParser.GREATER_THAN
				|| node.getType() == JavaParser.GREATER_OR_EQUAL
				|| node.getType() == JavaParser.LESS_THAN
				|| node.getType() == JavaParser.LESS_OR_EQUAL
				|| node.getType() == JavaParser.LOGICAL_AND
				|| node.getType() == JavaParser.LOGICAL_OR
				|| node.getType() == JavaParser.AND
				|| node.getType() == JavaParser.OR ) {
			StringBuilder sb = new StringBuilder();
			CommonTree opNode1 = (CommonTree) node.getChild(0);
			CommonTree opNode2 = (CommonTree) node.getChild(1);
			String p1 = getNodeText(opNode1).get(0);
			String p2 = getNodeText(opNode2).get(0);
			sb.append(p1);
			sb.append(node.getText());
			sb.append(p2);
			result.add(sb.toString());
		} else {
			if (!( node.getType() == JavaParser.STRING_LITERAL
					|| node.getType() == JavaParser.CHARACTER_LITERAL
					|| node.getType() == JavaParser.DECIMAL_LITERAL
					|| node.getType() == JavaParser.HEX_LITERAL
					|| node.getType() == JavaParser.OCTAL_LITERAL
					|| node.getType() == JavaParser.FLOATING_POINT_LITERAL
					|| node.getType() == JavaParser.NULL
					|| node.getType() == JavaParser.STATIC_ARRAY_CREATOR
					|| node.getType() == JavaParser.POST_DEC
					|| node.getType() == JavaParser.POST_INC
					|| node.getType() == JavaParser.UNARY_MINUS ) || allNode) {
				result.add(node.getText().trim());
			}
		}
		return result;
	}
	
    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchDefLocation(int, int, java.lang.String)
	 */
    @Override
	public LocationInfo searchDefLocation(int line, int col,String sourceType) {
    	
        CommonTree tree = parseResult.getTree();
        CommonTree node = searchMatchedNode(tree,line,col);
        if (node == null ) {
            System.out.println("can't find node here");
            return null;
        }
        visibleVars = parseAllVisibleVar(node);
        try {
	        LocationInfo info = searchNodeDefLocation(node,sourceType);
	        return info;
        } catch (LocationNotFoundException e) {
        	return null;
        }
    }

    @SuppressWarnings("all")
    private String getClassFilePath(String className) {
    	String path = ctx.findSourceOrBinPath(className);
    	
    	//try same package class
    	if (path.equals("None")) {
	    	String classFullName  = ctx.buildClassName(this.currentFileName);
	    	if (classFullName !=null && classFullName.lastIndexOf(".") > -1 ) {
	    		String packageName = classFullName.substring(0,classFullName.lastIndexOf("."));
	    		path = ctx.findSourceOrBinPath(packageName+"."+className);
	    	}
    	}
    	// try inner class
    	if (path.equals("None")) {
	    	String classFullName  = ctx.buildClassName(this.currentFileName);
	    	while (true) {
	    		Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[]{classFullName}, null);
		    	classFullName = classFullName +"$"+className;
	    		path = ctx.findSourceOrBinPath(classFullName);
	    		if (!path.equals("None")) break;
	    		if (aClass == null || aClass.equals("java.lang.Object")) break;
	    		aClass = aClass.getSuperclass();
	    		if (aClass == null || aClass.equals("java.lang.Object")) break;
	    		classFullName = aClass.getCanonicalName();
	    	}
    	}
	    	
    	//try class under the java.lang package
    	if (path.equals("None")) {
    		className = "java.lang." + className;
    		path = ctx.findSourceOrBinPath(className);
    	}
        return path;
    }


    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchNodeDefLocation(org.antlr.runtime.tree.CommonTree, java.lang.String)
	 */
	public LocationInfo searchNodeDefLocation(CommonTree node,String sourceType) {
        LocationInfo info = new LocationInfo();

        if (node.getType() == JavaParser.IDENT) {
            CommonTree parent = (CommonTree)node.getParent();
            if (parent.getType() == JavaParser.METHOD_CALL) {
                String methodName = parent.getChild(0).getText();
                List<String> typenameList = parseArgumentTypenameList((CommonTree)parent.getChild(1));
                searchMethod(methodName, typenameList, info);
			} else if (parent.getType() == JavaParser.DOT) {
				CommonTree leftNode = (CommonTree) parent.getChild(0);
				String leftNodeTypeName = parseNodeTypeName(leftNode);

				String leftNodeFilePath = getClassFilePath(leftNodeTypeName);

				if (leftNodeFilePath == null || leftNodeFilePath.equals("None")) {
					// try inner class
					int dotIndex = leftNodeTypeName.indexOf(".");
					if (dotIndex > 0) {
						String outterClass = this.convertTypeName(leftNodeTypeName.substring(0, dotIndex));
						leftNodeFilePath = getClassFilePath(outterClass);
						String innerClass = leftNodeTypeName .substring(dotIndex + 1);
						
						if (node.getCharPositionInLine() > parent .getCharPositionInLine()) { 
							CommonTree rightNode = (CommonTree) parent.getChild(1);
							MemberType memberType = MemberType.FIELD;
							String memberName = rightNode.getText();
							
							JavaSourceSearcher searcher = createSearcher(leftNodeFilePath, ctx);
					        memberType = MemberType.FIELD;
					        searcher.searchMemberInHierachy(outterClass, memberType, innerClass, null,info);
					        
					        MemberInfo memberInfo = info.getMemberInfo();
					        if (memberInfo != null) {
					        	List<MemberInfo> subMembers = memberInfo.getSubMemberList();
					        	if (subMembers.size() > 0) {
					        		for (MemberInfo subMember: subMembers) {
					        			if (subMember.getName().equals(memberName)) {
					        				info.setLine(subMember.getLineNum());
					    					info.setCol(subMember.getColumn());
					    					info.setMemberInfo(subMember);
					        			}
					        		}
					        	}
					        }
						} else {
							info = searchIdentDefLocation(node);
						}
					} else {
						throw new LocationNotFoundException();
					}
				} else { 

					info.setFilePath(leftNodeFilePath);

					if (node.getCharPositionInLine() > parent .getCharPositionInLine()) {
						CommonTree rightNode = (CommonTree) parent.getChild(1);
						CommonTree pparent = (CommonTree) parent.getParent();
						MemberType memberType = MemberType.FIELD;
						String memberName = rightNode.getText();
						List<String> typenameList = null;

						if (pparent.getType() == JavaParser.METHOD_CALL) {
							memberType = MemberType.METHOD;
							typenameList = parseArgumentTypenameList((CommonTree) pparent.getChild(1));
						}

						if (sourceType != null && sourceType.equals("impl")) {
							ClassMetaInfoManager cmm = this.ctx.getClassMetaInfoManager();
							ClassInfo classInfo = cmm.getMetaInfo(leftNodeTypeName);
							if (classInfo != null) {
								Set<String> subNames = classInfo.getSubNames();
								if (subNames.size() == 1) {
									leftNodeTypeName = subNames.toArray(new String[] {})[0];
								}
							}
						}
						searchMemberInHierachy(leftNodeTypeName, memberType, memberName, typenameList, info);
					} 
					else {
						// if cursor is under the left node , only locate to
						// class level, no need to locate the member of the
						// class
						info = searchIdentDefLocation(node);
					}
				}

			} else if (parent.getType() == JavaParser.QUALIFIED_TYPE_IDENT ) {
            		if ( parent.getParent().getType() == JavaParser.CLASS_CONSTRUCTOR_CALL ) {
            			//search constructor call like " new ArrayList()" or " View.OnClickListener() "
            			String className = convertTypeName(node.getText());
            			if (parent.getChildCount() > 1) {
            				className = convertTypeName(parent.getChild(0).getText());
            				if (node.getChildIndex() == 1) {
	            				className = className + "$" + node.getText();
            				}
            			}
		            	info = searchConstructorDefLocation(className, node); 	
            		} else if ( parent.getParent().getType() == JavaParser.TYPE
            				&& parent.getChildCount() > 1 ) {
            			//seachr static inner class like "Map.Entry"
		            	info = searchInnerClassDefLocation(parent); 	
            		} else {
		            	info = searchIdentDefLocation(node);
            		}
            } else {
            	info = searchIdentDefLocation(node);
            }
        }
        return info;
    }
    
    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchInnerClassDefLocation(org.antlr.runtime.tree.CommonTree)
	 */
	public LocationInfo searchInnerClassDefLocation(CommonTree node) {
    	LocationInfo info = new LocationInfo();
    	CommonTree outterClassNode = (CommonTree)node.getChild(0);
    	CommonTree innerClassNode = (CommonTree)node.getChild(1);
    	
    	String innerClassName = innerClassNode.getText();
    	
    	String className = convertTypeName(outterClassNode.getText());
        String classPath = getClassFilePath(className);
        info.setFilePath(classPath);
        
        JavaSourceSearcher searcher = createSearcher(classPath, ctx);
        MemberType memberType = MemberType.FIELD;
        
        searcher.searchMemberInHierachy(outterClassNode.getText(), memberType, innerClassName, null,info);
        
        return info;
    }
    
    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchConstructorDefLocation(java.lang.String, org.antlr.runtime.tree.CommonTree)
	 */
	public LocationInfo searchConstructorDefLocation(String className, CommonTree node) {
    	LocationInfo info = new LocationInfo();
    	
        String classPath = getClassFilePath(className);
        //can't find classpath , try imported names
        if (classPath.equals("None") && className.indexOf(".") < 0 ) {
        	boolean found = false;
        	for (String importedName: this.importedNames) {
        		String lastName = importedName.substring(importedName.lastIndexOf(".")+1);
        		if (lastName.equals("*")) {
        			className = importedName.substring(0,importedName.lastIndexOf(".")+1) + className;
        			found = true;
        			break;
        		}
        	}
        	if (found) classPath = getClassFilePath(className);
        }
        info.setFilePath(classPath);
        CommonTree parent = (CommonTree)node.getParent(); 
        List<String> typenameList = parseArgumentTypenameList((CommonTree)parent.getParent().getChild(1));
        
        String constructName = className;
        if (className.indexOf(".") > -1 ) {
        	constructName = className.substring(className.lastIndexOf(".")+1);
        }
        IJavaSourceSearcher searcher = createSearcher(classPath, ctx);
        List<MemberInfo> leftClassMembers = searcher.getMemberInfos();
        MemberInfo memberInfo = findMatchedMethod(constructName, typenameList,leftClassMembers);
        if (memberInfo != null ) {
            info.setLine(memberInfo.getLineNum());
            info.setCol(memberInfo.getColumn());
        }
        return info;
    }
    
    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchIdentDefLocation(org.antlr.runtime.tree.CommonTree)
	 */
	public LocationInfo searchIdentDefLocation(CommonTree node) {
    	LocationInfo info = new LocationInfo();
        //searching order
    	//1: local variable and fields (done)
    	//2: fields in parent class
    	//3: imported class
    	//4: class name under the same package
    	//5: static imported name
    	boolean found = false;
        for (LocalVariableInfo var : visibleVars) {
            if (var.getName().equals(node.getText())) {
                info.setCol(var.getCol());
                info.setLine(var.getLine());
                info.setFilePath(currentFileName);
                found = true;
                break;
            }
        }
        if (!found ) {
        	for (String importedName: this.importedNames) {
        		String lastName = importedName.substring(importedName.lastIndexOf(".")+1);
        		String matchedName = null;
        		if (importedName.endsWith("*") ) {
        			matchedName = importedName.substring(0,importedName.lastIndexOf(".")+1) + node.getText();
        		}
        		if (node.getText().equals(lastName)) {
        			matchedName = importedName;
        		}
        		if (matchedName != null) {
        			String path = this.getClassFilePath(matchedName);
        			if (path != null && !path.equals("None")) {
	        			info.setFilePath(path);
	                    IJavaSourceSearcher searcher = createSearcher(info.getFilePath(), ctx);
	        			info.setLine(searcher.getClassScopeLine());
	        			info.setCol(1);
	        			found = true ;
	        			break;
        			}
        		}
        	}
       }
       if (!found) {
	   String classFullName  = ctx.buildClassName(this.currentFileName);
	    	if (classFullName.lastIndexOf(".") > -1 ) {
	    		String packageName = classFullName.substring(0,classFullName.lastIndexOf("."));
	    		String path = ctx.findSourceOrBinPath(packageName+"."+node.getText());
	    		if (!path.equals("None")) {
	    			info.setFilePath(path);
	                IJavaSourceSearcher searcher = createSearcher(info.getFilePath(), ctx);
	    			info.setLine(searcher.getClassScopeLine());
	    			info.setCol(1);
	    			found = true;
	    		}
	    	} 
       }
       if (!found) {
    	   String path = ctx.findSourceOrBinPath("java.lang."+node.getText());
	    		if (!path.equals("None")) {
	    			info.setFilePath(path);
	                IJavaSourceSearcher searcher = createSearcher(info.getFilePath(), ctx);
	    			info.setLine(searcher.getClassScopeLine());
	    			info.setCol(1);
	    			found = true;
	    		}
       }
       if (!found) {
    	   for (String importedName: this.staticImportedNames) {
       		String lastName = importedName.substring(importedName.lastIndexOf(".")+1);
       		if (node.getText().equals(lastName)) {
       			importedName = importedName.substring(0, importedName.lastIndexOf("."));
       			info.setFilePath(this.getClassFilePath(importedName));
                IJavaSourceSearcher searcher = createSearcher(info.getFilePath(), ctx);
                MemberInfo memberInfo = findMatchedField(node.getText(), searcher.getMemberInfos());
				if (memberInfo != null) {
					info.setLine(memberInfo.getLineNum());
					info.setCol(memberInfo.getColumn());
					found = true;
					break;
				}
       		}
       	}
       }
       if (!found) {
    	   MemberType memberType = MemberType.FIELD;
           searchMemberInHierachy(this.curFullClassName, memberType, node.getText(), null,info);
       }
       return info;
    
    }
    
    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchMethod(java.lang.String, java.util.List, com.github.vinja.parser.LocationInfo)
	 */
	public void searchMethod(String methodName,List<String> typenameList, LocationInfo info) {
        boolean isStaticImported = false;
        for (String importedName: this.staticImportedNames) {
       		String lastName = importedName.substring(importedName.lastIndexOf(".")+1);
       		if (methodName.equals(lastName)) {
       			isStaticImported = true;
       			importedName = importedName.substring(0, importedName.lastIndexOf("."));
       			info.setFilePath(this.getClassFilePath(importedName));
       			searchMemberInHierachy(importedName, MemberType.METHOD, methodName, typenameList, info);
       		}
        }
        if (!isStaticImported) {
            MemberType memberType = MemberType.METHOD;
            searchMemberInHierachy(this.curFullClassName, memberType, methodName, typenameList,info);
        }
    }
    
	/* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchMemberInHierachy(java.lang.String, com.github.vinja.parser.MemberType, java.lang.String, java.util.List, com.github.vinja.parser.LocationInfo)
	 */
	@SuppressWarnings("rawtypes")
	public boolean searchMemberInHierachy(String className,
			MemberType memberType, String memberName, 
			List<String> typenameList,LocationInfo info) {

		boolean found = searchMemberInClass(className,memberType, memberName, typenameList, info);
		if (found) return true;
		
		//if can't find, search the super class
	 	Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[]{className},null);
	 	if (! aClass.isInterface()) {
			if (aClass == null || aClass.getName().equals("java.lang.Object")) { return false; }
		 	aClass = aClass.getSuperclass();
			if (aClass == null || aClass.getName().equals("java.lang.Object")) { return false; }
		 	String superClassName = aClass.getCanonicalName();
		 	return searchMemberInHierachy(superClassName,memberType, memberName, typenameList, info);
	 	} else {
	 		Class[] classes = aClass.getInterfaces();
	 		for (Class clazz : classes) {
	 			String itfName = clazz.getCanonicalName();
	 			found = searchMemberInHierachy(itfName,memberType, memberName, typenameList, info);
	 			if (found) return true;
	 		}
	 	}
	 	return false;
	
	}
	
	/* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#searchMemberInClass(java.lang.String, com.github.vinja.parser.MemberType, java.lang.String, java.util.List, com.github.vinja.parser.LocationInfo)
	 */
	public boolean searchMemberInClass(String className,
			MemberType memberType, String memberName, 
			List<String> typenameList,LocationInfo info ) {
		
		String classFilePath = getClassFilePath(className);
		if (classFilePath == null || classFilePath.equals("None"))
			throw new LocationNotFoundException();

		IJavaSourceSearcher searcher = createSearcher(classFilePath, ctx);
		List<MemberInfo> leftClassMembers = searcher.getMemberInfos();
		
		//if classname not equals to filename, find member in subclass or inner enum .
		if (!FilenameUtils.getBaseName(classFilePath).equals(className)) {
			if (className.indexOf("$") > 0) className = className.substring(className.indexOf("$")+1);
			for (MemberInfo classMember : leftClassMembers) {
				if ((classMember.getMemberType() == MemberType.ENUM
						|| classMember.getMemberType() == MemberType.SUBCLASS)
						&& classMember.getName().equals(className)){
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
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#findFiledDefInItf(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("rawtypes")
	public LocationInfo findFiledDefInItf(String className,String fieldName) {
		Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[]{className},null);
		if (aClass != null ) {
			Class[] interfaces = aClass.getInterfaces();
			for (Class itfClazz : interfaces) {
				String itfName = itfClazz.getCanonicalName();
				String classFilePath = getClassFilePath(itfName);
				IJavaSourceSearcher searcher = createSearcher(classFilePath, ctx);
				List<MemberInfo> leftClassMembers = searcher.getMemberInfos();
				MemberInfo memberInfo = findMatchedField(fieldName, leftClassMembers);
				if (memberInfo != null) {
					LocationInfo info = new LocationInfo();
					info.setLine(memberInfo.getLineNum());
					info.setCol(memberInfo.getColumn());
					info.setFilePath(classFilePath);
					info.setMemberInfo(memberInfo);
					return info;
				}
			}
		}
		return null;
	}

    /* (non-Javadoc)
	 * @see com.github.vinja.parser.IJavaSourceSearcher#readClassInfo(org.antlr.runtime.tree.CommonTree, java.util.List)
	 */
	public void readClassInfo(CommonTree t,List<MemberInfo> memberInfos) {
        if ( t != null ) {
        	boolean needGoDeeper = true;
            if (t.getType() == JavaParser.CLASS_TOP_LEVEL_SCOPE
            		|| t.getType() == JavaParser.INTERFACE_TOP_LEVEL_SCOPE ) {
            	if (classScopeLine == 1) {
            		classScopeLine = t.getLine();
            	}
                int count = t.getChildCount();
                for (int i=0; i< count; i++) {
                    CommonTree child =(CommonTree)t.getChild(i);
                    MemberInfo info = null;
                    if (child.getType() == JavaParser.VOID_METHOD_DECL
                            || child.getType() == JavaParser.FUNCTION_METHOD_DECL) {
                        info = parseFuncDecl(child, MemberType.METHOD); 
                    } else if (child.getType() == JavaParser.CONSTRUCTOR_DECL) {
                        info = parseFuncDecl(child,MemberType.CONSTRUCTOR); 
                    } else if (child.getType() == JavaParser.VAR_DECLARATION) {
                        List<MemberInfo> infos = parseFieldDecl(child); 
                        memberInfos.addAll(infos);
                    } else if (child.getType() == JavaParser.ENUM) {
                    	info = parseEnumDecl(child);
                    } else if (child.getType() == JavaParser.CLASS || child.getType() == JavaParser.INTERFACE ) {
                    	info = new MemberInfo();
                    	for (int j=0; j<child.getChildCount(); j++) {
                    		CommonTree subChild = (CommonTree)child.getChild(j);
                    		if (subChild.getType() == JavaParser.IDENT) {
                    			info.setName(subChild.getText());
                    			info.setLineNum(subChild.getLine());
                    			info.setColumn(child.getCharPositionInLine());
	                    	 } else if (subChild.getType() == JavaParser.MODIFIER_LIST) {
	                            info.setModifierList( parseModifierList(subChild));
	                    	 }
                    	}
                    	List<MemberInfo> subMemberInfos = new ArrayList<MemberInfo>();
                    	readClassInfo(child, subMemberInfos);
                    	info.setMemberType(MemberType.SUBCLASS);
                    	info.setSubMemberList(subMemberInfos);
                    	needGoDeeper = false;
                    }
                    	
                    if (info !=null) {
	                    memberInfos.add(info);
                    }
                }
            }
            if (t.getType() == JavaParser.ENUM) {
            	for (int i=0; i<t.getChildCount(); i++) {
            		CommonTree child = (CommonTree)t.getChild(i);
            		 if (child.getType() == JavaParser.ENUM_TOP_LEVEL_SCOPE) {
            			for (int j=0; j<child.getChildCount(); j++) {
            				CommonTree enumChild = (CommonTree)child.getChild(j);
            				if (enumChild.getType() == JavaParser.IDENT) {
            					MemberInfo enumItem = new MemberInfo();
            					enumItem.setName(enumChild.getText());
            					enumItem.setLineNum(enumChild.getLine());
            					enumItem.setColumn(enumChild.getCharPositionInLine());
            					enumItem.setRtnType("enum");
            					memberInfos.add(enumItem);
            				}
            			}
            		}
            	}
            }
            	
            if (t.getType() ==  JavaParser.IMPORT) {
            	if (t.getChild(0).getText().equals("static")) {
            		StringBuilder sb = new StringBuilder();
	                buildImportStr((CommonTree)t.getChild(1),sb);
	                if (sb.length()> 0) {
		                sb.deleteCharAt(0);
		                staticImportedNames.add(sb.toString());
	                }
            	} else {
	                StringBuilder sb = new StringBuilder();
	                buildImportStr((CommonTree)t.getChild(0),sb);
	                if (sb.length()> 0) {
		                sb.deleteCharAt(0);
		                if (t.getChildCount()> 1 && ((CommonTree)t.getChild(1)).getType() == JavaParser.DOTSTAR) {
		                	sb.append(".*");
		                }
		                importedNames.add(sb.toString());
	                }
            	}
	    	}
            if (needGoDeeper) {
	            for ( int i = 0; i < t.getChildCount(); i++ ) {
	            	readClassInfo((CommonTree)t.getChild(i),memberInfos);
	            }
            }
        }
    }
    
    private MemberInfo parseEnumDecl(CommonTree t) {
    	MemberInfo info = new MemberInfo();
    	info.setLineNum(t.getLine());
    	info.setMemberType(MemberType.ENUM);
    	List<MemberInfo> list = new ArrayList<MemberInfo>();
    	for (int i=0; i<t.getChildCount(); i++) {
    		CommonTree child = (CommonTree)t.getChild(i);
    		if (child.getType() == JavaParser.IDENT) {
    			info.setName(child.getText());
    			info.setColumn(child.getCharPositionInLine());
    			
    		} else if (child.getType() == JavaParser.ENUM_TOP_LEVEL_SCOPE) {
    			for (int j=0; j<child.getChildCount(); j++) {
    				CommonTree enumChild = (CommonTree)child.getChild(j);
    				if (enumChild.getType() == JavaParser.IDENT) {
    					MemberInfo enumItem = new MemberInfo();
    					enumItem.setName(enumChild.getText());
    					enumItem.setLineNum(enumChild.getLine());
    					enumItem.setColumn(enumChild.getCharPositionInLine());
    					enumItem.setRtnType("enum");
    					list.add(enumItem);
    				}
    				
    			}
    		}
    	}
    
    	info.setSubMemberList(list);
    	return info;
    }
    
   
    
   
    
    private void buildImportStr(CommonTree t, StringBuilder b) {
        if (t.getType() == JavaParser.IDENT) {
            b.append(".").append(t.getText());
        } else if (t.getType() == JavaParser.DOT) {
            for (int i=0; i<t.getChildCount(); i++) {
                buildImportStr((CommonTree)t.getChild(i), b);
            }
        }
    }

    private List<String> parseArgumentTypenameList(CommonTree tree) {
        List<String> typenameList = new ArrayList<String>();
        for (int i=0; i<tree.getChildCount(); i++) {
            CommonTree arguNode = (CommonTree) tree.getChild(i);
          
            String typeName = parseNodeTypeName(arguNode);
            typenameList.add(typeName);
        }
        return typenameList;
    }

    @SuppressWarnings("all")
    private String parseNodeTypeName(CommonTree node) {
        if (node.getType() == JavaParser.EXPR) {
            node = (CommonTree)node.getChild(0);
        }
       
        String typename = "";

        switch (node.getType()) {
            case JavaParser.PLUS :
                typename = parseNodeTypeName((CommonTree)node.getChild(0));
                break;
            case JavaParser.IDENT:
                typename = findvarType(node.getText());
                break;
            case JavaParser.DECIMAL_LITERAL :
            case JavaParser.HEX_LITERAL :
            case JavaParser.OCTAL_LITERAL:
                typename = "int";
                break;
            case JavaParser.CHARACTER_LITERAL:
                typename = "char";
                break;
            case JavaParser.STRING_LITERAL :
                typename = "String";
                break;
            case JavaParser.FLOATING_POINT_LITERAL :
                typename = "float";
                break;
            case JavaParser.METHOD_CALL:
            	CommonTree subNode0 = (CommonTree)node.getChild(0);
            	if ( subNode0.getType() == JavaParser.IDENT) {
	                String methodName = node.getChild(0).getText();
	                List<String> typenameList = parseArgumentTypenameList((CommonTree)node.getChild(1));
	                LocationInfo info = new LocationInfo();
	                searchMethod(methodName, typenameList, info);
	                MemberInfo memberInfo = info.getMemberInfo();
	                if (memberInfo != null )  {
	                    typename = memberInfo.getRtnType();
	                } else {
	                    typename = "unknow";
	                }
            	} else if (subNode0.getType() == JavaParser.DOT) {
            		typename = parseNodeTypeName((CommonTree)subNode0.getChild(0));
            		String methodName = subNode0.getChild(1).getText();
            		Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[]{typename}, null);
                	if (aClass != null) {
            			ModifierFilter filter = new ModifierFilter(false,true);
            			String memberType = "method";
            			aClass = JavaExpUtil.searchMemberInHierarchy(aClass, methodName ,memberType ,filter,false);
            			if (aClass != null) {
            				typename = aClass.getCanonicalName();
            			}
                	}
            	}
                break;
            case JavaParser.DOT:
            	String className=parseNodeTypeName((CommonTree)node.getChild(0));
            	if (className.equals("this")) className = this.curFullClassName;
            	String memberName = node.getChild(1).getText();
            	   
            	Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[]{className}, null);
            	if (aClass != null) {
        			ModifierFilter filter = new ModifierFilter(false,true);
        			String memberType = "field";
        			aClass = JavaExpUtil.searchMemberInHierarchy(aClass, memberName ,memberType ,filter,false);
        			if (aClass != null) {
        				//typename = aClass.getCanonicalName();
        				typename = aClass.getName();
        			}
            	}
            	break;
            case JavaParser.THIS:
                typename = "this";
                break;
            case JavaParser.SUPER:
                typename = "super";
                break;
            case JavaParser.NULL:
                typename = NULL_TYPE;
                break;
            case JavaParser.QUALIFIED_TYPE_IDENT:
            case JavaParser.TYPE:
            case JavaParser.CLASS_CONSTRUCTOR_CALL:
            case JavaParser.PARENTESIZED_EXPR:
            case JavaParser.CAST_EXPR :
            	typename = parseNodeTypeName((CommonTree)node.getChild(0));
            	break;
            default :
                typename = "unknown";
        }
        return convertTypeName(typename);


    }
    
    
    @SuppressWarnings("rawtypes")
	private String convertTypeName(String typeName) {
    	if (typeName.equals("this")) {
    		return ctx.buildClassName(this.currentFileName);
    	} 
    	if (typeName.equals("super")) {
    		Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[]{this.curFullClassName}, null);
    		return aClass.getSuperclass().getCanonicalName();
    	}
    	for (String importedName: this.importedNames) {
    		String lastName = importedName.substring(importedName.lastIndexOf(".")+1);
    		if (typeName.equals(lastName)) {
    			return importedName;
    		}
    	}
    	if (typeName.indexOf(".") < 0 && curFullClassName != null ) {
    		String packageName = "";
    		if (curFullClassName.indexOf(".")> 0) {
	    		packageName = curFullClassName.substring(0,curFullClassName.lastIndexOf("."));
    		}
    		Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[]{packageName+"."+typeName},null);
    		if (aClass != null) { 
    			return aClass.getCanonicalName(); 
			}
    	}
    	return typeName;
    }

    private MemberInfo findMatchedField(String fieldName,  List<MemberInfo> memberInfos) {
        for (MemberInfo member: memberInfos) { 
            if (member.getName().equals(fieldName) ) {
                return member;
            }
        }
        return null;
    }

    private MemberInfo findMatchedMethod(String methodName, List<String> argTypes, 
            List<MemberInfo> memberInfos) {
    	
    	List<MemberInfo> paramCountMatchedList = new ArrayList<MemberInfo>();
    	
        for (MemberInfo member: memberInfos) { 
            if (member.getName().equals(methodName)) {
                
                List<String[]> memberParamList = member.getParamList();
                if ( (argTypes == null || argTypes.size() == 0 )
                        && ( memberParamList == null || memberParamList.size() ==0)) {
                    return member;
                }
                
                int matchedCount = memberParamList.size();

                boolean hasVarArg = false;
                String lastDefType = null;
                if (memberParamList.size() > 0) {
                    lastDefType = memberParamList.get(memberParamList.size()-1)[0];
                    //vararg 
                    if (lastDefType.endsWith("...")) {
                        matchedCount = memberParamList.size() - 1 ;
                        hasVarArg = true;
                        lastDefType = lastDefType.substring(0,lastDefType.length()-3);
                    }
                }
                if (!hasVarArg) {
                    //如果没有变参,同时参数个数又不匹配,则匹配不成功
                    if (matchedCount != argTypes.size()) {
                        continue;
                    } else {
                        paramCountMatchedList.add(member);
                    }
                }
                
                boolean noMatch = false;
                int i;
                for (i=0; i<matchedCount; i++) {
                    String actTypeName = argTypes.get(i);
                    String defTypeName = memberParamList.get(i)[0];
                    if (!arguMatch(defTypeName, actTypeName)) {
                        noMatch = true;
                        break;
                    }
                }
                if (hasVarArg) {
                    while (true ) {
                        if (i >= argTypes.size()) break;
                        String actTypeName = argTypes.get(i);
                        if (!arguMatch(lastDefType,actTypeName)) {
                            noMatch = true;
                            break;
                        }
                        i = i + 1;
                    }
                }
                if (! noMatch) return member;
            }
        }
        if (paramCountMatchedList.size() > 0) {
        	return paramCountMatchedList.get(0);
        }
        return null;
    }

    private boolean arguMatch(String defTypeName, String actTypeName) {
    	if (defTypeName == null) return true;
    	if (actTypeName.equals("String") && defTypeName.equals("java.lang.String")) return true;
        if (defTypeName.equals(actTypeName)) return true;
        if (actTypeName.equals(NULL_TYPE)) return true;
        return false;
    }

    @SuppressWarnings("all")
    private String findvarType(String varName ) {
 
        for (LocalVariableInfo var : visibleVars) {
            if (var.getName().equals(varName)) {
                return var.getType();
            }
        }
        
    	Class aClass = ClassInfoUtil.getExistedClass(this.ctx, new String[]{this.curFullClassName}, this.currentFileName);
    	if (aClass != null) {
			ModifierFilter filter = new ModifierFilter(false,true);
			String memberType = "field";
			aClass = JavaExpUtil.searchMemberInHierarchy(aClass, varName ,memberType ,filter,false);
			if (aClass != null) {
				String className = aClass.getCanonicalName();
				return className;
			}
    	}
    	
		for (String importedName : this.staticImportedNames) {
			String lastName = importedName.substring(importedName .lastIndexOf(".") + 1);
   			importedName = importedName.substring(0, importedName.lastIndexOf("."));
			if (varName.equals(lastName)) {
				String filePath = this.getClassFilePath(importedName);
				IJavaSourceSearcher searcher = createSearcher(filePath, ctx);
				MemberInfo memberInfo = findMatchedField(varName, searcher.getMemberInfos());
				return memberInfo.getRtnType();
			}
		}
		return varName;
    }

    private CommonTree searchMatchedNode(CommonTree tree, int line, int col) {
          LinkedList<CommonTree> nodes = new LinkedList<CommonTree>();
          searchNodeAtLine(tree,nodes,line);
          CommonTree matchedNode = null;
          for (CommonTree t : nodes) {
              if (col >= t.getCharPositionInLine() ) {
                  matchedNode = t;
                  break;
              }
          }
          return matchedNode;
    }


    private void searchNodeAtLine(CommonTree t, LinkedList<CommonTree> list, int line) {
        if (t.getLine() == line)  {
            if (t.getChildCount() == 0 
            		|| t.getChild(0).getType() == JavaParser.GENERIC_TYPE_ARG_LIST) {
            	//FIXME : getChildCount equals to zero is not always right
                list.add(0,t);
            }
        }
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree)t.getChild(i);
            searchNodeAtLine(c,list,line);
        }
    }
    

    private void searchAllNodeAtLine(CommonTree t, LinkedList<CommonTree> list, int line) {
    	 if (t.getLine() == line)  {
             list.add(0,t);
         }
         for (int i=0; i<t.getChildCount(); i++) {
             CommonTree c = (CommonTree)t.getChild(i);
             searchAllNodeAtLine(c,list,line);
         }
    }

    
    private List<LocalVariableInfo> parseAllVisibleVar(CommonTree t) {
        List<LocalVariableInfo> infoList = new ArrayList<LocalVariableInfo>();
    	CommonTree parent = t;
    	while (true) {
	    	parent = (CommonTree)parent.getParent();
	    	if (parent == null) break;
	    	if (parent.getType() == JavaParser.FOR_EACH) {
                LocalVariableInfo info = new LocalVariableInfo();
	    		 for (int i=0; i<parent.getChildCount(); i++) {
    	            CommonTree c = (CommonTree) parent.getChild(i);
    	            if (c.getType() == JavaParser.LOCAL_MODIFIER_LIST) continue;
    	            if (c.getType() == JavaParser.TYPE) {
    	                info.setType(parseType(c));
    	            } else if (c.getType() == JavaParser.IDENT){
    	            	info.setName(c.getText());
    	            	info.setLine(c.getLine());
    	            	info.setCol(c.getCharPositionInLine());
    	            }
    	        }
	    		infoList.add(info);
	    	}
	    	for (int i=0; i < parent.getChildCount(); i++) {
	    		CommonTree child = (CommonTree) parent.getChild(i);
	    		if (child.getType() == JavaParser.VAR_DECLARATION) {
                    List<MemberInfo> membeInfos = parseFieldDecl(child);
                    for (MemberInfo membeInfo: membeInfos) {
	                    LocalVariableInfo info = new LocalVariableInfo();
	                    info.setName(membeInfo.getName());
	                    info.setType(membeInfo.getRtnType());
	                    info.setLine(membeInfo.getLineNum());
	                    info.setCol(membeInfo.getColumn());
	                    infoList.add(info);
                    }
	    		}
                if (child.getType() == JavaParser.FORMAL_PARAM_LIST) {
                    infoList.addAll(parseParamlist(child));
                }
                if (child.getType() == JavaParser.CATCH) { 
                    infoList.addAll(parseParamlist(child));
                }
                
	    	}
    	}
        return infoList;
    }

    

    private List<LocalVariableInfo> parseParamlist(CommonTree t) {
        List<LocalVariableInfo> result = new ArrayList<LocalVariableInfo>();
        for (int i=0; i< t.getChildCount(); i++) {
            CommonTree c = (CommonTree)t.getChild(i);
            if (c.getType() == JavaParser.FORMAL_PARAM_STD_DECL) {

                LocalVariableInfo info = new LocalVariableInfo();

                for (int j=0; j< c.getChildCount(); j++) {
                    CommonTree part = (CommonTree)c.getChild(j);
                    if (part.getType() == JavaParser.LOCAL_MODIFIER_LIST) continue;
                    if (part.getType() == JavaParser.TYPE) {
                        info.setType(parseType(part));
                    } else {
                        info.setCol(part.getCharPositionInLine());
                        info.setLine(part.getLine());
                        info.setName(part.getText());
                    }
                }
                result.add(info);
            }
        }
        return result;
    }

    private List<MemberInfo> parseFieldDecl(CommonTree t) {
    	
    	
        List<MemberInfo> infos = new ArrayList<MemberInfo>();
        String rtnType = "";
        List<String> modifierList = null;
        

        for (int i=0; i< t.getChildCount(); i++) {
            CommonTree c = (CommonTree)t.getChild(i);
            if (c.getType() == JavaParser.VAR_DECLARATOR_LIST) {
            	
            	for (int j=0; j<c.getChildCount(); j++ ) {
            		CommonTree tmp = (CommonTree)c.getChild(j);
	                CommonTree variableDeclaratorId = (CommonTree)tmp.getChild(0);
	                MemberInfo info = new MemberInfo();
			        info.setMemberType(MemberType.FIELD);
	                info.setLineNum(variableDeclaratorId.getLine());
	                info.setColumn(variableDeclaratorId.getCharPositionInLine());
	                info.setName(variableDeclaratorId.getText());
	                info.setRtnType(rtnType);
	                info.setModifierList(modifierList);
	                infos.add(info);
            	}
            } else if (c.getType() == JavaParser.MODIFIER_LIST) {
                //info.setModifierList( parseModifierList(c));
            	modifierList = parseModifierList(c);
            } else if (c.getType() == JavaParser.TYPE) {
                //info.setRtnType(parseType(c));
            	rtnType = parseType(c);
            }
        }
        return infos;
    }

    private MemberInfo parseFuncDecl(CommonTree t,MemberType memberType ) {

        MemberInfo info = new MemberInfo();
        info.setLineNum(t.getLine());
        info.setMemberType(memberType);
        if (memberType== MemberType.CONSTRUCTOR) {
        	if (curFullClassName !=null) {
	        	String className = curFullClassName;
	        	if (curFullClassName.lastIndexOf(".")> -1) {
	        		className = curFullClassName.substring(curFullClassName.lastIndexOf(".")+1);
	        	}
	        	info.setName(className);
        	} else {
        		String className = FilenameUtils.getBaseName(this.currentFileName);
        		info.setName(className);
        	}
        }

        for (int i=0; i< t.getChildCount(); i++) {
            CommonTree c = (CommonTree)t.getChild(i);
            if (c.getType() == JavaParser.IDENT) {
            	info.setColumn(c.getCharPositionInLine());
                info.setName(c.getText());
                info.setLineNum(c.getLine());
            } else if (c.getType() == JavaParser.MODIFIER_LIST) {
                info.setModifierList( parseModifierList(c));
            } else if (c.getType() == JavaParser.FORMAL_PARAM_LIST) {
                info.setParamList(parseFormalParamList(c));
            } else if (c.getType() == JavaParser.TYPE) {
                info.setRtnType(parseType(c));
            }
        }
        return info;
    }
    
    private String parseType(CommonTree t) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            if (c.getType() == JavaParser.QUALIFIED_TYPE_IDENT) {
                sb.append(parseQualifiedTypeIdent(c));
            } else if (c.getType() == JavaParser.ARRAY_DECLARATOR_LIST){
            	 sb.append(parseArrayDeclaratorList(c));
            } else {
                sb.append(c.getText());
            }
        }
        return convertTypeName(sb.toString());
    }
    
    private String parseArrayDeclaratorList(CommonTree t) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<t.getChildCount(); i++) {
            //CommonTree c = (CommonTree) t.getChild(i);
            sb.append("[]");
        }
        return sb.toString();
    }


    private String parseQualifiedTypeIdent(CommonTree t) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            if (i>0) sb.append(".");
            sb.append(parseTypeWithGenericInfo(c));
        }
        return sb.toString();
    }
    
    private String parseTypeWithGenericInfo(CommonTree t) {
    	if (t.getChildCount() == 0) return t.getText();
    		
    	StringBuilder sb = new StringBuilder(t.getText());
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            if (c.getType() == JavaParser.GENERIC_TYPE_ARG_LIST) {
            	sb.append("<");
		        for (int j=0; j<c.getChildCount(); j++) {
		            CommonTree genericTypeNode = (CommonTree) c.getChild(j);
		            if (j>0) sb.append(",");
		            if (genericTypeNode.getType() == JavaParser.QUESTION) {
		            	sb.append("?");
		            	if (genericTypeNode.getChildCount() > 0) {
			            	CommonTree extendsNode = (CommonTree)genericTypeNode.getChild(0);
			            	CommonTree typeNode = (CommonTree)extendsNode.getChild(0);
			            	sb.append(" extends ");
				        	sb.append(parseType(typeNode));
		            	}
		            } else {
			        	sb.append(parseType(genericTypeNode));
		            }
		        }
		        sb.append(">");
            }
        }
    	return sb.toString();
    }

    private List<String> parseModifierList(CommonTree t) {
        List<String> result = new ArrayList<String>();
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            if (c.getType() == JavaParser.AT) {
            	continue;
            }
            result.add(c.getText());
        }
        return result;
    }

    private List<String[]> parseFormalParamList(CommonTree t) {
        List<String[]> paramList = new ArrayList<String[]>();
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            String[] param = new String[2];
            if (c.getType() == JavaParser.FORMAL_PARAM_STD_DECL) {
                param = parseFormalParamStdDecl(c);
            }
            if (c.getType() == JavaParser.FORMAL_PARAM_VARARG_DECL) {
                param = parseFormalParamVarargDecl(c);
            }
            paramList.add(param);
        }
        return paramList;
    }
    
    private String[] parseFormalParamStdDecl(CommonTree t) {
        String[] param = new String[2];
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            if (c.getType() == JavaParser.LOCAL_MODIFIER_LIST) continue;
            if (c.getType() == JavaParser.TYPE) {
                param[0] = parseType(c);
            } else {
                param[1] = c.getText();
                if (c.getChildren()!=null) {
                	CommonTree tmpTree = (CommonTree)c.getChild(0);
                	if (tmpTree.getType() == JavaParser.ARRAY_DECLARATOR_LIST){
                		param[0] = param[0] + parseArrayDeclaratorList(tmpTree);
                	}
                }
            }
        }
        return param;
    }
   
    private String[] parseFormalParamVarargDecl(CommonTree t) {
        String[] param = new String[2];
        for (int i=0; i<t.getChildCount(); i++) {
            CommonTree c = (CommonTree) t.getChild(i);
            if (c.getType() == JavaParser.LOCAL_MODIFIER_LIST) continue;
            if (c.getType() == JavaParser.TYPE) {
                param[0] = parseType(c) + "...";
            } else {
                param[1] = c.getText();
                if (c.getChildren()!=null) {
                    CommonTree tmpTree = (CommonTree)c.getChild(0);
                    if (tmpTree.getType() == JavaParser.ARRAY_DECLARATOR_LIST){
                        param[0] = param[0] + parseArrayDeclaratorList(tmpTree);
                    }
                }
            }
        }
        return param;
    }


}
