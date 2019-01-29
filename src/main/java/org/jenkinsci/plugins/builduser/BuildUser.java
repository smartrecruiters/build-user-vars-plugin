package org.jenkinsci.plugins.builduser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.jenkins.github_pr_label_build.GitHubPullRequestLabelCause;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Cause.RemoteCause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Cause.UserCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SCMTrigger.SCMTriggerCause;
import hudson.triggers.TimerTrigger.TimerTriggerCause;
import jenkins.branch.BranchEventCause;
import jenkins.branch.BranchIndexingCause;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildWrapper;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;
import org.jenkinsci.plugins.builduser.varsetter.impl.BranchEventCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.BranchIndexingTriggerDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.GitHubPullRequestLabelCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.RemoteCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.SCMTriggerCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.TimerTriggerCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.UserCauseDeterminant;
import org.jenkinsci.plugins.builduser.varsetter.impl.UserIdCauseDeterminant;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This plugin is used to set build user variables, see {@link IUsernameSettable}:
 *
 * @author GKonovalenko
 * @see IUsernameSettable
 */
public class BuildUser extends SimpleBuildWrapper {

    private static final Logger log = Logger.getLogger(BuildUser.class.getName());

    private static final String EXTENSION_DISPLAY_NAME = "Set jenkins user build variables";


    @DataBoundConstructor
    public BuildUser() {
        //noop
    }

    public void setUp(Context context, Run<?, ?> build, FilePath workspace,
                      Launcher launcher, TaskListener listener, EnvVars initialEnvironment) {
        Map<String, String> variables = new HashMap<>();
        makeUserBuildVariables(build, variables, listener);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            context.env(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Retrieve user cause that triggered this build and populate variables accordingly
     * <p>
     * TODO: The whole hierarchy and way of applying could be refactored.
     */
    @Restricted(NoExternalUse.class)
    static void makeUserBuildVariables(@NonNull Run<?, ?> build, @NonNull Map<String, String> variables, TaskListener listener) {

        /* Try to use UserIdCause to get & set jenkins user build variables */
        UserIdCause userIdCause = build.getCause(UserIdCause.class);
        if (new UserIdCauseDeterminant().setJenkinsUserBuildVars(build, userIdCause, variables, listener)) {
            return;
        }

        // Try to use deprecated UserCause to get & set jenkins user build variables
        @SuppressWarnings("deprecation")
        UserCause userCause = build.getCause(UserCause.class);
        if (new UserCauseDeterminant().setJenkinsUserBuildVars(build, userCause, variables, listener)) {
            return;
        }

        // If build has been triggered form an upstream build, get UserCause from there to set user build variables
        UpstreamCause upstreamCause = build.getCause(UpstreamCause.class);
        if (upstreamCause != null) {
            Job<?, ?> job = Jenkins.get().getItemByFullName(upstreamCause.getUpstreamProject(), Job.class);
            if (job != null) {
                Run<?, ?> upstream = job.getBuildByNumber(upstreamCause.getUpstreamBuild());
                if (upstream != null) {
                    makeUserBuildVariables(upstream, variables, listener);
                    return;
                }
            }
        }

        // Other causes should be checked after as build can be triggered automatically and later rerun manually by a human.
        // In that case there will be multiple causes and the direct manually one is preferred to set in a variable.
        try {
            handleOtherCausesOrLogWarningIfUnhandled(build, variables, listener);
        } catch (Exception e) {
            listener.error("Failed to detect BUILD USER");
        }
    }

    private static void handleOtherCausesOrLogWarningIfUnhandled(@NonNull Run<?, ?> build, @NonNull Map<String, String> variables, TaskListener listener) throws Exception {
        // set BUILD_USER_NAME and ID to fixed value if the build was triggered by a change in the scm, timer or remotely with token
        SCMTriggerCause scmTriggerCause = build.getCause(SCMTriggerCause.class);
        if (new SCMTriggerCauseDeterminant().setJenkinsUserBuildVars(build, scmTriggerCause, variables, listener)) {
            return;
        }

        TimerTriggerCause timerTriggerCause = build.getCause(TimerTriggerCause.class);
        if (new TimerTriggerCauseDeterminant().setJenkinsUserBuildVars(build, timerTriggerCause, variables, listener)) {
            return;
        }

        RemoteCause remoteTriggerCause = build.getCause(RemoteCause.class);
        if (new RemoteCauseDeterminant().setJenkinsUserBuildVars(build, remoteTriggerCause, variables, listener)) {
            return;
        }

        GitHubPullRequestLabelCause prLabelCause = build.getCause(GitHubPullRequestLabelCause.class);
        if (new GitHubPullRequestLabelCauseDeterminant().setJenkinsUserBuildVars(build, prLabelCause, variables, listener)) {
            return;
        }
        BranchIndexingCause branchIndexingCause = build.getCause(BranchIndexingCause.class);
        if (new BranchIndexingTriggerDeterminant().setJenkinsUserBuildVars(build, branchIndexingCause, variables, listener)) {
            return;
        }

        BranchEventCause branchEventCause = build.getCause(BranchEventCause.class);
        if (new BranchEventCauseDeterminant().setJenkinsUserBuildVars(build, branchEventCause, variables, listener)) {
            return;
        }

        log.warning(() -> "Unsupported cause type(s): " + Arrays.toString(build.getCauses().toArray()));
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return EXTENSION_DISPLAY_NAME;
        }
    }
}

