package de.syquel.maven.reactorstate.common.persistence;

import java.io.IOException;

import org.apache.maven.project.MavenProject;

import de.syquel.maven.reactorstate.common.data.MavenProjectState;

/**
 * The persistence repository for Maven state information.
 */
public interface IReactorStateRepository {

	/**
	 * Reads the persisted state of a specific Maven module
	 *
	 * @param mavenProject The Maven module to read the persisted state for.
	 * @return The persisted Maven state or null if no state has been persisted yet.
	 * @throws IOException if an error occurred while reading the persisted state.
	 */
	MavenProjectState read(MavenProject mavenProject) throws IOException;

	/**
	 * Saves the current state of a specific Maven module.
	 *
	 * @param mavenProjectState The current state of a Maven module.
	 * @throws IOException if an error occurred while saving the state
	 */
	void save(MavenProjectState mavenProjectState) throws IOException;

	/**
	 * Deletes the persisted state of a specific Maven module.
	 *
	 * @param mavenProject The Maven module to delete the persisted state for.
	 * @throws IOException if an error occurred while deleting the persisted state.
	 */
	void delete(MavenProject mavenProject) throws IOException;

}
