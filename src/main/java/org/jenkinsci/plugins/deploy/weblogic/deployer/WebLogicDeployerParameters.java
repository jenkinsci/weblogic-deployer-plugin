/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.deployer;

import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicStageMode;
import org.jenkinsci.plugins.deploy.weblogic.data.WeblogicEnvironment;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.JDK;


/**
 * @author rchaumie
 *
 */
public class WebLogicDeployerParameters {

	
	private AbstractBuild<?, ?> build;
	
	private Launcher launcher;
	
	private BuildListener listener;
	
	private JDK usedJdk;
	
	private String deploymentName;
	
	private boolean isLibrary;
	
	private String deploymentTargets;
	
	private WeblogicEnvironment environment;
	
	private String artifactName;
	
	private String source;
	
	private WebLogicCommand command;
	
	private boolean silentMode;
	
	private String javaOpts;
	
	private String classpath;
	
	private WebLogicStageMode stageMode;
	
	private String deploymentPlan;
	public WebLogicDeployerParameters(){}
	
	
	/**
	 * @param build
	 * @param launcher
	 * @param listener
	 * @param usedJdk
	 * @param javaOpts
	 * @param classpath
	 */
	public WebLogicDeployerParameters(AbstractBuild<?, ?> build,
			Launcher launcher, BuildListener listener, JDK usedJdk,
			String javaOpts, String classpath) {
		this.build = build;
		this.launcher = launcher;
		this.listener = listener;
		this.usedJdk = usedJdk;
		this.javaOpts = javaOpts;
		this.classpath = classpath;
	}


	/**
	 * 	
	 * @param build
	 * @param launcher
	 * @param listener
	 * @param usedJdk
	 * @param deploymentName
	 * @param isLibrary
	 * @param deploymentTargets
	 * @param environment
	 * @param artifactName
	 * @param source
	 * @param command
	 * @param silentMode
	 */
	public WebLogicDeployerParameters(AbstractBuild<?, ?> build,
			Launcher launcher, BuildListener listener, JDK usedJdk,
			String deploymentName, boolean isLibrary, String deploymentTargets,
			WeblogicEnvironment environment, String artifactName, String source,
			WebLogicCommand command, boolean silentMode,String javaOpts, String classpath,
			WebLogicStageMode stageMode, String deploymentPlan) {
		super();
		this.build = build;
		this.launcher = launcher;
		this.listener = listener;
		this.usedJdk = usedJdk;
		this.deploymentName = deploymentName;
		this.isLibrary = isLibrary;
		this.deploymentTargets = deploymentTargets;
		this.environment = environment;
		this.artifactName = artifactName;
		this.source = source;
		this.command = command;
		this.silentMode = silentMode;
		this.javaOpts = javaOpts;
		this.classpath = classpath;
		this.stageMode = stageMode;
		this.deploymentPlan = deploymentPlan;
	}

	/**
	 * @return the build
	 */
	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	/**
	 * @param build the build to set
	 */
	public void setBuild(AbstractBuild<?, ?> build) {
		this.build = build;
	}

	/**
	 * @return the launcher
	 */
	public Launcher getLauncher() {
		return launcher;
	}

	/**
	 * @param launcher the launcher to set
	 */
	public void setLauncher(Launcher launcher) {
		this.launcher = launcher;
	}

	/**
	 * @return the listener
	 */
	public BuildListener getListener() {
		return listener;
	}

	/**
	 * @param listener the listener to set
	 */
	public void setListener(BuildListener listener) {
		this.listener = listener;
	}

	/**
	 * @return the usedJdk
	 */
	public JDK getUsedJdk() {
		return usedJdk;
	}

	/**
	 * @param usedJdk the usedJdk to set
	 */
	public void setUsedJdk(JDK usedJdk) {
		this.usedJdk = usedJdk;
	}

	/**
	 * @return the deploymentName
	 */
	public String getDeploymentName() {
		return deploymentName;
	}

	/**
	 * @param deploymentName the deploymentName to set
	 */
	public void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}

	/**
	 * @return the isLibrary
	 */
	public boolean isLibrary() {
		return isLibrary;
	}

	/**
	 * @param isLibrary the isLibrary to set
	 */
	public void setLibrary(boolean isLibrary) {
		this.isLibrary = isLibrary;
	}

	/**
	 * @return the deploymentTargets
	 */
	public String getDeploymentTargets() {
		return deploymentTargets;
	}

	/**
	 * @param deploymentTargets the deploymentTargets to set
	 */
	public void setDeploymentTargets(String deploymentTargets) {
		this.deploymentTargets = deploymentTargets;
	}

	/**
	 * @return the environment
	 */
	public WeblogicEnvironment getEnvironment() {
		return environment;
	}

	/**
	 * @param environment the environment to set
	 */
	public void setEnvironment(WeblogicEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * @return the artifactName
	 */
	public String getArtifactName() {
		return artifactName;
	}

	/**
	 * @param artifactId the artifactName to set
	 */
	public void setArtifactName(String artifactName) {
		this.artifactName = artifactName;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the command
	 */
	public WebLogicCommand getCommand() {
		return command;
	}

	/**
	 * @param command the command to set
	 */
	public void setCommand(WebLogicCommand command) {
		this.command = command;
	}

	/**
	 * @return the silentMode
	 */
	public boolean isSilentMode() {
		return silentMode;
	}

	/**
	 * @param silentMode the silentMode to set
	 */
	public void setSilentMode(boolean silentMode) {
		this.silentMode = silentMode;
	}

	/**
	 * @return the javaOpts
	 */
	public String getJavaOpts() {
		return javaOpts;
	}

	/**
	 * @param javaOpts the javaOpts to set
	 */
	public void setJavaOpts(String javaOpts) {
		this.javaOpts = javaOpts;
	}

	/**
	 * @return the classpath
	 */
	public String getClasspath() {
		return classpath;
	}

	/**
	 * @param classpath the classpath to set
	 */
	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}

	/**
	 * @return the stageMode
	 */
	public WebLogicStageMode getStageMode() {
		return stageMode;
	}

	/**
	 * @param stageMode the stageMode to set
	 */
	public void setStageMode(WebLogicStageMode stageMode) {
		this.stageMode = stageMode;
	}
	
	public String getDeploymentPlan() {
		return deploymentPlan;
	}

	public void setDeploymentPlan(String deploymentPlan) {
		this.deploymentPlan = deploymentPlan;
	}
}
