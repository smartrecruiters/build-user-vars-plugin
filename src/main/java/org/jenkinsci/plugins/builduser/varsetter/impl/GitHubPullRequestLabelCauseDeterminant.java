package org.jenkinsci.plugins.builduser.varsetter.impl;

import com.jenkins.github_pr_label_build.GitHubPullRequestLabelCause;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.builduser.utils.UserUtils;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;

public class GitHubPullRequestLabelCauseDeterminant implements IUsernameSettable<GitHubPullRequestLabelCause> {

    final Class<GitHubPullRequestLabelCause> causeClass = GitHubPullRequestLabelCause.class;

    public boolean setJenkinsUserBuildVars(Run run, GitHubPullRequestLabelCause cause,
                                           EnvVars envVars, TaskListener listener) {

        if (cause != null) {
            boolean matched = UserUtils.setVarsForUser(envVars, cause.getSender());
            if (!matched) {
                matched = UserUtils.setVarsForUser(envVars, cause.getPrOwner());
            }
            if (!matched) {
                envVars.put(BUILD_USER_ID, cause.getSender());
            }

            return true;
        } else {
            return false;
        }
    }

    public Class<GitHubPullRequestLabelCause> getUsedCauseClass() {
        return causeClass;
    }

}
