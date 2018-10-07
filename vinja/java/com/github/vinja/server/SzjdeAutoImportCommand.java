package com.github.vinja.server;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.omni.ClassInfoUtil;
import com.github.vinja.omni.PackageInfo;

public class SzjdeAutoImportCommand extends SzjdeCommand {

	@Override
	@SuppressWarnings("rawtypes")
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String currentPkg = params.get(SzjdeConstants.PARAM_PKG_NAME);
		String tmpFilePath = params.get(SzjdeConstants.PARAM_TMP_FILE_PATH);

		String[] classNameList = params.get("classnames").split(",");
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		Class aClass = ClassInfoUtil.getExistedClass(classPathXml, classNameList, sourceFile);

		CompilerContext ctx = getCompilerContext(classPathXml);
		PackageInfo packageInfo = ctx.getPackageInfo();

		Set<String> varNames = searchImportedTokens(tmpFilePath);

		Set<Class> declaredClass = ClassInfoUtil.getAllDeclaredClass(aClass);

		StringBuilder sb = new StringBuilder();
		for (String varName : varNames) {
			varName = varName.trim();

			StringBuilder tmpSb = new StringBuilder();

			List<String> binClassNames = packageInfo.findPackage(varName);
			if (binClassNames.size() == 0)
				continue;
			boolean noNeedImport = false;
			for (String binClassName : binClassNames) {
				String pkgName = binClassName.substring(0, binClassName.lastIndexOf("."));
				if (pkgName.equals("java.lang") || pkgName.equals(currentPkg)) {
					noNeedImport = true;
					break;
				}
				tmpSb.append(binClassName).append(";");
			}

			for (Class clazz : declaredClass) {
				if (clazz.getCanonicalName().endsWith(varName)) {
					noNeedImport = true;
				}
			}

			if (noNeedImport)
				continue;
			sb.append(tmpSb.toString()).append("\n");
		}

		return sb.toString();
	}

	@SuppressWarnings("all")
	public static Set<String> searchImportedTokens(String sourcePath) {
		
		final Set<String> names = new HashSet<String>();

		try {
			CompilationUnit compilationUnit = JavaParser.parse(new File(sourcePath));
			VoidVisitorAdapter vistor = new VoidVisitorAdapter<Void>() {
				@Override
				public void visit(final ClassOrInterfaceType n, final Void arg) {
					names.add(n.getNameAsString());
					super.visit(n, arg);
				}
				@Override
				public void visit(final SingleMemberAnnotationExpr n, final Void arg) {
					names.add(n.getNameAsString());
					super.visit(n, arg);
				}
				@Override
				public void visit(final NormalAnnotationExpr n, final Void arg) {
					names.add(n.getNameAsString());
					super.visit(n, arg);	
				}

				@Override
				public void visit(final MarkerAnnotationExpr n, final Void arg) {
					names.add(n.getNameAsString());
					super.visit(n, arg);	
				}
				
				public void visit(final MethodCallExpr n, final Void arg) {
					if (n.getScope().isPresent()) {
						Expression exp = n.getScope().get();
						if (exp instanceof NameExpr) {
							String name = ((NameExpr)exp).getNameAsString();
							if (Character.isUpperCase(name.charAt(0))) {
								names.add(name);
							}
						}
					}
					super.visit(n, arg);
				}
				public void visit(final FieldAccessExpr n, final Void arg) {
					Expression exp = n.getScope();
					if (exp instanceof NameExpr) {
						String name = ((NameExpr)exp).getNameAsString();
						if (Character.isUpperCase(name.charAt(0))) {
							names.add(name);
						}
					}
					super.visit(n, arg);
				}
				
			};
			vistor.visit(compilationUnit, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return names;
	}

}
