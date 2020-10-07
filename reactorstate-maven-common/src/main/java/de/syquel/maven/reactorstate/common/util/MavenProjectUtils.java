package de.syquel.maven.reactorstate.common.util;

import java.nio.file.Path;

import org.apache.maven.project.MavenProject;

public final class MavenProjectUtils {

	private MavenProjectUtils() {}

	public static Path resolveProjectBuildPath(final MavenProject project) {
		final Path projectBasePath = project.getBasedir().toPath();
		final Path projectBuildPath = projectBasePath.resolve(project.getBuild().getDirectory());

		return projectBuildPath;
	}

}
