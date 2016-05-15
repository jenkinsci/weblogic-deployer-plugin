/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic;

import hudson.FilePath;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.MavenAbstractArtifactRecord;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Run.Artifact;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author rchaumie
 *
 */
public class MavenJobArtifactSelectorImpl implements ArtifactSelector {
	
	private static transient final Pattern ARTIFACT_DEPLOYABLE_PATTERN = Pattern.compile(".*\\.(ear|war|jar)", Pattern.CASE_INSENSITIVE);
	
	/*
	 * (non-Javadoc)
	 * @see org.jenkinsci.plugins.deploy.weblogic.ArtifactSelector#selectArtifactRecorded(hudson.model.AbstractBuild, hudson.model.BuildListener, java.lang.String, java.lang.String)
	 */
	public FilePath selectArtifactRecorded(AbstractBuild<?, ?> build, BuildListener listener, String filteredResource, String baseDirectory) throws IOException, XmlPullParserException, InterruptedException  {
		
		FilePath selectedArtifact = null;
		
      	List<MavenAbstractArtifactRecord<MavenBuild>> mars = getActions( build, listener);
        if(mars==null || mars.isEmpty()) {
            listener.getLogger().println("[WeblogicDeploymentPlugin] - No artifacts are recorded. Is this a Maven project?");
        }
        
        listener.getLogger().println("[WeblogicDeploymentPlugin] - Retrieving artifacts recorded [filtered resources on "+filteredResource+"]...");
        List<Artifact> artifactsRecorded = new ArrayList<Artifact>();
        String patternToUse = StringUtils.defaultIfEmpty(filteredResource, ARTIFACT_DEPLOYABLE_PATTERN.pattern());
        for (MavenAbstractArtifactRecord<MavenBuild> mar : mars) {
        	listener.getLogger().println("[WeblogicDeploymentPlugin] - "+mar.getBuild().getArtifacts().size()+ " artifacts recorded in "+mar.getBuild().getArtifactsDir());
            for(Artifact artifact : mar.getBuild().getArtifacts()){
            	//Si une expression reguliere est fournie on filtre en priorite sur la regex
            	if(Pattern.matches(patternToUse, artifact.getFileName())){
            		listener.getLogger().println("[WeblogicDeploymentPlugin] - the following artifact recorded "+artifact.getFileName()+" is eligible.");
            	    artifactsRecorded.add(artifact);
            	} else {
            		listener.getLogger().println("[WeblogicDeploymentPlugin] - the following artifact "+artifact.getFileName()+" doesn't match "+filteredResource);
            	}
            }
        }
        
        if(artifactsRecorded.size() < 1){
        	throw new RuntimeException("No artifact to deploy ["+patternToUse+"] found.");
        }
        
        if(artifactsRecorded.size() > 1){
        	listener.getLogger().println("[WeblogicDeploymentPlugin] - More than 1 artifact found : The first one "+artifactsRecorded.get(0)+ " will be deployed!!!");
        }
        
        if(artifactsRecorded.get(0) != null && artifactsRecorded.get(0).getFile() != null){
        	selectedArtifact = new FilePath(artifactsRecorded.get(0).getFile());
        }
        
		// Erreur si l'artifact n'existe pas
		if(selectedArtifact == null){
			throw new RuntimeException("No artifact to deploy found.");
		}
        
		return selectedArtifact;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jenkinsci.plugins.deploy.weblogic.ArtifactSelector#getName()
	 */
	public String getName() {
		return "MavenModuleProject";
	}
	
	/**
	 * 
	 * @param build
	 * @param listener
	 * @return
	 */
	protected List<MavenAbstractArtifactRecord<MavenBuild>> getActions(AbstractBuild<?, ?> build, BuildListener listener) {
        List<MavenAbstractArtifactRecord<MavenBuild>> actions = new ArrayList<MavenAbstractArtifactRecord<MavenBuild>>();
        if (!(build instanceof MavenModuleSetBuild)) {
            return actions;
        }
        for (Entry<MavenModule, MavenBuild> e : ((MavenModuleSetBuild)build).getModuleLastBuilds().entrySet()) {
            MavenAbstractArtifactRecord<MavenBuild> a = e.getValue().getAction(MavenAbstractArtifactRecord.class);
            if (a == null) {
                listener.getLogger().println("[WeblogicDeploymentPlugin] - No artifacts are recorded for module" + e.getKey().getName() + ". Is this a Maven project?");
            } else {
                actions.add(a);    
            }
            
        }
        return actions;
    }

}
