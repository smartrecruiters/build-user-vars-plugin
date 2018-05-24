package org.jenkinsci.plugins.builduser.varsetter.impl;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.triggers.SCMTrigger;
import hudson.triggers.SCMTrigger.SCMTriggerCause;

import java.lang.reflect.Field;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.builduser.utils.UserUtils;
import org.jenkinsci.plugins.builduser.utils.UsernameUtils;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;

public class SCMTriggerCauseDeterminant implements IUsernameSettable<SCMTrigger.SCMTriggerCause> {

    final Class<SCMTrigger.SCMTriggerCause> causeClass = SCMTrigger.SCMTriggerCause.class;

    public boolean setJenkinsUserBuildVars(Run run, SCMTriggerCause cause,
                                           EnvVars envVars, TaskListener listener) throws Exception {

        if (cause != null) {
            UsernameUtils.setUsernameVars("SCMTrigger", envVars);
            // sets pushedBy provided by GitHubPushCause as BUILD_USER_ID
            Field pushedByField = cause.getClass().getDeclaredField("pushedBy");
            pushedByField.setAccessible(true);
            String pushedBy = (String) pushedByField.get(cause);
            if (StringUtils.isNotEmpty(pushedBy)) {
                boolean matched = UserUtils.setVarsForUser(envVars, pushedBy);
                if (!matched) {
                    envVars.put(BUILD_USER_ID, pushedBy);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public Class<SCMTriggerCause> getUsedCauseClass() {
        return causeClass;
    }

}
