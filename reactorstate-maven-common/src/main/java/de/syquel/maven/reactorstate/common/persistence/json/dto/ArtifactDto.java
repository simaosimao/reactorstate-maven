package de.syquel.maven.reactorstate.common.persistence.json.dto;

import java.util.Map;

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

}
