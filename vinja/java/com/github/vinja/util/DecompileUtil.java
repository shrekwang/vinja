package com.github.vinja.util;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class DecompileUtil {

	public static String decompile(String jarPath, String innerPath) {
		String tmpBinPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "tmp_class_dump");
		try {
			JarFile jarFile = new JarFile(jarPath);
			ZipEntry zipEntry = jarFile.getEntry(innerPath);
			InputStream is = jarFile.getInputStream(zipEntry);
			OutputStream os = new FileOutputStream(new File(tmpBinPath));
			IOUtils.copy(is,os);
			Pair<String, NavigableMap<Integer, Integer>>  result = decompile(tmpBinPath);
			return result.getFirst();
		} catch (Exception e) {
		}
		return "";
	}
	
	public static void main(String[] args) {
		DecompileUtil.decompile("sdf", "sdf");
	}

	public static Pair<String, NavigableMap<Integer, Integer>> decompile(String classFilePath) {

			final StringBuilder sb = new StringBuilder(8192);

			final NavigableMap<Integer, Integer> lineMapping = new TreeMap<Integer, Integer>();

			OutputSinkFactory mySink = new OutputSinkFactory() {
				@Override
				public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
					return Arrays.asList(SinkClass.STRING, SinkClass.DECOMPILED, SinkClass.DECOMPILED_MULTIVER,
							SinkClass.EXCEPTION_MESSAGE, SinkClass.LINE_NUMBER_MAPPING);
				}

				@Override
				public <T> Sink<T> getSink(final SinkType sinkType, final SinkClass sinkClass) {
					return new Sink<T>() {
						@Override
						public void write(T sinkable) {
							// skip message like: Analysing type demo.MathGame
							if (sinkType == SinkType.PROGRESS) {
								return;
							}
							if (sinkType == SinkType.LINENUMBER) {
								SinkReturns.LineNumberMapping mapping = (SinkReturns.LineNumberMapping) sinkable;
								NavigableMap<Integer, Integer> classFileMappings = mapping.getClassFileMappings();
								NavigableMap<Integer, Integer> mappings = mapping.getMappings();
								if (classFileMappings != null && mappings != null) {
									for (Map.Entry<Integer, Integer> entry : mappings.entrySet()) {
										Integer srcLineNumber = classFileMappings.get(entry.getKey());
										lineMapping.put(entry.getValue(), srcLineNumber);
									}
								}
								return;
							}
							sb.append(sinkable);
						}
					};
				}
			};

			HashMap<String, String> options = new HashMap<String, String>();
			options.put("showversion", "false");
			options.put("hideutf", "false");
			options.put("trackbytecodeloc", "true");

			CfrDriver driver = new CfrDriver.Builder().withOptions(options).withOutputSink(mySink).build();
			List<String> toAnalyse = new ArrayList<String>();
			toAnalyse.add(classFilePath);
			driver.analyse(toAnalyse);

			String resultCode = sb.toString();
			if (lineMapping.isEmpty()) {
				resultCode = addLineNumber(resultCode, lineMapping);
			}

			return Pair.make(resultCode, lineMapping);
		}

	private static String addLineNumber(String src, Map<Integer, Integer> lineMapping) {
		int maxLineNumber = 0;
		for (Integer value : lineMapping.values()) {
			if (value != null && value > maxLineNumber) {
				maxLineNumber = value;
			}
		}

		String formatStr = "/*%2d*/ ";
		String emptyStr = "       ";

		StringBuilder sb = new StringBuilder();

		List<String> lines = toLines(src);

		if (maxLineNumber >= 100) {
			formatStr = "/*%3d*/ ";
			emptyStr = "        ";
		} else if (maxLineNumber >= 1000) {
			formatStr = "/*%4d*/ ";
			emptyStr = "         ";
		}

		int index = 0;
		for (String line : lines) {
			Integer srcLineNumber = lineMapping.get(index + 1);
			if (srcLineNumber != null) {
				sb.append(String.format(formatStr, srcLineNumber));
			} else {
				sb.append(emptyStr);
			}
			sb.append(line).append("\n");
			index++;
		}

		return sb.toString();
	}

	public static List<String> toLines(String text) {
		List<String> result = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new StringReader(text));
		try {
			String line = reader.readLine();
			while (line != null) {
				result.add(line);
				line = reader.readLine();
			}
		} catch (IOException exc) {
			// quit
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return result;
	}
}
