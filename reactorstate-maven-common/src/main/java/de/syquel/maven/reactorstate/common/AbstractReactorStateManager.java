package de.syquel.maven.reactorstate.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;

/**
 * Base class for implementations which operate on the state of a Maven project and its modules.
 */
public abstract class AbstractReactorStateManager {

	/**
	 * The states of Maven modules within a Maven project.
	 */
	private final Map<MavenProject, MavenProjectState> projectStates = new HashMap<>();

	protected AbstractReactorStateManager(final Set<MavenProjectState> projectStates) {
		for (final MavenProjectState projectState : projectStates) {
			this.projectStates.put(projectState.getProject(), projectState);
		}
	}

	/**
	 * Returns the states of Maven modules within a Maven project.
	 *
	 * @return
	 */
	public Set<MavenProjectState> getProjectStates() {
		return new HashSet<>(projectStates.values());
	}

	/**
	 * Returns the state of a specific Maven module within a Maven project.
	 *
	 * @param project The Maven module to return the state for.
	 * @return The state of the requested Maven module.
	 */
	public MavenProjectState getProjectState(final MavenProject project) {
		return projectStates.get(project);
	}

}
