package de.syquel.maven.reactorstate.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

import de.syquel.maven.reactorstate.common.persistence.IReactorStateRepository;
import de.syquel.maven.reactorstate.common.persistence.PropertiesReactorStateRepository;
import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;

public class RuntimeReactorStateManagerTest {

	@Rule
	public final TestResources resources = new TestResources();

	@Rule
	public final TestMavenRuntime testMavenRuntime = new TestMavenRuntime();

	@Test
	public void testSaveProjectStates() throws Exception {
		// given
		final File baseDir = resources.getBasedir("maven-project-stub");
		final MavenProjectHelper projectHelper = testMavenRuntime.lookup(MavenProjectHelper.class);

		final MavenProject topLevelProject = testMavenRuntime.readMavenProject(baseDir);
		final MavenProjectState topLevelState = fetchReactorState(topLevelProject);
		Files.delete(topLevelProject.getBasedir().toPath().resolve(topLevelProject.getBuild().getDirectory()));

		final MavenProject module1Project = testMavenRuntime.readMavenProject(new File(baseDir, "module1"));
		final MavenProjectState module1State = fetchReactorState(module1Project);

		final MavenProject module3Project = testMavenRuntime.readMavenProject(new File(module1Project.getBasedir(), "module3"));
		final MavenProjectState module3State = fetchReactorState(module3Project);
		{
			final Path projectBasePath = module3Project.getBasedir().toPath();

			final Path artifactPath = projectBasePath.resolve("target/reactorstate-maven-extension-stub-module3-1.0-SNAPSHOT.jar");
			module3Project.getArtifact().setFile(artifactPath.toFile());
		}

		final MavenProject module2Project = testMavenRuntime.readMavenProject(new File(baseDir, "module2"));
		final MavenProjectState module2State = fetchReactorState(module2Project);
		{
			final Path projectBasePath = module2Project.getBasedir().toPath();

			final Path artifactPath = projectBasePath.resolve("target/reactorstate-maven-extension-stub-module2-1.0-SNAPSHOT.jar");
			module2Project.getArtifact().setFile(artifactPath.toFile());

			final Path javadocPath = projectBasePath.resolve("target/reactorstate-maven-extension-stub-module2-1.0-SNAPSHOT-javadoc.jar");
			projectHelper.attachArtifact(module2Project, javadocPath.toFile(), "javadoc");
		}

		final MavenSession session = testMavenRuntime.newMavenSession(topLevelProject);
		session.setProjects(Arrays.asList(topLevelProject, module1Project, module2Project, module3Project));

		// when
		final RuntimeReactorStateManager reactorStateManager = RuntimeReactorStateManager.create(session);
		Assume.assumeThat(
			"Exactly four runtime project states are present",
			reactorStateManager.getProjectStates().size(),
			is(4)
		);

		reactorStateManager.saveProjectStates();

		// then
		final MavenProjectState savedTopLevelState = fetchReactorState(topLevelProject);
		assertMavenProjectState(topLevelState, savedTopLevelState);

		final MavenProjectState savedModule1State = fetchReactorState(module1Project);
		assertMavenProjectState(module1State, savedModule1State);

		final MavenProjectState savedModule2State = fetchReactorState(module2Project);
		assertMavenProjectState(module2State, savedModule2State);

		final MavenProjectState savedModule3State = fetchReactorState(module3Project);
		assertMavenProjectState(module3State, savedModule3State);
	}

	private static void assertMavenProjectState(final MavenProjectState expected, final MavenProjectState actual) {
		assertArtifact(expected.getPom(), actual.getPom());
		assertArtifact(expected.getMainArtifact(), actual.getMainArtifact());

		final Map<String, Artifact> actualAttachedArtifacts = new HashMap<>();
		for (final Artifact actualAttachedArtifact : actual.getAttachedArtifacts()) {
			actualAttachedArtifacts.put(ArtifactIdUtils.toId(actualAttachedArtifact), actualAttachedArtifact);
		}

		for (final Artifact expectedAttachedArtifact : expected.getAttachedArtifacts()) {
			final Artifact actualAttachedArtifact = actualAttachedArtifacts.get(ArtifactIdUtils.toId(expectedAttachedArtifact));

			MatcherAssert.assertThat("Artifact state exists", actualAttachedArtifact, notNullValue(Artifact.class));
			assertArtifact(expectedAttachedArtifact, actualAttachedArtifact);
		}
	}

	private static void assertArtifact(final Artifact expected, final Artifact actual) {
		MatcherAssert.assertThat(
			"Artifact coordinates are correct",
			ArtifactIdUtils.toId(actual),
			is(ArtifactIdUtils.toId(expected))
		);
		MatcherAssert.assertThat(
			"Artifact file path is correct",
			actual.getFile(),
			is(expected.getFile())
		);
	}

	private static MavenProjectState fetchReactorState(final MavenProject project) throws IOException {
		final IReactorStateRepository reactorStateRepository = new PropertiesReactorStateRepository();

		final MavenProjectState mavenProjectState = reactorStateRepository.read(project);
		reactorStateRepository.delete(mavenProjectState.getProject());

		return mavenProjectState;
	}

}