package de.syquel.maven.reactorstate.common;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.artifact.Artifact;

import de.syquel.maven.reactorstate.common.persistence.IReactorStateRepository;
import de.syquel.maven.reactorstate.common.persistence.json.JsonReactorStateRepository;

/**
 * The implementation of a Maven Reactor state manager which operates on the current state of Maven modules within a Maven project.
 */
public class RuntimeReactorStateManager extends AbstractReactorStateManager {

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

}
