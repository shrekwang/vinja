package com.google.code.vimsztool.compiler;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class ReflectAbleClassLoader extends URLClassLoader {

	public ReflectAbleClassLoader(URL[] urls, ClassLoader parent,
			URLStreamHandlerFactory factory) {
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
	

}
