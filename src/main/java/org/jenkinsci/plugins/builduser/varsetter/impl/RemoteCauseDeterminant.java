package org.jenkinsci.plugins.builduser.varsetter.impl;

import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.builduser.utils.UsernameUtils;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;

import static java.lang.String.format;

public class RemoteCauseDeterminant implements IUsernameSettable<Cause.RemoteCause> {

    @Override
    public boolean setJenkinsUserBuildVars(Run run, Cause.RemoteCause cause, EnvVars envVars) {
        if (cause == null) {
            return false;
        }

        //As of Jenkins 2.51 remote cause is set the build was triggered using token and real user is not set
        UsernameUtils.setUsernameVars(format("%s %s", cause.getAddr(), cause.getNote()), envVars);
        envVars.put(BUILD_USER_ID, "remoteRequest");
        return true;
    }

    @Override
    public Class<Cause.RemoteCause> getUsedCauseClass() {
        return Cause.RemoteCause.class;
    }
}
