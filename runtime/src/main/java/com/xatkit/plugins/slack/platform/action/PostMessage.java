package com.xatkit.plugins.slack.platform.action;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeMessageAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.StateContext;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.MessageFormat;

import static com.xatkit.plugins.slack.util.SlackUtils.logSlackApiResponse;
import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * Posts a {@code message} to the Slack {@code channel} in the workspace identified by the provided {@code teamId}.
 */
public class PostMessage extends RuntimeMessageAction<SlackPlatform> {

    /**
     * The unique identifier of the Slack workspace containing the channel to post the message to.
     */
    protected String teamId;

    /**
     * The Slack channel to post the message to.
     */
    protected String channel;

    /**
     * The timestamp of the thread to post the message to.
     */
    protected String threadTs;

    /**
     * Constructs a {@link PostMessage} instance with the provided {@code runtimePlatform}, {@code session}, {@code
     * message}, {@code channel}, and {@code teamId}.
     *
     * @param platform the {@link SlackPlatform} containing this action
     * @param context  the {@link StateContext} associated to this action
     * @param message  the message to post
     * @param channel  the Slack channel to post the message to
     * @param teamId   the unique identifier of the Slack workspace containing the channel to post the message to
     * @throws IllegalArgumentException if the provided {@code message}, {@code channel}, or {@code teamId} is {@code
     *                                  null} or empty.
     */
    public PostMessage(@NonNull SlackPlatform platform, @NonNull StateContext context, @NonNull String message,
                       @NonNull String channel, @NonNull String teamId) {
        this(platform, context, message, channel, teamId, null);
    }

    /**
     * Constructs a new {@link PostMessage} with the provided {@code runtimePlatform}, {@code session}, {@code
     * message}, {@code channel}, {@code teamId}, and {@code threadTs}.
     *
     * @param platform the {@link SlackPlatform} containing this action
     * @param context  the {@link StateContext} associated to this action
     * @param message  the message to post
     * @param channel  the Slack channel to post the message to
     * @param teamId   the unique identifier of the Slack workspace containing the channel to post the message to
     * @param threadTs the timestamp of the thread to post the message to
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message}, {@code channel}, or {@code teamId} is {@code
     *                                  null} or empty.
     */
    public PostMessage(@NonNull SlackPlatform platform, @NonNull StateContext context, @NonNull String message,
                       @NonNull String channel, @NonNull String teamId, @Nullable String threadTs) {
        super(platform, context, message);
        checkArgument(!teamId.isEmpty(), "Cannot construct a %s action with the provided teamId " +
                "%s, expected a non-null and not empty String", this.getClass().getSimpleName(), teamId);
        this.teamId = teamId;
        checkArgument(!channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;
        /*
         * Do not check if threadTs is null, it is the case if the user input is not in a thread.
         */
        this.threadTs = threadTs;
    }

    /**
     * Posts the provided {@code message} to the {@code teamId} workspace's {@code channel} with the given {@code
     * threadTs}.
     * <p>
     * <b>Note</b>: if {@code threadTs} is {@code null} this method posts the provided {@code message} in the
     * {@code channel} itself. If {@code threadTs} contains a value the provided {@code message} is posted as a reply
     * to the thread.
     * <p>
     *
     * @return the {@code timestamp} of the posted message
     * @throws XatkitException if an error occurred when sending the message
     */
    @Override
    public Object compute() {
        ChatPostMessageRequest.ChatPostMessageRequestBuilder builder = ChatPostMessageRequest.builder();
        builder.token(runtimePlatform.getSlackToken(teamId))
                .channel(this.runtimePlatform.getChannelId(teamId, channel))
                .text(message)
                .unfurlLinks(true)
                .unfurlMedia(true);
        if (nonNull(threadTs) && !threadTs.isEmpty()) {
            builder.threadTs(threadTs);
        }
        ChatPostMessageRequest request = builder.build();
        try {
            ChatPostMessageResponse response = runtimePlatform.getSlack().methods().chatPostMessage(request);
            logSlackApiResponse(response);
            if (response.isOk()) {
                Log.trace("Request {0} successfully sent to the Slack API", request);
                return response.getTs();
            } else {
                throw new XatkitException(MessageFormat.format("An error occurred when processing the request {0}: " +
                        "received response {1}", request, response));
            }
        } catch (SlackApiException | IOException e) {
            throw new XatkitException(MessageFormat.format("Cannot send the message {0} to the Slack API", request), e);
        }
    }

    @Override
    protected XatkitSession getClientSession() {
        return this.runtimePlatform.createSessionFromChannel(teamId, channel);
    }
}
