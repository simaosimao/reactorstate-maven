package de.syquel.maven.reactorstate.common.persistence.json;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

import com.fasterxml.jackson.jr.ob.JSON;

import de.syquel.maven.reactorstate.common.data.MavenArtifactState;
import de.syquel.maven.reactorstate.common.data.MavenProjectState;
import de.syquel.maven.reactorstate.common.persistence.IReactorStateRepository;
import de.syquel.maven.reactorstate.common.persistence.json.dto.ArtifactDto;
import de.syquel.maven.reactorstate.common.persistence.json.dto.MavenProjectStateDto;
import de.syquel.maven.reactorstate.common.util.MavenProjectUtils;

/**
 * Implementation of a persistence repository for Maven state information via JSON.
 */
public class JsonReactorStateRepository implements IReactorStateRepository {

	/**
	 * The location to persist Maven state information to.
	 */
	private static final String STATE_PROPERTIES_FILENAME = "reactorstate-maven.json";

	@Override
	public MavenProjectState read(final MavenProject mavenProject) throws IOException {
		final Path reactorStatePath = getReactorStatePath(mavenProject);
		if (!Files.isReadable(reactorStatePath)) {
			return null;
		}

		final JSON objectMapper = buildObjectMapper();

		final MavenProjectStateDto dto;
		try (final Reader reactorStateReader = Files.newBufferedReader(reactorStatePath)) {
			dto = objectMapper.beanFrom(MavenProjectStateDto.class, reactorStateReader);
		}
		final MavenProjectState projectState = convert(mavenProject, dto);

		return projectState;
	}

	@Override
	public void save(final MavenProjectState mavenProjectState) throws IOException {
		final Path reactorStatePath = getReactorStatePath(mavenProjectState.getProject());
		if (!Files.isDirectory(reactorStatePath.getParent())) {
			Files.createDirectories(reactorStatePath.getParent());
		}

		final MavenProjectStateDto dto = convert(mavenProjectState);

		final JSON objectMapper = buildObjectMapper();
		try (final Writer reactorStateWriter = Files.newBufferedWriter(reactorStatePath)) {
			objectMapper.write(dto, reactorStateWriter);
		}
	}

	@Override
	public void delete(final MavenProject mavenProject) throws IOException {
		final Path reactorStatePath = getReactorStatePath(mavenProject);
		Files.deleteIfExists(reactorStatePath);
	}

	/**
	 * Deserializes a persisted Maven module state.
	 *
	 * @param mavenProject The Maven module to deserialize the state for.
	 * @param mavenProjectStateDto The serialized representation of the Maven module state.
	 * @return The deserialized representation of the Maven module state.
	 */
	private static MavenProjectState convert(final MavenProject mavenProject, final MavenProjectStateDto mavenProjectStateDto) {
		final Path projectBasePath = mavenProject.getBasedir().toPath();

		final Artifact pom = buildArtifactDto(mavenProjectStateDto.getPom(), projectBasePath);
		final MavenArtifactState mainArtifactState = convert(mavenProjectStateDto.getMainArtifact(), projectBasePath);
		final Set<MavenArtifactState> attachedArtifactStates =
			mavenProjectStateDto.getAttachedArtifacts().stream().map(artifactDto -> convert(artifactDto, projectBasePath)).collect(Collectors.toSet());

		final MavenProjectState mavenProjectState = new MavenProjectState(mavenProject, pom, mainArtifactState, attachedArtifactStates);
		return mavenProjectState;
	}

	/**
	 * Deserializes a persisted Maven artifact state.
	 *
	 * @param artifactDto The serialized representation of the Maven artifact state.
	 * @param projectBasePath The base path of the Maven module to deserialize the state for.
	 * @return The deserialized representation of the Maven artifact state.
	 */
	private static MavenArtifactState convert(final ArtifactDto artifactDto, final Path projectBasePath) {
		final Artifact artifact = buildArtifactDto(artifactDto, projectBasePath);

		final MavenArtifactState artifactState = new MavenArtifactState(artifact);
		artifactState.setArtifactRepositoryMetadata(artifactDto.getArtifactRepositoryMetadata());
		artifactState.setGroupRepositoryMetadata(artifactDto.getGroupRepositoryMetadata());
		artifactState.setSnapshotRepositoryMetadata(artifactDto.getSnapshotRepositoryMetadata());

		return artifactState;
	}

	/**
	 * Serializes a Maven module state.
	 *
	 * @param projectState The deserialized representation of the Maven module state.
	 * @return The serialized representation of the Maven module state.
	 */
	private static MavenProjectStateDto convert(final MavenProjectState projectState) {
		final String projectId = projectState.getProject().getId();
		final Path projectBasePath = projectState.getProject().getBasedir().toPath();

		final ArtifactDto pomDto = buildArtifactDto(projectState.getPom(), projectBasePath);
		final ArtifactDto mainArtifactDto = convert(projectState.getMainArtifactState(), projectBasePath);
		final Set<ArtifactDto> attachedArtifactDtos =
			projectState.getAttachedArtifactStates().stream().map(artifact -> convert(artifact, projectBasePath)).collect(Collectors.toSet());

		final MavenProjectStateDto projectStateDto = new MavenProjectStateDto(projectId, pomDto, mainArtifactDto, attachedArtifactDtos);
		return projectStateDto;
	}

	/**
	 * Serializes a Maven artifact state.
	 *
	 * @param artifactState The deserialized representation of the Maven artifact state.
	 * @param projectBasePath The base path of the Maven module to serialize the state for.
	 * @return The serialized representation of the Maven artifact state.
	 */
	private static ArtifactDto convert(final MavenArtifactState artifactState, final Path projectBasePath) {
		final Artifact artifact = artifactState.getArtifact();

		final ArtifactDto artifactDto = buildArtifactDto(artifact, projectBasePath);
		artifactDto.setArtifactRepositoryMetadata(artifactState.getArtifactRepositoryMetadata());
		artifactDto.setGroupRepositoryMetadata(artifactState.getGroupRepositoryMetadata());
		artifactDto.setSnapshotRepositoryMetadata(artifactState.getSnapshotRepositoryMetadata());

		return artifactDto;
	}

	/**
	 * Builds a Maven artifact from its persistent representation.
	 *
	 * @param artifactDto The serialized representation of the Maven artifact state.
	 * @param projectBasePath The base path of the Maven module to deserialize the state for.
	 * @return The deserialized representation of the Maven artifact.
	 */
	private static Artifact buildArtifactDto(final ArtifactDto artifactDto, final Path projectBasePath) {
		final File artifactFile = (artifactDto.getPath() != null) ? projectBasePath.resolve(artifactDto.getPath()).toFile() : null;

		final Artifact artifact =
			new DefaultArtifact(artifactDto.getCoordinates(), artifactDto.getProperties())
				.setFile(artifactFile);

		return artifact;
	}

	/**
	 * Builds a persistent representation for a Maven artifact.
	 *
	 * @param artifact The deserialized representation of the Maven artifact.
	 * @param projectBasePath The base path of the Maven module to deserialize the state for.
	 * @return The serialized representation of the Maven artifact.
	 */
	private static ArtifactDto buildArtifactDto(final Artifact artifact, final Path projectBasePath) {
		final String artifactCoordinates = ArtifactIdUtils.toId(artifact);

		final File artifactFile = artifact.getFile();
		final String artifactPath = (artifactFile != null) ? projectBasePath.relativize(artifactFile.toPath()).toString() : null;

		final ArtifactDto artifactDto = new ArtifactDto(artifactCoordinates, artifactPath, artifact.getProperties());
		return artifactDto;
	}

	/**
	 * Determines the filesystem path to persisted state information for a Maven module.
	 *
	 * @param mavenProject The Maven module to determine the path for.
	 * @return The filesystem path to persisted state information.
	 */
	private static Path getReactorStatePath(final MavenProject mavenProject) {
		final Path projectBuildPath = MavenProjectUtils.resolveProjectBuildPath(mavenProject);
		final Path reactorStatePath = projectBuildPath.resolve(STATE_PROPERTIES_FILENAME);

		return reactorStatePath;
	}

	/**
	 * @return a pre-configured Json object mapper.
	 */
	private static JSON buildObjectMapper() {
		return
			JSON.builder()
				.enable(JSON.Feature.PRETTY_PRINT_OUTPUT, JSON.Feature.WRITE_NULL_PROPERTIES, JSON.Feature.READ_ONLY)
				.disable(JSON.Feature.USE_DEFERRED_MAPS)
				.build();
	}

}
