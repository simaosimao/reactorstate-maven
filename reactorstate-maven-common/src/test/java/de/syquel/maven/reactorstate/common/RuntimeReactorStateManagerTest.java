package de.syquel.maven.reactorstate.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.hamcrest.MatcherAssert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

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
		final Properties topLevelProperties = fetchReactorStateProperties(topLevelProject);
		Files.delete(topLevelProject.getBasedir().toPath().resolve(topLevelProject.getBuild().getDirectory()));

		final MavenProject module1Project = testMavenRuntime.readMavenProject(new File(baseDir, "module1"));
		final Properties module1Properties = fetchReactorStateProperties(module1Project);

		final MavenProject module3Project = testMavenRuntime.readMavenProject(new File(module1Project.getBasedir(), "module3"));
		final Properties module3Properties = fetchReactorStateProperties(module3Project);
		{
			final Path projectBasePath = module3Project.getBasedir().toPath();

			final Path artifactPath = projectBasePath.resolve("target/reactorstate-maven-extension-stub-module3-1.0-SNAPSHOT.jar");
			module3Project.getArtifact().setFile(artifactPath.toFile());
		}

		final MavenProject module2Project = testMavenRuntime.readMavenProject(new File(baseDir, "module2"));
		final Properties module2Properties = fetchReactorStateProperties(module2Project);
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
		final Properties savedTopLevelProperties = fetchReactorStateProperties(topLevelProject);
		assertProperties("Saved top-level reactor state is correct", topLevelProperties, savedTopLevelProperties);

		final Properties savedModule1Properties = fetchReactorStateProperties(module1Project);
		assertProperties("Saved module1 reactor state is correct", module1Properties, savedModule1Properties);

		final Properties savedModule2Properties = fetchReactorStateProperties(module2Project);
		assertProperties("Saved module2 reactor state is correct", module2Properties, savedModule2Properties);

		final Properties savedModule3Properties = fetchReactorStateProperties(module3Project);
		assertProperties("Saved module3 reactor state is correct", module3Properties, savedModule3Properties);
	}

	private static void assertProperties(final String message, final Properties expected, final Properties actual) {
		MatcherAssert.assertThat(
			"Main artifact is correct",
			actual.getProperty(AbstractReactorStateManager.PROPERTY_KEY_MAIN_ARTIFACT),
			is(expected.getProperty(AbstractReactorStateManager.PROPERTY_KEY_MAIN_ARTIFACT))
		);

		for (final String propertyName : expected.stringPropertyNames()) {
			if (AbstractReactorStateManager.PROPERTY_KEY_MAIN_ARTIFACT.equals(propertyName)) {
				continue;
			}

			final String expectedPath = expected.getProperty(propertyName);
			final String actualPath = actual.getProperty(propertyName);
			MatcherAssert.assertThat("Path is set for " + propertyName, actualPath, notNullValue());

			MatcherAssert.assertThat(
				message + ": '" + propertyName + "'",
				Paths.get(actualPath),
				is(Paths.get(expectedPath)
				)
			);
		}
	}

	private static Properties fetchReactorStateProperties(final MavenProject project) throws IOException {
		final Path projectBasePath = project.getBasedir().toPath();
		final Path projectBuildPath = projectBasePath.resolve(project.getBuild().getDirectory());
		final Path reactorStatePath = projectBuildPath.resolve(AbstractReactorStateManager.STATE_PROPERTIES_FILENAME);

		final Properties properties = new Properties();
		try (final Reader propertiesReader = Files.newBufferedReader(reactorStatePath)) {
			properties.load(propertiesReader);
		}

		Files.delete(reactorStatePath);

		return properties;
	}

}