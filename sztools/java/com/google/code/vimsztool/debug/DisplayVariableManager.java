package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.List;

import com.google.code.vimsztool.debug.eval.ExpEval;
import com.google.code.vimsztool.exception.ExpressionEvalException;

public class DisplayVariableManager {
	
	private List<String> allExps = new ArrayList<String>();
	private static DisplayVariableManager instance = new DisplayVariableManager();

	private DisplayVariableManager() {
	}

	public static DisplayVariableManager getInstance() {
		return instance;
	}
	
	public String addWatchExpression(String exp) {
		if (!allExps.contains(exp)) {
			allExps.add(exp);
		}
		return "success";
	}
	
	public String removeWatchVariables(String exp) {
		
		if (allExps.contains(exp)) {
			allExps.remove(exp);
		}
		return "success";
	}
	
	public List<String> getWatchVariables() {
		return this.allExps;
	}
	
	public String evalWatchVariables() {
		if (allExps.size() < 1 ) return "";
		try {
			String result = ExpEval.eval(allExps);
			return result;
		} catch (ExpressionEvalException e) {
			return e.getMessage();
		}
	}

}
