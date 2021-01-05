package com.xatkit.plugins.slack.platform.action;

import com.xatkit.execution.StateContext;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import lombok.NonNull;

import java.io.File;

/**
 * Replies to a message by uploading a {@code file} using the input {@code teamId} workspace's {@code channel}.
 * <p>
 * This action relies on the provided {@link StateContext} to retrieve the Slack {@code teamId} and {@code channel}
 * associated to the user input.
 * <p>
 *
 * @see PostFileMessage
 */
public class ReplyFileMessage extends PostFileMessage {

    /**
     * Constructs a new {@link ReplyFileMessage} with the provided {@code platform}, {@code context}, {@code
     * message}, and {@code file}.
     *
     * @param platform the {@link SlackPlatform} containing this action
     * @param context  the {@link StateContext} associated to this action
     * @param message  the message to associated to the uploaded {@link File}
     * @param file     the {@link File} to upload
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty, or if the provided
     *                                  {@code file} is {@code null} or does not exist
     * @see Reply#getTeamId(StateContext)
     * @see Reply#getChannel(StateContext)
     * @see PostFileMessage#PostFileMessage(SlackPlatform, StateContext, String, File, String, String)
     */
    public ReplyFileMessage(@NonNull SlackPlatform platform, @NonNull StateContext context, @NonNull String message,
                            @NonNull File file) {
        super(platform, context, message, file, Reply.getChannel(context), Reply.getTeamId(context));
    }

    /**
     * Contructs a new {@link ReplyFileMessage} with the provided {@code platform}, {@code context}, {@code title},
     * {@code message}, and {@code content}.
     *
     * @param platform the {@link SlackPlatform} containing this action
     * @param context  the {@link StateContext} associated to this action
     * @param title    the title of the file to upload
     * @param message  the message to associate to the uploaded file
     * @param content  the content of the file to upload
     * @throws IllegalArgumentException if the provided {@code title}, {@code message}, or {@code content} is {@code
     *                                  empty}
     * @throws NullPointerException     if one of the provided parameter is {@code null}
     *
     * @see PostFileMessage#PostFileMessage(SlackPlatform, StateContext, String, String, String, String, String)
     */
    public ReplyFileMessage(@NonNull SlackPlatform platform, @NonNull StateContext context, @NonNull String title,
                            @NonNull String message, @NonNull String content) {
        super(platform, context, title, message, content, Reply.getChannel(context), Reply.getTeamId(context));
    }
}
