package de.syquel.maven.reactorstate.common.persistence.json.dto;

import java.util.Map;

import org.apache.maven.artifact.repository.metadata.Metadata;

/**
 * The JSON-specific representation of a Maven artifact.
 */
public class ArtifactDto {

	/**
	 * The coordinates of a Maven artifact in the format [groupId]:[artifactId]:[extension]:[type]:[version]
	 */
	private String coordinates;

	/**
	 * The filesystem path to the Maven artifact.
	 */
	private String path;

	/**
	 * Additional metadata which are a bound to the Maven artifact.
	 */
	private Map<String, String> properties;

	/**
	 * The associated module-group-level repository metadata
	 */
	private Metadata groupRepositoryMetadata;

	/**
	 * The associated Maven artifact-level repository metadata.
	 */
	private Metadata artifactRepositoryMetadata;

	/**
	 * The associated snapshot-specific repository metadata.
	 */
	private Metadata snapshotRepositoryMetadata;

	public ArtifactDto(final String coordinates, final String path, final Map<String, String> properties) {
		this.coordinates = coordinates;
		this.path = path;
		this.properties = properties;
	}

	protected ArtifactDto() {
		// Jackson constructor
	}

	public String getCoordinates() {
		return coordinates;
	}

	protected void setCoordinates(final String coordinates) {
		this.coordinates = coordinates;
	}

	public String getPath() {
		return path;
	}

	protected void setPath(final String path) {
		this.path = path;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	protected void setProperties(final Map<String, String> properties) {
		this.properties = properties;
	}

	public Metadata getGroupRepositoryMetadata() {
		return groupRepositoryMetadata;
	}

	public void setGroupRepositoryMetadata(final Metadata groupRepositoryMetadata) {
		this.groupRepositoryMetadata = groupRepositoryMetadata;
	}

	public Metadata getArtifactRepositoryMetadata() {
		return artifactRepositoryMetadata;
	}

	public void setArtifactRepositoryMetadata(final Metadata artifactRepositoryMetadata) {
		this.artifactRepositoryMetadata = artifactRepositoryMetadata;
	}

	public Metadata getSnapshotRepositoryMetadata() {
		return snapshotRepositoryMetadata;
	}

	public void setSnapshotRepositoryMetadata(final Metadata snapshotRepositoryMetadata) {
		this.snapshotRepositoryMetadata = snapshotRepositoryMetadata;
	}

}
