package de.syquel.maven.reactorstate.common.persistence.json.dto;

import java.util.Map;

public class ArtifactDto {

	private String coordinates;
	private String path;
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
