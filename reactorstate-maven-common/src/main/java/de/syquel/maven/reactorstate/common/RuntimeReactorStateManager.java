package de.syquel.maven.reactorstate.common;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

public class RuntimeReactorStateManager extends AbstractReactorStateManager {

	private RuntimeReactorStateManager(final Set<MavenProjectState> projectStates) {
		super(projectStates);
	}

	public static RuntimeReactorStateManager create(final MavenSession mavenSession) {
		final Set<MavenProjectState> projectStates = new HashSet<>();
		for (final MavenProject project : mavenSession.getProjects()) {
			final MavenProjectState projectState = buildProjectState(project);
			projectStates.add(projectState);
		}

		return new RuntimeReactorStateManager(projectStates);
	}

	public void saveProjectStates() throws IOException {
		for (final MavenProjectState state : getProjectStates()) {
			saveProjectState(state);
		}
	}

	private void saveProjectState(final MavenProjectState projectState) throws IOException {
		final MavenProject project = projectState.getProject();

		final Path projectBuildPath = resolveProjectBuildPath(project);
		if (!Files.isDirectory(projectBuildPath)) {
			Files.createDirectory(projectBuildPath);
		}

		final Properties stateProperties = buildProjectStateProperties(projectState);

		final Path projectStatePath = projectBuildPath.resolve(STATE_PROPERTIES_FILENAME);
		try (final Writer stateWriter = Files.newBufferedWriter(projectStatePath, StandardCharsets.UTF_8)) {
			stateProperties.store(stateWriter, "Maven session state");
		}
	}

	private static MavenProjectState buildProjectState(final MavenProject project) {
		final Artifact mainArtifact = RepositoryUtils.toArtifact(project.getArtifact());

		final Artifact pom =
			ArtifactDescriptorUtils
				.toPomArtifact(mainArtifact)
				.setFile(project.getFile());

		final Set<Artifact> attachedArtifacts = new HashSet<>(RepositoryUtils.toArtifacts(project.getAttachedArtifacts()));

		final MavenProjectState projectState;
		if ("pom".equals(mainArtifact.getExtension())) {
			projectState = new MavenProjectState(project, pom, pom, attachedArtifacts);
		} else {
			projectState = new MavenProjectState(project, pom, mainArtifact, attachedArtifacts);
		}

		return projectState;
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

}
