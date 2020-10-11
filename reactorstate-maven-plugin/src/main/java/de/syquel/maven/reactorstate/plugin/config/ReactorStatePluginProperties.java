package de.syquel.maven.reactorstate.plugin.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * An Accessor for internal configuration properties.
 */
public class ReactorStatePluginProperties {

	/**
	 * The location of the persisted properties.
	 */
	private static final String PROPERTIES_FILENAME = "reactorstate-maven-plugin.properties";

	/**
	 * The property key for the reactorstate-maven-extension artifact property.
	 */
	private static final String PROPERTY_KEY_EXTENSION_ARTIFACT = "extension.artifact";

	/**
	 * The specific artifact coordinated for the reactorstate-maven-extension.
	 */
	private final Artifact extensionArtifact;

	/**
	 * Constructs a new instance with its properties.
	 *
	 * @param extensionArtifact The specific artifact coordinated for the reactorstate-maven-extension.
	 */
	private ReactorStatePluginProperties(final Artifact extensionArtifact) {
		this.extensionArtifact = extensionArtifact;
	}

	/**
	 * Instantiate this accessor based on its persisted state.
	 *
	 * @return An internal properties accessor.
	 * @throws IOException if the properties file cannot be read.
	 */
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

	/**
	 * @return The specific artifact coordinated for the reactorstate-maven-extension.
	 */
	public Artifact getExtensionArtifact() {
		return getExtensionArtifact(null);
	}

	/**
	 * Returns the specific artifact coordinated for the reactorstate-maven-extension with a custom artifact classifier.
	 *
	 * @param classifier The custom artifact classifier.
	 * @return The specific artifact coordinated for the reactorstate-maven-extension.
	 */
	public Artifact getExtensionArtifact(final String classifier) {
		return new DefaultArtifact(
			extensionArtifact.getGroupId(), extensionArtifact.getArtifactId(), ((classifier != null) ? classifier : extensionArtifact.getClassifier()),
			extensionArtifact.getExtension(), extensionArtifact.getVersion()
		);
	}

}
