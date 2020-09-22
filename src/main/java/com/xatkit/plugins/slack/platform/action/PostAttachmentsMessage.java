package com.xatkit.plugins.slack.platform.action;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.github.seratch.jslack.api.model.Attachment;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeArtifactAction;
import com.xatkit.execution.StateContext;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.xatkit.plugins.slack.util.SlackUtils.logSlackApiResponse;
import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * Posts {@code attachment}s to the Slack {@code channel} in the workspace identified by the provided {@code teamId}.
 */
public class PostAttachmentsMessage extends RuntimeArtifactAction<SlackPlatform> {

    /**
     * The unique identifier of the Slack workspace containing the channel to post the message to.
     */
    protected String teamId;

    /**
     * The Slack channel to post the attachments to.
     */
    protected String channel;

    /**
     * The attachments to post to Slack channel.
     */
    protected List<Attachment> attachments;

    /**
     * Constructs a {@link PostAttachmentsMessage} instance with the provided {@code runtimePlatform}, {@code session},
     * {@code attachments}, {@code channel}, and {@code teamId}.
     *
     * @param platform    the {@link SlackPlatform} containing this action
     * @param context     the {@link StateContext} associated to this action
     * @param attachments the {@link Attachment} list to post
     * @param channel     the Slack channel to post the attachments to
     * @param teamId      the unique identifier of the Slack workspace containing the channel to post the
     *                    attachment to
     * @throws IllegalArgumentException if the text parameter of any entry of the provided {@code attachments} list
     *                                  is {@code null} or empty, or if the provided {@code channel} or {@code teamId
     *                                  } is {@code null} or empty
     * @see PostMessage#PostMessage(SlackPlatform, StateContext, String, String, String)
     */
    public PostAttachmentsMessage(@NonNull SlackPlatform platform, @NonNull StateContext context,
                                  @NonNull List<Attachment> attachments,
                                  @NonNull String channel, @NonNull String teamId) {
        super(platform, context);
        checkArgument(!teamId.isEmpty(), "Cannot construct a %s action with the provided team %s, " +
                "expected a non-null and not empty String", this.getClass().getSimpleName(), teamId);
        this.teamId = teamId;
        checkArgument(!channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;
        for (Attachment attch : attachments) {
            checkArgument(nonNull(attch.getText()) && !attch.getText().isEmpty(), "Cannot construct a %s action with " +
                            "the provided text %s, expected a non-null and not empty String for the attachment text",
                    this.getClass().getSimpleName(), attch.getText());
        }
        this.attachments = attachments;
    }

    /**
     * Constructs a {@link PostAttachmentsMessage} instance with the provided {@code runtimePlatform}, {@code session},
     * {@code pretext}, {@code title}, {@code text}, {@code attchColor}, {@code timestamp}, {@code channel}, and
     * {@code teamId}.
     *
     * @param platform   the {@link SlackPlatform} containing this action
     * @param context    the {@link StateContext} associated to this action
     * @param pretext    the pretext of the {@link Attachment} to post
     * @param title      the title of the {@link Attachment} to post
     * @param text       the text of the {@link Attachment} to post
     * @param attchColor the color of the {@link Attachment} to post in HEX format
     * @param timestamp  the timestamp of the {@link Attachment} to post in epoch format
     * @param channel    the Slack channel to post the attachments to
     * @param teamId     the unique identifier of the Slack workspace containing the channel to post the
     *                   attachment to
     * @throws IllegalArgumentException if the provided {@code text} list is {@code null} or empty, or if the
     *                                  provided {@code channel} or {@code teamId} is {@code null} or empty
     * @see PostMessage#PostMessage(SlackPlatform, StateContext, String, String, String)
     */
    public PostAttachmentsMessage(@NonNull SlackPlatform platform,
                                  @NonNull StateContext context,
                                  String pretext,
                                  String title,
                                  @NonNull String text,
                                  String attchColor,
                                  String timestamp,
                                  @NonNull String channel,
                                  @NonNull String teamId) {
        super(platform, context);
        checkArgument(!teamId.isEmpty(), "Cannot construct a %s action with the provided team %s, " +
                "expected a non-null and not empty String", this.getClass().getSimpleName(), teamId);
        this.teamId = teamId;

        checkArgument(!channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;
        checkArgument(!text.isEmpty(), "Cannot construct a %s action with the provided text %s, " +
                        "expected a non-null and not empty String for the attachment text",
                this.getClass().getSimpleName(), text);
        Attachment attachment = createAttachment(pretext, title, text, attchColor, timestamp);
        this.attachments = new ArrayList<>();
        this.attachments.add(attachment);
    }

    /**
     * Constructs a {@link PostAttachmentsMessage} with the provided {@code runtimePlatform}, {@code session},
     * {@code pretext}, {@code title}, {@code text}, {@code attchColor}, {@code channel}, and {@code teamId}.
     *
     * @param platform   the {@link SlackPlatform} containing this action
     * @param context    the {@link StateContext} associated to this action
     * @param pretext    the pretext of the {@link Attachment} to post
     * @param title      the title of the {@link Attachment} to post
     * @param text       the text of the {@link Attachment} to post
     * @param attchColor the color of the {@link Attachment} to post in HEX format
     * @param channel    the Slack channel to post the attachment to
     * @param teamId     the unique identifier of the Slack workspace containing the channel to post the
     *                   attachment to
     * @throws IllegalArgumentException if the provided {@code text} list is {@code null} or empty, or if the
     *                                  provided {@code channel} or {@code teamId} is {@code null} or empty
     * @see PostMessage#PostMessage(SlackPlatform, StateContext, String, String, String)
     */
    public PostAttachmentsMessage(@NonNull SlackPlatform platform,
                                  @NonNull StateContext context,
                                  String pretext,
                                  String title,
                                  @NonNull String text,
                                  String attchColor,
                                  @NonNull String channel,
                                  @NonNull String teamId) {
        super(platform, context);
        checkArgument(!teamId.isEmpty(), "Cannot construct a %s action with the provided team %s, " +
                "expected a non-null and not empty String", this.getClass().getSimpleName(), teamId);
        this.teamId = teamId;

        checkArgument(!channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;

        checkArgument(!text.isEmpty(), "Cannot construct a %s action with the provided text %s, " +
                        "expected a non-null and not empty String for the attachment text",
                this.getClass().getSimpleName(), text);

        String timestamp = String.valueOf(Calendar.getInstance().getTime().getTime() / 1000);
        Attachment attachment = createAttachment(pretext, title, text, attchColor, timestamp);
        this.attachments = new ArrayList<>();
        this.attachments.add(attachment);
    }

    /**
     * Posts the provided {@code attachments} to the {@code teamId} workspace's {@code channel}.
     * <p>
     * This method relies on the containing {@link SlackPlatform}'s Slack bot API token to authenticate the bot and
     * post the {@code attachments} to the given {@code channel}.
     *
     * @return {@code null}
     * @throws IOException     if an I/O error occurred when sending the message
     * @throws XatkitException if the provided token does not authenticate the bot
     */
    @Override
    public Object compute() throws IOException {
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .token(runtimePlatform.getSlackToken(teamId))
                .channel(this.runtimePlatform.getChannelId(teamId, channel))
                .attachments(attachments)
                .unfurlLinks(true)
                .unfurlMedia(true)
                .build();
        try {
            ChatPostMessageResponse response = runtimePlatform.getSlack().methods().chatPostMessage(request);
            logSlackApiResponse(response);
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

    /**
     * Creates a new {@link Attachment} with the provided {@code pretext}, {@code title}, {@code text}, {@code
     * attchColor}, {@code timestamp}.
     *
     * @param pretext   the pretext of the {@link Attachment} to post
     * @param title     the title of the {@link Attachment} to post
     * @param text      the text of the {@link Attachment} to post
     * @param color     the color of the {@link Attachment} to post in HEX format
     * @param timestamp the timestamp of the {@link Attachment} to post in epoch
     *                  format
     */
    private Attachment createAttachment(String pretext, String title, String text, String color, String timestamp) {
        Attachment.AttachmentBuilder attachmentBuilder = Attachment.builder();
        attachmentBuilder.pretext(pretext);
        attachmentBuilder.title(title);
        attachmentBuilder.text(text);
        attachmentBuilder.color(color);
        attachmentBuilder.ts(timestamp);

        return attachmentBuilder.build();
    }

    @Override
    protected StateContext getClientStateContext() {
        return this.runtimePlatform.createSessionFromChannel(teamId, channel);
    }
}
