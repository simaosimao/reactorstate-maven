package de.syquel.maven.reactorstate.common;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;

public abstract class AbstractReactorStateManager {

	static final String STATE_PROPERTIES_FILENAME = "reactorstate-maven.properties";
	static final String PROPERTY_KEY_MAIN_ARTIFACT = "main-artifact";

	private final Map<MavenProject, MavenProjectState> projectStates = new HashMap<>();

	protected AbstractReactorStateManager(final Set<MavenProjectState> projectStates) {
		for (final MavenProjectState projectState : projectStates) {
			this.projectStates.put(projectState.getProject(), projectState);
		}
	}

	public Set<MavenProjectState> getProjectStates() {
		return new HashSet<>(projectStates.values());
	}

	public MavenProjectState getProjectState(final MavenProject project) {
		return projectStates.get(project);
	}

	protected static Path resolveProjectBuildPath(final MavenProject project) {
		final Path projectBasePath = project.getBasedir().toPath();
		final Path projectBuildPath = projectBasePath.resolve(project.getBuild().getDirectory());

		return projectBuildPath;
	}

}
