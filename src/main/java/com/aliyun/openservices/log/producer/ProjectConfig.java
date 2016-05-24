package com.aliyun.openservices.log.producer;

public class ProjectConfig {
	public String projectName;
	public String endpoint;
	public String accessKeyId;
	public String accessKey;
	public String stsToken;
	
	public ProjectConfig()
	{}
	public ProjectConfig(String projectName, String endpoint,
			String accessKeyId, String accessKey, String stsToken) {
		super();
		this.projectName = projectName;
		this.endpoint = endpoint;
		this.accessKeyId = accessKeyId;
		this.accessKey = accessKey;
		this.stsToken = stsToken;
	}

	public ProjectConfig(String projectName, String endpoint,
			String accessKeyId, String accessKey) {
		super();
		this.projectName = projectName;
		this.endpoint = endpoint;
		this.accessKeyId = accessKeyId;
		this.accessKey = accessKey;
	}

}
