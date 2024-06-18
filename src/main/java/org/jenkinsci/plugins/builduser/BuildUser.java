package org.jenkinsci.plugins.builduser;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jenkins.github_pr_label_build.GitHubPullRequestLabelCause;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Cause.RemoteCause;
import hudson.model.Cause.UserCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.triggers.SCMTrigger.SCMTriggerCause;
import hudson.triggers.TimerTrigger.TimerTriggerCause;
import jenkins.branch.BranchEventCause;
import jenkins.branch.BranchIndexingCause;
import jenkins.model.Jenkins;
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

    private static final Logger log = Logger.getLogger(BuildUser.class.getName());

    @Override
    public void buildEnvironmentFor(Run r, EnvVars env, TaskListener listener) {
        if (env.get(BUILD_USER_ID) == null) {
            try {
                makeUserBuildVariables(r, env, listener);
            } catch (Exception e) {
                String message = String.format("Failed to detect BUILD USER for build %s", r.toString());
                log.log(Level.FINE, message);
            }
        }
    }


    /**
     * Retrieve user cause that triggered this build and populate variables accordingly
     * <p>
     * TODO: The whole hierarchy and way of applying could be refactored.
     */
    @Restricted(NoExternalUse.class)
    static void makeUserBuildVariables(@Nonnull Run build, @Nonnull EnvVars envVars, TaskListener listener) {

        /* Try to use UserIdCause to get & set jenkins user build envVars */
        UserIdCause userIdCause = (UserIdCause) build.getCause(UserIdCause.class);
        if (new UserIdCauseDeterminant().setJenkinsUserBuildVars(build, userIdCause, envVars, listener)) {
            return;
        }

        // Try to use deprecated UserCause to get & set jenkins user build envVars
        @SuppressWarnings("deprecation")
        UserCause userCause = (UserCause) build.getCause(UserCause.class);
        if (new UserCauseDeterminant().setJenkinsUserBuildVars(build, userCause, envVars, listener)) {
            return;
        }

        // If build has been triggered form an upstream build, get UserCause from there to set user build envVars
        Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) build.getCause(Cause.UpstreamCause.class);
        if (upstreamCause != null) {
            Job<?, ?> job = Jenkins.get().getItemByFullName(upstreamCause.getUpstreamProject(), Job.class);
            if (job != null) {
                Run<?, ?> upstream = job.getBuildByNumber(upstreamCause.getUpstreamBuild());
                if (upstream != null) {
                    makeUserBuildVariables(upstream, envVars, listener);
                    return;
                }
            }
        }

        // Other causes should be checked after as build can be triggered automatically and later rerun manually by a human.
        // In that case there will be multiple causes and the direct manually one is preferred to set in a variable.
        try {
            handleOtherCausesOrLogWarningIfUnhandled(build, envVars, listener);
        } catch (Exception e) {
            String message = String.format("Failed to detect BUILD USER for build %s", build);
            log.log(Level.FINE, message);        }
    }

    private static void handleOtherCausesOrLogWarningIfUnhandled(@NonNull Run<?, ?> build, @NonNull EnvVars envVars, TaskListener listener) throws Exception {
        // set BUILD_USER_NAME and ID to fixed value if the build was triggered by a change in the scm, timer or remotely with token
        SCMTriggerCause scmTriggerCause = build.getCause(SCMTriggerCause.class);
        if (new SCMTriggerCauseDeterminant().setJenkinsUserBuildVars(build, scmTriggerCause, envVars, listener)) {
            return;
        }

        TimerTriggerCause timerTriggerCause = build.getCause(TimerTriggerCause.class);
        if (new TimerTriggerCauseDeterminant().setJenkinsUserBuildVars(build, timerTriggerCause, envVars, listener)) {
            return;
        }

        RemoteCause remoteTriggerCause = build.getCause(RemoteCause.class);
        if (new RemoteCauseDeterminant().setJenkinsUserBuildVars(build, remoteTriggerCause, envVars, listener)) {
            return;
        }

        GitHubPullRequestLabelCause prLabelCause = build.getCause(GitHubPullRequestLabelCause.class);
        if (new GitHubPullRequestLabelCauseDeterminant().setJenkinsUserBuildVars(build, prLabelCause, envVars, listener)) {
            return;
        }
        BranchIndexingCause branchIndexingCause = build.getCause(BranchIndexingCause.class);
        if (new BranchIndexingTriggerDeterminant().setJenkinsUserBuildVars(build, branchIndexingCause, envVars, listener)) {
            return;
        }

        BranchEventCause branchEventCause = build.getCause(BranchEventCause.class);
        if (new BranchEventCauseDeterminant().setJenkinsUserBuildVars(build, branchEventCause, envVars, listener)) {
            return;
        }

        log.warning(() -> "Unsupported cause type(s): " + Arrays.toString(build.getCauses().toArray()));
    }
}

