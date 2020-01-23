package com.xatkit.plugins.slack.platform.action;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.users.UsersGetPresenceRequest;
import com.github.seratch.jslack.api.methods.response.users.UsersGetPresenceResponse;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.platform.SlackPlatform;

import java.io.IOException;

import static com.xatkit.plugins.slack.util.SlackUtils.logSlackApiResponse;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * Returns whether a given user in a given team is online.
 * <p>
 * This action accepts user ID, name, and real name.
 *
 * @see SlackPlatform#getUserId(String, String)
 */
public class IsOnline extends RuntimeAction<SlackPlatform> {

    /**
     * The name of the user to check.
     * <p>
     * This name can be either the user's ID, name, or real name.
     *
     * @see SlackPlatform#getUserId(String, String)
     */
    private String username;

    /**
     * The unique identifier of the Slack workspace containing the user to check.
     */
    private String teamId;

    /**
     * Constructs an {@link IsOnline} with the provided {@code runtimePlatform}, {@code session}, {@code username},
     * and {@code teamId}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param username        the name of the user to check
     * @param teamId          the unique identifier of the Slack workspace containing the user to check.
     */
    public IsOnline(SlackPlatform runtimePlatform, XatkitSession session, String username, String teamId) {
        super(runtimePlatform, session);
        checkNotNull(username, "Cannot build a %s action with the provided user %s", this.getClass().getSimpleName(),
                username);
        checkNotNull(teamId, "Cannot build a %s action with the provided team %s", this.getClass().getSimpleName(),
                teamId);
        this.username = username;
        this.teamId = teamId;
    }

    /**
     * Returns whether the given user is online.
     *
     * @return {@code true} if the user is online, {@code false} otherwise
     * @throws XatkitException if an error occurred when accessing the Slack API
     */
    @Override
    protected Object compute() {

        String userId = this.runtimePlatform.getUserId(teamId, username);

        UsersGetPresenceRequest request = UsersGetPresenceRequest.builder()
                .token(this.runtimePlatform.getSlackToken(teamId))
                .user(userId)
                .build();
        UsersGetPresenceResponse usersGetPresenceResponse;
        try {
            usersGetPresenceResponse = this.runtimePlatform.getSlack().methods().usersGetPresence(request);
        } catch (IOException | SlackApiException e) {
            throw new XatkitException("An error occurred when accessing the Slack API, see attached exception", e);
        }
        logSlackApiResponse(usersGetPresenceResponse);
        return usersGetPresenceResponse.getPresence().equals("active");
    }
}
