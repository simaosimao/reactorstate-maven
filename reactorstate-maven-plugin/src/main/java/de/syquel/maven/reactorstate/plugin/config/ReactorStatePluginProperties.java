package de.syquel.maven.reactorstate.plugin.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class ReactorStatePluginProperties {

	private static final String PROPERTIES_FILENAME = "reactorstate-maven-plugin.properties";
	private static final String PROPERTY_KEY_EXTENSION_ARTIFACT = "extension.artifact";

	private final Artifact extensionArtifact;

	private ReactorStatePluginProperties(final Artifact extensionArtifact) {
		this.extensionArtifact = extensionArtifact;
	}

	public static ReactorStatePluginProperties create() throws IOException {
		final Properties properties = new Properties();
		try (final InputStream propertiesStream = ReactorStatePluginProperties.class.getResourceAsStream(PROPERTIES_FILENAME)) {
			properties.load(propertiesStream);
		}

		final String artifactCoordinates = properties.getProperty(PROPERTY_KEY_EXTENSION_ARTIFACT);
		final Artifact extensionArtifact = new DefaultArtifact(artifactCoordinates);

		final ReactorStatePluginProperties reactorStatePluginProperties = new ReactorStatePluginProperties(extensionArtifact);
		return reactorStatePluginProperties;
	}

	public Artifact getExtensionArtifact() {
		return getExtensionArtifact(null);
	}

	public Artifact getExtensionArtifact(final String classifier) {
		return new DefaultArtifact(
			extensionArtifact.getGroupId(), extensionArtifact.getArtifactId(), ((classifier != null) ? classifier : extensionArtifact.getClassifier()),
			extensionArtifact.getExtension(), extensionArtifact.getVersion()
		);
	}

}
