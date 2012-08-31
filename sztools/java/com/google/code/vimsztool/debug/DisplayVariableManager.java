package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.List;

import com.google.code.vimsztool.debug.eval.ExpEval;
import com.google.code.vimsztool.exception.ExpressionEvalException;

public class DisplayVariableManager {
	
	private List<String> allExps = new ArrayList<String>();
	private List<String> inspectExps = new ArrayList<String>();
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
		return Debugger.CMD_SUCCESS + ": add displayed expression \"" + exp +"\"";
	}
	
	public String addInspectExpression(String exp) {
		if (!inspectExps.contains(exp)) {
			inspectExps.add(exp);
		}
		return Debugger.CMD_SUCCESS + ": add inspected expression \"" + exp +"\"";
	}
	
	public String removeWatchVariables(String exp) {
		
		boolean foundExp = false;
		if (allExps.contains(exp)) {
			foundExp = true;
			allExps.remove(exp);
		}
		if (inspectExps.contains(exp)) {
			foundExp = true;
			inspectExps.remove(exp);
		}
		if (foundExp) {
			return Debugger.CMD_SUCCESS + ": remove displayed(inspected) expression \"" + exp +"\"";
		} else {
			return "expression \"" + exp + "\" not exists in displayed expression list.";
		}
	}
	
	public List<String> getWatchVariables() {
		return this.allExps;
	}
	
	public String evalWatchVariables() {
		StringBuilder sb = new StringBuilder();
		if (allExps.size() > 0 ) {
			try {
				String result = ExpEval.eval(allExps);
				sb.append(result);
			} catch (ExpressionEvalException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		if (inspectExps.size() > 0 ) {
			for (String exp : inspectExps) {
				try {
					sb.append(ExpEval.inspect(exp,false)).append("\n");
					sb.append(ExpEval.SEP_ROW_TXT);
				} catch (ExpressionEvalException e) {
					sb.append(e.getMessage()).append("\n");
				}
			}
		}
		return sb.toString();
		
		
	}

}
