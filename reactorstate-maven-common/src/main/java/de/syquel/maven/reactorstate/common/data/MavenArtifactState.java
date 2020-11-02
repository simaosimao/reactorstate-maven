package de.syquel.maven.reactorstate.common.data;

import java.util.Objects;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.eclipse.aether.artifact.Artifact;

/**
 * Representation of the state of a Maven artifact.
 */
public class MavenArtifactState {

	/**
	 * The represented Maven artifact.
	 */
	private final Artifact artifact;

	/**
	 * The associated Maven artifact-level repository metadata.
	 */
	private Metadata artifactRepositoryMetadata = null;

	/**
	 * The associated module-group-level repository metadata
	 */
	private Metadata groupRepositoryMetadata = null;

	/**
	 * The associated snapshot-specific repository metadata.
	 */
	private Metadata snapshotRepositoryMetadata = null;

	public MavenArtifactState(final Artifact artifact) {
		this.artifact = artifact;
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public Metadata getArtifactRepositoryMetadata() {
		return artifactRepositoryMetadata;
	}

	public void setArtifactRepositoryMetadata(final Metadata artifactRepositoryMetadata) {
		this.artifactRepositoryMetadata = artifactRepositoryMetadata;
	}

	public Metadata getGroupRepositoryMetadata() {
		return groupRepositoryMetadata;
	}

	public void setGroupRepositoryMetadata(final Metadata groupRepositoryMetadata) {
		this.groupRepositoryMetadata = groupRepositoryMetadata;
	}

	public Metadata getSnapshotRepositoryMetadata() {
		return snapshotRepositoryMetadata;
	}

	public void setSnapshotRepositoryMetadata(final Metadata snapshotRepositoryMetadata) {
		this.snapshotRepositoryMetadata = snapshotRepositoryMetadata;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final MavenArtifactState that = (MavenArtifactState) o;
		return artifact.equals(that.artifact);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifact);
	}

}
