package de.syquel.maven.reactorstate.common.util;

import java.nio.file.Path;

import org.apache.maven.project.MavenProject;

/**
 * Utility class for Maven project related functionality.
 */
public final class MavenProjectUtils {

	private MavenProjectUtils() {}

	/**
	 * Resolves the path of the Maven build directory for a specific Maven module.
	 *
	 * @param project The Maven module to resolve the build directory for.
	 * @return The path to the build directory of the Maven module.
	 */
	public static Path resolveProjectBuildPath(final MavenProject project) {
		final Path projectBasePath = project.getBasedir().toPath();
		final Path projectBuildPath = projectBasePath.resolve(project.getBuild().getDirectory());

		return projectBuildPath;
	}

}
