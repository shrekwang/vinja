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
	
	public String addWatchVariables(String expXmlStr) {
		List<Expression> tmpVarList = Expression.parseExpXmlStr(expXmlStr);
		StringBuilder sb = new StringBuilder();
		for (Expression exp : tmpVarList) {
			if (!allVariables.contains(exp)) {
				allVariables.add(exp);
				sb.append(exp.getOriExp()+",");
			}
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append(" had been added to watch list");
		return sb.toString();
	}
	
	public String removeWatchVariables(String expXmlStr) {
		
		List<Expression> tmpVarList = Expression.parseExpXmlStr(expXmlStr);
		StringBuilder sb = new StringBuilder();
		for (Expression exp : tmpVarList) {
			if (allVariables.contains(exp)) {
				allVariables.remove(exp);
				sb.append(exp.getOriExp()+",");
			}
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append(" has been removed from watch list");
		return sb.toString();
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
