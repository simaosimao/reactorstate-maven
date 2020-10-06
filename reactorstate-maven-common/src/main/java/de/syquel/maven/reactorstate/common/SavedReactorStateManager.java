package de.syquel.maven.reactorstate.common;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.syquel.maven.reactorstate.common.persistence.IReactorStateRepository;
import de.syquel.maven.reactorstate.common.persistence.PropertiesReactorStateRepository;

public class SavedReactorStateManager extends AbstractReactorStateManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(SavedReactorStateManager.class);

	private SavedReactorStateManager(final Set<MavenProjectState> projectStates) {
		super(projectStates);
	}

	public static SavedReactorStateManager create(final MavenSession session, final ProjectBuilder projectBuilder)
		throws ProjectBuildingException, IOException
	{
		final Set<MavenProject> projects = new HashSet<>(session.getProjects());

		LOGGER.info("Resolving Maven project tree");
		contributeWorkspaceProjects(session.getCurrentProject(), projects, session.getProjectBuildingRequest(), projectBuilder);

		final IReactorStateRepository reactorStateRepository = new PropertiesReactorStateRepository();

		final Set<MavenProjectState> projectStates = new HashSet<>();
		for (final MavenProject project : projects) {
			final MavenProjectState projectState = reactorStateRepository.read(project);
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

		// Discover downstream Maven projects within workspace
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

		// Discover upstream Maven projects within workspace
		final MavenProject parentProject = project.getParent();
		if (parentProject != null && !discoveredProjects.contains(parentProject) && isWorkspaceProject(parentProject)) {
			LOGGER.info("Discovered Maven parent project {}", parentProject.getId());
			discoveredProjects.add(parentProject);

			contributeWorkspaceProjects(parentProject, discoveredProjects, buildingRequest, projectBuilder);
		}
	}

	private static boolean isWorkspaceProject(final MavenProject project) {
		return project.getBasedir() != null;
	}

}
