package org.jenkinsci.plugins.deploy.weblogic;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicDeployment;
import org.jenkinsci.plugins.deploy.weblogic.properties.WebLogicDeploymentPluginConstantes;
import org.jenkinsci.plugins.deploy.weblogic.task.TaskStatusUnSuccesfullPredicate;


public class PrintingWebLogicDeploymentLastSuccessResultAction implements Action  {

	private WebLogicDeployment lastDeploymentSucessfull;
	
	private static transient final String lastSuccessIconFileName = WebLogicDeploymentPluginConstantes.PLUGIN_RESOURCES_PATH + "/icons/48x48/BEA.png";
	
	
	/**
	 * 
	 */
	public PrintingWebLogicDeploymentLastSuccessResultAction(){
		super();
	}
	
	/**
	 * 
	 * @param project
	 */
	public PrintingWebLogicDeploymentLastSuccessResultAction(AbstractProject<?, ?> project){
		super();
		
		List<AbstractBuild<?, ?>> builds = (List<AbstractBuild<?, ?>>) project.getBuilds();

        if(builds == null) { return;}

		for (AbstractBuild<?, ?> build : builds) {
			WatchingWeblogicDeploymentAction action = build.getAction(WatchingWeblogicDeploymentAction.class);
				if(action != null && ! CollectionUtils.exists(action.getResults(), new TaskStatusUnSuccesfullPredicate())){
					lastDeploymentSucessfull = new WebLogicDeployment(build.getNumber(), build.getTime(), null);
					break;
				}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.model.Action#getDisplayName()
	 */
	public String getDisplayName() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see hudson.model.Action#getIconFileName()
	 */
	public String getIconFileName() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see hudson.model.Action#getUrlName()
	 */
	public String getUrlName() {
		return null;
	}
	
	/**
	 * 
	 * @return
	 */
	public WebLogicDeployment getLastDeploymentSucessfull() {
		return lastDeploymentSucessfull;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean hasAtLeastOneDeploymentSuccessfull(){
		return lastDeploymentSucessfull != null;
	}

    public String getLastSuccessIconFileName() {
        return lastSuccessIconFileName;
    }
}
