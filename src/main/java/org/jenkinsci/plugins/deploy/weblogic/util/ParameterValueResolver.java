/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hudson.EnvVars;

/**
 * @author RaphaelC
 *
 */
public class ParameterValueResolver {

	private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{?([\\S&&[^}]]*)\\}?");
	
	
	/**
	 * 
	 * @param label
	 * @param envars all of the environment variable
	 * @return
	 */
	public static String resolveEnvVar(String label, EnvVars envars) {
		Matcher matcher = ENV_VAR_PATTERN.matcher(label);
		if(matcher.matches()){
            String key = matcher.group(1);
            return envars.get(key, label);
		}

		return label;
		
	}
	
	/**
	 * Resolve zero or more Jenkins enviroment variables in label, like:
	 * 
	 * <code>
	 * String myVar = "artifact-${NAME}-$VERSION"; // when ${NAME} and $VERSION are env. variables.
	 * </code>
	 * 
	 * @param label Name containing zero or more variables to resolve.
	 * @param envars All of the environment variable.
	 * @return Name with resolved variables.
	 * @author ecavinat
	 */
	public static String resolveEnvVars(String label, EnvVars envars) {
		if (label != null) {
			Matcher m = ENV_VAR_PATTERN.matcher(label);
			if (m != null) {
				while (m.find()) {
					label = label.replace(m.group(), resolveEnvVar(m.group(), envars));
				}
			}
		}
		return label;		
	}
}
