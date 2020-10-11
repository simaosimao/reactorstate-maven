package de.syquel.maven.reactorstate.common;

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
	 * The main artifact of the Maven module.
	 *
	 * This is the primarily generated artifact, e.g. the JAR.
	 */
	private final Artifact mainArtifact;

	/**
	 * Additional attached artifacts on the Maven module.
	 *
	 * These are generated supporting artifacts, e.g. JavaDoc and sources JARs.
	 */
	private final Set<Artifact> attachedArtifacts;

	public MavenProjectState(final MavenProject project, final Artifact pom, final Artifact mainArtifact, final Set<Artifact> attachedArtifacts) {
		this.project = project;
		this.pom = pom;
		this.mainArtifact = mainArtifact;
		this.attachedArtifacts = Collections.unmodifiableSet(new HashSet<>(attachedArtifacts));
	}

	public MavenProject getProject() {
		return project;
	}

	public Artifact getPom() {
		return pom;
	}

	public Artifact getMainArtifact() {
		return mainArtifact;
	}

	public Collection<Artifact> getAttachedArtifacts() {
		return attachedArtifacts;
	}

}
