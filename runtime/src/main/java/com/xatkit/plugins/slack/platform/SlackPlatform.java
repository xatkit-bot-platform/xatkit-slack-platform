package com.xatkit.plugins.slack.platform;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.auth.AuthTestRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsListRequest;
import com.github.seratch.jslack.api.methods.request.oauth.OAuthAccessRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersListRequest;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsListResponse;
import com.github.seratch.jslack.api.methods.response.oauth.OAuthAccessResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersInfoResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersListResponse;
import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.ConversationType;
import com.github.seratch.jslack.api.model.User;
import com.google.gson.JsonObject;
import com.xatkit.core.XatkitCore;
import com.xatkit.core.XatkitException;
import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.server.HttpMethod;
import com.xatkit.core.server.HttpUtils;
import com.xatkit.core.server.RestHandlerFactory;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.chat.platform.ChatPlatform;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.plugins.slack.platform.io.SlackIntentProvider;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xatkit.plugins.slack.util.SlackUtils.logSlackApiResponse;
import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A {@link RuntimePlatform} class that connects and interacts with the Slack API.
 * <p>
 * This runtimePlatform manages connections to the Slack API, and provides a set of {@link RuntimeAction}s to
 * interact with the Slack API (see {@link com.xatkit.plugins.slack.platform.action}).
 * <p>
 * This class is part of xatkit's core platform, and can be used in an execution model by importing the
 * <i>SlackPlatform</i> package.
 */
public class SlackPlatform extends ChatPlatform {

    /**
     * The {@code clientId} of the Slack app associated to the deployed bot.
     * <p>
     * <b>Note</b>: the {@code clientId} is provided in the {@link Configuration} (using the key
     * {@link SlackUtils#SLACK_CLIENT_ID_KEY}) for <i>distributed </i> applications. Applications that are currently
     * under development can specify the Slack {@code token} corresponding to their application using the
     * {@link SlackUtils#SLACK_TOKEN_KEY} configuration key.
     * <p>
     * If a {@code clientId} is provided the {@link Configuration} must also contain a {@code clientSecret}.
     *
     * @see #clientSecret
     */
    private String clientId;

    /**
     * The {@code clientSecret} of the Slack app associated to the deployed bot.
     * <p>
     * <b>Note</b>: the {@code clientSecret} is provided in the {@link Configuration} (using the key
     * {@link SlackUtils#SLACK_CLIENT_SECRET_KEY}) for <i>distributed</i> applications. Applications that are
     * currently under development can specify the Slack {@code token} corresponding to their application using the
     * {@link SlackUtils#SLACK_TOKEN_KEY} configuration key.
     * <p>
     * If a {@code clientSecret} is provided the {@link Configuration} must also contain a {@code clientId}.
     *
     * @see #clientId
     */
    private String clientSecret;


    /**
     * The {@link Slack} API client used to post messages.
     */
    private Slack slack;

    /**
     * A {@link Map} containing the {@code name -> ID} mapping of all the channels in the workspaces where the bot is
     * installed.
     * <p>
     * This {@link Map} contains entries for conversation name, user name, user display name, and IDs, allowing fast
     * lookups to retrieve a channel identifier from a given name.
     * <p>
     * Keys in this {@link Map} are {@code teamId}s.
     * <p>
     * This {@link Map} is populated by {@link #loadChannels(String)}.
     *
     * @see #loadChannels(String)
     */
    private Map<String, Map<String, String>> channelNames;

    /**
     * A {@link Map} containing the IDs of all the group channels for all the workspaces where the bot is installed.
     * <p>
     * A group channel corresponds to any conversation that can be joined by multiple users (typically everything
     * excepted user channels).
     * <p>
     * Keys in this {@link Map} are {@code teamId}s.
     * <p>
     * This {@link Map} is populated by {@link #loadChannels(String)}.
     *
     * @see #loadChannels(String)
     */
    private Map<String, List<String>> groupChannels;

    /**
     * A {@link Map} containing the IDs of all the user channels for all the workspaces where the bot is installed.
     * <p>
     * A user channel is a direct message channel between an user and the bot.
     * <p>
     * Keys in this {@link Map} are {@code teamId}s.
     * <p>
     * This {@link Map} is populated by {@link #loadChannels(String)}.
     *
     * @see #loadChannels(String)
     */
    private Map<String, List<String>> userChannels;

    /**
     * A {@link Map} containins the Slack {@code tokens} associated to the workspace's {@code teamId}s.
     * <p>
     * The {@code teamId} value corresponds to the identifier of the workspaces where the app is installed. This
     * identifier is used in {@code Reply*} action to target specific workspaces.
     */
    private Map<String, String> teamIdToSlackToken;

    /**
     * Constructs a new {@link SlackPlatform} from the provided {@link XatkitCore} and {@link Configuration}.
     * <p>
     * This constructor initializes the underlying {@link Slack} client. If the provided {@code configuration}
     * contains a Slack {@code token} (using the key {@link SlackUtils#SLACK_TOKEN_KEY}) the constructor initializes
     * the platform with the provided token, and does not allow additional installation of the Slack app. This
     * feature is typically used in development mode to quickly test the bot under development in a single Slack
     * workspace.
     * <p>
     * If the {@code configuration} contains a Slack {@code clientId} and {@code clientSecret} the platform starts a
     * dedicated REST handler that will receive OAuth queries when the Slack app is installed by clients. This
     * handler manages internal caches to ensure that {@code Reply*} actions are correctly sent to the appropriate
     * workspaces.
     *
     * @param xatkitCore    the {@link XatkitCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to retrieve the Slack bot API token
     * @throws NullPointerException     if the provided {@code xatkitCore} or {@code configuration} is {@code null}
     * @throws IllegalArgumentException if the {@code configuration} neither contains a Slack {@code token} or a
     *                                  {@code clientId} and {@code clientSecret}
     * @see SlackUtils#SLACK_TOKEN_KEY
     * @see SlackUtils#SLACK_CLIENT_ID_KEY
     * @see SlackUtils#SLACK_CLIENT_SECRET_KEY
     */
    public SlackPlatform(XatkitCore xatkitCore, Configuration configuration) {
        super(xatkitCore, configuration);
        this.teamIdToSlackToken = new HashMap<>();
        slack = new Slack();
        this.channelNames = new HashMap<>();
        this.groupChannels = new HashMap<>();
        this.userChannels = new HashMap<>();
        String slackToken = configuration.getString(SlackUtils.SLACK_TOKEN_KEY);
        if (nonNull(slackToken)) {
            AuthTestRequest request = AuthTestRequest.builder().token(slackToken).build();
            try {
                AuthTestResponse response = slack.methods().authTest(request);
                logSlackApiResponse(response);
                String teamId = response.getTeamId();
                teamIdToSlackToken.put(teamId, slackToken);
                this.loadChannels(teamId);
                this.notifyNewInstallation(teamId, slackToken);
            } catch (IOException | SlackApiException e) {
                throw new XatkitException("Cannot retrieve the team associated to the provided Slack token", e);
            }
        } else {
            Log.info("The configuration does not contain a Slack token, starting {0} in OAuth mode",
                    SlackPlatform.class.getSimpleName());
            clientId = configuration.getString(SlackUtils.SLACK_CLIENT_ID_KEY);
            checkArgument(nonNull(clientId) && !clientId.isEmpty(), "Cannot construct a %s from the provided clientId" +
                            " %s, please ensure that Xatkit configuration contains a valid clientId associated to the" +
                            " key %s"
                    , SlackPlatform.class.getSimpleName(), clientId, SlackUtils.SLACK_CLIENT_ID_KEY);
            clientSecret = configuration.getString(SlackUtils.SLACK_CLIENT_SECRET_KEY);
            checkArgument(nonNull(clientSecret) && !clientSecret.isEmpty(), "Cannot construct a %s from the provided " +
                            "clientSecret %s, please ensure that Xatkit configuration contains a valid clientSecret " +
                            "associated to the key %s", SlackPlatform.class.getSimpleName(), clientSecret,
                    SlackUtils.SLACK_CLIENT_SECRET_KEY);
            registerOAuthRestHandler();
        }
    }

    /**
     * Registers the REST handler that manages OAuth requests sent by Slack when the app is installed.
     * <p>
     * The defined endpoint URI is {@code <basePath>/slack/oauth/redirect}, this URI must be specified in the
     * associated Slack app settings to ensure that Xatkit receives the OAuth requests and populate its internal data
     * structure with the new installation information.
     * <p>
     * <b>Note</b>: the OAuth REST handler is not started if the {@code configuration} used to initialize the
     * platform contains a Slack {@code token}. In this case the platform assumes that the app is currently under
     * development and does not authorize multiple installations.
     *
     * @see com.xatkit.core.server.XatkitServer
     */
    private void registerOAuthRestHandler() {
        this.xatkitCore.getXatkitServer().registerRestEndpoint(HttpMethod.GET, "/slack/oauth/redirect",
                RestHandlerFactory.createJsonRestHandler((headers, param, content) -> {
                    JsonObject result = new JsonObject();
                    String code = HttpUtils.getParameterValue("code", param);
                    try {
                        OAuthAccessResponse response = this.slack.methods().oauthAccess(OAuthAccessRequest.builder()
                                .clientId(clientId)
                                .clientSecret(clientSecret)
                                .code(code)
                                .build());
                        logSlackApiResponse(response);
                        String teamId = response.getTeamId();
                        if (isNull(teamId)) {
                            result.addProperty("Error", "The Slack API response does not contain a team identifier");
                            return result;
                        }
                        String botAccessToken = response.getBot().getBotAccessToken();
                        if (isNull(botAccessToken)) {
                            result.addProperty("Error", "The Slack API response does not contain a bot access token");
                            return result;
                        }
                        Log.info("Adding installation mapping {0} -> {1}", teamId, botAccessToken);
                        this.teamIdToSlackToken.put(teamId, botAccessToken);
                        loadChannels(teamId);
                        this.notifyNewInstallation(teamId, botAccessToken);
                    } catch (IOException | SlackApiException e) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintWriter printWriter = new PrintWriter(baos, true);
                        e.printStackTrace(printWriter);
                        result.addProperty("Error", baos.toString());
                        return result;
                    }
                    result.addProperty("Message", "Installed!");
                    return result;
                }));
    }

    /**
     * Notifies the started {@link com.xatkit.core.platform.io.RuntimeEventProvider}s that the Slack app has been
     * installed in a new workspace.
     *
     * @param teamId     the identifier of the workspace where the app has been installed
     * @param slackToken the Slack {@code token} associated to the new installation
     */
    private void notifyNewInstallation(String teamId, String slackToken) {
        this.getEventProviderMap().forEach((providerName, providerThread) -> {
            if (providerThread.getRuntimeEventProvider() instanceof SlackIntentProvider) {
                SlackIntentProvider slackIntentProvider =
                        (SlackIntentProvider) providerThread.getRuntimeEventProvider();
                slackIntentProvider.notifyNewInstallation(teamId, slackToken);
            }
        });
    }

    /**
     * Returns the {@link Map} containing the Slack {@code token}s associated to the identifiers of the workspaces
     * where the Slack app is installed.
     *
     * @return the {@link Map} containing the Slack {@code token}s associated to the identifiers of the workspaces
     * where the Slack app is installed
     */
    public Map<String, String> getTeamIdToSlackTokenMap() {
        return this.teamIdToSlackToken;
    }

    /**
     * Returns the Slack {@code token} associated to the provided {@code teamId}.
     *
     * @param teamId a workspace identifier
     * @return the Slack {@code token} associated to the provided {@code teamId} if it exists, {@code null} otherwise
     */
    public String getSlackToken(String teamId) {
        return teamIdToSlackToken.get(teamId);
    }

    /**
     * Returns the Slack API client.
     *
     * @return the Slack API client.
     */
    public Slack getSlack() {
        return slack;
    }

    /**
     * Returns the {@link XatkitSession} associated to the provided {@code teamId} and {@code channel}.
     * <p>
     * The provided {@code teamId} <b>must</b> be a valid workspace identifier, while the provided {@code channel}
     * can be an identifier or a channel name.
     *
     * @param teamId  the identifier of the workspace to create a session for
     * @param channel the workspace's {@code channel} to create a session for
     * @return the {@link XatkitSession} associated to the provided {@code teamId} and {@code channel}
     */
    public XatkitSession createSessionFromChannel(String teamId, String channel) {
        return this.xatkitCore.getOrCreateXatkitSession(teamId + "@" + this.getChannelId(teamId, channel));
    }

    /**
     * Retrieves the User ID associated to the provided {@code username} from the workspace identified with {@code
     * teamId}.
     * <p>
     * This method looks for any user with a {@code id}, {@code name}, or {@code realName} matching the provided {@code
     * username}, and returns its identifier.
     *
     * @param teamId   the idetnfier of the workspace containing the user to retrieve the ID of
     * @param username the name of the user to retrieve the ID of
     * @return the User ID if it exists
     * @throws XatkitException      if an error occurred when accessing the Slack API
     * @throws NullPointerException if the provided {@code teamId} or {@code username} is {@code null}
     */
    public String getUserId(String teamId, String username) {
        checkNotNull(teamId, "Cannot retrieve the user ID from the provided team %s", teamId);
        checkNotNull(username, "Cannot retrieve the user ID from the provided username %s", username);
        UsersListResponse usersListResponse;
        try {
            usersListResponse = this.getSlack().methods().usersList(UsersListRequest.builder()
                    .token(getSlackToken(teamId))
                    .build());
        } catch (IOException | SlackApiException e) {
            throw new XatkitException("An error occurred when accessing the Slack API, see attached exception", e);
        }
        logSlackApiResponse(usersListResponse);
        for (User user : usersListResponse.getMembers()) {
            if (user.getId().equals(username) || user.getName().equals(username) || user.getRealName().equals(username)) {
                return user.getId();
            }
        }
        return null;
    }

    /**
     * Retrieves the ID of the {@code channelName} from the workspace identified with {@code teamId}.
     * <p>
     * This method supports channel IDs, names, as well as user names, real names, and display names (in this case
     * the private {@code im} channel ID between the bot and the user is returned). The returned ID can be used to
     * send messages to the channel.
     *
     * @param teamId      the identifier of the workspace containing the channel to retrieve the identifier
     * @param channelName the name of the channel to retrieve the ID from
     * @return the channel ID if it exists
     * @throws XatkitException if the provided {@code teamId} does not correspond to a valid Slack app installation,
     *                         or if the provided {@code channelName} does not correspond to any channel accessible
     *                         by the bot
     */
    public String getChannelId(String teamId, String channelName) {
        if (this.channelNames.containsKey(teamId)) {
            String id = this.channelNames.get(teamId).get(channelName);
            if (isNull(id)) {
                /*
                 * Check if the channel has been created since the previous lookup. This is not done by default because
                 * it reloads all the channel and may take some time.
                 */
                loadChannels(teamId);
                id = this.channelNames.get(teamId).get(channelName);
                if (isNull(id)) {
                    /*
                     * Cannot find the channel after a fresh lookup.
                     */
                    throw new XatkitException(MessageFormat.format("Cannot find the channel {0}, please ensure that " +
                            "the " +
                            "provided channel is either a valid channel ID, name, or a valid user name, real name, or" +
                            " " +
                            "display name", channelName));
                }
            }
            return id;
        } else {
            throw new XatkitException(MessageFormat.format("Unknown teamId {0}, please ensure that the bot is " +
                    "installed in this workspace", teamId));
        }
    }

    /**
     * Returns whether the {@code channelId} from the workspace {@code teamId} is a group channel (that can contain
     * multiple users) or not.
     *
     * @param teamId    the identifier of the workspace containing the channel to check
     * @param channelId the identifier of the Slack channel to check
     * @return {@code true} if the channel is a group channel, {@code false} otherwise
     * @throws XatkitException if the provided {@code teamId} does not correspond to a valid Slack app installation
     */
    public boolean isGroupChannel(String teamId, String channelId) {
        if (this.userChannels.containsKey(teamId)) {
            /*
             * First check if it's a user channel, if so no need to do additional calls on the Slack API, we know it's
             * not a group channel.
             */
            if (this.userChannels.get(teamId).contains(channelId)) {
                return false;
            } else {
                if (this.groupChannels.get(teamId).contains(channelId)) {
                    return true;
                } else {
                    /*
                     * Reload the channels in case the group channel has been created since the last check.
                     */
                    loadChannels(teamId);
                    return this.groupChannels.get(teamId).contains(channelId);
                }
            }
        } else {
            throw new XatkitException(MessageFormat.format("Unknown team {0}, please ensure that the bot is installed" +
                    " in this workspace", teamId));
        }
    }

    /**
     * Loads the channels associated to the workspace's {@code teamId} and store channel-related information.
     * <p>
     * The stored information can be retrieved with dedicated methods, and reduce the number of calls to the Slack API.
     *
     * @see #getChannelId(String, String)
     * @see #isGroupChannel(String, String)
     */
    private void loadChannels(String teamId) {
        Map<String, String> workspaceChannelNames = new HashMap<>();
        List<String> workspaceGroupChannels = new ArrayList<>();
        List<String> workspaceUserChannels = new ArrayList<>();
        this.channelNames.put(teamId, workspaceChannelNames);
        this.groupChannels.put(teamId, workspaceGroupChannels);
        this.userChannels.put(teamId, workspaceUserChannels);
        String teamSlackToken = teamIdToSlackToken.get(teamId);
        if (isNull(teamSlackToken)) {
            throw new XatkitException(MessageFormat.format("Cannot load the channels for team {0}, the bot is not " +
                    "installed in this workspace", teamId));
        }
        try {
            ConversationsListResponse response =
                    slack.methods().conversationsList(ConversationsListRequest.builder()
                            .token(teamSlackToken)
                            .types(Arrays.asList(ConversationType.PUBLIC_CHANNEL, ConversationType.PUBLIC_CHANNEL,
                                    ConversationType.IM, ConversationType.MPIM))
                            .build());
            logSlackApiResponse(response);
            for (Conversation conversation : response.getChannels()) {
                String conversationId = conversation.getId();
                /*
                 * Store the conversation ID as an entry for itself, this is because we cannot differentiate IDs from
                 * regular strings when retrieving a channel ID.
                 */
                workspaceChannelNames.put(conversationId, conversationId);
                if (nonNull(conversation.getName())) {
                    workspaceChannelNames.put(conversation.getName(), conversation.getId());
                    workspaceGroupChannels.add(conversation.getId());
                    Log.debug("Conversation name: {0}, ID: {1}", conversation.getName(), conversationId);
                } else {
                    String userId = conversation.getUser();
                    UsersInfoResponse userResponse = slack.methods().usersInfo(UsersInfoRequest.builder()
                            .token(teamSlackToken)
                            .user(userId)
                            .build());
                    logSlackApiResponse(userResponse);
                    workspaceChannelNames.put(userResponse.getUser().getName(), conversationId);
                    workspaceChannelNames.put(userResponse.getUser().getRealName(), conversationId);
                    workspaceChannelNames.put(userResponse.getUser().getProfile().getDisplayName(), conversationId);
                    workspaceUserChannels.add(conversationId);
                    Log.debug("User name: {0}", userResponse.getUser().getName());
                    Log.debug("User real name: {0}", userResponse.getUser().getRealName());
                    Log.debug("User display name: {0}", userResponse.getUser().getProfile().getDisplayName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
