
package com.google.code.vimsztool.util;

import java.util.Comparator;

public class ClassNameComparator implements Comparator<String> {

	public int compare(String o1, String o2) {
		int priority1 = getPriority(o1);
		int priority2 = getPriority(o2);

		if (priority1 > priority2)
			return -1;
		if (priority1 < priority2)
			return 1;

		String baseName1 = o1.substring(o1.lastIndexOf(".")+1);
		String baseName2 = o2.substring(o2.lastIndexOf(".")+1);

		int len1 = baseName1.length();
		int len2 = baseName2.length();

		if (len1 > len2)
			return 1;
		if (len1 < len2)
			return -1;

		return 0;
	}

	public int getPriority(String className) {

		if (className.startsWith("java.lang"))
			return 100;
		if (className.startsWith("java.io"))
			return 99;
		if (className.startsWith("java.util"))
			return 98;
		if (className.startsWith("java.sql"))
			return 97;
		if (className.startsWith("java."))
			return 90;

		if (className.startsWith("javax."))
			return 80;

		return 70;

	}

}
