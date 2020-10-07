package de.syquel.maven.reactorstate.common.persistence.json.dto;

import java.util.Collection;
import java.util.Set;

public class MavenProjectStateDto {

	private String projectId;
	private ArtifactDto pom;
	private ArtifactDto mainArtifact;
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
