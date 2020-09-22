package com.xatkit.plugins.slack.platform.action;

import com.github.seratch.jslack.api.model.Attachment;
import com.xatkit.execution.StateContext;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import lombok.NonNull;

import java.util.List;

/**
 * Replies to a message by uploading {@code attachment}s using the input {@code teamId} workspace's channel.
 * <p>
 * This action relies on the provided {@link StateContext} to retrieve the Slack {@code teamId} and {@code channel}
 * associated to the user input.
 * <p>
 *
 * @see PostMessage
 */
public class ReplyAttachmentsMessage extends PostAttachmentsMessage {

    /**
     * Constructs a new {@link ReplyAttachmentsMessage} with the provided {@code platform}, {@code context},
     * and {@code attachments}.
     *
     * @param platform    the {@link SlackPlatform} containing this action
     * @param context     the {@link StateContext} associated to this action
     * @param attachments the {@link Attachment} list to post
     * @throws IllegalArgumentException if the text parameter of each entry of the provided {@code attachments} list
     *                                  is {@code null} or empty
     * @see Reply#getTeamId(StateContext)
     * @see Reply#getChannel(StateContext)
     * @see PostMessage#PostMessage(SlackPlatform, StateContext, String, String, String)
     */
    public ReplyAttachmentsMessage(@NonNull SlackPlatform platform, @NonNull StateContext context,
                                   @NonNull List<Attachment> attachments) {
        super(platform, context, attachments, Reply.getChannel(context), Reply.getTeamId(context));
    }

    /**
     * Constructs a new {@link ReplyAttachmentsMessage} with the provided {@code platform}, {@code context},
     * {@code pretext}, {@code title}, {@code text}, {@code attchColor}, and {@code timestamp}.
     *
     * @param platform   the {@link SlackPlatform} containing this action
     * @param context    the {@link StateContext} associated to this action
     * @param pretext    the pretext of the {@link Attachment} to post
     * @param title      the title of the {@link Attachment} to post
     * @param text       the text of the {@link Attachment} to post
     * @param attchColor the color of the {@link Attachment} to post in HEX
     *                   format
     * @param timestamp  the timestamp of the {@link Attachment} to post in
     *                   epoch format
     * @throws IllegalArgumentException if the provided {@code text} is {@code null} or empty
     * @see Reply#getTeamId(StateContext)
     * @see Reply#getChannel(StateContext)
     * @see PostMessage#PostMessage(SlackPlatform, StateContext, String, String, String)
     */
    public ReplyAttachmentsMessage(@NonNull SlackPlatform platform,
                                   @NonNull StateContext context,
                                   String pretext,
                                   String title,
                                   @NonNull String text,
                                   String attchColor,
                                   String timestamp) {
        super(platform, context, pretext, title, text, attchColor, timestamp, Reply.getChannel(context),
                Reply.getTeamId(context));
    }

    /**
     * Constructs a new {@link ReplyAttachmentsMessage} with the provided {@code platform}, {@code context},
     * {@code pretext}, {@code title}, {@code text}, and {@code attchColor}.
     *
     * @param platform   the {@link SlackPlatform} containing this action
     * @param context    the {@link StateContext} associated to this action
     * @param pretext    the pretext of the {@link Attachment} to post
     * @param title      the title of the {@link Attachment} to post
     * @param text       the text of the {@link Attachment} to post
     * @param attchColor the color of the {@link Attachment} to post in HEX format
     *                   be set to the actual time
     * @throws IllegalArgumentException if the provided {@code text} list is {@code null} or empty
     * @see Reply#getTeamId(StateContext)
     * @see Reply#getChannel(StateContext)
     * @see PostMessage#PostMessage(SlackPlatform, StateContext, String, String, String)
     */
    public ReplyAttachmentsMessage(@NonNull SlackPlatform platform,
                                   @NonNull StateContext context,
                                   String pretext,
                                   String title,
                                   @NonNull String text,
                                   String attchColor) {
        super(platform, context, pretext, title, text, attchColor, Reply.getChannel(context), Reply.getTeamId(context));
    }
}
