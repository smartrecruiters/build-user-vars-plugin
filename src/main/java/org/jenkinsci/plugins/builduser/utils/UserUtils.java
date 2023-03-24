package org.jenkinsci.plugins.builduser.utils;

import java.util.Map;

import hudson.model.User;
import hudson.tasks.Mailer;
import org.jenkinsci.plugins.builduser.GithubUserProperty;
import org.jenkinsci.plugins.builduser.SlackUserProperty;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable.BUILD_USER_EMAIL;
import static org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable.BUILD_USER_ID;
import static org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable.BUILD_USER_SLACK;

public final class UserUtils {

    public static boolean setVarsForUser(Map<String, String> variables, String githubId) {
        return User.getAll().stream()
                .filter(user -> user.getId().equals(githubId) || isPushByMatchingUser(githubId, user))
                .findFirst()
                .map(user -> addCustomVariables(variables, user))
                .isPresent();
    }

    private static Map<String, String> addCustomVariables(final Map<String, String> variables, final User user) {
        variables.put(BUILD_USER_ID, user.getId());
        Mailer.UserProperty prop = user.getProperty(Mailer.UserProperty.class);
        if (null != prop) {
            variables.put(BUILD_USER_EMAIL, trimToEmpty(prop.getAddress()));
        }

        SlackUserProperty slackProperty = user.getProperty(SlackUserProperty.class);
        if (null != slackProperty) {
            variables.put(BUILD_USER_SLACK, slackProperty.getSlackWrappedUsername());
        }

        UsernameUtils.setUsernameVars(user.getDisplayName(), variables);

        return variables;
    }

    private static boolean isPushByMatchingUser(String pushedBy, User user) {
        GithubUserProperty property = user.getProperty(GithubUserProperty.class);
        if (null != property) {
            String githubUsername = trimToEmpty(property.getGithubUsername());
            return githubUsername.equals(pushedBy);
        }

        String description = user.getDescription();
        return isNotEmpty(description) && description.contains(pushedBy);
    }
}
