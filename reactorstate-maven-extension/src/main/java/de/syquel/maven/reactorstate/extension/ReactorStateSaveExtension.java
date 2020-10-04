package de.syquel.maven.reactorstate.extension;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.syquel.maven.reactorstate.common.RuntimeReactorStateManager;
import de.syquel.maven.reactorstate.common.SavedReactorStateManager;

@Named(ReactorStateSaveExtension.EXTENSION_ID)
@Singleton
public class ReactorStateSaveExtension extends AbstractMavenLifecycleParticipant {

	public static final String EXTENSION_ID = "reactorstate-save";

	private static final Logger LOGGER = LoggerFactory.getLogger(ReactorStateSaveExtension.class);

	private final MavenProjectHelper projectHelper;
	private final ProjectBuilder projectBuilder;

	@Inject
	public ReactorStateSaveExtension(final MavenProjectHelper projectHelper, final ProjectBuilder projectBuilder) {
		this.projectHelper = projectHelper;
		this.projectBuilder = projectBuilder;
	}

	@Override
	public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
		try {
			final SavedReactorStateManager projectStateManager = SavedReactorStateManager.create(session, projectBuilder);
			if (projectStateManager.getProjectStates().isEmpty()) {
				// Skip restoring of reactor state if there is no state yet
				return;
			}

			projectStateManager.restoreProjectStates(session, projectHelper);
		} catch (final ProjectBuildingException | IOException e) {
			throw new MavenExecutionException("Cannot restore saved Maven project state", e);
		}
	}

	@Override
	public void afterSessionEnd(final MavenSession session) throws MavenExecutionException {
		LOGGER.info("Saving state of Maven session");
		final RuntimeReactorStateManager projectStateManager = RuntimeReactorStateManager.create(session);

		try {
			projectStateManager.saveProjectStates();
		} catch (final IOException e) {
			throw new MavenExecutionException("Cannot save reactor state", e);
		}
	}

}
