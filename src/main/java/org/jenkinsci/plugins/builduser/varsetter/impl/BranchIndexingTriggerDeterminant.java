package org.jenkinsci.plugins.builduser.varsetter.impl;

import java.io.IOException;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.branch.BranchIndexingCause;
import jenkins.scm.api.SCMSource;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.builduser.utils.UserUtils;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;
import org.jenkinsci.plugins.github_branch_source.Connector;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public class BranchIndexingTriggerDeterminant implements IUsernameSettable<BranchIndexingCause> {
    private static final Class<BranchIndexingCause> causeClass = BranchIndexingCause.class;

    @Override
    public boolean setJenkinsUserBuildVars(Run run, BranchIndexingCause cause, EnvVars envVars, TaskListener listener) throws IOException, InterruptedException {

        if (cause != null) {
            String changeAuthor = envVars.get("CHANGE_AUTHOR");
            if (StringUtils.isNotEmpty(changeAuthor)) {
                boolean matched = UserUtils.setVarsForUser(envVars, changeAuthor);
                if (!matched) {
                    envVars.put(BUILD_USER_ID, changeAuthor);
                }
            } else {
                Job job = run.getParent();
                String branch = envVars.get("BRANCH_NAME");
                SCMSource source = SCMSource.SourceByItem.findSource(job);
                if (source instanceof GitHubSCMSource) {
                    GitHubSCMSource gitHubSCMSource = (GitHubSCMSource) source;
                    StandardCredentials credentials = Connector.lookupScanCredentials(job, gitHubSCMSource.getApiUri(), gitHubSCMSource.getCredentialsId());
                    GitHub gitHub = Connector.connect(gitHubSCMSource.getApiUri(), credentials);
                    try {
                        GHRepository ghRepository = gitHub.getRepository(gitHubSCMSource.getRepoOwner() + "/" + gitHubSCMSource.getRepository());
                        String sha = ghRepository.getRef("heads/" + branch).getObject().getSha();
                        String author = ghRepository.getCommit(sha).getAuthor().getLogin();
                        if (StringUtils.isNotEmpty(author)) {
                            boolean matched = UserUtils.setVarsForUser(envVars, author);
                            if (!matched) {
                                envVars.put(BUILD_USER_ID, author);
                            }
                        }
                    } finally {
                        Connector.release(gitHub);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Class<BranchIndexingCause> getUsedCauseClass() {
        return causeClass;
    }
}
