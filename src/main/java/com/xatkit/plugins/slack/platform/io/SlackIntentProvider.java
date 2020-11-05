package com.xatkit.plugins.slack.platform.io;

import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.auth.AuthTestRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersInfoResponse;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.github.seratch.jslack.api.rtm.RTMCloseHandler;
import com.github.seratch.jslack.api.rtm.RTMMessageHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.io.IntentRecognitionHelper;
import com.xatkit.core.platform.io.RuntimeEventProvider;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.chat.platform.io.ChatIntentProvider;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.xatkit.plugins.slack.util.SlackUtils.logSlackApiResponse;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A Slack-based {@link ChatIntentProvider}.
 * <p>
 * This class relies on the Slack RTM API to receive direct messages from workspaces where the Slack app is
 * installed and react to them.
 * <p>
 * This class loads the Slack {@code token}s stored in its containing {@link SlackPlatform} to initialize its RTM
 * listeners. New installations of the Slack app are handled by {@link #notifyNewInstallation(String, String)}.
 *
 * @see SlackUtils
 * @see RuntimeEventProvider
 */
public class SlackIntentProvider extends ChatIntentProvider<SlackPlatform> {

    /**
     * The default username returned by {@link #getUsernameFromUserId(String, String)}.
     *
     * @see #getUsernameFromUserId(String, String)
     */
    private static String DEFAULT_USERNAME = "unknown user";

    /**
     * The delay (in ms) to wait before attempting to reconnect disconnected RTM clients.
     * <p>
     * When a RTM client is disconnected abnormally the {@link SlackIntentProvider} attempts to reconnect it by
     * waiting {@code RECONNECT_WAIT_TIME * <number_of_attempts>} ms. The delay is reset after each successful
     * reconnection.
     *
     * @see XatkitRTMCloseHandler
     */
    private static int RECONNECT_WAIT_TIME = 2000;

    /**
     * The {@link Map} containing the {@link RTMClient}s associated to each workspace where the Slack app is installed.
     * <p>
     * Keys in this {@link Map} are {@code teamId}.
     */
    private Map<String, RTMClient> rtmClients = new HashMap<>();

    /**
     * The {@link JsonParser} used to manipulate Slack API answers.
     */
    private JsonParser jsonParser;

    /**
     * Specifies whether {@code DEFAULT_FALLBACK_INTENT}s should be ignored in group channel (default to {@code false}).
     */
    private boolean ignoreFallbackOnGroupChannels;

    /**
     * Specified whether the bot should listen to mentions on group channels.
     * <p>
     * When set to {@code true}, this feature allows to define bots that only react on mentions in group channels.
     */
    private boolean listenMentionsOnGroupChannels;

    /**
     * Constructs a {@link SlackIntentProvider} and binds it to the provided {@code slackPlatform}.
     *
     * @param slackPlatform the {@link SlackPlatform} managing this provider
     */
    public SlackIntentProvider(SlackPlatform slackPlatform) {
        super(slackPlatform);
    }

    /**
     * Constructs a new {@link SlackIntentProvider} from the provided {@code runtimePlatform} and
     * {@code configuration}.
     * <p>
     * This constructor initializes the underlying RTM connections and creates a message listener that forwards to
     * the {@code xatkitBot} instance not empty messages sent in channels the bot is listening to.
     *
     * @param configuration the {@link Configuration} used to retrieve the Slack bot API token
     * @throws NullPointerException if the provided {@code configuration} is {@code null}
     * @throws XatkitException      if an error occurred when starting the RTM clients
     */
    @Override
    public void start(Configuration configuration) {
        super.start(configuration);
        checkNotNull(configuration, "Cannot construct a SlackIntentProvider from a null configuration");
        this.ignoreFallbackOnGroupChannels =
                configuration.getBoolean(SlackUtils.IGNORE_FALLBACK_ON_GROUP_CHANNELS_KEY,
                        SlackUtils.DEFAULT_IGNORE_FALLBACK_ON_GROUP_CHANNELS);
        this.listenMentionsOnGroupChannels =
                configuration.getBoolean(SlackUtils.LISTEN_MENTIONS_ON_GROUP_CHANNELS_KEY,
                        SlackUtils.DEFAULT_LISTEN_MENTIONS_ON_GROUP_CHANNELS);
        this.jsonParser = new JsonParser();
        this.rtmClients = new HashMap<>();
        this.runtimePlatform.getTeamIdToSlackTokenMap().forEach(this::notifyNewInstallation);
    }

    /**
     * Starts a new {@link RTMClient} for the provided {@code teamId} and {@code token}.
     * <p>
     * This method is typically called when the Slack app is installed in a new workspace. In this case this provider
     * starts a new {@link RTMClient} associated to the provided {@code teamId} that will listen to the new
     * installation.
     *
     * @param teamId the identifier of the workspace where the bot has been installed
     * @param token  the Slack {@code token} corresponding to the new installation
     */
    public void notifyNewInstallation(String teamId, String token) {
        String workspaceBotId = this.getSelfId(token);
        RTMClient rtmClient;
        try {
            rtmClient = this.runtimePlatform.getSlack().rtm(token);
        } catch (IOException e) {
            throw new XatkitException("An error occurred when starting the RTM client, see the attached exception", e);
        }
        rtmClient.addMessageHandler(new XatkitRTMMessageHandler(workspaceBotId));
        rtmClient.addCloseHandler(new XatkitRTMCloseHandler(teamId));
        try {
            rtmClient.connect();
        } catch (DeploymentException | IOException e) {
            String errorMessage = "Cannot start the Slack RTM websocket, please check your internet connection";
            Log.error(errorMessage);
            throw new XatkitException(errorMessage, e);
        }
        rtmClients.put(teamId, rtmClient);
    }

    /**
     * Returns the unique identifier of the bot in the workspace defined by the provided {@code slackToken}.
     * <p>
     * This identifier is used to check input messages and filter the ones that are sent by this bot, in order to
     * avoid infinite message loops. Note that only messages from this specific bot are ignored. This allows to
     * define bot swarms where each bot can interact with the other ones.
     *
     * @param slackToken the Slack {@code token} corresponding to the workspace to get the bot identifier from
     * @return the unique identifier of the bot in the provide workspace
     */
    protected String getSelfId(String slackToken) {
        AuthTestRequest request = AuthTestRequest.builder().token(slackToken).build();
        try {
            AuthTestResponse response = this.runtimePlatform.getSlack().methods().authTest(request);
            logSlackApiResponse(response);
            return response.getUserId();
        } catch (IOException | SlackApiException e) {
            throw new XatkitException("Cannot retrieve the bot identifier", e);
        }
    }

    /**
     * Returns the Slack username associated to the provided {@code teamId} and {@code userId}.
     * <p>
     * This method returns the <i>display name</i> associated to the provided {@code userId} if it is set in the user
     * profile. If the user profile does not contain a non-empty display name this method returns the <i>real
     * name</i> associated to the provided {@code userId}.
     * <p>
     * This method returns {@link #DEFAULT_USERNAME} if the Slack API is not reachable or if the provided {@code
     * userId} does not match any known user.
     *
     * @param teamId the identifier of the workspace to retrieve the username from
     * @param userId the user identifier to retrieve the username from
     * @return the Slack username associated to the provided {@code userId}
     */
    private String getUsernameFromUserId(String teamId, String userId) {
        String username = DEFAULT_USERNAME;
        try {
            User user = getUserFromUserId(teamId, userId);
            if (nonNull(user)) {
                User.Profile profile = user.getProfile();
                /*
                 * Use the display name if it exists, otherwise use the real name that should always be set.
                 */
                username = profile.getDisplayName();
                if (isNull(username) || username.isEmpty()) {
                    username = profile.getRealName();
                }
                Log.debug("Found username \"{0}\"", username);
            } else {
                Log.error("Cannot retrieve the username for {0}, returning the default username {1}", userId,
                        DEFAULT_USERNAME);
            }
        } catch (IOException | SlackApiException e) {
            Log.error("Cannot retrieve the username for {0}, returning the default username {1}", userId,
                    DEFAULT_USERNAME);
        }
        return username;
    }

    /**
     * Returns the Slack user email associated to the provided {@code teamId} and {@code userId}.
     * <p>
     * This method returns the email set in the user's profile. If an error occurred or if the email is not set an
     * empty {@link String} is returned.
     *
     * @param teamId the identifier of the workspace to retrieve the user email from
     * @param userId the user identifier to retrieve the user email from
     * @return the Slack user email associated to the provided {@code userId}
     */
    private String getUserEmailFromUserId(String teamId, String userId) {
        String email = "";
        try {
            User user = getUserFromUserId(teamId, userId);
            if (nonNull(user)) {
                User.Profile profile = user.getProfile();
                email = profile.getEmail();
            } else {
                Log.error("Cannot retrieve the user email for {0}, returning an empty email", userId);
            }
        } catch (IOException | SlackApiException e) {
            Log.error("Cannot retrieve the user email for {0}, returning an empty email", userId);
        }
        return email;
    }

    /**
     * Retrieves the {@link User} instance associated to the provided {@code teamId} and {@code userId}.
     * <p>
     * This method is used to access user-related information. Note that each call to this method will call the Slack
     * API (see #3).
     *
     * @param teamId the identifier of the workspace to retrieve the {@link User} instance from
     * @param userId the user identifier to retrieve the {@link User} instance from
     * @return the {@link User} instance associated to the provided {@code userId}
     * @throws SlackApiException if the Slack API returns an error
     * @throws IOException       if an error occurred when reaching the Slack API
     */
    private User getUserFromUserId(String teamId, String userId) throws SlackApiException, IOException {
        Log.debug("Retrieving User for the user ID {0}", userId);
        UsersInfoRequest usersInfoRequest = UsersInfoRequest.builder()
                .token(this.runtimePlatform.getSlackToken(teamId))
                .user(userId)
                .build();
        UsersInfoResponse response = this.runtimePlatform.getSlack().methods().usersInfo(usersInfoRequest);
        logSlackApiResponse(response);
        return response.getUser();
    }

    /**
     * Returns the {@link RTMClient} associated to the workspace defined by the provided {@code teamId}.
     *
     * @param teamId the identifier of the workspace to retrieve the {@link RTMClient} from
     * @return the {@link RTMClient} associated to this class
     */
    public RTMClient getRtmClient(String teamId) {
        return rtmClients.get(teamId);
    }

    @Override
    public void run() {
        /*
         * Required because the RTM listener is started in another threadTs, and if this threadTs terminates the main
         * application terminates.
         */
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * Disconnects the underlying Slack RTM clients.
     */
    @Override
    public void close() {
        Log.info("Closing Slack RTM clients");
        this.rtmClients.forEach((teamId, rtmClient) -> {
            try {
                rtmClient.disconnect();
            } catch (IOException e) {
                Log.error("Cannot disconnect the RTM client for workspace {0}, see the attached exception", teamId,
                        e);
            }
        });
    }

    /**
     * The {@link RTMMessageHandler} used to process user messages.
     */
    private class XatkitRTMMessageHandler implements RTMMessageHandler {

        /**
         * The unique identifier of the bot in the workspace this handler listens to.
         * <p>
         * This identifier is used to check input messages and filter the ones that are sent by this bot, in order to
         * avoid infinite message loops. Note that only messages from this specific bot are ignored. This allows to
         * define bot swarms where each bot can interact with the other ones.
         */
        private String botSelfId;

        /**
         * Constructs a {@link XatkitRTMMessageHandler} with the provided {@code botSelfId}.
         *
         * @param botSelfId the unique identifier of the bot in the workspace this handler listens to
         */
        public XatkitRTMMessageHandler(String botSelfId) {
            this.botSelfId = botSelfId;
        }

        @Override
        public void handle(String message) {
            JsonObject json = jsonParser.parse(message).getAsJsonObject();
            if (nonNull(json.get("type"))) {
                /*
                 * The message has a type, this should always be true
                 */
                Log.debug("received {0}", json);
                if (json.get("type").getAsString().equals(SlackUtils.HELLO_TYPE)) {
                    Log.info("Slack listener connected");
                }
                if (json.get("type").getAsString().equals(SlackUtils.MESSAGE_TYPE)) {
                    /*
                     * The message hasn't been sent by a bot
                     */
                    JsonElement teamObject = json.get("team");
                    if (nonNull(teamObject)) {
                        String team = teamObject.getAsString();
                        JsonElement channelObject = json.get("channel");
                        if (nonNull(channelObject)) {
                            /*
                             * The message channel is set
                             */
                            String channel = channelObject.getAsString();
                            JsonElement userObject = json.get("user");
                            if (nonNull(userObject)) {
                                /*
                                 * The name of the user that sent the message
                                 */
                                String user = userObject.getAsString();
                                if (!user.equals(botSelfId)) {
                                    JsonElement textObject = json.get("text");
                                    if (nonNull(textObject)) {
                                        String text = textObject.getAsString();
                                        if (!text.isEmpty()) {
                                            Log.debug("Received message {0} from user {1} (channel: {2})", text,
                                                    user, channel);

                                            if (listenMentionsOnGroupChannels && SlackIntentProvider.this.runtimePlatform.isGroupChannel(team, channel)) {
//                                                String botMention = "<@" + SlackIntentProvider.this.getSelfId() + ">";
                                                String botMention = "<@" + botSelfId + ">";
                                                if (text.contains(botMention)) {
                                                    /*
                                                     * The message contains a mention to the bot, we need to remove it
                                                     * before sending it to the NLP engine to avoid pollution and false
                                                     * negative matches.
                                                     */
                                                    text = text.replaceAll(botMention, "").trim();
                                                } else {
                                                    /*
                                                     * Nothing to do, the bot listens to mentions and the message is not
                                                     * a mention.
                                                     */
                                                    return;
                                                }
                                            }

                                            /*
                                             * Extract thread-related information. The thread_ts field contains a value
                                             * if the received message is part of a thread, otherwise the field is not
                                             * specified.
                                             */
                                            JsonElement threadTsObject = json.get("thread_ts");
                                            String threadTs = "";
                                            if (nonNull(threadTsObject)) {
                                                threadTs = threadTsObject.getAsString();
                                            }

                                            JsonElement tsObject = json.get("ts");
                                            String messageTs = "";
                                            if (nonNull(tsObject)) {
                                                messageTs = tsObject.getAsString();
                                            }

                                            StateContext context =
                                                    runtimePlatform.createSessionFromChannel(team, channel);
                                            /*
                                             * Call getRecognizedIntent before setting any context variable, the
                                             * recognition triggers a decrement of all the context variables.
                                             */
                                            RecognizedIntent recognizedIntent;
                                            try {
                                                recognizedIntent =
                                                        IntentRecognitionHelper.getRecognizedIntent(text, context,
                                                                SlackIntentProvider.this.xatkitBot);
                                            } catch (IntentRecognitionProviderException e) {
                                                throw new RuntimeException("An internal error occurred when computing" +
                                                        " the intent, see attached exception", e);
                                            }
                                            /*
                                             * Chat-related values (from ChatUtils). These are required for all the
                                             * platforms extending ChatPlatform.
                                             */
                                            recognizedIntent.getPlatformData().put(ChatUtils.CHAT_CHANNEL_CONTEXT_KEY
                                                    , channel);
                                            recognizedIntent.getPlatformData().put(ChatUtils.CHAT_USERNAME_CONTEXT_KEY, getUsernameFromUserId(team, user));
                                            recognizedIntent.getPlatformData().put(ChatUtils.CHAT_RAW_MESSAGE_CONTEXT_KEY, text);
                                            /*
                                             * Slack-specific platform values.
                                             */
                                            recognizedIntent.getPlatformData().put(SlackUtils.SLACK_TEAM_CONTEXT_KEY,
                                                    team);
                                            recognizedIntent.getPlatformData().put(SlackUtils.SLACK_USER_EMAIL_CONTEXT_KEY, getUserEmailFromUserId(team, user));
                                            recognizedIntent.getPlatformData().put(SlackUtils.SLACK_USER_ID_CONTEXT_KEY, user);
                                            recognizedIntent.getPlatformData().put(SlackUtils.SLACK_THREAD_TS,
                                                    threadTs);
                                            recognizedIntent.getPlatformData().put(SlackUtils.SLACK_MESSAGE_TS,
                                                    messageTs);
                                            if (recognizedIntent.getDefinition().getName().equals(
                                                    "Default_Fallback_Intent") && ignoreFallbackOnGroupChannels) {
                                                /*
                                                 * First check the property, if fallback intents are not ignored no
                                                 * need to
                                                 * check if this is a group channel or not (this may trigger additional
                                                 * Slack
                                                 * API calls).
                                                 */
                                                if (!SlackIntentProvider.this.runtimePlatform.isGroupChannel(team,
                                                        channel)) {
                                                    SlackIntentProvider.this.sendEventInstance(recognizedIntent,
                                                            context);
                                                } else {
                                                    /*
                                                     * Do nothing, fallback intents are ignored in group channels and
                                                     * this is a group channel.
                                                     */
                                                }
                                            } else {
                                                SlackIntentProvider.this.sendEventInstance(recognizedIntent, context);
                                            }
                                        } else {
                                            Log.warn("Received an empty message, skipping it");
                                        }
                                    } else {
                                        Log.warn("The message does not contain a \"text\" field, skipping it");
                                    }
                                } else {
                                    Log.trace("Skipping {0}, the message was sent by this bot", json);
                                }
                            } else {
                                Log.warn("Skipping {0}, the message does not contain a \"user\" field",
                                        json);
                            }
                        } else {
                            Log.warn("Skipping {0}, the message does not contain a \"channel\" field", json);
                        }
                    } else {
                        Log.warn("Skipping {0}, the message does not contain a \"team\" field", json);
                    }
                } else {
                    Log.trace("Skipping {0}, the message type is not \"{1}\"", json, SlackUtils.MESSAGE_TYPE);
                }
            } else {
                Log.error("The message does not define a \"type\" field, skipping it");
            }
        }
    }

    /**
     * The {@link RTMCloseHandler} used to handle RTM client connection issues.
     * <p>
     * This handler will attempt to reconnect the RTM client by creating a new {@link RTMClient} instance after
     * waiting {@code RECONNECT_WAIT_TIME * <number_of_attempts>} ms. Note that reconnecting the RTM client will be
     * executed in the main threadTs and will block Xatkit execution.
     *
     * @see #RECONNECT_WAIT_TIME
     */
    private class XatkitRTMCloseHandler implements RTMCloseHandler {

        private String teamId;

        public XatkitRTMCloseHandler(String teamId) {
            this.teamId = teamId;
        }

        @Override
        public void handle(CloseReason reason) {
            if (reason.getCloseCode().equals(CloseReason.CloseCodes.CLOSED_ABNORMALLY)) {
                Log.error("Connection to the Slack RTM client lost");
                int attempts = 0;
                while (true) {
                    try {
                        attempts++;
                        int waitTime = attempts * RECONNECT_WAIT_TIME;
                        Log.info("Trying to reconnect in {0}ms", waitTime);
                        Thread.sleep(waitTime);
                        String slackToken = SlackIntentProvider.this.getRuntimePlatform().getSlackToken(teamId);
                        RTMClient rtmClient = SlackIntentProvider.this.getRuntimePlatform().getSlack().rtm(slackToken);
                        rtmClient.addMessageHandler(new XatkitRTMMessageHandler(SlackIntentProvider.this.getSelfId(slackToken)));
                        rtmClient.addCloseHandler(new XatkitRTMCloseHandler(teamId));
                        rtmClient.connect();
                        rtmClients.put(teamId, rtmClient);
                        /*
                         * The RTM client is reconnected and the handlers are set.
                         */
                        break;
                    } catch (DeploymentException | IOException e) {
                        Log.error("Unable to reconnect the RTM client");
                    } catch (InterruptedException e) {
                        Log.error("An error occurred while waiting to reconnect the RTM client");
                    }
                }
            }
        }
    }
}
