package org.jenkinsci.plugins.builduser.varsetter.impl;

import java.util.Map;

import com.jenkins.github_pr_label_build.GitHubPullRequestLabelCause;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.builduser.utils.UserUtils;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;

public class GitHubPullRequestLabelCauseDeterminant implements IUsernameSettable<GitHubPullRequestLabelCause> {

    final Class<GitHubPullRequestLabelCause> causeClass = GitHubPullRequestLabelCause.class;

    public boolean setJenkinsUserBuildVars(Run run, GitHubPullRequestLabelCause cause,
                                           Map<String, String> variables, TaskListener listener) {

        if (cause != null) {
            boolean matched = UserUtils.setVarsForUser(variables, cause.getSender());
            if (!matched) {
                variables.put(BUILD_USER_ID, cause.getSender());
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
