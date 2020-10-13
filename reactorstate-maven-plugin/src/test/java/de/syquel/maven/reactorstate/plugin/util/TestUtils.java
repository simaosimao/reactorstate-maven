package de.syquel.maven.reactorstate.plugin.util;

import io.takari.maven.testing.TestProperties;

public final class TestUtils {

	private static final TestProperties testProperties = new TestProperties();

	private TestUtils() {
	}

	public static String getProjectGroupId() {
		return testProperties.get("project.groupId");
	}

	public static String getProjectArtifactId() {
		return testProperties.get("project.artifactId");
	}

	public static String getProjectVersion() {
		return testProperties.getPluginVersion();
	}

	public static String getProjectGav() {
		return getProjectGroupId() + ":" + getProjectArtifactId() + ":" + getProjectVersion();
	}

}
