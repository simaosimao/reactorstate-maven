package de.syquel.maven.reactorstate.plugin;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import de.syquel.maven.reactorstate.common.SavedReactorStateManager;

/**
 * Restores the state of a previous Maven execution into the current one.
 */
@Mojo(name = "restore", requiresDirectInvocation = true, threadSafe = true, inheritByDefault = false, aggregator = true)
public class ReactorStateRestoreMojo extends AbstractMojo {

	/**
	 * The current Maven execution context.
	 */
	private final MavenSession session;

	/**
	 * The helper for Maven-related operations on the current state.
	 */
	private final MavenProjectHelper projectHelper;

	/**
	 * The builder for Maven projects from POMs.
	 */
	private final ProjectBuilder projectBuilder;

	/**
	 * Constructs a new instance based on the current Maven execution context.
	 *
	 * @param session The current Maven execution context.
	 * @param projectHelper The helper for Maven-related operations on the current state.
	 * @param projectBuilder The builder for Maven projects from POMs.
	 */
	@Inject
	public ReactorStateRestoreMojo(final MavenSession session, final MavenProjectHelper projectHelper, final ProjectBuilder projectBuilder) {
		this.session = session;
		this.projectHelper = projectHelper;
		this.projectBuilder = projectBuilder;
	}

	/**
	 * Restores the state of a previous Maven execution into the current one.
	 *
	 * @throws MojoExecutionException if an error occurred while restoring the saved state of a previous Maven execution.
	 * @throws MojoFailureException never.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!session.getCurrentProject().isExecutionRoot()) {
			return;
		}

		try {
			final SavedReactorStateManager projectStateManager = SavedReactorStateManager.create(session, projectBuilder);
			projectStateManager.restoreProjectStates(session, projectHelper);
		} catch (final ProjectBuildingException | IOException e) {
			throw new MojoExecutionException("Cannot restore saved Maven project state", e);
		}
	}

}
