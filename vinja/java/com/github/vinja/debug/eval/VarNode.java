package com.github.vinja.debug.eval;

public class VarNode {

	private String name;
	private String nodeType;
	private String javaType;
	private String value;
	private String uuid;

	public VarNode() {
	}

	public VarNode(String name, String nodeType, String javaType, String value) {
		this.name = name;
		this.nodeType = nodeType;
		this.javaType = javaType;
		this.value = value;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public String getNodeType() {
		return this.nodeType;
	}

	public void setJavaType(String javaType) {
		this.javaType = javaType;
	}

	public String getJavaType() {
		return this.javaType;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
