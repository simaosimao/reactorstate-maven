package de.syquel.maven.reactorstate.plugin;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.syquel.maven.reactorstate.plugin.util.TestUtils;
import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions( { "3.5.4", "3.6.3" })
public class ReactorStateRestoreMojoIT {

	private static final String PLUGIN_GAV = TestUtils.getProjectGav();
	private static final Logger LOGGER = LoggerFactory.getLogger(ReactorStateRestoreMojoIT.class);

	@Rule
	public final TestResources resources = new TestResources();

	public final MavenRuntime mavenRuntime;

	public ReactorStateRestoreMojoIT(final MavenRuntime.MavenRuntimeBuilder mavenRuntimeBuilder) throws Exception {
		mavenRuntime =
			mavenRuntimeBuilder
				.withCliOptions("-B", "-e")
				.build();
	}

	@Test
	public void testRestore() throws Exception {
		final File baseDir = resources.getBasedir("maven-project-stub");
		final MavenExecution mavenExecution = mavenRuntime.forProject(baseDir);

		LOGGER.info("Execute Maven stage 'verify'");
		final MavenExecutionResult verifyResult = mavenExecution.execute("verify", PLUGIN_GAV + ":save");
		verifyResult.assertErrorFreeLog();

		LOGGER.info("Execute Maven stage 'site'");
		final MavenExecutionResult siteResult = mavenExecution.execute(PLUGIN_GAV + ":restore", "site", PLUGIN_GAV + ":save");
		siteResult.assertErrorFreeLog();
	}

}
