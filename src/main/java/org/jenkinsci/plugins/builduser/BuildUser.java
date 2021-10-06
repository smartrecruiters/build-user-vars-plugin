package org.jenkinsci.plugins.builduser;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Cause.UserCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SCMTrigger;
import jenkins.branch.BranchEventCause;
import jenkins.branch.BranchIndexingCause;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildWrapper;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;
import org.jenkinsci.plugins.builduser.varsetter.impl.BranchEventCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.BranchIndexingCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.GitHubPullRequestLabelCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.SCMTriggerCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.UserCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.UserIdCauseDeterminant;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This plugin is used to set build user variables, see {@link IUsernameSettable}:
 *
 * @author GKonovalenko
 * @see IUsernameSettable
 */
@SuppressWarnings("deprecation")
public class BuildUser extends SimpleBuildWrapper {

    private static final Logger log = Logger.getLogger(BuildUser.class.getName());

    private static final String EXTENSION_DISPLAY_NAME = "Set jenkins user build variables";

    @DataBoundConstructor
    public BuildUser() {
        //noop
    }

    public void setUp(Context context, Run<?, ?> build, FilePath workspace,
                      Launcher launcher, TaskListener listener, EnvVars initialEnvironment){

        Map<String, String> variables = new HashMap<>();
        makeUserBuildVariables(build, variables, listener);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            context.env(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Retrieve user cause that triggered this build and populate variables accordingly
     */
    private void makeUserBuildVariables(@Nonnull Run build, @Nonnull Map<String, String> variables, TaskListener listener) {

        try {
            // If build has been triggered form an upstream build, get UserCause from there to set user build variables
            Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) build.getCause(Cause.UpstreamCause.class);
            if (upstreamCause != null) {
                Job job = Jenkins.getInstance().getItemByFullName(upstreamCause.getUpstreamProject(), Job.class);
                if (job != null) {
                    Run upstream = job.getBuildByNumber(upstreamCause.getUpstreamBuild());
                    if (upstream != null) {
                        makeUserBuildVariables(upstream, variables, listener);
                    }
                }
            }

            BranchIndexingCause branchIndexingCause = (BranchIndexingCause) build.getCause(BranchIndexingCause.class);
            if (new BranchIndexingCauseDeterminant().setJenkinsUserBuildVars(build, branchIndexingCause, variables, listener)) {
                return;
            }

            BranchEventCause branchEventCause = (BranchEventCause) build.getCause(BranchEventCause.class);
            if (new BranchEventCauseDeterminant().setJenkinsUserBuildVars(build, branchEventCause, variables, listener)) {
                return;
            }

            GitHubPullRequestLabelCause prLabelCause = (GitHubPullRequestLabelCause) build.getCause(GitHubPullRequestLabelCause.class);
            if (new GitHubPullRequestLabelCauseDeterminant().setJenkinsUserBuildVars(build, prLabelCause, variables, listener)) {
                return;
            }

            // set BUILD_USER_NAME to fixed value if the build was triggered by a change in the scm
            SCMTrigger.SCMTriggerCause scmTriggerCause = (SCMTrigger.SCMTriggerCause) build.getCause(SCMTrigger.SCMTriggerCause.class);
            if (new SCMTriggerCauseDeterminant().setJenkinsUserBuildVars(build, scmTriggerCause, variables, listener)) {
                return;
            }

            /* Try to use UserIdCause to get & set jenkins user build variables */
            UserIdCause userIdCause = (UserIdCause) build.getCause(UserIdCause.class);
            if (new UserIdCauseDeterminant().setJenkinsUserBuildVars(build, userIdCause, variables, listener)) {
                return;
            }

            // Try to use deprecated UserCause to get & set jenkins user build variables
            UserCause userCause = (UserCause) build.getCause(UserCause.class);
            if (new UserCauseDeterminant().setJenkinsUserBuildVars(build, userCause, variables, listener)) {
                return;
            }
        } catch (Exception e) {
            listener.error("Failed to detect BUILD USER");
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return EXTENSION_DISPLAY_NAME;
        }
    }
}

