/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.exception;

import org.jenkinsci.plugins.deploy.weblogic.data.DeploymentTaskResult;

/**
 * @author rchaumie
 *
 */
public class DeploymentTaskException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1969850732609489498L;

	private DeploymentTaskResult result;
	
	/**
	 * 
	 */
	public DeploymentTaskException(){
		super();
	}
	
	/**
	 * 
	 * @param message
	 * @param cause
	 */
	public DeploymentTaskException(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * 
	 * @param message
	 */
	public DeploymentTaskException(String message) {
		super(message);
	}
	
	/**
	 * 
	 * @param cause
	 */
	public DeploymentTaskException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * 
	 * @param result
	 */
	public DeploymentTaskException(DeploymentTaskResult result) {
		super();
		this.result = result;
	}

	/**
	 * @return the result
	 */
	public DeploymentTaskResult getResult() {
		return result;
	}

	/**
	 * @param result the result to set
	 */
	public void setResult(DeploymentTaskResult result) {
		this.result = result;
	}

}
