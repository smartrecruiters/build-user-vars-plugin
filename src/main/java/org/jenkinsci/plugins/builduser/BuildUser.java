package org.jenkinsci.plugins.builduser;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jenkins.github_pr_label_build.GitHubPullRequestLabelCause;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Cause.UserCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.triggers.SCMTrigger;
import jenkins.branch.BranchEventCause;
import jenkins.branch.BranchIndexingCause;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;
import org.jenkinsci.plugins.builduser.varsetter.impl.BranchEventCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.BranchIndexingCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.GitHubPullRequestLabelCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.SCMTriggerCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.UserCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.UserIdCauseDeterminant;

import static org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable.BUILD_USER_ID;

/**
 * This plugin is used to set build user variables, see {@link IUsernameSettable}:
 *
 * @author GKonovalenko
 * @see IUsernameSettable
 */
@SuppressWarnings("deprecation")
@Extension(ordinal = 1000)
public class BuildUser extends EnvironmentContributor {

    private static final Logger LOGGER = Logger.getLogger(BuildUser.class.getName());

    @Override
    public void buildEnvironmentFor(Run r, EnvVars env, TaskListener listener) throws IOException, InterruptedException {
        if (env.get(BUILD_USER_ID) == null) {
            makeUserBuildVariables(r, env, listener);
        }
    }

    /**
     * Retrieve user cause that triggered this build and populate envVars accordingly
     */
    private void makeUserBuildVariables(@Nonnull Run build, @Nonnull EnvVars envVars, TaskListener listener) {

        try {
            // If build has been triggered form an upstream build, get UserCause from there to set user build envVars
            Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) build.getCause(Cause.UpstreamCause.class);
            if (upstreamCause != null) {
                Job job = Jenkins.getInstance().getItemByFullName(upstreamCause.getUpstreamProject(), Job.class);
                if (job != null) {
                    Run upstream = job.getBuildByNumber(upstreamCause.getUpstreamBuild());
                    if (upstream != null) {
                        makeUserBuildVariables(upstream, envVars, listener);
                    }
                }
            }

            BranchIndexingCause branchIndexingCause = (BranchIndexingCause) build.getCause(BranchIndexingCause.class);
            if (new BranchIndexingCauseDeterminant().setJenkinsUserBuildVars(build, branchIndexingCause, envVars, listener)) {
                return;
            }

            BranchEventCause branchEventCause = (BranchEventCause) build.getCause(BranchEventCause.class);
            if (new BranchEventCauseDeterminant().setJenkinsUserBuildVars(build, branchEventCause, envVars, listener)) {
                return;
            }

            GitHubPullRequestLabelCause prLabelCause = (GitHubPullRequestLabelCause) build.getCause(GitHubPullRequestLabelCause.class);
            if (new GitHubPullRequestLabelCauseDeterminant().setJenkinsUserBuildVars(build, prLabelCause, envVars, listener)) {
                return;
            }

            // set BUILD_USER_NAME to fixed value if the build was triggered by a change in the scm
            SCMTrigger.SCMTriggerCause scmTriggerCause = (SCMTrigger.SCMTriggerCause) build.getCause(SCMTrigger.SCMTriggerCause.class);
            if (new SCMTriggerCauseDeterminant().setJenkinsUserBuildVars(build, scmTriggerCause, envVars, listener)) {
                return;
            }

            /* Try to use UserIdCause to get & set jenkins user build envVars */
            UserIdCause userIdCause = (UserIdCause) build.getCause(UserIdCause.class);
            if (new UserIdCauseDeterminant().setJenkinsUserBuildVars(build, userIdCause, envVars, listener)) {
                return;
            }

            // Try to use deprecated UserCause to get & set jenkins user build envVars
            UserCause userCause = (UserCause) build.getCause(UserCause.class);
            if (new UserCauseDeterminant().setJenkinsUserBuildVars(build, userCause, envVars, listener)) {
                return;
            }
        } catch (Exception e) {
            String message = String.format("Failed to detect BUILD USER for build %s", build.toString());
            LOGGER.log(Level.SEVERE, message);
        }
    }
}

