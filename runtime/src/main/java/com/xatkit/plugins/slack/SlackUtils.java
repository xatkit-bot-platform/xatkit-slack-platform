package com.xatkit.plugins.slack;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import com.xatkit.plugins.slack.platform.io.SlackIntentProvider;
import org.apache.commons.configuration2.Configuration;

/**
 * An utility interface that holds Slack-related helpers.
 * <p>
 * This class defines the xatkit configuration key to store the Slack bot API token, as well as a set of API response
 * types that are used internally to check connection and filter incoming events.
 */
public interface SlackUtils extends ChatUtils {

    /**
     * The {@link Configuration} key to store the Slack bot API token.
     * <p>
     * This {@link Configuration} key can be provided to start the platform in development mode, meaning that it will
     * be bound to the provided token and will not accept additional installations.
     * <p>
     * The platform can be started in deployment mode by providing the app's {@code clientId} and {@code clientSecret
     * } using the dedicated keys.
     *
     * @see SlackIntentProvider#SlackIntentProvider(SlackPlatform, Configuration)
     * @see SlackPlatform#SlackPlatform(XatkitCore, Configuration)
     */
    String SLACK_TOKEN_KEY = "xatkit.slack.token";

    /**
     * The {@link Configuration} key to store the Slack app client id.
     * <p>
     * This {@link Configuration} key is provided when deploying and distributing the bot, and allows multiple
     * installations of it. In this scenario the {@link Configuration} should not contain a Slack token.
     * <p>
     * If this key is specified the {@link Configuration} must also contain the {@link #SLACK_CLIENT_SECRET_KEY} key.
     *
     * @see #SLACK_CLIENT_SECRET_KEY
     * @see SlackIntentProvider#SlackIntentProvider(SlackPlatform, Configuration)
     * @see SlackPlatform#SlackPlatform(XatkitCore, Configuration)
     */
    String SLACK_CLIENT_ID_KEY = "xatkit.slack.client.id";

    /**
     * The {@link Configuration} key to store the Slack app client secret.
     * <p>
     * This {@link Configuration} key is provided when deploying and distributing the bot, and allows multiple
     * installations of it. In this scenario the {@link Configuration} should not contain a Slakc token.
     * <p>
     * If this key is specified the {@link Configuration} must also contain the {@link #SLACK_CLIENT_ID_KEY} key.
     *
     * @see #SLACK_CLIENT_ID_KEY
     * @see SlackIntentProvider#SlackIntentProvider(SlackPlatform, Configuration)
     * @see SlackPlatform#SlackPlatform(XatkitCore, Configuration)
     */
    String SLACK_CLIENT_SECRET_KEY = "xatkit.slack.client.secret";

    /**
     * The {@link Configuration} key to store whether to ignore fallback intents on group channels.
     * <p>
     * This value is set to {@code false} by default.
     *
     * @see #DEFAULT_IGNORE_FALLBACK_ON_GROUP_CHANNELS
     */
    String IGNORE_FALLBACK_ON_GROUP_CHANNELS_KEY = "xatkit.slack.ignore_fallback_on_group_channels";

    /**
     * The {@link Configuration} key to store whether to listen to mentions in group channels.
     * <p>
     * This value is set to {@code false} by default (meaning that the bot will listen to all the messages).
     *
     * @see #DEFAULT_LISTEN_MENTIONS_ON_GROUP_CHANNELS
     */
    String LISTEN_MENTIONS_ON_GROUP_CHANNELS_KEY = "xatkit.slack.listen_mentions_on_group_channels";

    /**
     * The default value of the {@link #IGNORE_FALLBACK_ON_GROUP_CHANNELS_KEY} {@link Configuration} key.
     */
    boolean DEFAULT_IGNORE_FALLBACK_ON_GROUP_CHANNELS = false;

    /**
     * The default value of the {@link #LISTEN_MENTIONS_ON_GROUP_CHANNELS_KEY} {@link Configuration} key.
     */
    boolean DEFAULT_LISTEN_MENTIONS_ON_GROUP_CHANNELS = false;

    /**
     * The Slack API answer type representing a {@code message}.
     */
    String MESSAGE_TYPE = "message";

    /**
     * The Slack API answer type representing a successful authentication.
     */
    String HELLO_TYPE = "hello";

    /**
     * The {@link RuntimeContexts} key used to store slack-related information.
     */
    String SLACK_CONTEXT_KEY = "slack";

    /**
     * The {@link RuntimeContexts} key used to store the slack team identifier information.
     */
    String SLACK_TEAM_CONTEXT_KEY = "team";

    /**
     * The {@link RuntimeContexts} key used to store the slack user email information.
     */
    String SLACK_USER_EMAIL_CONTEXT_KEY = "userEmail";

    /**
     * The {@link RuntimeContexts} key used to store the slack user identifier information.
     */
    String SLACK_USER_ID_CONTEXT_KEY = "userId";

    /**
     * The {@link RuntimeContexts} key used to store the timestamp of the thread of a received message.
     */
    String SLACK_THREAD_TS = "threadTs";

    /**
     * The {@link RuntimeContexts} key used to store the timestamp of the received message.
     */
    String SLACK_MESSAGE_TS = "messageTs";

}
