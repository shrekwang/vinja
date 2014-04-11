package com.github.vinja.debug;

import java.util.ArrayList;
import java.util.List;

import com.github.vinja.debug.eval.ExpEval;
import com.github.vinja.exception.ExpressionEvalException;

public class DisplayVariableManager {
	
	private Debugger debugger;
	private List<String> allExps = new ArrayList<String>();

	public DisplayVariableManager(Debugger debugger) {
		this.debugger = debugger;
	}

	public String addWatchExpression(String exp) {
		if (!allExps.contains(exp)) {
			allExps.add(exp);
		}
		return Debugger.CMD_SUCCESS + ": add displayed expression \"" + exp +"\"";
	}
	
	public String removeWatchVariables(String exp) {
		
		boolean foundExp = false;
		if (allExps.contains(exp)) {
			foundExp = true;
			allExps.remove(exp);
		}
		if (foundExp) {
			return Debugger.CMD_SUCCESS + ": remove displayed(inspected) expression \"" + exp +"\"";
		} else {
			return "expression \"" + exp + "\" not exists in displayed expression list.";
		}
	}
	
	public String showWatchVariables() {
		if (this.allExps.size() == 0) return "there's no display expression yet.";
		StringBuilder sb = new StringBuilder();
		for (String exp : allExps) {
			sb.append(exp).append(",");
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	public String evalWatchVariables() {
		StringBuilder sb = new StringBuilder();
		if (allExps.size() > 0 ) {
			try {
				
				String result =debugger.getExpEval().eval(allExps);
				sb.append(result);
			} catch (ExpressionEvalException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.toString();
		
		
	}

}
