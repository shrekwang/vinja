package com.google.code.vimsztool.omni;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.CompilerContextManager;
import com.google.code.vimsztool.compiler.ReflectAbleClassLoader;
import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.Paranamer;

public class ClassInfoUtil {
	
	private ClassInfoUtil() {}
	
	
	@SuppressWarnings("unchecked")
	public static String dumpClassInfo(Class aClass) {
		if (aClass == null ) return "";
		
		List<String> superClassNames = new ArrayList<String>();
		List<String> interfaceNames = new ArrayList<String>();
		Class tmpClass = aClass;
		StringBuilder sb=new StringBuilder();
		while (true) {
			if (tmpClass == null) break;
			superClassNames.add(tmpClass.getName());
			Class[] itfs=tmpClass.getInterfaces();
			for (Class itf : itfs ) {
				interfaceNames.add(itf.getName());
			}
			tmpClass=tmpClass.getSuperclass();
		}
		sb.append("Class ").append(aClass.getName()).append("\n\n");
		sb.append("Hierarchy: \n");
		for (String name : superClassNames) {
			sb.append("    ").append(name).append("\n");
		}
		sb.append("\n");
		sb.append("Interface: \n");
		for (String name : interfaceNames) {
			sb.append("    ").append(name).append("\n");
		}
		sb.append("\n");
		sb.append("Constructor: \n");
		List<MemberInfo> infos = getConstructorInfo(aClass);
		for (MemberInfo info : infos) {
			sb.append("    ").append(info.getFullDecleration()).append("\n");
		}
		sb.append("\n");
		sb.append("static members: \n");
		infos = getMemberInfo(aClass, true,false);
		for (MemberInfo info : infos) {
			sb.append("    ").append(info.getFullDecleration()).append("\n");
		}
		sb.append("\n");
		sb.append("non-static members: \n");
		infos = getMemberInfo(aClass, false,false);
		for (MemberInfo info : infos) {
			sb.append("    ").append(info.getFullDecleration()).append("\n");
		}
		sb.append("\n");
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static ArrayList<MemberInfo> getConstructorInfo(Class aClass) {
		ArrayList<MemberInfo> memberInfos=new ArrayList<MemberInfo>();
		Constructor[] constructors = aClass.getDeclaredConstructors();
		for (int i = 0; i < constructors.length; i++) {
			if (Modifier.isPublic(constructors[i].getModifiers())) {
				MemberInfo memberInfo=new MemberInfo();
				memberInfo.setMemberType(MemberInfo.TYPE_CONSTRUCTOR);
				memberInfo.setModifiers(modifiers(constructors[i].getModifiers()));
				memberInfo.setName(constructors[i].getName());
				memberInfo.setExceptions(getExceptionInfo(constructors[i]));
				memberInfo.setParams(getParameterInfo(constructors[i]));
				memberInfos.add(memberInfo);
			}
		}
		return memberInfos;
	}
	
	public static boolean isValidateModifier(boolean staticMember,boolean protectedMember,int mod) {
		if (staticMember && ! Modifier.isStatic(mod)) return false;
		//if (!staticMember && Modifier.isStatic(mod)) return false;
		if (protectedMember ) {
			if ( Modifier.isProtected(mod) || Modifier.isPublic(mod)) return true;
		} else {
			if (Modifier.isPublic(mod)) return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<MemberInfo> getMemberInfo(Class aClass,
			boolean staticMember, boolean protectedMember) {

		ArrayList<MemberInfo> memberInfos = new ArrayList<MemberInfo>();

		Field[] fields = aClass.getDeclaredFields(); // Look up fields.
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();
			if (!isValidateModifier(staticMember, protectedMember, mod))
				continue;
			MemberInfo memberInfo = new MemberInfo();
			memberInfo.setModifiers(modifiers(fields[i].getModifiers()));
			memberInfo.setMemberType(MemberInfo.TYPE_FIELD);
			memberInfo.setName(fields[i].getName());
			memberInfo.setReturnType(typeName(fields[i].getType()));
			memberInfos.add(memberInfo);
		}

		Method[] methods = aClass.getDeclaredMethods(); // Look up methods.
		for (int i = 0; i < methods.length; i++) {
			int mod = methods[i].getModifiers();
			if (!isValidateModifier(staticMember, protectedMember, mod))
				continue;
			MemberInfo memberInfo = new MemberInfo();
			memberInfo.setModifiers(modifiers(methods[i].getModifiers()));
			memberInfo.setMemberType(MemberInfo.TYPE_METHOD);
			memberInfo.setName(methods[i].getName());
			memberInfo.setReturnType(typeName(methods[i].getReturnType()));
			memberInfo.setExceptions(getExceptionInfo(methods[i]));
			memberInfo.setParams(getParameterInfo(methods[i]));
			memberInfos.add(memberInfo);
		}
		return memberInfos;

	}

	public static String typeName(Class t) {
		String brackets = "";
		while (t.isArray()) {
			brackets += "[]";
			t = t.getComponentType();
		}
		String name = t.getName();
		int pos = name.lastIndexOf('.');
		if (pos != -1)
			name = name.substring(pos + 1);
		return name + brackets;
	}

	public static String modifiers(int m) {
		if (m == 0)
			return "";
		else
			return Modifier.toString(m) + " ";
	}

	public static String getFieldFullDeclaration(Field f) {
		return "  " + modifiers(f.getModifiers())
				+ typeName(f.getType()) + " " + f.getName() + ";";
	}


	@SuppressWarnings("unchecked")
	public static String getParameterInfo(Member member) {
		Class parameters[] ;
		String[] paramnames;
		StringBuilder sb=new StringBuilder();
		Paranamer namer = new BytecodeReadingParanamer();
		if (member instanceof Method) {
			Method m = (Method) member;
			parameters = m.getParameterTypes();
			paramnames = namer.lookupParameterNames(m, false);
		} else {
			Constructor c = (Constructor) member;
			parameters = c.getParameterTypes();
			paramnames = namer.lookupParameterNames(c, false);
		}
		if (parameters.length > 0) {
			if (paramnames==null || paramnames.length ==0 ) {
				paramnames = createDefaultParamNames(parameters.length);
			}
		}

		for (int i = 0; i < parameters.length; i++) {
			if (i > 0) 
				sb.append(", ");
			sb.append(typeName(parameters[i])).append(" ");
			sb.append(paramnames[i]);
		}
		return sb.toString();
	}
	
	public static String[] createDefaultParamNames(int length) {
		String[] names = new String[length]; 
		for (int i=0; i<length; i++) {
			names[i] = "arg"+String.valueOf(i);
		}
		return names;
	}
	
	@SuppressWarnings("unchecked")
	public static String getExceptionInfo(Member member) {
		Class exceptions[];
		StringBuilder sb=new StringBuilder(" ");
		if (member instanceof Method) {
			Method m = (Method) member;
			exceptions = m.getExceptionTypes();
		} else {
			Constructor c = (Constructor) member;
			exceptions = c.getExceptionTypes();
		}
		if (exceptions.length < 1) return "";
		for (int i = 0; i < exceptions.length; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(typeName(exceptions[i]));
		}
		return sb.toString();
	}
	
	@SuppressWarnings("rawtypes")
	public static LinkedList<Class> getAllSuperClass(Class aClass) {
		LinkedList<Class> classList = new LinkedList<Class>();
		if (aClass == null) return classList;
		if (aClass.isInterface()) {
			classList.add(aClass);
		} else {
			Class tmpClass =  aClass;
			while (true) {
				classList.add(tmpClass);
				tmpClass =  tmpClass.getSuperclass();
				if (tmpClass == null) break;
				if (tmpClass.getName().equals("java.lang.Object")) break;
			}
		}
		addInterfaceToList(classList, aClass);
		return classList;
	}
	
	@SuppressWarnings("rawtypes")
	private static void addInterfaceToList(List<Class> result, Class aClass) {
		if (aClass ==null || aClass.getInterfaces() == null) return;
		for (Class intf : aClass.getInterfaces()) {
			result.add(intf);
			addInterfaceToList(result, intf);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static Class getExistedClass(String classPathXml , String[] classNameList,String sourceFile) {
		CompilerContextManager ccm = CompilerContextManager.getInstnace();
		CompilerContext ctx = ccm.getCompilerContext(classPathXml);
		return getExistedClass(ctx, classNameList, sourceFile);
	}
	
	@SuppressWarnings("rawtypes")
	public static Class getExistedClass(CompilerContext ctx , String[] classNameList,String sourceFile) {
		ReflectAbleClassLoader classLoader = ctx.getClassLoader();
		Class aClass = null;
		
		for (String className : classNameList) {
			if (className.equals("this") && sourceFile !=null ) {
				className = ctx.buildClassName(sourceFile);
			}
			try {
				aClass = classLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
				try {
					aClass = classLoader.loadClass("java.lang."+className);
				} catch (ClassNotFoundException e2) {
					try {
						String mainClass = ctx.buildClassName(sourceFile);
						aClass = classLoader.loadClass(mainClass+"$"+className);
					} catch (ClassNotFoundException e3) { }
				}
			}
			if (aClass != null) break;
		}
		return aClass;
	}
	

}
