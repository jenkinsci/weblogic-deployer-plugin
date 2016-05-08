/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.task;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.JDK;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deploy.weblogic.ArtifactSelector;
import org.jenkinsci.plugins.deploy.weblogic.FreeStyleJobArtifactSelectorImpl;
import org.jenkinsci.plugins.deploy.weblogic.WeblogicDeploymentPlugin.WeblogicDeploymentPluginDescriptor;
import org.jenkinsci.plugins.deploy.weblogic.WeblogicDeploymentPluginLog;
import org.jenkinsci.plugins.deploy.weblogic.data.DeploymentTask;
import org.jenkinsci.plugins.deploy.weblogic.data.DeploymentTaskResult;
import org.jenkinsci.plugins.deploy.weblogic.data.TransfertConfiguration;
import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicDeploymentStatus;
import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicPreRequisteStatus;
import org.jenkinsci.plugins.deploy.weblogic.data.WeblogicEnvironment;
import org.jenkinsci.plugins.deploy.weblogic.deployer.WebLogicCommand;
import org.jenkinsci.plugins.deploy.weblogic.deployer.WebLogicDeployer;
import org.jenkinsci.plugins.deploy.weblogic.deployer.WebLogicDeployerParameters;
import org.jenkinsci.plugins.deploy.weblogic.deployer.WebLogicDeployerTokenResolver;
import org.jenkinsci.plugins.deploy.weblogic.exception.DeploymentTaskException;
import org.jenkinsci.plugins.deploy.weblogic.exception.RequiredJDKNotFoundException;
import org.jenkinsci.plugins.deploy.weblogic.jdk.JdkToolService;
import org.jenkinsci.plugins.deploy.weblogic.properties.WebLogicDeploymentPluginConstantes;
import org.jenkinsci.plugins.deploy.weblogic.util.FTPUtils;
import org.jenkinsci.plugins.deploy.weblogic.util.ParameterValueResolver;
import org.jenkinsci.plugins.deploy.weblogic.util.VarUtils;

import com.google.inject.Inject;

/**
 * @author Raphael
 *
 */
@Extension
public class DeploymentTaskServiceImpl implements DeploymentTaskService {

	@Inject
	private WeblogicDeploymentPluginDescriptor descriptor;
	
	@Inject
	private WebLogicDeployerTokenResolver tokenResolver;
	
	/**
	 * 
	 */
	public DeploymentTaskServiceImpl() {}

	/*
	 * (non-Javadoc)
	 * @see org.jenkinsci.plugins.deploy.weblogic.task.DeploymentTaskService#perform(org.jenkinsci.plugins.deploy.weblogic.data.DeploymentTask, hudson.model.JDK, hudson.model.AbstractBuild, hudson.model.BuildListener, hudson.Launcher)
	 */
	public DeploymentTaskResult perform(DeploymentTask task, String globalJdk, AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher) throws DeploymentTaskException {
		
		//Recuperation des variables
		EnvVars envVars = VarUtils.getEnvVars(build, listener);

        // Verification si la tache est a ignorer du fait de la presence d'une variable de la forme ${DEPLOY_<task_name>_SKIP}
        String taskEnvVarSkippedFlag = String.format("DEPLOY_%s_SKIP",task.getTaskName()).toUpperCase();
        if((envVars != null) && BooleanUtils.toBoolean(envVars.get(taskEnvVarSkippedFlag))){
            listener.getLogger().println("[WeblogicDeploymentPlugin] - The variable '"+taskEnvVarSkippedFlag+"' has been set to true. The following deployment task "+task.getTaskName()+" is currently disabled.");
            return new DeploymentTaskResult(WebLogicPreRequisteStatus.OK, WebLogicDeploymentStatus.DISABLED, convertParameters(task, envVars), null);
        }

		// Recuperation du JDK
		// The default JDK
		JDK selectedJdk = null;
		try {
			
			if(task.getJdk() != null && task.getJdk().getExists()){
				selectedJdk = task.getJdk(); 
			} else {
				selectedJdk = JdkToolService.getJDKByName(globalJdk);
			}
			
			// Check exists
			if(selectedJdk == null || ! selectedJdk.getExists()){
				String execu = selectedJdk != null ? selectedJdk.getHome(): "";
				throw new RequiredJDKNotFoundException("Unable to find PATH to the JDK's executable ["+execu+"]");
			}
			
			// Check version
			JdkToolService.checkJdkVersion(selectedJdk, listener.getLogger());
			
		} catch (RequiredJDKNotFoundException rjnfe) {
			listener.getLogger().println("[WeblogicDeploymentPlugin] - No JDK found. The plugin execution is disabled.");
			throw new DeploymentTaskException(new DeploymentTaskResult(WebLogicPreRequisteStatus.OK, WebLogicDeploymentStatus.ABORTED, convertParameters(task, envVars), null));
		}
		listener.getLogger().println("[WeblogicDeploymentPlugin] - The JDK " +selectedJdk.getHome() + " will be used.");
		
		// write out the log
        OutputStream deploymentLogOut;
		try {
			deploymentLogOut = new FileOutputStream(WeblogicDeploymentPluginLog.getDeploymentLogFile(build, task.getId()));
		} catch (FileNotFoundException fnfe) {
			listener.error("[WeblogicDeploymentPlugin] - Failed to find deployment log file : " + fnfe.getMessage());
            throw new DeploymentTaskException(new DeploymentTaskResult(WebLogicPreRequisteStatus.OK, WebLogicDeploymentStatus.ABORTED, convertParameters(task, envVars), null));
		}
		
		// Identification de la ressource a deployer
        FilePath archivedArtifact = null;
		String artifactName = null;
		String fullArtifactFinalName = null;
		try {
			// En fonction du type de projet on utilise pas le meme selecteur
			Class<? extends AbstractProject> jobType = build.getProject().getClass();

			ArtifactSelector artifactSelector = new FreeStyleJobArtifactSelectorImpl();

			FilePath selectedArtifact = artifactSelector.selectArtifactRecorded(build, listener, task.getBuiltResourceRegexToDeploy(), task.getBaseResourcesGeneratedDirectory());
			// Ne devrait pas etre le nom mais la valeur finale du artifact.name (sans l'extension)
			artifactName = StringUtils.substringBeforeLast(selectedArtifact.getBaseName(), ".");
			archivedArtifact = selectedArtifact;
			fullArtifactFinalName = selectedArtifact.getName();
		} catch (Throwable e) {
			e.printStackTrace(listener.getLogger());
            listener.error("[WeblogicDeploymentPlugin] - Failed to get artifact from archive directory.");
            throw new DeploymentTaskException(new DeploymentTaskResult(WebLogicPreRequisteStatus.OK, WebLogicDeploymentStatus.ABORTED, convertParameters(task, envVars), null));
        } finally {
            IOUtils.closeQuietly(deploymentLogOut);
        }
		
		//Deploiement
		//Recuperation du parametrage
		WeblogicEnvironment weblogicEnvironmentTargeted = null;
		try {
            
			//Gestion de liste d'exclusions
			Pattern pattern = Pattern.compile(getDescriptor().getExcludedArtifactNamePattern());
			Matcher matcher = pattern.matcher(artifactName);
			if(matcher.matches()){
				listener.error("[WeblogicDeploymentPlugin] - The artifact Name " +artifactName+ " is excluded from deployment (see exclusion list).");
				throw new DeploymentTaskException(new DeploymentTaskResult(WebLogicPreRequisteStatus.OK, WebLogicDeploymentStatus.ABORTED, convertParameters(task, envVars), fullArtifactFinalName));
			}
			
			//Recuperation du parametrage
			weblogicEnvironmentTargeted = getWeblogicEnvironmentTargeted(task.getWeblogicEnvironmentTargetedName(), listener);
			
			if(weblogicEnvironmentTargeted == null){
				listener.error("[WeblogicDeploymentPlugin] - WebLogic environment Name " +task.getWeblogicEnvironmentTargetedName()+ " not found in the list. Please check the configuration file.");
				throw new DeploymentTaskException(new DeploymentTaskResult(WebLogicPreRequisteStatus.OK, WebLogicDeploymentStatus.ABORTED, convertParameters(task, envVars), fullArtifactFinalName));
			}
			listener.getLogger().println("[WeblogicDeploymentPlugin] - Deploying the artifact to the following target : (name="+task.getWeblogicEnvironmentTargetedName()+") (host=" + weblogicEnvironmentTargeted.getHost() + ") (port=" +weblogicEnvironmentTargeted.getPort()+ ")");
			
			
			if(StringUtils.isBlank(task.getCommandLine())){
				// undeploy task
				undeploy(task, build, listener, launcher, weblogicEnvironmentTargeted, selectedJdk, artifactName, deploymentLogOut, envVars);
		        
		        //Execution commande deploy
				deploy(task, build, listener, launcher, weblogicEnvironmentTargeted, 
						selectedJdk, artifactName, deploymentLogOut, archivedArtifact, fullArtifactFinalName, envVars);
			} else {
				// Execution commande specifique
				customize(task, build, listener, launcher, weblogicEnvironmentTargeted, selectedJdk, artifactName, deploymentLogOut, archivedArtifact, fullArtifactFinalName, envVars);
			}
			
        } catch (Throwable e) {
        	e.printStackTrace(listener.getLogger());
        	listener.error("[WeblogicDeploymentPlugin] - Failed to deploy.");
            throw new DeploymentTaskException(new DeploymentTaskResult(WebLogicPreRequisteStatus.OK, WebLogicDeploymentStatus.FAILED, convertParameters(task, envVars), fullArtifactFinalName));
        } finally {
        	IOUtils.closeQuietly(deploymentLogOut);
        }
		
		return new DeploymentTaskResult(WebLogicPreRequisteStatus.OK, WebLogicDeploymentStatus.SUCCEEDED, convertParameters(task, envVars), fullArtifactFinalName);
	}

    /**
     *
     * @param task
     * @param build
     * @param listener
     * @param launcher
     * @param weblogicEnvironmentTargeted
     * @param selectedJdk
     * @param artifactName
     * @param deploymentLogOut
     * @param archivedArtifact
     * @param fullArtifactFinalName
     * @param envVars
     * @throws IOException
     * @throws InterruptedException
     */
	private void deploy(DeploymentTask task, AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher, 
			WeblogicEnvironment weblogicEnvironmentTargeted, JDK selectedJdk, String artifactName, OutputStream deploymentLogOut,
			FilePath archivedArtifact, String fullArtifactFinalName, EnvVars envVars)  throws IOException, InterruptedException {
		
		String sourceFile = null;
		String remoteFilePath = null;
		
		//Transfert FTP pour les librairies (contrainte weblogic)
        if(task.getIsLibrary()){
        	//Par defaut si ftp n'est pas renseigne on prend le host
        	String ftpHost = StringUtils.isBlank(weblogicEnvironmentTargeted.getFtpHost()) ? weblogicEnvironmentTargeted.getHost() : weblogicEnvironmentTargeted.getFtpHost();
        	// path to remote resource
            remoteFilePath = weblogicEnvironmentTargeted.getRemoteDir() + "/" + fullArtifactFinalName;
            String localFilePath = archivedArtifact.getRemote();
            listener.getLogger().println("[WeblogicDeploymentPlugin] - TRANSFERING LIBRARY : (local=" +fullArtifactFinalName+ ") (remote=" + remoteFilePath + ") to (ftp=" +ftpHost + "@" +weblogicEnvironmentTargeted.getFtpUser()+ ") ...");
            FTPUtils.transfertFile(new TransfertConfiguration(ftpHost, weblogicEnvironmentTargeted.getFtpUser(), weblogicEnvironmentTargeted.getFtpPassowrd(), localFilePath, remoteFilePath),listener.getLogger());
        	listener.getLogger().println("[WeblogicDeploymentPlugin] - LIBRARY TRANSFERED SUCCESSFULLY.");
        }
        
		//source file correspond au remote file pour les librairies
        if(task.getIsLibrary()){
        	sourceFile = remoteFilePath;
        } else {
        	sourceFile = archivedArtifact.getRemote();
        }
        
        WebLogicDeployerParameters deployWebLogicDeployerParameters = new WebLogicDeployerParameters(
        		build,launcher,listener, selectedJdk, task.getDeploymentName(), task.getIsLibrary(), task.getDeploymentTargets(),
        		weblogicEnvironmentTargeted, artifactName, sourceFile, WebLogicCommand.DEPLOY, false,
        		getDescriptor().getJavaOpts(),getDescriptor().getExtraClasspath(), task.getStageMode(), task.getDeploymentPlan());
        String[] deployCommand = WebLogicDeployer.getWebLogicCommandLine(deployWebLogicDeployerParameters, envVars);
        listener.getLogger().println("[WeblogicDeploymentPlugin] - DEPLOYING ARTIFACT...");
        deploymentLogOut.write("------------------------------------  ARTIFACT DEPLOYMENT ------------------------------------------------\r\n".getBytes());
        final Proc deploymentProc = launcher.launch().cmds(deployCommand).envs(envVars).stdout(deploymentLogOut).start();
        int exitStatus = deploymentProc.join();
        if(exitStatus != 0){
//        	listener.error("[WeblogicDeploymentPlugin] - Command " +StringUtils.join(deployCommand, '|')+" completed abnormally (exit code = "+exitStatus+")");
        	throw new RuntimeException("task completed abnormally (exit code = "+exitStatus+")");
        }
        listener.getLogger().println("[WeblogicDeploymentPlugin] - ARTIFACT DEPLOYED SUCCESSFULLY.");
	}
	
	/**
	 * 
	 * @param task
	 * @param build
	 * @param listener
	 * @param launcher
	 * @param weblogicEnvironmentTargeted
	 * @param selectedJdk
	 * @param artifactName
	 * @param deploymentLogOut
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void undeploy(DeploymentTask task, AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher, 
			WeblogicEnvironment weblogicEnvironmentTargeted,
			JDK selectedJdk, String artifactName, OutputStream deploymentLogOut, EnvVars envVars) throws IOException, InterruptedException {
		//Execution commande undeploy
		WebLogicDeployerParameters undeployWebLogicDeployerParameters = new WebLogicDeployerParameters(
				build, launcher, listener, selectedJdk, task.getDeploymentName(), task.getIsLibrary(), task.getDeploymentTargets(),
				weblogicEnvironmentTargeted, artifactName, null, WebLogicCommand.UNDEPLOY, true,
				getDescriptor().getJavaOpts(), getDescriptor().getExtraClasspath(), task.getStageMode(), null);
		String[] undeployCommand = WebLogicDeployer.getWebLogicCommandLine(undeployWebLogicDeployerParameters, envVars);
        
        deploymentLogOut.write("------------------------------------  ARTIFACT UNDEPLOYMENT ------------------------------------------------\r\n".getBytes());
        listener.getLogger().println("[WeblogicDeploymentPlugin] - UNDEPLOYING ARTIFACT...");
        final Proc undeploymentProc = launcher.launch().cmds(undeployCommand).envs(envVars).stdout(deploymentLogOut).start();
        undeploymentProc.join();
        listener.getLogger().println("[WeblogicDeploymentPlugin] - ARTIFACT UNDEPLOYED SUCCESSFULLY.");
	}
	
	/**
	 * 
	 * @param task
	 * @param build
	 * @param listener
	 * @param launcher
	 * @param weblogicEnvironmentTargeted
	 * @param selectedJdk
	 * @param artifactName
	 * @param deploymentLogOut
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void customize(DeploymentTask task, AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher, 
			WeblogicEnvironment weblogicEnvironmentTargeted,
			JDK selectedJdk, String artifactName, OutputStream deploymentLogOut,
			FilePath archivedArtifact, String fullArtifactFinalName, EnvVars envVars) throws IOException, InterruptedException {
		
		String sourceFile = null;
		String remoteFilePath = null;
		
		//Transfert FTP pour les librairies (contrainte weblogic)
        if(task.getIsLibrary()){
        	//Par defaut si ftp n'est pas renseigne on prend le host
        	String ftpHost = StringUtils.isBlank(weblogicEnvironmentTargeted.getFtpHost()) ? weblogicEnvironmentTargeted.getHost() : weblogicEnvironmentTargeted.getFtpHost();
        	// path to remote resource
            remoteFilePath = weblogicEnvironmentTargeted.getRemoteDir() + "/" + fullArtifactFinalName;
            String localFilePath = archivedArtifact.getRemote();
            listener.getLogger().println("[WeblogicDeploymentPlugin] - TRANSFERING LIBRARY : (local=" +fullArtifactFinalName+ ") (remote=" + remoteFilePath + ") to (ftp=" +ftpHost + "@" +weblogicEnvironmentTargeted.getFtpUser()+ ") ...");
            FTPUtils.transfertFile(new TransfertConfiguration(ftpHost, weblogicEnvironmentTargeted.getFtpUser(), weblogicEnvironmentTargeted.getFtpPassowrd(), localFilePath, remoteFilePath),listener.getLogger());
        	listener.getLogger().println("[WeblogicDeploymentPlugin] - LIBRARY TRANSFERED SUCCESSFULLY.");
        }
        
		//source file correspond au remote file pour les librairies
        if(task.getIsLibrary()){
        	sourceFile = remoteFilePath;
        } else {
        	sourceFile = archivedArtifact.getRemote();
        }
		
		WebLogicDeployerParameters executionDeployerParameters = new WebLogicDeployerParameters(
				build, launcher, listener, selectedJdk, task.getDeploymentName(), task.getIsLibrary(), task.getDeploymentTargets(),
				weblogicEnvironmentTargeted, artifactName, sourceFile, null, true,
				getDescriptor().getJavaOpts(), getDescriptor().getExtraClasspath(), task.getStageMode(), task.getDeploymentPlan());
		
		
		String[] commandLines = StringUtils.split(task.getCommandLine(), WebLogicDeploymentPluginConstantes.WL_DEPLOYMENT_CMD_LINE_SEPARATOR);
		
        for(String command: commandLines) {
        	
        	if(StringUtils.isBlank(command)){
        		continue;
        	}
        	
        	String newCommand = replaceTokens(StringUtils.trim(command), executionDeployerParameters);
        	String[] executionCommand = WebLogicDeployer.getWebLogicCommandLine(executionDeployerParameters, newCommand, envVars);
        	
        	deploymentLogOut.write("------------------------------------  TASK EXECUTION ------------------------------------------------\r\n".getBytes());
            listener.getLogger().println("[WeblogicDeploymentPlugin] - EXECUTING TASK ...");
            // FilePath ws = build.getWorkspace();
            //launcher.launch().cmds(cmd).envs(envVars).stdout(listener).pwd(ws).join();
	        final Proc executionProc = launcher.launch().cmds(executionCommand).envs(envVars).stdout(deploymentLogOut).start();
	        int exitStatus = executionProc.join();
	        if(exitStatus != 0){
//	        	listener.error("[WeblogicDeploymentPlugin] - Command " +StringUtils.join(executionCommand, '|')+" completed abnormally (exit code = "+exitStatus+")");
	        	throw new RuntimeException("task completed abnormally (exit code = "+exitStatus+")");
	        }
        }
        listener.getLogger().println("[WeblogicDeploymentPlugin] - ARTIFACT DEPLOYED SUCCESSFULLY.");
	}

	/**
	 * 
	 * @param task
	 * @param envVars
	 */
	private DeploymentTask convertParameters(DeploymentTask task, EnvVars envVars) {
		DeploymentTask taskHistory = new DeploymentTask(task);
		taskHistory.setDeploymentTargets(ParameterValueResolver.resolveEnvVar(task.getDeploymentTargets(), envVars));
//		task.getWeblogicEnvironmentTargetedName();
//		ParameterValueResolver.resolveEnvVar(parameter.getEnvironment().getHost(), envars);
//		ParameterValueResolver.resolveEnvVar(parameter.getEnvironment().getPort(), envars);
//		ParameterValueResolver.resolveEnvVar(parameter.getEnvironment().getLogin(), envars));
//		ParameterValueResolver.resolveEnvVar(parameter.getEnvironment().getPassword(), envars);
		return taskHistory;
	}
	
	/**
	 * 
	 * @param text
	 * @return
	 */
	private String replaceTokens(String text, WebLogicDeployerParameters parameters){
		Pattern pattern = Pattern.compile(WebLogicDeploymentPluginConstantes.COMMAND_LINE_TOKEN);
		StringBuilder output = new StringBuilder();
		Matcher tokenMatcher = pattern.matcher(text);
		int cursor = 0;
        while (tokenMatcher.find()) {
            int tokenStart = tokenMatcher.start();
            int tokenEnd = tokenMatcher.end();
            int keyStart = tokenMatcher.start(1);
            int keyEnd = tokenMatcher.end(1);

            output.append(text.substring(cursor, tokenStart));

            String token = text.substring(tokenStart, tokenEnd);
            String key = text.substring(keyStart, keyEnd);

            // find token value
            String value = findValue(key, parameters);
            if (value != null) {
                output.append(value);
            } else {
                output.append(token);
            }

            cursor = tokenEnd;
        }
        output.append(text.substring(cursor));

        return output.toString();
	}
	
	private String findValue(String key, WebLogicDeployerParameters parameters) {
		String result =  key;
		
		// Variables specifiques
		if(key.startsWith(WebLogicDeployerTokenResolver.WL_DEPLOYMENT_CMD_TOKEN_PREF)){
			return tokenResolver.resolveKey(key, parameters);
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param weblogicEnvironmentTargetedName
	 * @param listener
	 * @return
	 */
	private WeblogicEnvironment getWeblogicEnvironmentTargeted(String weblogicEnvironmentTargetedName,BuildListener listener) {
		
		WeblogicEnvironment out = null;
		WeblogicEnvironment[] targets = getDescriptor().getWeblogicEnvironments();
		
		if(targets == null){
			return out;
		}
		
		for (int i = 0;i< targets.length;i++) {
			if(weblogicEnvironmentTargetedName.equalsIgnoreCase(targets[i].getName())){
				out = targets[i];
				break;
			}
		}
		
		return out;
	}
	
	

	/**
	 * @return the descriptor
	 */
	public WeblogicDeploymentPluginDescriptor getDescriptor() {
		return descriptor;
	}
	
}
