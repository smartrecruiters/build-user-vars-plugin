package org.jenkinsci.plugins.builduser.varsetter.impl;

import hudson.EnvVars;
import hudson.model.Cause.UserIdCause;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.builduser.SlackUserProperty;
import org.jenkinsci.plugins.builduser.utils.UsernameUtils;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;
import hudson.tasks.Mailer;
import hudson.model.User;
import hudson.model.UserProperty;

/**
 * This implementation is used to determine build username variables from <b>{@link UserIdCause}</b>.
 * This will let to get whole set of variables:
 * <ul>
 *   <li>{@link IUsernameSettable#BUILD_USER_ID}</li>
 *   <li>{@link IUsernameSettable#BUILD_USER_VAR_NAME}</li>
 *   <li>{@link IUsernameSettable#BUILD_USER_FIRST_NAME_VAR_NAME}</li>
 *   <li>{@link IUsernameSettable#BUILD_USER_LAST_NAME_VAR_NAME}</li>
 * </ul>
 *
 * @author GKonovalenko
 */
public class UserIdCauseDeterminant implements IUsernameSettable<UserIdCause> {

	final Class<UserIdCause> causeClass = UserIdCause.class;

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>{@link UserIdCause}</b> based implementation.
	 */
	public boolean setJenkinsUserBuildVars(Run run, UserIdCause cause,
										   EnvVars envVars, TaskListener listener) {
		if(null != cause) {
			String username = cause.getUserName();
			UsernameUtils.setUsernameVars(username, envVars);

			String userid = StringUtils.trimToEmpty(cause.getUserId());
			envVars.put(BUILD_USER_ID, userid);

            		User user=User.get(userid);
            		if(null != user) {
            		    UserProperty prop = user.getProperty(Mailer.UserProperty.class);
            		    if(null != prop) {
            		        String adrs = StringUtils.trimToEmpty(((Mailer.UserProperty)prop).getAddress());
            		        envVars.put(BUILD_USER_EMAIL, adrs);
            		    }
                        SlackUserProperty slackProperty = user.getProperty(SlackUserProperty.class);
                        if (null != slackProperty) {
                            String slackUsername = StringUtils.trimToEmpty(slackProperty.getSlackUsername());
                            envVars.put(IUsernameSettable.BUILD_USER_SLACK, slackUsername);
                        }
            		}

			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Class<UserIdCause> getUsedCauseClass() {
		return causeClass;
	}

}
