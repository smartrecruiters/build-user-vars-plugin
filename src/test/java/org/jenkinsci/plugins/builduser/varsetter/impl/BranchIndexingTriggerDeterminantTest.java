package org.jenkinsci.plugins.builduser.varsetter.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import jenkins.branch.BranchIndexingCause;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;

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
        Run run = r.createFreeStyleProject().getBuild("0");
        assertFalse(new BranchIndexingTriggerDeterminant().setJenkinsUserBuildVars(run, null, variables));
        assertThat(variables, equalTo(Collections.emptyMap()));
    }
}
