package com.github.vinja.parser;

public enum VinjaSourceLoadPriority {
	

	HIGHEST(0), HIGH(1), MEDIUM(2), LOW(3), LOWEST(4);

	int value;

	VinjaSourceLoadPriority(int val) {
		this.value = val;
	}

	public int getValue() {
		return value;
	}


}
