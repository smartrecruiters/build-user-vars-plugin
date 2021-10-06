package org.jenkinsci.plugins.builduser.utils;

import java.util.Map;

import hudson.model.User;
import hudson.model.UserProperty;
import hudson.tasks.Mailer;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;

public final class UserUtils {

	public static boolean setVarsForUser(Map<String, String> variables, String githubId) {
        boolean userFound = false;
        for (User user : User.getAll()) {
            if (user.getId().equals(githubId) || isDescriptionContain(githubId, user)) {
                userFound = true;
                variables.put(IUsernameSettable.BUILD_USER_ID, user.getId());
                UserProperty prop = user.getProperty(Mailer.UserProperty.class);
                if (null != prop) {
                    String adrs = StringUtils.trimToEmpty(((Mailer.UserProperty) prop).getAddress());
                    variables.put(IUsernameSettable.BUILD_USER_EMAIL, adrs);
                }
                UsernameUtils.setUsernameVars(user.getDisplayName(), variables);
            }
        }
        return userFound;
    }
	private static boolean isDescriptionContain(String pushedBy, User user) {
        return StringUtils.isNotEmpty(user.getDescription()) && user.getDescription().contains(pushedBy);
    }
}
