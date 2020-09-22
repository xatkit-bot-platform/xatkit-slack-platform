package com.xatkit.plugins.slack.util;

import com.github.seratch.jslack.api.methods.SlackApiResponse;
import fr.inria.atlanmod.commons.log.Log;

/**
 * Utility methods to handle {@link com.github.seratch.jslack.Slack} API calls and responses.
 */
public class SlackUtils {

    /**
     * Logs the potential Slack API error contained in the provided {@code response}.
     * <p>
     * This method should be called after every {@code slack.methods().[...]} call in order to properly log potential
     * error messages. Client code should follow the pattern:
     * <pre>
     * {@code
     * SlackApiResponse response = slack.methods().<ApiMethod>(<args>)
     * SlackUtils.logSlackApiResponse(response)
     * if(response.isOk) {
     *     // handle the response
     * } else {
     *     // handle the error
     * }
     * </pre>
     * <p>
     *
     * @param response the {@link SlackApiResponse} to log the error from
     */
    public static void logSlackApiResponse(SlackApiResponse response) {
        if (!response.isOk()) {
            if (response.getError().equals("missing_scope")) {
                Log.error("Missing OAuth scope: provided [{0}], required [{1}]", response.getProvided(),
                        response.getNeeded());
            } else {
                Log.error("The Slack API returns the following error: {0}", response.getError());
            }
        }
    }
}
