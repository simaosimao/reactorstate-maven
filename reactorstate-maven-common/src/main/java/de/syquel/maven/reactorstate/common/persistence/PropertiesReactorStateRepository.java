package de.syquel.maven.reactorstate.common.persistence;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

import de.syquel.maven.reactorstate.common.MavenProjectState;
import de.syquel.maven.reactorstate.common.util.MavenProjectUtils;

public class PropertiesReactorStateRepository implements IReactorStateRepository {

	private static final String STATE_PROPERTIES_FILENAME = "reactorstate-maven.properties";
	private static final String PROPERTY_KEY_MAIN_ARTIFACT = "main-artifact";
	private static final String PROPERTY_PREFIX_ARTIFACT_PROPERTY = "property.";

	@Override
	public MavenProjectState read(final MavenProject mavenProject) throws IOException {
		// Read project state from filesystem
		final Properties stateProperties = fetchStateProperties(mavenProject);
		if (stateProperties == null) {
			return null;
		}

		final Path projectBasePath = mavenProject.getBasedir().toPath();

		// Extract all artifact properties
		final Map<String, String> artifactsProperties = extractArtifactProperties(stateProperties);

		// Extract main artifact
		final String mainArtifactCoordinates = stateProperties.getProperty(PROPERTY_KEY_MAIN_ARTIFACT);
		final Path mainArtifactPath = projectBasePath.resolve(stateProperties.getProperty(mainArtifactCoordinates));
		final Map<String, String> mainArtifactProperties = findArtifactProperties(mainArtifactCoordinates, artifactsProperties);
		stateProperties.remove(PROPERTY_KEY_MAIN_ARTIFACT);
		stateProperties.remove(mainArtifactCoordinates);

		final Artifact mainArtifact = buildArtifact(mainArtifactCoordinates, mainArtifactPath, mainArtifactProperties);
		if (mainArtifact == null) {
			throw new IllegalStateException("Main Artifact is missing in saved state for Maven project " + mavenProject.getId());
		}

		// Extract POM artifact
		Artifact pom = ArtifactDescriptorUtils.toPomArtifact(mainArtifact);
		if (pom != mainArtifact) {
			final String pomArtifactCoordinates = ArtifactIdUtils.toId(pom);
			final Path pomPath = Paths.get(stateProperties.getProperty(pomArtifactCoordinates));
			stateProperties.remove(pomArtifactCoordinates);

			pom = pom
				.setFile(pomPath.toFile())
				.setProperties(findArtifactProperties(pomArtifactCoordinates, artifactsProperties));
		}

		// Extract attached artifacts
		final Set<Artifact> attachedArtifacts = new HashSet<>();
		for (final Map.Entry<Object, Object> artifactEntry : stateProperties.entrySet()) {
			final String artifactCoordinates = (String) artifactEntry.getKey();
			final Path artifactPath = projectBasePath.resolve((String) artifactEntry.getValue());

			final Map<String, String> artifactProperties = findArtifactProperties(artifactCoordinates, artifactsProperties);

			final Artifact artifact = buildArtifact(artifactCoordinates, artifactPath, artifactProperties);
			attachedArtifacts.add(artifact);
		}

		// Build project state
		final MavenProjectState projectState = new MavenProjectState(mavenProject, pom, mainArtifact, attachedArtifacts);
		return projectState;
	}

	@Override
	public void save(final MavenProjectState mavenProjectState) throws IOException {
		final MavenProject project = mavenProjectState.getProject();

		final Path projectBuildPath = MavenProjectUtils.resolveProjectBuildPath(project);
		if (!Files.isDirectory(projectBuildPath)) {
			Files.createDirectory(projectBuildPath);
		}

		final Properties stateProperties = buildProjectStateProperties(mavenProjectState);

		final Path projectStatePath = projectBuildPath.resolve(STATE_PROPERTIES_FILENAME);
		try (final Writer stateWriter = Files.newBufferedWriter(projectStatePath, StandardCharsets.UTF_8)) {
			stateProperties.store(stateWriter, "Maven session state");
		}
	}

	@Override
	public void delete(final MavenProject mavenProject) throws IOException {
		final Path reactorStatePath = MavenProjectUtils.resolveProjectBuildPath(mavenProject).resolve(STATE_PROPERTIES_FILENAME);
		Files.deleteIfExists(reactorStatePath);
	}

	private static Properties fetchStateProperties(final MavenProject mavenProject) throws IOException {
		final Path projectBuildPath = MavenProjectUtils.resolveProjectBuildPath(mavenProject);
		final Path projectStatePath = projectBuildPath.resolve(STATE_PROPERTIES_FILENAME);
		if (!Files.isDirectory(projectBuildPath) || !Files.isReadable(projectStatePath)) {
			return null;
		}

		final Properties stateProperties = new Properties();
		try (final Reader stateReader = Files.newBufferedReader(projectStatePath, StandardCharsets.UTF_8)) {
			stateProperties.load(stateReader);
		}

		return stateProperties;
	}

	private static Map<String, String> findArtifactProperties(final String artifactCoordinates, final Map<String, String> artifactsProperties) {
		final String artifactPropertiesPrefix = buildArtifactPropertiesPrefix(artifactCoordinates);

		final Map<String, String> artifactProperties = new HashMap<>();
		for (final Map.Entry<String, String> artifactProperty : artifactsProperties.entrySet()) {
			final String artifactPropertyKey = artifactProperty.getKey();
			if (!artifactPropertyKey.startsWith(artifactPropertiesPrefix)) {
				continue;
			}

			final String artifactPropertyName = artifactPropertyKey.substring(artifactPropertiesPrefix.length());
			final String artifactPropertyValue = artifactProperty.getValue();

			artifactProperties.put(artifactPropertyName, artifactPropertyValue);
		}

		return artifactProperties;
	}

	private static Map<String, String> extractArtifactProperties(final Properties stateProperties) {
		final Map<String, String> artifactProperties = new HashMap<>();
		for (final String statePropertyName : stateProperties.stringPropertyNames()) {
			if (!statePropertyName.startsWith(PROPERTY_PREFIX_ARTIFACT_PROPERTY)) {
				continue;
			}

			final String artifactPropertyValue = stateProperties.getProperty(statePropertyName);
			artifactProperties.put(statePropertyName, artifactPropertyValue);

			stateProperties.remove(statePropertyName);
		}

		return artifactProperties;
	}

	private static Properties buildProjectStateProperties(final MavenProjectState projectState) {
		final Path projectBasePath = projectState.getProject().getBasedir().toPath();

		final Properties stateProperties = new Properties();

		final Artifact pomArtifact = projectState.getPom();
		final String pomArtifactCoordinates = ArtifactIdUtils.toId(pomArtifact);
		stateProperties.put(pomArtifactCoordinates, projectBasePath.relativize(pomArtifact.getFile().toPath()).toString());

		contributeProjectStateArtifactProperties(pomArtifact, stateProperties);

		final Artifact mainArtifact = projectState.getMainArtifact();
		final String mainArtifactCoordinates = ArtifactIdUtils.toId(mainArtifact);
		final File mainArtifactFile = mainArtifact.getFile();
		stateProperties.put(PROPERTY_KEY_MAIN_ARTIFACT, mainArtifactCoordinates);
		stateProperties.put(mainArtifactCoordinates, (mainArtifactFile != null) ? projectBasePath.relativize(mainArtifactFile.toPath()).toString() : "");

		contributeProjectStateArtifactProperties(mainArtifact, stateProperties);

		for (final Artifact attachedArtifact : projectState.getAttachedArtifacts()) {
			stateProperties.put(ArtifactIdUtils.toId(attachedArtifact), projectBasePath.relativize(attachedArtifact.getFile().toPath()).toString());
			contributeProjectStateArtifactProperties(attachedArtifact, stateProperties);
		}

		return stateProperties;
	}

	private static void contributeProjectStateArtifactProperties(final Artifact artifact, final Properties stateProperties) {
		final String artifactPropertiesPrefix = buildArtifactPropertiesPrefix(ArtifactIdUtils.toId(artifact));

		for (final Map.Entry<String, String> artifactProperty : artifact.getProperties().entrySet()) {
			stateProperties.put(artifactPropertiesPrefix + artifactProperty.getKey(), artifactProperty.getValue());
		}
	}

	private static String buildArtifactPropertiesPrefix(final String artifactCoordinates) {
		return PROPERTY_PREFIX_ARTIFACT_PROPERTY + artifactCoordinates + '.';
	}

	private static Artifact buildArtifact(final String artifactCoordinates, final Path artifactPath, final Map<String, String> artifactProperties) {
		final Artifact artifact =
			new DefaultArtifact(artifactCoordinates, artifactProperties)
				.setFile(artifactPath.toFile());

		return artifact;
	}

}
