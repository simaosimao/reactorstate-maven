package de.syquel.maven.reactorstate.extension.workspacereader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.repository.ChainedWorkspaceReader;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Maven {@link WorkspaceReader}, which registers itself with the current Maven execution and delegates to other registered workspace readers.
 *
 * The reason for this workspace reader implementation is that Maven only uses one workspace reader, which might conflict in case an IDE registers its own
 * besides ours.
 */
@Named(ReactorStateWorkspaceReader.WORKSPACE_READER_IDE_QUALIFIER)
@Singleton
@Priority(Integer.MAX_VALUE)
public class DelegatingIdeWorkspaceReader extends AbstractMavenLifecycleParticipant implements WorkspaceReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingIdeWorkspaceReader.class);

	/**
	 * The Maven dependency injection and management container.
	 */
	private final PlexusContainer plexusContainer;

	/**
	 * The workspace readers to delegate to.
	 */
	private ChainedWorkspaceReader delegate = new ChainedWorkspaceReader();

	/**
	 * Constructs a new Workspace reader based on the Maven dependency injection container.
	 *
	 * @param plexusContainer The Maven dependency injection and management container to retrieve other workspace reader implementations from.
	 */
	@Inject
	public DelegatingIdeWorkspaceReader(final PlexusContainer plexusContainer) {
		this.plexusContainer = plexusContainer;
	}

	/**
	 * Retrieves other workspace readers from the {@link PlexusContainer}.
	 *
	 * This is a Maven lifecycle hook, which is executed directly after the Maven session has been constructed, but before it has started or projects were read.
	 *
	 * @param session The current Maven execution.
	 * @throws MavenExecutionException if other workspace readers could not be retrieved from the {@link PlexusContainer}.
	 */
	@Override
	public void afterSessionStart(final MavenSession session) throws MavenExecutionException {
		final Map<String, WorkspaceReader> workspaceReaders;
		try {
			workspaceReaders = plexusContainer.lookupMap(WorkspaceReader.class);
		} catch (final ComponentLookupException e) {
			throw new MavenExecutionException("No workspace readers registered", e);
		}

		final List<WorkspaceReader> ideWorkspaceReaders = new ArrayList<>();
		for (final Map.Entry<String, WorkspaceReader> workspaceReaderEntry : workspaceReaders.entrySet()) {
			final String workspaceReaderQualifier = workspaceReaderEntry.getKey();
			if (!ReactorStateWorkspaceReader.WORKSPACE_READER_IDE_QUALIFIER.equals(workspaceReaderQualifier)) {
				continue;
			}

			final WorkspaceReader workspaceReader = workspaceReaderEntry.getValue();
			if (workspaceReader == this) {
				continue;
			}

			LOGGER.info("Register workspace reader {}", workspaceReader.getRepository().getId());
			ideWorkspaceReaders.add(workspaceReader);
		}

		delegate = new ChainedWorkspaceReader(ideWorkspaceReaders.toArray(new WorkspaceReader[0]));
	}

	/**
	 * Delegates to other workspace readers to determine the filesystem location of a Maven artifact.
	 *
	 * @param artifact The Maven artifact to determine the filesystem location for.
	 * @return The filesystem location of the Maven artifact.
	 */
	@Override
	public File findArtifact(final Artifact artifact) {
		return delegate.findArtifact(artifact);
	}

	/**
	 * Delegates to other workspace readers to determine the available versions of a Maven artifact.
	 *
	 * @param artifact The Maven artifact to determine available version for.
	 * @return The list of available versions of the Maven artifact.
	 */
	@Override
	public List<String> findVersions(final Artifact artifact) {
		return delegate.findVersions(artifact);
	}

	/**
	 * Returns the underlying workspace repository information.
	 *
	 * @return The underlying workspace repository information.
	 */
	@Override
	public WorkspaceRepository getRepository() {
		return delegate.getRepository();
	}

}
