/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deploy.weblogic.util.VarUtils;

/**
 * @author rchaumie
 *
 */
public class FreeStyleJobArtifactSelectorImpl implements ArtifactSelector {
	
	/*
	 * (non-Javadoc)
	 * @see org.jenkinsci.plugins.deploy.weblogic.ArtifactSelector#selectArtifactRecorded(hudson.model.AbstractBuild, hudson.model.BuildListener, java.lang.String, java.lang.String)
	 */
	public FilePath selectArtifactRecorded(AbstractBuild<?, ?> build, BuildListener listener, String filteredResource, String baseDirectory) throws IOException, InterruptedException  {
		
		FilePath selectedArtifact = null;
		
        listener.getLogger().println("[WeblogicDeploymentPlugin] - Retrieving artifacts recorded [filtered resources on "+filteredResource+"]...");
        List<FilePath> artifactsRecorded = new ArrayList<FilePath>();
        
        // On parcours le workspace si aucun repertoire de base specifie a la recherche d'un fichier correspondant a l'expression reguliere
        Collection<?> filesToCheck = CollectionUtils.EMPTY_COLLECTION;
        String baseDirName = "";
        if(StringUtils.isBlank(baseDirectory)){
	        FilePath workspace = build.getWorkspace();
            baseDirName = workspace.getName();
            filesToCheck = FileUtils.listFiles(new File(workspace.toURI()), null, true);
        } else {

            //Recuperation des variables
            EnvVars vars = VarUtils.getEnvVars(build, listener);
            String resolvedBaseDirectory = vars.expand(baseDirectory);

            File baseDir = new File(resolvedBaseDirectory);

        	//si un repertoire est specifie mais qu'il est inacessible ou invalide on renvoit une erreur
            if(! baseDir.exists() || ! baseDir.isDirectory() || ! baseDir.canRead()){
            	listener.getLogger().println("[WeblogicDeploymentPlugin] - the base directory specified ["+resolvedBaseDirectory+"] is invalid (doesn't exists or is not a directory or has insufficient privilege). Please check the job configuration");
            	throw new RuntimeException("The base directory specified ["+resolvedBaseDirectory+"] is invalid (doesn't exists or is not a directory or has insufficient privilege)");
            }

            baseDirName = baseDir.getName();
            filesToCheck = FileUtils.listFiles(baseDir, null, true);
         }

        listener.getLogger().println("[WeblogicDeploymentPlugin] - "+filesToCheck.size() +" files found under "+baseDirName);
        for(File file : (Collection<File>) filesToCheck){
            if(! file.isDirectory() && Pattern.matches(filteredResource, file.getName())){
                listener.getLogger().println("[WeblogicDeploymentPlugin] - the following resource recorded "+file.getAbsolutePath()+" is eligible.");
                artifactsRecorded.add(new FilePath(file));
            } else {
                listener.getLogger().println("[WeblogicDeploymentPlugin] - the following resource ['"+file.getName()+"'] doesn't match "+filteredResource);
            }
        }
        
        if(artifactsRecorded.size() < 1){
        	throw new RuntimeException("No artifact to deploy ["+filteredResource+"] found.");
        }
        
        if(artifactsRecorded.size() > 1){
        	listener.getLogger().println("[WeblogicDeploymentPlugin] - More than 1 artifact found : The first one "+artifactsRecorded.get(0)+ " will be deployed!!!");
        }
        
        selectedArtifact = artifactsRecorded.get(0);
        
    	
		
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
		return "FreeStyleProject";
	}

}
