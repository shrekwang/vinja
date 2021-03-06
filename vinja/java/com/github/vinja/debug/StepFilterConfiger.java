package com.github.vinja.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.github.vinja.util.Preference;
import com.github.vinja.util.VjdeUtil;

public class StepFilterConfiger {

	@SuppressWarnings("all")
	public static List<String> getDefaultFilter() {
		File cfgFile = getConfigFile();
		if (cfgFile == null) return Collections.EMPTY_LIST;
		List<String> results = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(cfgFile));
			while (true ){
				String tmp = br.readLine();
				if (tmp ==null) break;
				tmp = tmp.trim();
				if (!tmp.equals("")) {
					results.add(tmp);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
	}

	private static File getConfigFile() {
		String vinjaHome = Preference.getInstance().getVinjaHome();
		String userCfgPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "stepfilters.txt");
		File tmpFile = new File(userCfgPath);
		if (tmpFile.exists()) return tmpFile;
		
		String defaultCfgPath = FilenameUtils.concat(vinjaHome, "share/conf/stepfilters.txt");
		tmpFile = new File(defaultCfgPath);
		if (tmpFile.exists()) return tmpFile;
		
		return null;
	}

}
