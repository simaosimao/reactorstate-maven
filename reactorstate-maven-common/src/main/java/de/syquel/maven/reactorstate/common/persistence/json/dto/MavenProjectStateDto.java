package de.syquel.maven.reactorstate.common.persistence.json.dto;

import java.util.Collection;
import java.util.Set;

/**
 * The JSON-specific representation of the state of a Maven module.
 */
public class MavenProjectStateDto {

	/**
	 * The full ID of the Maven module in the format [groupId]:[artifactId]:[packaging]:[version]
	 */
	private String projectId;

	/**
	 * The POM of the Maven module as Maven artifact.
	 */
	private ArtifactDto pom;

	/**
	 * The main artifact of the Maven module.
	 *
	 * This is the primarily generated artifact, e.g. the JAR.
	 */
	private ArtifactDto mainArtifact;

	/**
	 * Additional attached artifacts on the Maven module.
	 *
	 * These are generated supporting artifacts, e.g. JavaDoc and sources JARs.
	 */
	private Collection<ArtifactDto> attachedArtifacts;

	public MavenProjectStateDto(final String projectId, final ArtifactDto pom, final ArtifactDto mainArtifact, final Set<ArtifactDto> attachedArtifacts) {
		this.projectId = projectId;
		this.pom = pom;
		this.mainArtifact = mainArtifact;
		this.attachedArtifacts = attachedArtifacts;
	}

	protected MavenProjectStateDto() {
		// Jackson constructor
	}

	public String getProjectId() {
		return projectId;
	}

	protected void setProjectId(final String projectId) {
		this.projectId = projectId;
	}

	public ArtifactDto getPom() {
		return pom;
	}

	protected void setPom(final ArtifactDto pom) {
		this.pom = pom;
	}

	public ArtifactDto getMainArtifact() {
		return mainArtifact;
	}

	protected void setMainArtifact(final ArtifactDto mainArtifact) {
		this.mainArtifact = mainArtifact;
	}

	public Collection<ArtifactDto> getAttachedArtifacts() {
		return attachedArtifacts;
	}

	protected void setAttachedArtifacts(final Collection<ArtifactDto> attachedArtifacts) {
		this.attachedArtifacts = attachedArtifacts;
	}

}
