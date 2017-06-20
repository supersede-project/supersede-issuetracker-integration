package eu.supersede.jira.plugins.servlet;

public class Difference {
	private String id;
	private String anomalyType;
	private String JIRAValue;
	private String SSValue;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getAnomalyType() {
		return anomalyType;
	}
	public void setAnomalyType(String anomalyType) {
		this.anomalyType = anomalyType;
	}
	public String getJIRAValue() {
		return JIRAValue;
	}
	public void setJIRAValue(String jIRAValue) {
		JIRAValue = jIRAValue;
	}
	public String getSSValue() {
		return SSValue;
	}
	public void setSSValue(String SSValue) {
		this.SSValue = SSValue;
	}
	
}
