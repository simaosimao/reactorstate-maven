package de.syquel.maven.reactorstate.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;

public class SavedReactorStateManagerTest {

	@Rule
	public final TestResources resources = new TestResources();

	@Rule
	public final TestMavenRuntime testMavenRuntime = new TestMavenRuntime();

	@Test
	public void testRestoreTopLevelState() throws Exception {
		// given
		final File baseDir = resources.getBasedir("maven-project-stub");

		final MavenProject topLevelProject = testMavenRuntime.readMavenProject(baseDir);
		final MavenProject module1Project = testMavenRuntime.readMavenProject(new File(baseDir, "module1"));
		final MavenProject module2Project = testMavenRuntime.readMavenProject(new File(baseDir, "module2"));

		final MavenSession session = testMavenRuntime.newMavenSession(topLevelProject);
		session.setProjects(Arrays.asList(topLevelProject, module1Project, module2Project));

		final ProjectBuilder projectBuilder = testMavenRuntime.lookup(ProjectBuilder.class);
		final MavenProjectHelper projectHelper = testMavenRuntime.lookup(MavenProjectHelper.class);

		// when
		final SavedReactorStateManager reactorStateManager = SavedReactorStateManager.create(session, projectBuilder);
		Assume.assumeThat(
			"Exactly three saved project states are present",
			reactorStateManager.getProjectStates().size(),
			is(3)
		);

		final MavenProjectState topLevelProjectState = reactorStateManager.getProjectState(topLevelProject);
		Assume.assumeThat("Project POM is for top-level project", topLevelProjectState.getPom().getFile(), is(topLevelProject.getFile()));
		Assume.assumeThat("Main artifact is the project POM", topLevelProjectState.getMainArtifact(), is(topLevelProjectState.getPom()));
		Assume.assumeThat("Top-level artifact is not resolved", topLevelProject.getArtifact().getFile(), nullValue(File.class));
		Assume.assumeThat("Top-level has no artifacts attached", topLevelProjectState.getAttachedArtifacts().size(), is(0));

		Assume.assumeThat("Sub-module1 artifact is not resolved", module1Project.getArtifact().getFile(), nullValue(File.class));
		Assume.assumeThat("Sub-module2 artifact is not resolved", module2Project.getArtifact().getFile(), nullValue(File.class));
		Assume.assumeThat("Sub-module2 has no atrifacts attached", module2Project.getAttachedArtifacts().size(), is(0));

		reactorStateManager.restoreProjectStates(session, projectHelper);

		// then
		MatcherAssert.assertThat("Top-level artifact is resolved", topLevelProject.getArtifact().getFile(), is(topLevelProject.getFile()));

		MatcherAssert.assertThat(
			"Sub-module1 artifact is resolved",
			module1Project.getArtifact().getFile(),
			is(module1Project.getBasedir().toPath().resolve("target/reactorstate-maven-extension-stub-module1-1.0-SNAPSHOT.jar").toFile())
		);

		MatcherAssert.assertThat(
			"Sub-module2 artifact is resolved",
			module2Project.getArtifact().getFile(),
			is(module2Project.getBasedir().toPath().resolve("target/reactorstate-maven-extension-stub-module2-1.0-SNAPSHOT.jar").toFile())
		);

		final Artifact module2AttachedArtifact = module2Project.getAttachedArtifacts().get(0);
		MatcherAssert.assertThat(
			"Sub-module2 has javadoc artifact attached",
			module2AttachedArtifact.getId(),
			is("de.syquel.maven.reactorstate:reactorstate-maven-extension-stub-module2:jar:javadoc:1.0-SNAPSHOT")
		);
		MatcherAssert.assertThat(
			"Sub-module2 has javadoc artifact file attached",
			module2AttachedArtifact.getFile(),
			is(module2Project.getBasedir().toPath().resolve("target/reactorstate-maven-extension-stub-module2-1.0-SNAPSHOT-javadoc.jar").toFile())
		);
	}

}