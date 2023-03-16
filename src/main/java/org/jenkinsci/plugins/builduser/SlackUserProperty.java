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

public class SlackUserProperty extends UserProperty {

    private String slackUsername;

    @DataBoundConstructor
    public SlackUserProperty(String slackUsername) {
        this.slackUsername = slackUsername;
    }

    private SlackUserProperty() {
        this(null);
    }

    public String getSlackUsername() {
        return slackUsername;
    }

    public void setSlackUsername(String slackUsername) {
        this.slackUsername = slackUsername;
    }

    @Extension
    @Symbol("slackUsername")
    public static class DescriptorImpl extends UserPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Slack";
        }

        @Override
        public UserProperty newInstance(User user) {
            return new SlackUserProperty();
        }
    }

    @Override
    public UserProperty reconfigure(StaplerRequest req, JSONObject form) throws Descriptor.FormException {
        req.bindJSON(this, form);
        return this;
    }
}
