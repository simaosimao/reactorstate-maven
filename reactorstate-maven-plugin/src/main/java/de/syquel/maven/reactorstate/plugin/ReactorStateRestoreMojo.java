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

@Mojo(name = "restore", requiresDirectInvocation = true, threadSafe = true, inheritByDefault = false, aggregator = true)
public class ReactorStateRestoreMojo extends AbstractMojo {

	private final MavenSession session;
	private final MavenProjectHelper projectHelper;
	private final ProjectBuilder projectBuilder;

	@Inject
	public ReactorStateRestoreMojo(final MavenSession session, final MavenProjectHelper projectHelper, final ProjectBuilder projectBuilder) {
		this.session = session;
		this.projectHelper = projectHelper;
		this.projectBuilder = projectBuilder;
	}

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
