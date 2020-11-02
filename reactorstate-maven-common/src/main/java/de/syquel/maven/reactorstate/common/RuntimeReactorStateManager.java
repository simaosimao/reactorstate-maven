package de.syquel.maven.reactorstate.common;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.syquel.maven.reactorstate.common.data.MavenArtifactState;
import de.syquel.maven.reactorstate.common.data.MavenProjectState;
import de.syquel.maven.reactorstate.common.persistence.IReactorStateRepository;
import de.syquel.maven.reactorstate.common.persistence.json.JsonReactorStateRepository;

/**
 * The implementation of a Maven Reactor state manager which operates on the current state of Maven modules within a Maven project.
 */
public class RuntimeReactorStateManager extends AbstractReactorStateManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeReactorStateManager.class);

	/**
	 * The persistence repository for Maven module states.
	 */
	private final IReactorStateRepository reactorStateRepository;

	private RuntimeReactorStateManager(final Set<MavenProjectState> projectStates, final IReactorStateRepository reactorStateRepository) {
		super(projectStates);
		this.reactorStateRepository = reactorStateRepository;
	}

	/**
	 * Instantiates a Reactor state manager based on the current state of the Maven execution.
	 *
	 * @param mavenSession The state of a Maven execution.
	 * @return A Reactor state manager with the current state of the Maven execution.
	 */
	public static RuntimeReactorStateManager create(final MavenSession mavenSession) {
		final Set<MavenProjectState> projectStates = new HashSet<>();
		for (final MavenProject project : mavenSession.getProjects()) {
			final MavenProjectState projectState = buildProjectState(project);
			projectStates.add(projectState);
		}

		return new RuntimeReactorStateManager(projectStates, new JsonReactorStateRepository());
	}

	/**
	 * Saves the current state of all Maven modules within the Maven execution.
	 *
	 * @throws IOException if an error occurred while saving the state.
	 */
	public void saveProjectStates() throws IOException {
		for (final MavenProjectState projectState : getProjectStates()) {
			reactorStateRepository.save(projectState);
		}
	}

	/**
	 * Builds the state of a Maven module based on its current state.
	 *
	 * @param project The Maven module to save the state for.
	 * @return The current state of the Maven module.
	 */
	private static MavenProjectState buildProjectState(final MavenProject project) {
		final MavenArtifactState mainArtifactState = buildArtifactState(project.getArtifact());

		final Artifact pom =
			ArtifactDescriptorUtils
				.toPomArtifact(mainArtifactState.getArtifact())
				.setFile(project.getFile());

		final Set<MavenArtifactState> attachedArtifactStates =
			project.getAttachedArtifacts().stream()
				.map(RuntimeReactorStateManager::buildArtifactState)
				.collect(Collectors.toSet());

		final MavenProjectState projectState;
		if (ArtifactIdUtils.toId(mainArtifactState.getArtifact()).equals(ArtifactIdUtils.toId(pom))) {
			projectState = new MavenProjectState(project, pom, new MavenArtifactState(pom), attachedArtifactStates);
		} else {
			projectState = new MavenProjectState(project, pom, mainArtifactState, attachedArtifactStates);
		}

		return projectState;
	}

	/**
	 * Builds the state of a Maven artifact based on its current state.
	 *
	 * @param repositoryArtifact The Maven artifact to save the state for.
	 * @return The current state of the Maven artifact.
	 */
	private static MavenArtifactState buildArtifactState(final org.apache.maven.artifact.Artifact repositoryArtifact) {
		final MavenArtifactState artifactState = new MavenArtifactState(RepositoryUtils.toArtifact(repositoryArtifact));

		for (@SuppressWarnings("deprecation") final ArtifactMetadata artifactMetadata : repositoryArtifact.getMetadataList()) {
			final Class<?> artifactMetadataClass = artifactMetadata.getClass();

			if (artifactMetadata instanceof ArtifactRepositoryMetadata) {
				final ArtifactRepositoryMetadata artifactRepositoryMetadata = (ArtifactRepositoryMetadata) artifactMetadata;
				artifactState.setArtifactRepositoryMetadata(artifactRepositoryMetadata.getMetadata());
			} else if (artifactMetadata instanceof GroupRepositoryMetadata) {
				final GroupRepositoryMetadata groupRepositoryMetadata = (GroupRepositoryMetadata) artifactMetadata;
				artifactState.setGroupRepositoryMetadata(groupRepositoryMetadata.getMetadata());
			} else if (artifactMetadata instanceof SnapshotArtifactRepositoryMetadata) {
				final SnapshotArtifactRepositoryMetadata snapshotRepositoryMetadata = (SnapshotArtifactRepositoryMetadata) artifactMetadata;
				artifactState.setSnapshotRepositoryMetadata(snapshotRepositoryMetadata.getMetadata());
			} else if (artifactMetadata instanceof RepositoryMetadata) {
				// Only report unknown RepositoryMetadata; legacy ArtifactMetadata are ignored.
				LOGGER.warn("Unknown metadata {} on artifact {}. Ignoring.", artifactMetadataClass.getName(), repositoryArtifact.getId());
			}
		}

		return artifactState;
	}

}
