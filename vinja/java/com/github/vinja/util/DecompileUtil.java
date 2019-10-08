package com.github.vinja.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.api.printer.Printer;

public class DecompileUtil {

	public static String decompile(final String jarPath, final String innerPath) throws Exception {

		
		Loader loader = new Loader() {
			@Override
			public byte[] load(String internalName) throws LoaderException {

				JarFile jarFile = null;
				try {

					jarFile = new JarFile(jarPath);
					ZipEntry zipEntry = jarFile.getEntry(innerPath);
					InputStream is = jarFile.getInputStream(zipEntry);

					try (InputStream in = is; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
						byte[] buffer = new byte[1024];
						int read = in.read(buffer);

						while (read > 0) {
							out.write(buffer, 0, read);
							read = in.read(buffer);
						}

						return out.toByteArray();
					} catch (IOException e) {
						throw new LoaderException(e);
					}
				} catch (Exception e) {
					throw new LoaderException(e);
				} finally {
					if (jarFile != null) {
						try { jarFile.close(); } catch (Exception e) { }
					}
				}
			}

			@Override
			public boolean canLoad(String internalName) {
				return this.getClass().getResource("/" + internalName + ".class") != null;
			}

		};

		Printer printer = new Printer() {
		    protected static final String TAB = "  ";
		    protected static final String NEWLINE = "\n";

		    protected int indentationCount = 0;
		    protected StringBuilder sb = new StringBuilder();

		    @Override public String toString() { return sb.toString(); }

		    @Override public void start(int maxLineNumber, int majorVersion, int minorVersion) {}
		    @Override public void end() {}

		    @Override public void printText(String text) { sb.append(text); }
		    @Override public void printNumericConstant(String constant) { sb.append(constant); }
		    @Override public void printStringConstant(String constant, String ownerInternalName) { sb.append(constant); }
		    @Override public void printKeyword(String keyword) { sb.append(keyword); }
		    @Override public void printDeclaration(int type, String internalTypeName, String name, String descriptor) { sb.append(name); }
		    @Override public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) { sb.append(name); }

		    @Override public void indent() { this.indentationCount++; }
		    @Override public void unindent() { this.indentationCount--; }

		    @Override public void startLine(int lineNumber) { for (int i=0; i<indentationCount; i++) sb.append(TAB); }
		    @Override public void endLine() { sb.append(NEWLINE); }
		    @Override public void extraLine(int count) { while (count-- > 0) sb.append(NEWLINE); }

		    @Override public void startMarker(int type) {}
		    @Override public void endMarker(int type) {}
		};

		Map<String, Object> configuration = new HashMap<>();
		configuration.put("realignLineNumbers", true);
		
		ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
	    String entryInternalName = innerPath.substring(0, innerPath.length() - 6); 
		decompiler.decompile(loader, printer, entryInternalName, configuration);
		String source = printer.toString();
		return source;
	}
	
	public static void main(String[] args) throws Exception {
		String result = DecompileUtil.decompile("/Users/wangsn/.m2/repository/com/tuya/atop_proxy/atop-service/1.0.1/atop-service-1.0.1.jar",
				"com/tuya/atop/service/alarm/NotifyServiceImpl.class");
		System.out.println(result);
	}
}
