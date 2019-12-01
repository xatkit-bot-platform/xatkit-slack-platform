package com.xatkit.plugins.slack.platform.action;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.action.RuntimeMessageAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.MessageFormat;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * A {@link RuntimeAction} that posts a {@code message} to a given Slack {@code channel}.
 * <p>
 * This class relies on the {@link SlackPlatform}'s {@link com.github.seratch.jslack.Slack} client and Slack bot API
 * token to connect to the Slack API and post messages.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackPlatform} has been loaded with a valid Slack bot
 * API token in order to authenticate the bot and post messages.
 */
public class PostMessage extends RuntimeMessageAction<SlackPlatform> {

    /**
     * The Slack channel to post the message to.
     */
    protected String channel;

    /**
     * The timestamp of the thread to post the message to.
     */
    protected String threadTs;

    /**
     * Constructs a new {@link PostMessage} with the provided {@code runtimePlatform}, {@code session}, {@code
     * message} and {@code channel}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param message         the message to post
     * @param channel         the Slack channel to post the message to
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} or {@code channel} is {@code null} or empty.
     */
    public PostMessage(SlackPlatform runtimePlatform, XatkitSession session, String message, String channel) {
        this(runtimePlatform, session, message, channel, null);
    }

    /**
     * Constructs a new {@link PostMessage} with the provided {@code runtimePlatform}, {@code session}, {@code
     * message}, {@code channel}, and {@code threadTs}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param message         the message to post
     * @param channel         the Slack channel to post the message to
     * @param threadTs        the timestamp of the thread to post the message to
     */
    public PostMessage(SlackPlatform runtimePlatform, XatkitSession session, String message, String channel,
                       @Nullable String threadTs) {
        super(runtimePlatform, session, message);

        checkArgument(nonNull(channel) && !channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        /*
         * Do not check if threadTs is null, it is the case if the user input is not in a thread.
         */
        this.channel = channel;
        this.threadTs = threadTs;
    }

    /**
     * Posts the provided {@code message} to the given {@code channel} and {@code threadTs}.
     * <p>
     * <b>Note</b>: if {@code threadTs} is {@code null} this method posts the provided {@code message} in the
     * {@code channel} itself. If {@code threadTs} contains a value the provided {@code message} is posted as a reply
     * to the thread.
     * <p>
     * This method relies on the containing {@link SlackPlatform}'s Slack bot API token to authenticate the bot and
     * post the {@code message} to the given {@code channel}.
     *
     * @return {@code null}
     * @throws IOException     if an I/O error occurred when sending the message
     * @throws XatkitException if the provided token does not authenticate the bot
     */
    @Override
    public Object compute() throws IOException {
        ChatPostMessageRequest.ChatPostMessageRequestBuilder builder = ChatPostMessageRequest.builder();
        builder.token(runtimePlatform.getSlackToken())
                .channel(this.runtimePlatform.getChannelId(channel))
                .text(message)
                .unfurlLinks(true)
                .unfurlMedia(true);
        if (nonNull(threadTs) && !threadTs.isEmpty()) {
            builder.threadTs(threadTs);
        }
        ChatPostMessageRequest request = builder.build();
        try {
            ChatPostMessageResponse response = runtimePlatform.getSlack().methods().chatPostMessage(request);
            if (response.isOk()) {
                Log.trace("Request {0} successfully sent to the Slack API", request);
                return response.getTs();
            } else {
                Log.error("An error occurred when processing the request {0}: received response {1}", request,
                        response);
            }
        } catch (SlackApiException e) {
            throw new XatkitException(MessageFormat.format("Cannot send the message {0} to the Slack API", request), e);
        }
        return null;
    }

    @Override
    protected XatkitSession getClientSession() {
        return this.runtimePlatform.createSessionFromChannel(channel);
    }
}
