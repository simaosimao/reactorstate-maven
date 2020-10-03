package de.syquel.maven.reactorstate.common;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SavedReactorStateManager extends AbstractReactorStateManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(SavedReactorStateManager.class);

	private SavedReactorStateManager(final Set<MavenProjectState> projectStates) {
		super(projectStates);
	}

	public static SavedReactorStateManager create(final MavenSession session, final ProjectBuilder projectBuilder)
		throws ProjectBuildingException, IOException
	{
		final Set<MavenProject> projects = new HashSet<>(session.getProjects());

		final MavenProject currentProject = session.getCurrentProject();
		final MavenProject parentProject = currentProject.getParent();
		if (session.getCurrentProject().isExecutionRoot() && parentProject != null) {
			LOGGER.info("Resolving Maven project tree");
			contributeWorkspaceProjects(parentProject, projects, session.getProjectBuildingRequest(), projectBuilder);
		}

		final Set<MavenProjectState> projectStates = new HashSet<>();
		for (final MavenProject project : projects) {
			final MavenProjectState projectState = loadProjectState(project);
			if (projectState != null) {
				projectStates.add(projectState);
			}
		}

		return new SavedReactorStateManager(projectStates);
	}

	public void restoreProjectStates(final MavenSession session, final MavenProjectHelper projectHelper) {
		final Set<MavenProject> projects = new HashSet<>(session.getProjects());

		for (final MavenProject project : projects) {
			final MavenProjectState projectState = getProjectState(project);
			if (projectState == null) {
				throw new IllegalStateException("No saved state found for Maven project " + project.getId() + ": Rebuild required.");
			}

			final org.apache.maven.artifact.Artifact mainArtifact = RepositoryUtils.toArtifact(projectState.getMainArtifact());
			project.getArtifact().setFile(mainArtifact.getFile());
			LOGGER.info("Restored main artifact {}", mainArtifact.getId());

			final Collection<org.apache.maven.artifact.Artifact> attachedArtifacts =
				projectState.getAttachedArtifacts().stream().map(RepositoryUtils::toArtifact).collect(Collectors.toList());
			project.getAttachedArtifacts().clear();
			for (final org.apache.maven.artifact.Artifact attachedArtifact : attachedArtifacts) {
				projectHelper.attachArtifact(project, attachedArtifact.getType(), attachedArtifact.getClassifier(), attachedArtifact.getFile());
				LOGGER.info("Restored attached artifact {}", attachedArtifact.getId());
			}
		}
	}

	private static void contributeWorkspaceProjects(
		final MavenProject project, final Set<MavenProject> discoveredProjects,
		final ProjectBuildingRequest buildingRequest, final ProjectBuilder projectBuilder
	) throws ProjectBuildingException
	{
		final Path projectBasePath = project.getBasedir().toPath();

		for (final String module : project.getModules()) {
			final Path modulePomPath = projectBasePath.resolve(module).resolve("pom.xml");

			final MavenProject moduleProject = projectBuilder.build(modulePomPath.toFile(), buildingRequest).getProject();
			if (discoveredProjects.contains(moduleProject)) {
				continue;
			}

			LOGGER.info("Discovered upstream Maven project {}", moduleProject.getId());
			discoveredProjects.add(moduleProject);

			contributeWorkspaceProjects(moduleProject, discoveredProjects, buildingRequest, projectBuilder);
		}

		final MavenProject parentProject = project.getParent();
		if (parentProject != null && !discoveredProjects.contains(parentProject)) {
			LOGGER.info("Discovered Maven parent project {}", parentProject.getId());
			discoveredProjects.add(parentProject);
			contributeWorkspaceProjects(parentProject, discoveredProjects, buildingRequest, projectBuilder);
		}
	}

	private static MavenProjectState loadProjectState(final MavenProject project) throws IOException {
		// Read project state from filesystem
		final Path projectBuildPath = resolveProjectBuildPath(project);
		final Path projectStatePath = projectBuildPath.resolve(STATE_PROPERTIES_FILENAME);
		if (!Files.isDirectory(projectBuildPath) || !Files.isReadable(projectStatePath)) {
			return null;
		}

		final Properties stateProperties = new Properties();
		try (final Reader stateReader = Files.newBufferedReader(projectStatePath, StandardCharsets.UTF_8)) {
			stateProperties.load(stateReader);
		}

		// Extract main artifact
		final String mainArtifactId = stateProperties.getProperty(PROPERTY_KEY_MAIN_ARTIFACT);
		final Path mainArtifactPath = Paths.get(stateProperties.getProperty(mainArtifactId));
		stateProperties.remove(PROPERTY_KEY_MAIN_ARTIFACT);
		stateProperties.remove(mainArtifactId);
		final Artifact mainArtifact = buildArtifact(mainArtifactId, mainArtifactPath);

		if (mainArtifact == null) {
			throw new IllegalStateException("Main Artifact is missing in saved state");
		}

		// Extract POM artifact
		Artifact pom = ArtifactDescriptorUtils.toPomArtifact(mainArtifact);
		if (pom != mainArtifact) {
			final String pomArtifactId = ArtifactIdUtils.toId(pom);
			final Path pomPath = Paths.get(stateProperties.getProperty(pomArtifactId));
			stateProperties.remove(pomArtifactId);

			pom = pom.setFile(pomPath.toFile());
		}

		// Extract attached artifacts
		final Set<Artifact> attachedArtifacts = new HashSet<>();
		for (final Map.Entry<Object, Object> artifactEntry : stateProperties.entrySet()) {
			final String artifactId = (String) artifactEntry.getKey();
			final Path artifactPath = Paths.get((String) artifactEntry.getValue());

			final Artifact artifact = buildArtifact(artifactId, artifactPath);
			attachedArtifacts.add(artifact);
		}

		// Build project state
		final MavenProjectState projectState = new MavenProjectState(project, pom, mainArtifact, attachedArtifacts);
		return projectState;
	}

	private static Artifact buildArtifact(final String artifactId, final Path artifactPath) {
		if (!Files.isReadable(artifactPath)) {
			LOGGER.warn("Artifact {} does not exist anymore. Ignoring.", artifactId);
			return null;
		}

		// TODO add properties (like "type")
		final Artifact artifact =
			new DefaultArtifact(artifactId)
				.setFile(artifactPath.toFile());

		return artifact;
	}

}
