package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.List;

import com.google.code.vimsztool.exception.ExpressionEvalException;

public class WatchVariableManager {
	
	private List<Expression> allVariables = new ArrayList<Expression>();
	private static WatchVariableManager instance = new WatchVariableManager();

	private WatchVariableManager() {
	}

	public static WatchVariableManager getInstance() {
		return instance;
	}
	
	public String setWatchVariables(String expXmlStr) {
		allVariables = Expression.parseExpXmlStr(expXmlStr);
		return "";
	}
	
	public List<Expression> getWatchVariables() {
		return this.allVariables;
	}
	
	public String evalWatchVariables() {
		if (allVariables.size() < 1 ) return "";
		try {
			String result = ExpressionEval.eval(allVariables);
			return result;
		} catch (ExpressionEvalException e) {
			return e.getMessage();
		}
	}

}
