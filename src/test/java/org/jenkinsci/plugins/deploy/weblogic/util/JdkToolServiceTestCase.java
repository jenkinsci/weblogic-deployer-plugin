/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.jenkinsci.plugins.deploy.weblogic.jdk.JdkToolService;
import org.junit.Test;

/**
 * @author rchaumie
 *
 */
public class JdkToolServiceTestCase {

	
	@Test
	public void checkJdkVersion() throws Exception{
		String out = "P:/Outils/jdk1.5.0_22/bin/java -version\r\njava version \"1.5.0_22\"\r\nJava(TM) 2 Runtime Environment, Standard Edition (build 1.5.0_22-b03)\r\nJava HotSpot(TM) Client VM (build 1.5.0_22-b03, mixed mode, sharing)\r\n";
		Matcher matcher = Pattern.compile(JdkToolService.JAVA_VERSION_COMMAND_VERSION_LINE_REGEX).matcher(out);
		if(! matcher.matches()){
			Assert.fail("pattern didn't matched.");
		}
		
//		System.out.println("[HudsonWeblogicDeploymentPlugin] - Displaying a warning about JDK version compatibility with WebLogic Deployer API {"+matcher.group(3)+"}");			
	}
}
