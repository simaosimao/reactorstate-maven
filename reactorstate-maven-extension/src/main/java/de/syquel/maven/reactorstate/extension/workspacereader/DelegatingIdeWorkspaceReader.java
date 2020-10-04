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

@Named(ReactorStateWorkspaceReader.WORKSPACE_READER_IDE_QUALIFIER)
@Singleton
@Priority(Integer.MAX_VALUE)
public class DelegatingIdeWorkspaceReader extends AbstractMavenLifecycleParticipant implements WorkspaceReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingIdeWorkspaceReader.class);

	private final PlexusContainer plexusContainer;
	private ChainedWorkspaceReader delegate = new ChainedWorkspaceReader();

	@Inject
	public DelegatingIdeWorkspaceReader(final PlexusContainer plexusContainer) {
		this.plexusContainer = plexusContainer;
	}

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

	@Override
	public File findArtifact(final Artifact artifact) {
		return delegate.findArtifact(artifact);
	}

	@Override
	public List<String> findVersions(final Artifact artifact) {
		return delegate.findVersions(artifact);
	}

	@Override
	public WorkspaceRepository getRepository() {
		return delegate.getRepository();
	}

}
