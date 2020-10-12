package de.syquel.maven.reactorstate.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

import de.syquel.maven.reactorstate.common.data.MavenArtifactState;
import de.syquel.maven.reactorstate.common.data.MavenProjectState;
import de.syquel.maven.reactorstate.common.persistence.IReactorStateRepository;
import de.syquel.maven.reactorstate.common.persistence.json.JsonReactorStateRepository;
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

			final Metadata artifactMetadata = module2State.getMainArtifactState().getArtifactRepositoryMetadata();
			final ArtifactRepositoryMetadata artifactRepositoryMetadata = new ArtifactRepositoryMetadata(module2Project.getArtifact());
			artifactRepositoryMetadata.setMetadata(artifactMetadata);
			module2Project.getArtifact().addMetadata(artifactRepositoryMetadata);

			final Metadata groupMetadata = module2State.getMainArtifactState().getGroupRepositoryMetadata();
			final GroupRepositoryMetadata groupRepositoryMetadata = new GroupRepositoryMetadata(module2Project.getGroupId());
			groupRepositoryMetadata.setMetadata(groupMetadata);
			module2Project.getArtifact().addMetadata(groupRepositoryMetadata);
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
		assertArtifactState(expected.getMainArtifactState(), actual.getMainArtifactState());

		final Map<String, MavenArtifactState> actualAttachedArtifactStates = new HashMap<>();
		for (final MavenArtifactState actualAttachedArtifactState : actual.getAttachedArtifactStates()) {
			final String artifactCoordinates = ArtifactIdUtils.toId(actualAttachedArtifactState.getArtifact());
			actualAttachedArtifactStates.put(artifactCoordinates, actualAttachedArtifactState);
		}

		for (final MavenArtifactState expectedAttachedArtifactState : expected.getAttachedArtifactStates()) {
			final String artifactCoordinates = ArtifactIdUtils.toId(expectedAttachedArtifactState.getArtifact());
			final MavenArtifactState actualAttachedArtifactState = actualAttachedArtifactStates.get(artifactCoordinates);

			MatcherAssert.assertThat("Artifact state exists", actualAttachedArtifactState, notNullValue(MavenArtifactState.class));
			assertArtifactState(expectedAttachedArtifactState, actualAttachedArtifactState);
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

	private static void assertMetadata(final Metadata expected, final Metadata actual) {
		if (expected == null) {
			MatcherAssert.assertThat("Metadata does not exist", actual, nullValue(Metadata.class));
		} else {
			MatcherAssert.assertThat("Metadata has correct group ID", expected.getGroupId(), is(actual.getGroupId()));
			MatcherAssert.assertThat("Metadata has correct artifact ID", expected.getArtifactId(), is(actual.getArtifactId()));
		}
	}

	private static void assertArtifactState(final MavenArtifactState expected, final MavenArtifactState actual) {
		assertArtifact(expected.getArtifact(), actual.getArtifact());

		assertMetadata(expected.getArtifactRepositoryMetadata(), actual.getArtifactRepositoryMetadata());
		assertMetadata(expected.getGroupRepositoryMetadata(), actual.getGroupRepositoryMetadata());
		assertMetadata(expected.getSnapshotRepositoryMetadata(), actual.getSnapshotRepositoryMetadata());
	}

	private static MavenProjectState fetchReactorState(final MavenProject project) throws IOException {
		final IReactorStateRepository reactorStateRepository = new JsonReactorStateRepository();

		final MavenProjectState mavenProjectState = reactorStateRepository.read(project);
		reactorStateRepository.delete(mavenProjectState.getProject());

		return mavenProjectState;
	}

}
