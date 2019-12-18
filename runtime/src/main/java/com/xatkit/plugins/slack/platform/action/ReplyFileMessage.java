package com.xatkit.plugins.slack.platform.action;

import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.platform.SlackPlatform;

import java.io.File;

/**
 * Replies to a message by uploading a {@code file} using the input {@code teamId} workspace's {@code channel}.
 * <p>
 * This action relies on the provided {@link XatkitSession} to retrieve the Slack {@code teamId} and {@code channel}
 * associated to the user input.
 * <p>
 *
 * @see PostFileMessage
 */
public class ReplyFileMessage extends PostFileMessage {

    /**
     * Constructs a new {@link ReplyFileMessage} with the provided {@code runtimePlatform}, {@code session}, {@code
     * message}, and {@code file}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param message         the message to associated to the uploaded {@link File}
     * @param file            the {@link File} to upload
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is
     *                                  {@code null}
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty, or if the provided
     *                                  {@code file} is {@code null} or does not exist
     * @see Reply#getTeamId(RuntimeContexts) 
     * @see Reply#getChannel(RuntimeContexts)
     * @see PostFileMessage#PostFileMessage(SlackPlatform, XatkitSession, String, File, String, String)
     */
    public ReplyFileMessage(SlackPlatform runtimePlatform, XatkitSession session, String message, File file) {
        super(runtimePlatform, session, message, file, Reply.getChannel(session.getRuntimeContexts()),
                Reply.getTeamId(session.getRuntimeContexts()));
    }
}
