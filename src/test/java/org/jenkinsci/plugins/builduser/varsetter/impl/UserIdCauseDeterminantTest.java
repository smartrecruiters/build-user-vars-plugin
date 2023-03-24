package org.jenkinsci.plugins.builduser.varsetter.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.model.Cause.UserIdCause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.util.LogTaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UserIdCauseDeterminantTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void testSetJenkinsUserBuildVars() throws IOException {
        User.getById("testuser", true);
        JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
        r.jenkins.setSecurityRealm(realm);
        EnvVars outputVars = new EnvVars();
        Run run = r.createFreeStyleProject().getBuild("0");
        realm.addGroups("testuser", "group1", "group2");
        UserIdCause cause = new UserIdCause("testuser");
        UserIdCauseDeterminant determinant = new UserIdCauseDeterminant();
        determinant.setJenkinsUserBuildVars(run, cause, outputVars);
        System.out.println(outputVars);
        assertThat(outputVars.get("BUILD_USER_GROUPS"), is(equalTo("authenticated,group1,group2")));
    }


    @Test
    public void testSetJenkinsUserBuildVarsInvalidUser() throws IOException {
        JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
        r.jenkins.setSecurityRealm(realm);
        EnvVars outputVars = new EnvVars();
        Run run = r.createFreeStyleProject().getBuild("0");
        UserIdCause cause = new UserIdCause("testuser");
        UserIdCauseDeterminant determinant = new UserIdCauseDeterminant();
        determinant.setJenkinsUserBuildVars(run, cause, outputVars);
        System.out.println(outputVars);
        // 'anonymous' user gets authenticated group automatically
        assertThat(outputVars.get("BUILD_USER_GROUPS"), is(equalTo("authenticated")));
    }

    @Test
    public void testSetJenkinsUserBuildVarsNoGroups() throws IOException {
        User.getById("testuser", true);
        JenkinsRule.DummySecurityRealm realm = r.createDummySecurityRealm();
        r.jenkins.setSecurityRealm(realm);
        EnvVars outputVars = new EnvVars();
        Run run = r.createFreeStyleProject().getBuild("0");
        UserIdCause cause = new UserIdCause("testuser");
        UserIdCauseDeterminant determinant = new UserIdCauseDeterminant();
        determinant.setJenkinsUserBuildVars(run, cause, outputVars);
        System.out.println(outputVars);
        // User still gets authenticated group automatically
        assertThat(outputVars.get("BUILD_USER_GROUPS"), is(equalTo("authenticated")));
    }

    @Test
    public void testSetJenkinsUserBuildVarsNoSecurityRealm() throws IOException {
        User.getById("testuser", true);
        UserIdCause cause = new UserIdCause("testuser");
        UserIdCauseDeterminant determinant = new UserIdCauseDeterminant();
        EnvVars outputVars = new EnvVars();
        Run run = r.createFreeStyleProject().getBuild("0");
        determinant.setJenkinsUserBuildVars(run, cause, outputVars);
        System.out.println(outputVars);
        assertThat(outputVars.get("BUILD_USER_GROUPS"), is(equalTo("")));
    }
}
