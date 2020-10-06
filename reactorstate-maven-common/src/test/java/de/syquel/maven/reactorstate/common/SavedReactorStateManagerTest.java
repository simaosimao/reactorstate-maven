package de.syquel.maven.reactorstate.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.hamcrest.MatcherAssert;
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
		final MavenProject module3Project = testMavenRuntime.readMavenProject(new File(module1Project.getBasedir(), "module3"));

		final MavenSession session = testMavenRuntime.newMavenSession(topLevelProject);
		session.setProjects(Arrays.asList(topLevelProject, module1Project, module2Project, module3Project));

		final ProjectBuilder projectBuilder = testMavenRuntime.lookup(ProjectBuilder.class);
		final MavenProjectHelper projectHelper = testMavenRuntime.lookup(MavenProjectHelper.class);

		// when
		final SavedReactorStateManager reactorStateManager = SavedReactorStateManager.create(session, projectBuilder);
		MatcherAssert.assertThat(
			"Exactly four saved project states are present",
			reactorStateManager.getProjectStates().size(),
			is(4)
		);

		final MavenProjectState topLevelProjectState = reactorStateManager.getProjectState(topLevelProject);
		MatcherAssert.assertThat("Project POM is for top-level project", topLevelProjectState.getPom().getFile(), is(topLevelProject.getFile()));
		MatcherAssert.assertThat("Main artifact is the project POM", topLevelProjectState.getMainArtifact(), is(topLevelProjectState.getPom()));
		MatcherAssert.assertThat("Top-level artifact is not resolved", topLevelProject.getArtifact().getFile(), nullValue(File.class));
		MatcherAssert.assertThat("Top-level has no artifacts attached", topLevelProjectState.getAttachedArtifacts().size(), is(0));

		MatcherAssert.assertThat("Sub-module1 artifact is not resolved", module1Project.getArtifact().getFile(), nullValue(File.class));
		MatcherAssert.assertThat("Sub-module2 artifact is not resolved", module2Project.getArtifact().getFile(), nullValue(File.class));
		MatcherAssert.assertThat("Sub-module2 has no artifacts attached", module2Project.getAttachedArtifacts().size(), is(0));

		reactorStateManager.restoreProjectStates(session, projectHelper);

		// then
		MatcherAssert.assertThat("Top-level artifact is resolved", topLevelProject.getArtifact().getFile(), is(topLevelProject.getFile()));

		MatcherAssert.assertThat(
			"Sub-module3 artifact is resolved",
			module3Project.getArtifact().getFile(),
			is(module3Project.getBasedir().toPath().resolve("target/reactorstate-maven-extension-stub-module3-1.0-SNAPSHOT.jar").toFile())
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

	@Test
	public void testRestoreSubModuleState() throws Exception {
		// given
		final File baseDir = resources.getBasedir("maven-project-stub");

		final MavenProject topLevelProject = testMavenRuntime.readMavenProject(baseDir);
		final MavenProject module1Project = testMavenRuntime.readMavenProject(new File(baseDir, "module1"));
		final MavenProject module2Project = testMavenRuntime.readMavenProject(new File(baseDir, "module2"));
		final MavenProject module3Project = testMavenRuntime.readMavenProject(new File(module1Project.getBasedir(), "module3"));

		final MavenSession session = testMavenRuntime.newMavenSession(module2Project);

		final ProjectBuilder projectBuilder = testMavenRuntime.lookup(ProjectBuilder.class);
		final MavenProjectHelper projectHelper = testMavenRuntime.lookup(MavenProjectHelper.class);

		// when
		final SavedReactorStateManager reactorStateManager = SavedReactorStateManager.create(session, projectBuilder);

		MatcherAssert.assertThat(
			"Top-level project state is present",
			reactorStateManager.getProjectState(topLevelProject),
			notNullValue(MavenProjectState.class)
		);
		MatcherAssert.assertThat(
			"Sub-module1 project state is present",
			reactorStateManager.getProjectState(module1Project),
			notNullValue(MavenProjectState.class)
		);
		MatcherAssert.assertThat(
			"Sub-module2 project state is present",
			reactorStateManager.getProjectState(module2Project),
			notNullValue(MavenProjectState.class)
		);
		MatcherAssert.assertThat(
			"Sub-module3 project state is present",
			reactorStateManager.getProjectState(module3Project),
			notNullValue(MavenProjectState.class)
		);

		reactorStateManager.restoreProjectStates(session, projectHelper);

		// then
		final List<MavenProject> sessionProjects = session.getProjects();
		MatcherAssert.assertThat("Maven session contains only one project", sessionProjects.size(), is(1));
		MatcherAssert.assertThat("Maven session contains only Sub-module2", sessionProjects.get(0), is(module2Project));

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