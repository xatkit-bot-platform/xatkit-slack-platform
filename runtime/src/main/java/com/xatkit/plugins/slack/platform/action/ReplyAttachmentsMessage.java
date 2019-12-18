package com.xatkit.plugins.slack.platform.action;

import com.github.seratch.jslack.api.model.Attachment;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.platform.SlackPlatform;

import java.util.List;

/**
 * Replies to a message by uploading {@code attachment}s using the input {@code teamId} workspace's channel.
 * <p>
 * This action relies on the provided {@link XatkitSession} to retrieve the Slack {@code teamId} and {@code channel}
 * associated to the user input.
 * <p>
 *
 * @see PostMessage
 */
public class ReplyAttachmentsMessage extends PostAttachmentsMessage {

    /**
     * Constructs a new {@link ReplyAttachmentsMessage} with the provided {@code runtimePlatform}, {@code session},
     * and {@code attachments}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param attachments     the {@link Attachment} list to post
     * @throws NullPointerException     if the provided {@code runtimePlatform} or
     *                                  {@code session} is {@code null}
     * @throws IllegalArgumentException if the text parameter of each entry of the
     *                                  provided {@code attachments} list is
     *                                  {@code null} or empty
     * @see Reply#getTeamId(RuntimeContexts)
     * @see Reply#getChannel(RuntimeContexts)
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String, String)
     */
    public ReplyAttachmentsMessage(SlackPlatform runtimePlatform, XatkitSession session, List<Attachment> attachments) {
        super(runtimePlatform, session, attachments, Reply.getChannel(session.getRuntimeContexts()),
                Reply.getTeamId(session.getRuntimeContexts()));
    }

    /**
     * Constructs a new {@link ReplyAttachmentsMessage} with the provided {@code runtimePlatform}, {@code session},
     * {@code pretext}, {@code title}, {@code text}, {@code attchColor}, and {@code timestamp}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param pretext         the pretext of the {@link Attachment} to post
     * @param title           the title of the {@link Attachment} to post
     * @param text            the text of the {@link Attachment} to post
     * @param attchColor      the color of the {@link Attachment} to post in HEX
     *                        format
     * @param timestamp       the timestamp of the {@link Attachment} to post in
     *                        epoch format
     * @throws NullPointerException     if the provided {@code runtimePlatform} or
     *                                  {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code text} list is
     *                                  {@code null} or empty
     * @see Reply#getTeamId(RuntimeContexts)
     * @see Reply#getChannel(RuntimeContexts)
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String, String)
     */
    public ReplyAttachmentsMessage(SlackPlatform runtimePlatform, XatkitSession session, String pretext, String title,
                                   String text, String attchColor, String timestamp) {
        super(runtimePlatform, session, pretext, title, text, attchColor, timestamp,
                Reply.getChannel(session.getRuntimeContexts()), Reply.getTeamId(session.getRuntimeContexts()));
    }

    /**
     * Constructs a new {@link ReplyAttachmentsMessage} with the provided {@code runtimePlatform}, {@code session},
	 * {@code pretext}, {@code title}, {@code text}, and {@code attchColor}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param pretext         the pretext of the {@link Attachment} to post
     * @param title           the title of the {@link Attachment} to post
     * @param text            the text of the {@link Attachment} to post
     * @param attchColor      the color of the {@link Attachment} to post in HEX format
     *                        be set to the actual time
     * @throws NullPointerException     if the provided {@code runtimePlatform} or
     *                                  {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code text} list is
     *                                  {@code null} or empty
     * @see Reply#getTeamId(RuntimeContexts)
	 * @see Reply#getChannel(RuntimeContexts)
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String, String)
     */
    public ReplyAttachmentsMessage(SlackPlatform runtimePlatform, XatkitSession session, String pretext, String title,
                                   String text, String attchColor) {
        super(runtimePlatform, session, pretext, title, text, attchColor,
                Reply.getChannel(session.getRuntimeContexts()), Reply.getTeamId(session.getRuntimeContexts()));
    }
}
