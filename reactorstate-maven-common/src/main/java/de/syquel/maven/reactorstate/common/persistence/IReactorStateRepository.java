package de.syquel.maven.reactorstate.common.persistence;

import java.io.IOException;

import org.apache.maven.project.MavenProject;

import de.syquel.maven.reactorstate.common.MavenProjectState;

public interface IReactorStateRepository {

	MavenProjectState read(MavenProject mavenProject) throws IOException;
	void save(MavenProjectState mavenProjectState) throws IOException;
	void delete(MavenProject mavenProject) throws IOException;

}
