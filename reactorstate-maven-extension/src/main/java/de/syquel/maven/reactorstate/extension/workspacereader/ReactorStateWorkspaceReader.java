package de.syquel.maven.reactorstate.extension.workspacereader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.syquel.maven.reactorstate.common.AbstractReactorStateManager;
import de.syquel.maven.reactorstate.common.MavenProjectState;
import de.syquel.maven.reactorstate.common.SavedReactorStateManager;

@Named(ReactorStateWorkspaceReader.WORKSPACE_READER_IDE_QUALIFIER)
@Singleton
public class ReactorStateWorkspaceReader extends AbstractMavenLifecycleParticipant implements WorkspaceReader {

	public static final String WORKSPACE_READER_IDE_QUALIFIER = "ide";

	private static final Logger LOGGER = LoggerFactory.getLogger(ReactorStateWorkspaceReader.class);

	private final ProjectBuilder projectBuilder;
	private final WorkspaceRepository repository = new WorkspaceRepository();
	private final Map<String, Artifact> artifactLookupMap = new HashMap<>();

	@Inject
	public ReactorStateWorkspaceReader(final ProjectBuilder projectBuilder) {
		this.projectBuilder = projectBuilder;
	}

	@Override
	public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
		final AbstractReactorStateManager projectStateManager;
		try {
			projectStateManager = SavedReactorStateManager.create(session, projectBuilder);
		} catch (final ProjectBuildingException | IOException e) {
			throw new MavenExecutionException("Cannot acquire saved reactor state", e);
		}

		for (final MavenProjectState projectState : projectStateManager.getProjectStates()) {
			add(projectState.getPom());
			add(projectState.getMainArtifact());

			for (final Artifact attachedArtifact : projectState.getAttachedArtifacts()) {
				add(attachedArtifact);
			}
		}
	}

	@Override
	public WorkspaceRepository getRepository() {
		return repository;
	}

	@Override
	public File findArtifact(final Artifact artifact) {
		final Artifact lookedupArtifact = artifactLookupMap.get(ArtifactIdUtils.toVersionlessId(artifact));
		if (ArtifactIdUtils.equalsBaseId(artifact, lookedupArtifact)) {
			return lookedupArtifact.getFile();
		}

		return null;
	}

	@Override
	public List<String> findVersions(final Artifact artifact) {
		final Artifact lookedupArtifact = artifactLookupMap.get(ArtifactIdUtils.toVersionlessId(artifact));
		if (ArtifactIdUtils.equalsBaseId(artifact, lookedupArtifact)) {
			return Collections.singletonList(lookedupArtifact.getVersion());
		}

		return Collections.emptyList();
	}

	private void add(final Artifact artifact) {
		artifactLookupMap.put(ArtifactIdUtils.toVersionlessId(artifact), artifact);
	}

}
