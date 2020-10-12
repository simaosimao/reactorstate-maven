package de.syquel.maven.reactorstate.common.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;

/**
 * Representation of the state of a Maven module.
 */
public class MavenProjectState {

	/**
	 * The represented Maven module.
	 */
	private final MavenProject project;

	/**
	 * The POM of the Maven module as Maven artifact.
	 */
	private final Artifact pom;

	/**
	 * The main artifact state of the Maven module.
	 *
	 * This is the primarily generated artifact, e.g. the JAR.
	 */
	private final MavenArtifactState mainArtifactState;

	/**
	 * Additional states of attached artifacts on the Maven module.
	 *
	 * These are generated supporting artifacts, e.g. JavaDoc and sources JARs.
	 */
	private final Set<MavenArtifactState> attachedArtifactStates;

	public MavenProjectState(
		final MavenProject project, final Artifact pom, final MavenArtifactState mainArtifactState, final Set<MavenArtifactState> attachedArtifactStates
	) {
		this.project = project;
		this.pom = pom;
		this.mainArtifactState = mainArtifactState;
		this.attachedArtifactStates = Collections.unmodifiableSet(new HashSet<>(attachedArtifactStates));
	}

	public MavenProject getProject() {
		return project;
	}

	public Artifact getPom() {
		return pom;
	}

	public MavenArtifactState getMainArtifactState() {
		return mainArtifactState;
	}

	public Collection<MavenArtifactState> getAttachedArtifactStates() {
		return attachedArtifactStates;
	}

}
