/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.jdk;

import hudson.Launcher;
import hudson.model.JDK;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * @author Raphael
 *
 */
public class JdkToolService {

public static final String EXTERNAL_ENV_JDK = "environment";
	
	public static final String SYSTEM_JDK = "system";
	
	public static final String JAVA_VERSION_COMMAND_VERSION_LINE_REGEX = ".*\\r*\\n*(java version )(\")(.+)(\").*\\r*\\n*.*\\r*\\n*.*\\r*\\n*";
	
	
	static List<JDK> jdkToolAvailables = new ArrayList<JDK>();
	
	public static void loadJdkToolAvailables() {

		//Ajout de la java home system
		if (StringUtils.isNotBlank(SystemUtils.JAVA_HOME)){
			jdkToolAvailables.add(new JDK(SYSTEM_JDK, System.getProperty("java.home")));
		}
		//Ajout de la java home avec lequel est demarre Jenkins
		if(StringUtils.isNotBlank(System.getenv("JAVA_HOME"))){
			jdkToolAvailables.add(new JDK(EXTERNAL_ENV_JDK, System.getenv("JAVA_HOME")));
		}
		// Ajout de tous les jdk declares dans Jenkins
		jdkToolAvailables.addAll(Jenkins.getInstance().getJDKs());
		
	}
	
	/**
	 * 
	 * @return
	 */
	public static List<JDK> getJdkToolAvailables() {
		if(CollectionUtils.isEmpty(jdkToolAvailables)){
			loadJdkToolAvailables();
		}
		return jdkToolAvailables;
	}
	
	public static JDK getJDKByName(String name) {
		JDK out = null;
		for(JDK jdk : getJdkToolAvailables()) {
			if(name.equalsIgnoreCase(jdk.getName())){
				out = jdk;
			}
		}
		return out;
	}
	
	public static String getJDKHomeByName(String name) {
		String out = null;
		for(JDK jdk : getJdkToolAvailables()) {
			if(jdk.getName().equalsIgnoreCase(name)){
				out = jdk.getHome();
			}
		}
		return out;
	}
	
	public static boolean checkJdkVersion(JDK jdk, PrintStream logger){
		
		if(jdk == null || ! jdk.getExists()){
			return false;
		}
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TaskListener listener = new StreamTaskListener(out);
			Launcher launcher = Jenkins.getInstance().createLauncher(listener);
			String cmd = jdk.getBinDir().getAbsolutePath().concat("/java");
			int result = launcher.launch().cmds(cmd,"-version").stdout(out).join();
//			L'executable n'existe pas
			if(result  != 0){
				logger.println("[WeblogicDeploymentPlugin] - Unable to detect JDK version");
				return false;
			}
			
			Pattern pattern = Pattern.compile(JAVA_VERSION_COMMAND_VERSION_LINE_REGEX);
			Matcher matcher = pattern.matcher(out.toString());
			if(matcher.matches()){
				logger.println("[WeblogicDeploymentPlugin] - Pay attention to JDK version {selected version is "+matcher.group(3)+"} compatibility with WebLogic Deployer API (see Oracle documentation).");
			}
			
		} catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        } catch(IllegalStateException ise){
        	return false;
        } catch(IndexOutOfBoundsException ioobe) {
        	return false;
        }
		
		return true;
	}
}
