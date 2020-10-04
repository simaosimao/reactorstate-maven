package de.syquel.maven.reactorstate.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;

public class MavenProjectState {

	private final MavenProject project;
	private final Artifact pom;
	private final Artifact mainArtifact;
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
