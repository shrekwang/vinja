package com.google.code.vimsztool.util;

import java.lang.reflect.Modifier;

public class ModifierFilter {
	
	private boolean onlyStatic;
	private boolean acceptPrctMember;
	

	public ModifierFilter(boolean onlyStatic, boolean acceptPrctMember) {
		this.onlyStatic = onlyStatic;
		this.acceptPrctMember = acceptPrctMember;
	}


	public boolean accept(int mod) {
		if (onlyStatic && ! Modifier.isStatic(mod)) return false;
		if (acceptPrctMember ) {
			if ( Modifier.isProtected(mod) || Modifier.isPublic(mod)) return true;
		} else {
			if (Modifier.isPublic(mod)) return true;
		}
		return false;
	}


	public void setOnlyStatic(boolean onlyStatic) {
		this.onlyStatic = onlyStatic;
	}

	public boolean isOnlyStatic() {
		return onlyStatic;
	}

	public void setAcceptPrctMember(boolean acceptPrctMember) {
		this.acceptPrctMember = acceptPrctMember;
	}

	public boolean isAcceptPrctMember() {
		return acceptPrctMember;
	}
	
}
