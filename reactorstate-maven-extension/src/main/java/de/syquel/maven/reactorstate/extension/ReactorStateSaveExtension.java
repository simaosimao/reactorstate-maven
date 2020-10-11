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

/**
 * A Maven Core Extension which saves the state of Maven executions afterwards and restores it beforehand.
 */
@Named(ReactorStateSaveExtension.EXTENSION_ID)
@Singleton
public class ReactorStateSaveExtension extends AbstractMavenLifecycleParticipant {

	/**
	 * The unique ID of this Maven Core Extension.
	 */
	public static final String EXTENSION_ID = "reactorstate-save";

	private static final Logger LOGGER = LoggerFactory.getLogger(ReactorStateSaveExtension.class);

	/**
	 * The helper for Maven-related operations on the current state.
	 */
	private final MavenProjectHelper projectHelper;

	/**
	 * The builder for Maven projects from POMs.
	 */
	private final ProjectBuilder projectBuilder;

	/**
	 * Constructs a new instance.
	 *
	 * @param projectHelper The helper for Maven-related operations on the current state.
	 * @param projectBuilder The builder for Maven projects from POMs.
	 */
	@Inject
	public ReactorStateSaveExtension(final MavenProjectHelper projectHelper, final ProjectBuilder projectBuilder) {
		this.projectHelper = projectHelper;
		this.projectBuilder = projectBuilder;
	}

	/**
	 * Restores the saved state of the Maven projects and its Maven modules within the current Maven execution.
	 *
	 * @param session The current Maven execution.
	 * @throws MavenExecutionException if an error occurred while restoring the saved state.
	 */
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

	/**
	 * Saves the current state of all Maven modules within the Maven execution.
	 *
	 * This is a Maven lifecycle hook, which is executed directly after the Maven session has read the project definitions of the Maven modules,
	 * but before it has started building the project.
	 *
	 * @param session The current Maven execution.
	 * @throws MavenExecutionException if an error occurred while saving the current state.
	 */
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
