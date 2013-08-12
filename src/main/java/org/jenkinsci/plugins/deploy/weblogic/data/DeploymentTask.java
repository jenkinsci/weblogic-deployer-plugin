/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.data;

import hudson.model.AbstractDescribableImpl;
import hudson.model.JDK;

import java.io.Serializable;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Raphael
 *
 */
public class DeploymentTask  extends AbstractDescribableImpl<DeploymentTask> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3924420945973321189L;

	/**
	 * Identify the task
	 */
	private String id;
	
	/**
     * Identifies {@link WeblogicEnvironment} to be used.
     */
	private String weblogicEnvironmentTargetedName;
	
	/**
	 * Le nom de deploiement. Si null on n'utilisera le nom de l'artifact
	 */
	private String deploymentName;
	
	/**
	 * Les targets de deploiement. Par defaut AdminServer
	 */
	private String deploymentTargets = "AdminServer";
	
	/**
	 * L'artifact est une librairie
	 */
	private boolean isLibrary;
	
	/**
	 * Regex permettant de filtrer la ressource a deployer si plusieurs ressources 
	 * correspondantes sont trouvees
	 */
	private String builtResourceRegexToDeploy;
	
	/**
	 * Repertoire parent dans lequel la ressource à deployer peut etre localisée.
	 * Utilise principalement pour les job non maven dans le cas où la ressource
	 * à deployer ne se trouve pas dans le workspace
	 */
	private String baseResourcesGeneratedDirectory;
	
	/**
	 * The task name (optional)
	 */
	private String taskName;
	
	/**
	 * The JDK to use
	 */
	private JDK jdk;
	
	private WebLogicStageMode stageMode;
	
	/**
	 * The command line to execute
	 */
	private String commandLine;
	
	/**
	 * Name of the deployment plan to use when deploying the resource
	 */
	private final String deploymentPlan;

	/**
	 * Invoque lors uniquement lors de la sauvegarde des données
	 * @param id
	 * @param taskName
	 * @param weblogicEnvironmentTargetedName
	 * @param deploymentName
	 * @param deploymentTargets
	 * @param isLibrary
	 * @param builtResourceRegexToDeploy
	 * @param baseResourcesGeneratedDirectory
	 * @param jdkTool
	 * @param nestedObject
	 * @param deploymentPlan
	 */
	@DataBoundConstructor
	public DeploymentTask(String id, String taskName, String weblogicEnvironmentTargetedName, String deploymentName, 
  		String deploymentTargets, boolean isLibrary, String builtResourceRegexToDeploy, String baseResourcesGeneratedDirectory, String jdkName, String jdkHome, 
  		WebLogicStageMode stageMode,
  		String commandLine, String deploymentPlan) {
		if (id == null) {
			this.id = RandomStringUtils.randomAlphanumeric(10);
		} else {
			this.id = id;
		}
		this.taskName = taskName;
		this.weblogicEnvironmentTargetedName = weblogicEnvironmentTargetedName;
		this.deploymentName = deploymentName;
		this.deploymentTargets = deploymentTargets;
		this.isLibrary = isLibrary;
		this.builtResourceRegexToDeploy = builtResourceRegexToDeploy;
		this.baseResourcesGeneratedDirectory = baseResourcesGeneratedDirectory;
		if(StringUtils.isNotBlank(jdkName)){
			this.jdk = new JDK(jdkName, jdkHome);
		}
		this.stageMode = stageMode;
		this.commandLine = commandLine;
      	this.deploymentPlan = deploymentPlan;
	}
	
	
	
	/* (non-Javadoc)
	 * @see hudson.model.AbstractDescribableImpl#getDescriptor()
	 */
	@Override
	public DeploymentTaskDescriptor getDescriptor() {
		return (DeploymentTaskDescriptor) super.getDescriptor();
	}

	/**
	 * 
	 * @return
	 */
	public String getWeblogicEnvironmentTargetedName() {
		return weblogicEnvironmentTargetedName;
	}
	
	/**
	 * 	
	 * @return
	 */
	public String getDeploymentName() {
		return deploymentName;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getDeploymentTargets() {
		return deploymentTargets;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean getIsLibrary() {
		return isLibrary;
	}

	/**
	 * @return the builtResourceRegexToDeploy
	 */
	public String getBuiltResourceRegexToDeploy() {
		return builtResourceRegexToDeploy;
	}

	/**
	 * @param builtResourceRegexToDeploy the builtResourceRegexToDeploy to set
	 */
	public void setBuiltResourceRegexToDeploy(String builtResourceRegexToDeploy) {
		this.builtResourceRegexToDeploy = builtResourceRegexToDeploy;
	}
	
	/**
	 * @return the baseResourcesGeneratedDirectory
	 */
	public String getBaseResourcesGeneratedDirectory() {
		return baseResourcesGeneratedDirectory;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the taskName
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * @return the jdk
	 */
	public JDK getJdk() {
		return jdk;
	}

	/**
	 * @return the stageMode
	 */
	public WebLogicStageMode getStageMode() {
		return stageMode;
	}

	/**
	 * 
	 * @return
	 */
	public String getCommandLine() {
		return commandLine;
	}
	
	/**
	 * @return the deploymentPlan
	 */
	public String getDeploymentPlan() {
		return deploymentPlan;
	}
}
