package org.jenkinsci.plugins.builduser.varsetter.impl;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import jenkins.branch.BranchIndexingCause;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BranchIndexingTriggerDeterminantTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void usedCauseClassIsBranchIndexingCause() {
        assertThat(new BranchIndexingTriggerDeterminant().getUsedCauseClass(), equalTo(BranchIndexingCause.class));
    }

    @Test
    public void setVarsReturnsFalseWithoutBuildUserVarsOnNullCause() throws IOException, InterruptedException {
        EnvVars variables = new EnvVars();
        TaskListener taskListener = new LogTaskListener(Logger.getLogger("test-logger"), Level.INFO);
        Run run = r.createFreeStyleProject().getBuild("0");
        assertFalse(new BranchIndexingTriggerDeterminant().setJenkinsUserBuildVars(run, null, variables, taskListener));
        assertThat(variables, equalTo(Collections.emptyMap()));
    }

//    @Test
//    public void setVarsReturnsTrueWithBuildUsersVarsOnValidCause() throws Exception {
//        EnvVars variables = new EnvVars();
//        TaskListener taskListener = new LogTaskListener(Logger.getLogger("test-logger"), Level.INFO);
//        Run run = r.createFreeStyleProject().getBuild("0");
//        assertTrue(new BranchIndexingTriggerDeterminant().setJenkinsUserBuildVars(run, mockCause(), variables, taskListener));
//        assertThat(variables, allOf(hasEntry(IUsernameSettable.BUILD_USER_VAR_NAME, "Branch Indexing"),
//                hasEntry(IUsernameSettable.BUILD_USER_FIRST_NAME_VAR_NAME, "Branch"),
//                hasEntry(IUsernameSettable.BUILD_USER_LAST_NAME_VAR_NAME, "Indexing"),
//                hasEntry(IUsernameSettable.BUILD_USER_ID, "branchIndexing")
//        ));
//    }

    private BranchIndexingCause mockCause() throws Exception {
        Constructor<BranchIndexingCause> ctor = BranchIndexingCause.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }
}
