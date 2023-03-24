/*
 * Copyright (c) 2018 SmartRecruiters Inc. All Rights Reserved.
 */

package org.jenkinsci.plugins.builduser;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class GithubUserProperty extends hudson.model.UserProperty {

    private String githubUsername;

    @DataBoundConstructor
    public GithubUserProperty(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    private GithubUserProperty() {
        this(null);
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    @Extension
    @Symbol("githubUsername")
    public static class DescriptorImpl extends UserPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Github";
        }

        @Override
        public UserProperty newInstance(User user) {
            return new GithubUserProperty();
        }
    }

    @Override
    public UserProperty reconfigure(StaplerRequest req, JSONObject form) throws Descriptor.FormException {
        req.bindJSON(this, form);
        return this;
    }
}
