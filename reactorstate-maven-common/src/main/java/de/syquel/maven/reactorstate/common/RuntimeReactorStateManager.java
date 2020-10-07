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

public class RuntimeReactorStateManager extends AbstractReactorStateManager {

	private final IReactorStateRepository reactorStateRepository;

	private RuntimeReactorStateManager(final Set<MavenProjectState> projectStates, final IReactorStateRepository reactorStateRepository) {
		super(projectStates);
		this.reactorStateRepository = reactorStateRepository;
	}

	public static RuntimeReactorStateManager create(final MavenSession mavenSession) {
		final Set<MavenProjectState> projectStates = new HashSet<>();
		for (final MavenProject project : mavenSession.getProjects()) {
			final MavenProjectState projectState = buildProjectState(project);
			projectStates.add(projectState);
		}

		return new RuntimeReactorStateManager(projectStates, new JsonReactorStateRepository());
	}

	public void saveProjectStates() throws IOException {
		for (final MavenProjectState projectState : getProjectStates()) {
			reactorStateRepository.save(projectState);
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

}
