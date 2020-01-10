package com.xatkit.plugins.slack.platform.action;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.action.RuntimeArtifactAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.platform.SlackPlatform;

import fr.inria.atlanmod.commons.log.Log;

/**
 * A {@link RuntimeAction} that posts the {@code layoutBlocks} list to a given Slack {@code channel}.
 * <p>
 * This class relies on the {@link SlackPlatform}'s {@link com.github.seratch.jslack.Slack} client and Slack bot API
 * token to connect to the Slack API and post messages.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link SlackPlatform} has been loaded with a valid Slack bot
 * API token in order to authenticate the bot and post messages.
 */
public class PostLayoutBlocksMessage extends RuntimeArtifactAction<SlackPlatform> {
    
    /**
     * The Slack channel to post the layout blocks to.
     */
    protected String channel;

    /**
     * The layout blocks to post to Slack channel.
     */
    protected List<LayoutBlock> layoutBlocks;

    /**
     * Constructs a new {@link PostLayoutBlocksMessage} with the provided {@code runtimePlatform}, {@code session},
     * {@code layoutBlocks} and {@code channel}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param layoutBlocks    the {@link LayoutBlock} list to post
     * @param channel         the Slack channel to post the layout blocks to
     * @throws NullPointerException if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String)
     */
    public PostLayoutBlocksMessage(SlackPlatform runtimePlatform, XatkitSession session, List<LayoutBlock> layoutBlocks,
            String channel) {
        super(runtimePlatform, session);

        checkArgument(nonNull(channel) && !channel.isEmpty(), "Cannot construct a %s action with the provided channel"
                + " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;
        this.layoutBlocks = layoutBlocks;
    }

    /**
     * Posts the provided {@code layoutBlocks} to the given {@code channel}.
     * <p>
     * This method relies on the containing {@link SlackPlatform}'s Slack bot API token to authenticate the bot and post
     * the {@code layoutBlocks} to the given {@code channel}.
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
                .blocks(layoutBlocks)
                .unfurlLinks(true)
                .unfurlMedia(true);
        ChatPostMessageRequest request = builder.build();
        try {
            ChatPostMessageResponse response = runtimePlatform.getSlack().methods().chatPostMessage(request);
            if (response.isOk()) {
                Log.trace("Request {0} successfully sent to the Slack API", request);
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
