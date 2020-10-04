package de.syquel.maven.reactorstate.plugin;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions( { "3.5.4", "3.6.3" })
public class ReactorStateInstallLocalMojoIT {

	private static final String PLUGIN_GAV = "de.syquel.maven.reactorstate:reactorstate-maven-plugin:1.0-SNAPSHOT";
	private static final Logger LOGGER = LoggerFactory.getLogger(ReactorStateInstallLocalMojoIT.class);

	@Rule
	public final TestResources resources = new TestResources();

	public final MavenRuntime mavenRuntime;

	public ReactorStateInstallLocalMojoIT(final MavenRuntime.MavenRuntimeBuilder mavenRuntimeBuilder) throws Exception {
		mavenRuntime =
			mavenRuntimeBuilder
				.withCliOptions("-B")
				.build();
	}

	@Test
	public void testExtensionLocalSetup() throws Exception {
		final File baseDir = resources.getBasedir("maven-project-stub");
		final MavenExecution mavenExecution = mavenRuntime.forProject(baseDir);

		LOGGER.info("Register reactor-maven-extension locally");
		final MavenExecutionResult registerResult = mavenExecution.execute(PLUGIN_GAV + ":install-local");
		registerResult.assertErrorFreeLog();

		LOGGER.info("Execute Maven stage 'clean'");
		final MavenExecutionResult cleanResult = mavenExecution.execute("clean");
		cleanResult.assertErrorFreeLog();

		LOGGER.info("Execute Maven stage 'verify'");
		final MavenExecutionResult verifyResult = mavenExecution.execute("verify");
		verifyResult.assertErrorFreeLog();

		LOGGER.info("Execute Maven stage 'site'");
		final MavenExecutionResult siteResult = mavenExecution.execute("site");
		siteResult.assertErrorFreeLog();
	}

}