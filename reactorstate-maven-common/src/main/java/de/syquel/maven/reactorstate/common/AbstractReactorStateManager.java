package de.syquel.maven.reactorstate.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;

public abstract class AbstractReactorStateManager {

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

}
