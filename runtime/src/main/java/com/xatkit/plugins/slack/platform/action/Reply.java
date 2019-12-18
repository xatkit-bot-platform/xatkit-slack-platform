package com.xatkit.plugins.slack.platform.action;

import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.plugins.slack.platform.SlackPlatform;

import javax.annotation.Nullable;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * Replies to a message using the input {@code teamId} workspace's {@code channel}.
 * <p>
 * This action relies on the provided {@link XatkitSession} to retrieve the Slack {@code teamId} and {@code channel}
 * associated to the user input.
 *
 * @see PostMessage
 */
public class Reply extends PostMessage {

    /**
     * Returns the Slack channel associated to the user input.
     * <p>
     * This method searches in the provided {@link RuntimeContexts} for the value stored with the key
     * {@link SlackUtils#SLACK_CONTEXT_KEY}.{@link SlackUtils#CHAT_CHANNEL_CONTEXT_KEY}. Note that if
     * the provided {@link RuntimeContexts} does not contain the requested value a {@link NullPointerException} is
     * thrown.
     *
     * @param context the {@link RuntimeContexts} to retrieve the Slack channel from
     * @return the Slack channel associated to the user input
     * @throws NullPointerException     if the provided {@code context} is {@code null}, or if it does not contain the
     *                                  channel information
     * @throws IllegalArgumentException if the retrieved channel is not a {@link String}
     * @see SlackUtils
     */
    public static String getChannel(RuntimeContexts context) {
        checkNotNull(context, "Cannot retrieve the channel from the provided %s %s", RuntimeContexts.class
                .getSimpleName(), context);
        Object channelValue = context.getContextValue(SlackUtils.SLACK_CONTEXT_KEY, SlackUtils
                .CHAT_CHANNEL_CONTEXT_KEY);
        checkNotNull(channelValue, "Cannot retrieve the Slack channel from the context");
        checkArgument(channelValue instanceof String, "Invalid Slack channel type, expected %s, found %s", String
                .class.getSimpleName(), channelValue.getClass().getSimpleName());
        return (String) channelValue;
    }


    /**
     * Returns the Slack team identifier associated to the user input.
     * <p>
     * This method searches in the provided {@link RuntimeContexts} for the value stored with the key
     * {@link SlackUtils#SLACK_CONTEXT_KEY}.{@link SlackUtils#SLACK_TEAM_CONTEXT_KEY}. Note that if the provided
     * {@link RuntimeContexts} does not contain the requested value a {@link NullPointerException} is thrown.
     *
     * @param context the {@link RuntimeContexts} to retrieve the team identifier from
     * @return the team identifier associated to the user input
     * @throws NullPointerException     if the provided {@code context} is {@code null}, or if it does not contain the
     *                                  team identifier information
     * @throws IllegalArgumentException if the retrieve team identifier is not a {@link String}
     * @see SlackUtils
     */
    public static String getTeamId(RuntimeContexts context) {
        checkNotNull(context, "Cannot retrieve the team from the provided %s %s",
                RuntimeContexts.class.getSimpleName(), context);
        Object teamValue = context.getContextValue(SlackUtils.SLACK_CONTEXT_KEY, SlackUtils.SLACK_TEAM_CONTEXT_KEY);
        checkNotNull(teamValue, "Cannot retrieve the Slack team from the context");
        checkArgument(teamValue instanceof String, "Invalid Slack team type, expected %s, found %s",
                String.class.getSimpleName(), teamValue.getClass().getSimpleName());
        return (String) teamValue;
    }

    /**
     * Returns the threadTs timestamp associated to the user input.
     * <p>
     * This method searches in the provided {@link RuntimeContexts} for the value stored with the key
     * {@link SlackUtils#SLACK_CONTEXT_KEY}.{@link SlackUtils#SLACK_THREAD_TS}. If the provided
     * {@link RuntimeContexts} does not contain this value this means that the user input is not in a threadTs.
     *
     * @param context the {@link RuntimeContexts} to retrieve the threadTs timestamp from
     * @return the threadTs timestamp if it exist, or {@code null} / an empty {@link String} if it doesn't (depending
     * on the NLP provider)
     * @see SlackUtils
     */
    public static @Nullable
    String getThreadTs(RuntimeContexts context) {
        checkNotNull(context, "Cannot retrieve the threadTs from the provided %s %s",
                RuntimeContexts.class.getSimpleName(), context);
        return (String) context.getContextValue(SlackUtils.SLACK_CONTEXT_KEY, SlackUtils.SLACK_THREAD_TS);
    }

    /**
     * Constructs a new {@link Reply} with the provided {@code runtimePlatform}, {@code session}, and {@code message}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param message         the message to post
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty
     * @see #getChannel(RuntimeContexts)
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String, String)
     */
    public Reply(SlackPlatform runtimePlatform, XatkitSession session, String message) {
        super(runtimePlatform, session, message, getChannel(session.getRuntimeContexts()),
                getTeamId(session.getRuntimeContexts()), getThreadTs(session.getRuntimeContexts()));
    }
}
