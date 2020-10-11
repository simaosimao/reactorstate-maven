package de.syquel.maven.reactorstate.plugin;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import de.syquel.maven.reactorstate.common.RuntimeReactorStateManager;

/**
 * Saves the state of the current Maven execution for usage in subsequent Maven executions.
 */
@Mojo(name = "save", requiresDirectInvocation = true, threadSafe = true, inheritByDefault = false, aggregator = true)
public class ReactorStateSaveMojo extends AbstractMojo {

	/**
	 * The current Maven execution context.
	 */
	private final MavenSession session;

	/**
	 * Constructs a new instance based on the current Maven execution context.
	 *
	 * @param session The current Maven execution context.
	 */
	@Inject
	public ReactorStateSaveMojo(final MavenSession session) {
		this.session = session;
	}

	/**
	 * Saves the state of the current Maven execution for usage in subsequent Maven executions.
	 *
	 * @throws MojoExecutionException if an error occurred while saving the state of the current Maven execution.
	 * @throws MojoFailureException never.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!session.getCurrentProject().isExecutionRoot()) {
			return;
		}

		final RuntimeReactorStateManager projectStateManager = RuntimeReactorStateManager.create(session);
		try {
			projectStateManager.saveProjectStates();
			getLog().info("Saved Maven reactor state");
		} catch (final IOException e) {
			throw new MojoExecutionException("Cannot save Maven project state", e);
		}
	}

}
