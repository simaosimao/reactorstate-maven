package de.syquel.maven.reactorstate.common.persistence;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

	@Override
	public MavenProjectState read(final MavenProject mavenProject) throws IOException {
		// Read project state from filesystem
		final Path projectBuildPath = MavenProjectUtils.resolveProjectBuildPath(mavenProject);
		final Path projectStatePath = projectBuildPath.resolve(STATE_PROPERTIES_FILENAME);
		if (!Files.isDirectory(projectBuildPath) || !Files.isReadable(projectStatePath)) {
			return null;
		}

		final Properties stateProperties = new Properties();
		try (final Reader stateReader = Files.newBufferedReader(projectStatePath, StandardCharsets.UTF_8)) {
			stateProperties.load(stateReader);
		}

		final Path projectBasePath = mavenProject.getBasedir().toPath();

		// Extract main artifact
		final String mainArtifactCoordinates = stateProperties.getProperty(PROPERTY_KEY_MAIN_ARTIFACT);
		final Path mainArtifactPath = projectBasePath.resolve(stateProperties.getProperty(mainArtifactCoordinates));
		stateProperties.remove(PROPERTY_KEY_MAIN_ARTIFACT);
		stateProperties.remove(mainArtifactCoordinates);
		final Artifact mainArtifact = buildArtifact(mainArtifactCoordinates, mainArtifactPath);

		if (mainArtifact == null) {
			throw new IllegalStateException("Main Artifact is missing in saved state for Maven project " + mavenProject.getId());
		}

		// Extract POM artifact
		Artifact pom = ArtifactDescriptorUtils.toPomArtifact(mainArtifact);
		if (pom != mainArtifact) {
			final String pomArtifactId = ArtifactIdUtils.toId(pom);
			final Path pomPath = Paths.get(stateProperties.getProperty(pomArtifactId));
			stateProperties.remove(pomArtifactId);

			pom = pom.setFile(pomPath.toFile());
		}

		// Extract attached artifacts
		final Set<Artifact> attachedArtifacts = new HashSet<>();
		for (final Map.Entry<Object, Object> artifactEntry : stateProperties.entrySet()) {
			final String artifactCoordinates = (String) artifactEntry.getKey();
			final Path artifactPath = projectBasePath.resolve((String) artifactEntry.getValue());

			final Artifact artifact = buildArtifact(artifactCoordinates, artifactPath);
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

	private static Properties buildProjectStateProperties(final MavenProjectState projectState) {
		final Properties stateProperties = new Properties();

		final Path projectBasePath = projectState.getProject().getBasedir().toPath();

		final Artifact pomArtifact = projectState.getPom();
		stateProperties.put(ArtifactIdUtils.toId(pomArtifact), projectBasePath.relativize(pomArtifact.getFile().toPath()).toString());

		final Artifact mainArtifact = projectState.getMainArtifact();
		final String mainArtifactId = ArtifactIdUtils.toId(mainArtifact);
		final File mainArtifactFile = mainArtifact.getFile();
		stateProperties.put(PROPERTY_KEY_MAIN_ARTIFACT, mainArtifactId);
		stateProperties.put(mainArtifactId, (mainArtifactFile != null) ? projectBasePath.relativize(mainArtifactFile.toPath()).toString() : "");

		for (final Artifact attachedArtifact : projectState.getAttachedArtifacts()) {
			stateProperties.put(ArtifactIdUtils.toId(attachedArtifact), projectBasePath.relativize(attachedArtifact.getFile().toPath()).toString());
		}

		return stateProperties;
	}

	private static Artifact buildArtifact(final String artifactCoordinates, final Path artifactPath) {
		// TODO add properties (like "type")
		final Artifact artifact =
			new DefaultArtifact(artifactCoordinates)
				.setFile(artifactPath.toFile());

		return artifact;
	}

}
