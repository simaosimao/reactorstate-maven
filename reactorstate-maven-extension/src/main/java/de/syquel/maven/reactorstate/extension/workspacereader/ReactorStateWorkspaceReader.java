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
import de.syquel.maven.reactorstate.common.SavedReactorStateManager;
import de.syquel.maven.reactorstate.common.data.MavenArtifactState;
import de.syquel.maven.reactorstate.common.data.MavenProjectState;

/**
 * A Maven {@link WorkspaceReader}, which contributes Maven artifacts from the current Maven workspace
 * based on their saved state to the current Maven execution.
 *
 * This workspace reader is able to restore the state of  Maven modules, which are not being built in the current Maven execution,
 * but belong to the same Maven workspace, to enable standalone builds of submodules.
 */
@Named(ReactorStateWorkspaceReader.WORKSPACE_READER_IDE_QUALIFIER)
@Singleton
public class ReactorStateWorkspaceReader extends AbstractMavenLifecycleParticipant implements WorkspaceReader {

	/**
	 * The Maven-defined qualifier for custom workspace reader implementations.
	 */
	public static final String WORKSPACE_READER_IDE_QUALIFIER = "ide";

	private static final Logger LOGGER = LoggerFactory.getLogger(ReactorStateWorkspaceReader.class);

	/**
	 * The builder for Maven projects from POMs.
	 */
	private final ProjectBuilder projectBuilder;

	/**
	 * The underlying workspace repository information.
	 */
	private final WorkspaceRepository repository = new WorkspaceRepository();

	/**
	 * The saved states of Maven artifacts within the current Maven workspace.
	 */
	private final Map<String, Artifact> artifactLookupMap = new HashMap<>();

	/**
	 * Constructs a new state-based workspace reader.
	 *
	 * @param projectBuilder The builder for Maven projects from POMs.
	 */
	@Inject
	public ReactorStateWorkspaceReader(final ProjectBuilder projectBuilder) {
		this.projectBuilder = projectBuilder;
	}

	/**
	 * Initializes the workspace reader with the saved state of the Maven modules within the current Maven workspace.
	 *
	 * This is a Maven lifecycle hook, which is executed directly after the Maven session has read the project definitions of the Maven modules,
	 * but before it has started building the project.
	 *
	 * @param session The current Maven execution.
	 * @throws MavenExecutionException if an error occurred while reading the saved state of the Maven modules.
	 */
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
			add(projectState.getMainArtifactState().getArtifact());

			for (final MavenArtifactState attachedArtifactState : projectState.getAttachedArtifactStates()) {
				add(attachedArtifactState.getArtifact());
			}
		}
	}

	/**
	 * Returns the underlying workspace repository information.
	 *
	 * @return The underlying workspace repository information.
	 */
	@Override
	public WorkspaceRepository getRepository() {
		return repository;
	}

	/**
	 * Determines the filesystem location of a Maven artifact.
	 *
	 * @param artifact The Maven artifact to determine the filesystem location for.
	 * @return The filesystem location of the Maven artifact.
	 */
	@Override
	public File findArtifact(final Artifact artifact) {
		final Artifact lookedupArtifact = artifactLookupMap.get(ArtifactIdUtils.toVersionlessId(artifact));
		if (ArtifactIdUtils.equalsBaseId(artifact, lookedupArtifact)) {
			return lookedupArtifact.getFile();
		}

		return null;
	}

	/**
	 * Determines the available versions of a Maven artifact.
	 *
	 * @param artifact The Maven artifact to determine available version for.
	 * @return The list of available versions of the Maven artifact.
	 */
	@Override
	public List<String> findVersions(final Artifact artifact) {
		final Artifact lookedupArtifact = artifactLookupMap.get(ArtifactIdUtils.toVersionlessId(artifact));
		if (ArtifactIdUtils.equalsBaseId(artifact, lookedupArtifact)) {
			return Collections.singletonList(lookedupArtifact.getVersion());
		}

		return Collections.emptyList();
	}

	/**
	 * Adds an Maven artifact to the list of available states.
	 *
	 * @param artifact The Maven artifact to add.
	 */
	private void add(final Artifact artifact) {
		artifactLookupMap.put(ArtifactIdUtils.toVersionlessId(artifact), artifact);
	}

}
