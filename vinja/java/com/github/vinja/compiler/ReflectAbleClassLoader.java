package com.github.vinja.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;

import com.github.vinja.util.LRUCache;

public class ReflectAbleClassLoader extends URLClassLoader {

	private static LRUCache<String, byte[]> jarByteCache = new LRUCache<String, byte[]>(1000);
	private static LRUCache<String, byte[]> classByteCache = new LRUCache<String, byte[]>(1000);

	public ReflectAbleClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
	}

	public ReflectAbleClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public ReflectAbleClassLoader(URL[] urls) {
		super(urls);
	}

	public Package[] getPackageInfo() {
		return this.getPackages();
	}

	public InputStream getResourceAsStream(String name) {
		return super.getResourceAsStream(name);
	}
	
	public void clearResourceByteCache(String name) {
		classByteCache.remove(name);
	}

	public byte[] getResourceBytes(String name) {
		if (jarByteCache.containsKey(name)) {
			return jarByteCache.get(name);
		}
		if (classByteCache.containsKey(name)) {
			return classByteCache.get(name);
		}

		URL url = getResource(name);
		if (url == null) {
			classByteCache.put(name, null);
			return null;
		}

		InputStream is = null;
		try {
			URLConnection urlc = url.openConnection();
			is = urlc.getInputStream();
			boolean isJarFile = false;
			if (urlc instanceof JarURLConnection) {
				isJarFile = true;
			}

			byte[] classBytes;
			byte[] buf = new byte[8192];
			ByteArrayOutputStream baos = new ByteArrayOutputStream(buf.length);
			int count;
			while ((count = is.read(buf, 0, buf.length)) > 0) {
				baos.write(buf, 0, count);
			}
			baos.flush();
			classBytes = baos.toByteArray();

			if (isJarFile) {
				jarByteCache.put(name, classBytes);
			} else {
				classByteCache.put(name, classBytes);

			}
			return classBytes;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try { is.close(); } catch (Exception e) {}
			}
		}
		return null;
	}

}
