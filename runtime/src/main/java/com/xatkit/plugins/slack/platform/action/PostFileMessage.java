package com.xatkit.plugins.slack.platform.action;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.files.FilesUploadRequest;
import com.github.seratch.jslack.api.methods.response.files.FilesUploadResponse;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.action.RuntimeArtifactAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.StateContext;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;

import static com.xatkit.plugins.slack.util.SlackUtils.logSlackApiResponse;
import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

/**
 * Uploads a {@code file} when an associated {@code message} to the Slack channel in the workspace identified by the
 * provided {@code teamId}.
 *
 * @see PostMessage
 */
public class PostFileMessage extends RuntimeArtifactAction<SlackPlatform> {

    /**
     * The unique identifier of the Slack workspace containing the channel to post the file to.
     */
    protected String teamId;

    /**
     * The Slack channel to post the attachments to.
     */
    protected String channel;

    /**
     * The {@link File} to upload to the given Slack {@code channel}.
     * <p>
     * If this field is {@code null} the class should hold a valid {@code content} value.
     */
    private File file;

    /**
     * The title of the file to upload to the given Slack {@code channel}.
     */
    private String title;

    /**
     * The content of the file to upload to the given Slack {@code channel}.
     * <p>
     * If this field is {@code null} the class should hold a valid {@code file} value.
     */
    private String content;

    /**
     * The initial comment associated to the file to upload to the given Slack {@code channel}.
     */
    private String message;

    /**
     * Constructs {@link PostFileMessage} instance with the provided {@code runtimePlatform}, {@code session}, {@code
     * message}, {@code file}, {@code channel}, and {@code teamId}.
     * <p>
     * This constructor builds a {@link PostFileMessage} action that uploads the provided {@code file} to the given
     * Slack {@code channel}. To upload a {@link String} as a file see
     * {@link #PostFileMessage(SlackPlatform, StateContext, String, String, String, String, String)}.
     *
     * @param platform the {@link SlackPlatform} containing this action
     * @param context         the {@link XatkitSession} associated to this action
     * @param message         the message to associate to the uploaded {@link File}
     * @param file            the file to upload
     * @param channel         the Slack channel to upload the {@link File} to
     * @param teamId          the unique identifier of the Slack workspace containing the channel to post the message to
     * @throws IllegalArgumentException if the provided {@code message}, {@code channel}, or {@code teamId} is {@code
     *                                  null} or empty, or if the provided {@code file} is {@code null} or does not
     *                                  exist
     * @see #PostFileMessage(SlackPlatform, StateContext, String, String, String, String, String)
     */
    public PostFileMessage(@NonNull SlackPlatform platform, @NonNull StateContext context, @NonNull String message,
                           @NonNull File file, @NonNull String channel, @NonNull String teamId) {
        super(platform, context);

        checkArgument(!teamId.isEmpty(), "Cannot construct a %s action with the provided team %s, " +
                "expected a non-null and not empty String", this.getClass().getSimpleName(), teamId);
        this.teamId = teamId;

        checkArgument(!channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;

        checkArgument(file.exists(), "Cannot construct a %s action with the provided file %s, " +
                "expected a non-null and existing file", this.getClass().getSimpleName(), file);
        this.file = file;
        this.message = message;
    }

    /**
     * Constructs a {@link PostFileMessage} instance with the provided {@code runtimePlatform}, {@code session}, {@code
     * title}, {@code message}, {@code content}, {@code channel}, and {@code teamId}.
     * <p>
     * This constructor builds a {@link PostFileMessage} action that uploads the provided {@code content} as a file
     * to the given Slack {@code channel}. To upload an existing {@link File} see
     * {@link #PostFileMessage(SlackPlatform, StateContext, String, File, String, String)}.
     *
     * @param platform the {@link SlackPlatform} containing this action
     * @param context         the {@link StateContext} associated to this action
     * @param title           the title of the file to upload
     * @param message         the message to associate to the uploaded {@link File}
     * @param content         the content of the file to upload
     * @param channel         the Slack channel to upload the {@link File} to
     * @param teamId          the unique identifier of the Slack workspace containing the channel to post the message to
     * @throws IllegalArgumentException if the provided {@code title}, {@code message}, {@code content}, {@code
     *                                  channel}, or {@code teamId} is {@code null} or empty.
     * @see #PostFileMessage(SlackPlatform, StateContext, String, File, String, String)
     */
    public PostFileMessage(@NonNull SlackPlatform platform, @NonNull StateContext context, @NonNull String title,
                           @NonNull String message, @NonNull String content, @NonNull String channel,
                           @NonNull String teamId) {
        super(platform, context);
        checkArgument(!teamId.isEmpty(), "Cannot construct a %s action with the provided team %s, " +
                "expected a non-null and not empty String", this.getClass().getSimpleName(), teamId);
        this.teamId = teamId;

        checkArgument(!channel.isEmpty(), "Cannot construct a %s action with the provided channel" +
                " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), channel);
        this.channel = channel;

        checkArgument(!title.isEmpty(), "Cannot construct a %s action with the provided title %s, "
                + "expected a non-null and not empty String", this.getClass().getSimpleName(), title);
        this.title = title;

        checkArgument(!content.isEmpty(), "Cannot construct a %s action with the provided content"
                + " %s, expected a non-null and not empty String", this.getClass().getSimpleName(), content);
        this.content = content;
        this.message = message;
    }

    /**
     * Uploads the provided {@code file} and post it with the associated {@code message} to the {@code teamId}
     * workspace's {@code channel}.
     * <p>
     * This method relies on the containing {@link SlackPlatform}'s Slack bot API token to authenticate the bot and
     * upload the {@code file} to the given {@code channel}.
     *
     * @return {@code null}
     */
    @Override
    public Object compute() {
        FilesUploadRequest.FilesUploadRequestBuilder builder = FilesUploadRequest.builder();
        builder.token(runtimePlatform.getSlackToken(teamId))
                .channels(Arrays.asList(this.runtimePlatform.getChannelId(teamId, channel)));
        if (nonNull(message) && !message.isEmpty()) {
            /*
             * Uploading the initial comment
             */
            builder.initialComment(message);
        }
        if (nonNull(file)) {
            /*
             * Uploading an existing file
             */
            builder.title(file.getName()).file(file).filename(file.getName());
        } else {
            /*
             * Uploading a String content as a file
             */
            builder.title(title).content(content).filename(title);
        }
        FilesUploadRequest request = builder.build();
        try {
            FilesUploadResponse response = runtimePlatform.getSlack().methods().filesUpload(request);
            logSlackApiResponse(response);
            if (response.isOk()) {
                Log.trace("Request {0} successfully sent to the Slack API", request);
            } else {
                Log.error("An error occurred when processing the request {0}: received response {1}", request,
                        response);
            }
        } catch (IOException | SlackApiException e) {
            throw new XatkitException(MessageFormat.format("Cannot send the message {0} to the Slack API", request), e);
        }
        return null;
    }

    @Override
    protected XatkitSession getClientSession() {
        return this.runtimePlatform.createSessionFromChannel(teamId, channel);
    }
}
