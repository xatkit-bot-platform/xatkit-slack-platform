package com.xatkit.plugins.slack.platform.action;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.EventInstance;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import lombok.NonNull;

import javax.annotation.Nullable;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * Replies to a message using the input {@code teamId} workspace's {@code channel}.
 * <p>
 * This action relies on the provided {@link StateContext} to retrieve the Slack {@code teamId} and {@code channel}
 * associated to the user input.
 *
 * @see PostMessage
 */
public class Reply extends PostMessage {

    /**
     * Returns the Slack channel associated to the user input.
     * <p>
     * This method searches for the value stored with the {@link ChatUtils#CHAT_CHANNEL_CONTEXT_KEY} key in the
     * platform data of the current {@link EventInstance}.
     *
     * @param context the {@link StateContext} to retrieve the Slack channel from
     * @return the Slack channel associated to the user input
     * @throws NullPointerException     if the provided {@code context} is {@code null}, or if it does not contain the
     *                                  channel information
     * @throws IllegalArgumentException if the retrieved channel is not a {@link String}
     * @see StateContext#getEventInstance()
     * @see EventInstance#getPlatformData()
     * @see ChatUtils#CHAT_CHANNEL_CONTEXT_KEY
     */
    public static String getChannel(@NonNull StateContext context) {
        Object channelValue = context.getEventInstance().getPlatformData().get(ChatUtils.CHAT_CHANNEL_CONTEXT_KEY);
        checkNotNull(channelValue, "Cannot retrieve the Slack channel from the context");
        checkArgument(channelValue instanceof String, "Invalid Slack channel type, expected %s, found %s", String
                .class.getSimpleName(), channelValue.getClass().getSimpleName());
        return (String) channelValue;
    }


    /**
     * Returns the Slack team identifier associated to the user input.
     * <p>
     * This method searches for the value stored with the {@link SlackUtils#SLACK_TEAM_CONTEXT_KEY} key in the
     * platform data of the current {@link EventInstance}.
     *
     * @param context the {@link StateContext} to retrieve the team identifier from
     * @return the team identifier associated to the user input
     * @throws NullPointerException     if the provided {@code context} is {@code null}, or if it does not contain the
     *                                  team identifier information
     * @throws IllegalArgumentException if the retrieve team identifier is not a {@link String}
     * @see SlackUtils#SLACK_TEAM_CONTEXT_KEY
     * @see StateContext#getEventInstance()
     * @see EventInstance#getPlatformData()
     */
    public static String getTeamId(@NonNull StateContext context) {
        Object teamValue = context.getEventInstance().getPlatformData().get(SlackUtils.SLACK_TEAM_CONTEXT_KEY);
        checkNotNull(teamValue, "Cannot retrieve the Slack team from the context");
        checkArgument(teamValue instanceof String, "Invalid Slack team type, expected %s, found %s",
                String.class.getSimpleName(), teamValue.getClass().getSimpleName());
        return (String) teamValue;
    }

    /**
     * Returns the threadTs timestamp associated to the user input.
     * <p>
     * This method searches for the value stored with the {@link SlackUtils#SLACK_THREAD_TS} key in the platform data
     * of the current {@link EventInstance}.
     *
     * @param context the {@link StateContext} to retrieve the threadTs timestamp from
     * @return the threadTs timestamp if it exist, or {@code null} / an empty {@link String} if it doesn't (depending
     * on the NLP provider)
     * @throws NullPointerException if the provided {@code context} is {@code null}
     * @see SlackUtils#SLACK_THREAD_TS
     * @see StateContext#getEventInstance()
     * @see EventInstance#getPlatformData()
     */
    public static @Nullable
    String getThreadTs(@NonNull StateContext context) {
        return (String) context.getEventInstance().getPlatformData().get(SlackUtils.SLACK_THREAD_TS);
    }

    /**
     * Constructs a new {@link Reply} with the provided {@code runtimePlatform}, {@code session}, and {@code message}.
     *
     * @param platform the {@link SlackPlatform} containing this action
     * @param context         the {@link StateContext} associated to this action
     * @param message         the message to post
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty
     * @see #getChannel(StateContext)
     * @see PostMessage#PostMessage(SlackPlatform, StateContext, String, String, String)
     */
    public Reply(@NonNull SlackPlatform platform, @NonNull StateContext context, @NonNull String message) {
        super(platform, context, message, getChannel(context), getTeamId(context), getThreadTs(context));
    }
}
