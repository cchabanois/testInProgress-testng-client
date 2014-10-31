package org.imaginea.jenkins.plugins.testinprogress.testng;

import java.io.IOException;

/**
 * A unchecked version of IOException. 
 * 
 * @author Cedric Chabanois (github id:cchabanois)
 *
 */
public class RuntimeIOException extends RuntimeException {

	private static final long serialVersionUID = -59616916435904155L;

	public RuntimeIOException(IOException exception) {
		super(exception);
	}
	
}
