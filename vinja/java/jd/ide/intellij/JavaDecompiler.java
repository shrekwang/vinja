package jd.ide.intellij;


public class JavaDecompiler {
	
	private static JavaDecompiler instance = null;

	public static JavaDecompiler getInstance(String path) {
		if (instance == null) {
			try {
				System.load(path);
				instance = new JavaDecompiler();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	/**
	 * Actual call to the native lib.
	 * 
	 * @param basePath
	 *            Path to the root of the classpath, either a path to a
	 *            directory or a path to a jar file.
	 * @param internalClassName
	 *            internal name of the class.
	 * @return Decompiled class text.
	 */
	public native String decompile(String basePath, String internalClassName);

	/**
	 * @return version of JD-Core
	 * @since JD-Core 0.7.0
	 */
	public native String getVersion();

	
	public static void main(String[] args) {
		
		JavaDecompiler decompiler = JavaDecompiler.getInstance("C:\\project\\vinja\\vinja\\lib\\jdcore-native\\win32\\x86\\jd-intellij.dll");
		String t=decompiler.decompile("C:\\project\\maven\\repo\\org\\apache\\hbase\\hbase\\0.92.1\\hbase-0.92.1.jar", "org/apache/hadoop/hbase/client/HBaseAdmin.class");
		System.out.println(t);
	}
	
	
}
