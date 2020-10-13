package de.syquel.maven.reactorstate.extension;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.syquel.maven.reactorstate.extension.util.TestUtils;
import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions( { "3.5.4", "3.6.3" })
public class ReactorStateSaveExtensionIT {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReactorStateSaveExtensionIT.class);

	@Rule
	public final TestResources resources = new TestResources();

	private final MavenRuntime mavenRuntime;

	public ReactorStateSaveExtensionIT(final MavenRuntime.MavenRuntimeBuilder mavenRuntimeBuilder) throws Exception {
		mavenRuntime =
			mavenRuntimeBuilder
				.withCliOptions("-B")
				.build();
	}

	@Test
	public void testInvocation() throws Exception {
		final File baseDir = resources.getBasedir("maven-project-stub");
		TestUtils.installExtension(baseDir.toPath());

		final MavenExecution mavenExecution = mavenRuntime.forProject(baseDir);

		LOGGER.info("Execute Maven stage 'verify'");
		final MavenExecutionResult verifyResult = mavenExecution.execute("verify");
		verifyResult.assertErrorFreeLog();

		LOGGER.info("Execute Maven stage 'site'");
		final MavenExecutionResult siteResult = mavenExecution.execute("site");
		siteResult.assertErrorFreeLog();
	}

	@Test
	public void testSubModuleBuild() throws Exception {
		// given
		final File baseDir = resources.getBasedir("maven-project-stub");
		TestUtils.installExtension(baseDir.toPath());

		LOGGER.info("Execute top-level Maven stage 'verify'");
		final MavenExecution mavenExecution = mavenRuntime.forProject(baseDir);
		final MavenExecutionResult verifyResult = mavenExecution.execute("verify");
		verifyResult.assertErrorFreeLog();

		// when
		LOGGER.info("Execute module-level Maven stage 'verify'");
		final MavenExecution moduleMavenExecution = mavenRuntime.forProject(baseDir, "module2");
		final MavenExecutionResult moduleVerifyResult = moduleMavenExecution.execute("verify");

		// then
		moduleVerifyResult.assertErrorFreeLog();
	}

}
