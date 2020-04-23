package com.xatkit.plugins.slack.platform;

import com.xatkit.test.util.VariableLoaderHelper;

/**
 * An abstract test for the Slack Platform.
 * <p>
 * This class provides utility methods to load the Slack token stored in the {@code test-variables.properties} file,
 * and can be extended by all the Slack tests to easily construct {@link SlackPlatform} instances.
 */
public class SlackTestUtils {

    /**
     * The {@code test-variables.properties} key used to retrieve the Slack token to use.
     */
    private static String XATKIT_SLACK_TOKEN_KEY = "XATKIT_SLACK_TOKEN";

    /**
     * Retrieves the Slack token stored in the {@code test-variables.properties} file.
     * @return the Slack token if it exists, {@code null} otherwise
     */
    public static String getSlackToken() {
        return VariableLoaderHelper.getVariable(XATKIT_SLACK_TOKEN_KEY);
    }
}
