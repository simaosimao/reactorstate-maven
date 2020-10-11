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

import de.syquel.maven.reactorstate.common.MavenProjectState;
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

	private static MavenProjectState convert(final MavenProject mavenProject, final MavenProjectStateDto mavenProjectStateDto) {
		final Path projectBasePath = mavenProject.getBasedir().toPath();

		final Artifact pom = convert(mavenProjectStateDto.getPom(), projectBasePath);
		final Artifact mainArtifact = convert(mavenProjectStateDto.getMainArtifact(), projectBasePath);
		final Set<Artifact> attachedArtifacts =
			mavenProjectStateDto.getAttachedArtifacts().stream().map(artifactDto -> convert(artifactDto, projectBasePath)).collect(Collectors.toSet());

		final MavenProjectState mavenProjectState = new MavenProjectState(mavenProject, pom, mainArtifact, attachedArtifacts);
		return mavenProjectState;
	}

	private static Artifact convert(final ArtifactDto artifactDto, final Path projectBasePath) {
		final File artifactFile = (artifactDto.getPath() != null) ? projectBasePath.resolve(artifactDto.getPath()).toFile() : null;

		final Artifact artifact =
			new DefaultArtifact(artifactDto.getCoordinates(), artifactDto.getProperties())
				.setFile(artifactFile);

		return artifact;
	}

	private static MavenProjectStateDto convert(final MavenProjectState projectState) {
		final String projectId = projectState.getProject().getId();
		final Path projectBasePath = projectState.getProject().getBasedir().toPath();

		final ArtifactDto pomDto = convert(projectState.getPom(), projectBasePath);
		final ArtifactDto mainArtifactDto = convert(projectState.getMainArtifact(), projectBasePath);
		final Set<ArtifactDto> attachedArtifactDtos =
			projectState.getAttachedArtifacts().stream().map(artifact -> convert(artifact, projectBasePath)).collect(Collectors.toSet());

		final MavenProjectStateDto projectStateDto = new MavenProjectStateDto(projectId, pomDto, mainArtifactDto, attachedArtifactDtos);
		return projectStateDto;
	}

	private static ArtifactDto convert(final Artifact artifact, final Path projectBasePath) {
		final String artifactCoordinates = ArtifactIdUtils.toId(artifact);

		final File artifactFile = artifact.getFile();
		final String artifactPath = (artifactFile != null) ? projectBasePath.relativize(artifactFile.toPath()).toString() : null;

		final ArtifactDto artifactDto = new ArtifactDto(artifactCoordinates, artifactPath, artifact.getProperties());

		return artifactDto;
	}

	private static Path getReactorStatePath(final MavenProject mavenProject) {
		final Path projectBuildPath = MavenProjectUtils.resolveProjectBuildPath(mavenProject);
		final Path reactorStatePath = projectBuildPath.resolve(STATE_PROPERTIES_FILENAME);

		return reactorStatePath;
	}

	private static JSON buildObjectMapper() {
		return
			JSON.builder()
				.enable(JSON.Feature.PRETTY_PRINT_OUTPUT, JSON.Feature.WRITE_NULL_PROPERTIES, JSON.Feature.READ_ONLY)
				.disable(JSON.Feature.USE_DEFERRED_MAPS)
				.build();
	}

}
