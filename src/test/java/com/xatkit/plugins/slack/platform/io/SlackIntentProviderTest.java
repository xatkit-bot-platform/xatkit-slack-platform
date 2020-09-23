package com.xatkit.plugins.slack.platform.io;

import com.xatkit.AbstractEventProviderTest;
import com.xatkit.core.ExecutionService;
import com.xatkit.core.recognition.IntentRecognitionProviderException;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import com.xatkit.plugins.slack.platform.SlackTestUtils;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.text.MessageFormat;
import java.util.Map;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SlackIntentProviderTest extends AbstractEventProviderTest<SlackIntentProvider, SlackPlatform> {

    private static IntentDefinition VALID_EVENT_DEFINITION;

    private static RecognizedIntent VALID_RECOGNIZED_INTENT;

    @BeforeClass
    public static void setUpBeforeClass() {
        VALID_EVENT_DEFINITION = IntentFactory.eINSTANCE.createIntentDefinition();
        VALID_EVENT_DEFINITION.setName("Default Welcome Intent");
        VALID_RECOGNIZED_INTENT = IntentFactory.eINSTANCE.createRecognizedIntent();
        VALID_RECOGNIZED_INTENT.setDefinition(VALID_EVENT_DEFINITION);
    }

    private String slackTeamId;

    private String slackChannel;

    private StateContext context;

    private ExecutionService mockedExecutionService;

    @Before
    public void setUp() {
        super.setUp();
        context = ExecutionFactory.eINSTANCE.createStateContext();
        context.setContextId("TEST");
        mockedExecutionService = mock(ExecutionService.class);
        when(mockedXatkitBot.getExecutionService()).thenReturn(mockedExecutionService);
        when(mockedXatkitBot.getOrCreateContext(any(String.class))).thenReturn(context);
        slackTeamId = platform.getTeamIdToSlackTokenMap().entrySet().stream()
                .filter((entry) -> entry.getValue().equals(SlackTestUtils.getSlackToken()))
                .findAny().get().getKey();
        slackChannel = platform.getChannelId(slackTeamId, "général");
    }

    @After
    public void tearDown() {
        if (nonNull(provider)) {
            provider.close();
        }
        super.tearDown();
    }

    @Test(expected = NullPointerException.class)
    public void constructNullPlatform() {
        provider = new SlackIntentProvider(null);
    }

    @Test(expected = NullPointerException.class)
    public void startNullConfiguration() {
        provider = new SlackIntentProvider(platform);
        provider.start(null);
    }

    @Test
    public void startValidConfiguration() {
        Configuration configuration = getValidSlackIntentProviderConfiguration();
        provider = new SlackIntentProvider(platform);
        provider.start(configuration);
        assertThat(provider.getRtmClient(slackTeamId)).as("Not null RTM client").isNotNull();
    }

    @Test
    public void sendValidSlackMessage() throws IntentRecognitionProviderException {
        /*
         * Configure the mock to return a valid IntentDefinition.
         */
        when(mockedIntentRecognitionProvider.getIntent(any(String.class), any(StateContext.class))).thenReturn(VALID_RECOGNIZED_INTENT);
        provider = getValidSlackInputProvider();
        provider.getRtmClient(slackTeamId).onMessage(getValidMessage());
        ArgumentCaptor<EventInstance> eventCaptor = ArgumentCaptor.forClass(EventInstance.class);
        verify(mockedExecutionService, times(1)).handleEventInstance(eventCaptor.capture(), any(StateContext.class));
        /*
         * Check on the EventInstance and not the context here: the event hasn't been assigned to the context yet
         * (it's done in the ExecutionService).
         */
        EventInstance sentEvent = eventCaptor.getValue();
        assertThat(sentEvent.getDefinition().getName()).isEqualTo(VALID_EVENT_DEFINITION.getName());

        verify(mockedXatkitBot, times(1)).getOrCreateContext(eq(slackTeamId + "@" + slackChannel));
        Object contextChannel = sentEvent.getPlatformData().get(SlackUtils.CHAT_CHANNEL_CONTEXT_KEY);
        assertThat(contextChannel).as("Not null channel context variable").isNotNull();
        assertThat(contextChannel).as("Channel context variable is a String").isInstanceOf(String.class);
        assertThat(contextChannel).as("Valid channel context variable").isEqualTo(slackChannel);
        Object contextUsername = sentEvent.getPlatformData().get(SlackUtils.CHAT_USERNAME_CONTEXT_KEY);
        assertThat(contextUsername).as("Not null username context variable").isNotNull();
        assertThat(contextUsername).as("Username context variable is a String").isInstanceOf(String.class);
        assertThat(contextUsername).as("Valid context username variable").isEqualTo("gwendal");
    }

    @Test
    public void sendMentionGroupChannelListenToMentionProperty() throws IntentRecognitionProviderException {
        /*
         * Configure the mock to return a valid IntentDefinition.
         */
        when(mockedIntentRecognitionProvider.getIntent(any(String.class), any(StateContext.class))).thenReturn(VALID_RECOGNIZED_INTENT);
        Configuration configuration = getValidSlackIntentProviderConfiguration();
        configuration.addProperty(SlackUtils.LISTEN_MENTIONS_ON_GROUP_CHANNELS_KEY, true);
        provider = new SlackIntentProvider(platform);
        provider.start(configuration);
        provider.getRtmClient(slackTeamId).onMessage(getValidMessageMention());
        ArgumentCaptor<EventInstance> eventCaptor = ArgumentCaptor.forClass(EventInstance.class);
        verify(mockedExecutionService, times(1)).handleEventInstance(eventCaptor.capture(), any(StateContext.class));
        /*
         * Check on the EventInstance and not the context here: the event hasn't been assigned to the context yet
         * (it's done in the ExecutionService).
         */
        EventInstance sentEvent = eventCaptor.getValue();
        String rawMessage =
                (String) sentEvent.getPlatformData().get(SlackUtils.CHAT_RAW_MESSAGE_CONTEXT_KEY);
        assertThat(rawMessage).as("Message not empty").isNotEmpty();
        assertThat(rawMessage).as("Filtered mention").doesNotContain("<@" + provider.getSelfId(slackTeamId) + ">");
    }

    @Test
    public void sendNoMentionGroupChannelListenToMentionProperty() {
        Configuration configuration = getValidSlackIntentProviderConfiguration();
        configuration.addProperty(SlackUtils.LISTEN_MENTIONS_ON_GROUP_CHANNELS_KEY, true);
        provider = new SlackIntentProvider(platform);
        provider.start(configuration);
        provider.getRtmClient(slackTeamId).onMessage(getValidMessage());
        verify(mockedExecutionService, times(0)).handleEventInstance(any(EventInstance.class),
                any(StateContext.class));
    }

    @Test
    public void sendSlackMessageInvalidType() {
        provider = getValidSlackInputProvider();
        provider.getRtmClient(slackTeamId).onMessage(getMessageInvalidType());
        verify(mockedExecutionService, times(0)).handleEventInstance(any(EventInstance.class),
                any(StateContext.class));
    }

    @Test
    public void sendSlackMessageNullText() {
        provider = getValidSlackInputProvider();
        provider.getRtmClient(slackTeamId).onMessage(getMessageNullText());
        verify(mockedExecutionService, times(0)).handleEventInstance(any(EventInstance.class),
                any(StateContext.class));
    }

    @Test
    public void sendSlackMessageNullChannel() {
        provider = getValidSlackInputProvider();
        provider.getRtmClient(slackTeamId).onMessage(getMessageNullChannel());
        verify(mockedExecutionService, times(0)).handleEventInstance(any(EventInstance.class),
                any(StateContext.class));
    }

    @Test
    public void sendSlackMessageNullUser() {
        provider = getValidSlackInputProvider();
        provider.getRtmClient(slackTeamId).onMessage(getMessageNullUser());
        verify(mockedExecutionService, times(0)).handleEventInstance(any(EventInstance.class),
                any(StateContext.class));
    }

    @Test
    public void sendSlackMessageEmptyMessage() {
        provider = getValidSlackInputProvider();
        provider.getRtmClient(slackTeamId).onMessage(getMessageEmptyText());
        verify(mockedExecutionService, times(0)).handleEventInstance(any(EventInstance.class),
                any(StateContext.class));
    }

    @Override
    protected SlackPlatform getPlatform() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(SlackUtils.SLACK_TOKEN_KEY, SlackTestUtils.getSlackToken());
        SlackPlatform slackPlatform = new SlackPlatform();
        slackPlatform.start(mockedXatkitBot, configuration);
        return slackPlatform;
    }

    private SlackIntentProvider getValidSlackInputProvider() {
        Configuration configuration = getValidSlackIntentProviderConfiguration();
        SlackIntentProvider provider = new SlackIntentProvider(platform);
        provider.start(configuration);
        return provider;
    }

    private Configuration getValidSlackIntentProviderConfiguration() {
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(SlackUtils.SLACK_TOKEN_KEY, SlackTestUtils.getSlackToken());
        return configuration;
    }

    private String getValidMessage() {
        return MessageFormat.format("'{'\"type\":\"message\",\"text\":\"hello\", \"team\":\"{0}\", " +
                "\"channel\":\"{1}\", \"user\":\"UBD4Z7SKH\"'}'", slackTeamId, slackChannel);
    }

    private String getValidMessageMention() {
        String botMention = "<@" + provider.getSelfId(platform.getSlackToken(slackTeamId)) + ">";
        return MessageFormat.format("'{'\"type\":\"message\",\"text\":\"hello {0}\", \"team\":\"{1}\", " +
                "\"channel\":\"{2}\", \"user\":\"UBD4Z7SKH\"'}'", botMention, slackTeamId, slackChannel);
    }

    private String getMessageInvalidType() {
        return MessageFormat.format("'{'\"type\":\"invalid\",\"text\":\"hello\", \"team\":\"{0}\", " +
                "\"channel\":\"{1}\", \"user\":\"123\"'}'", slackTeamId, slackChannel);
    }

    private String getMessageNullText() {
        return MessageFormat.format("'{'\"type\":\"message\",  \"team\":\"{0}\", \"channel\":\"{1}\", " +
                "\"user\":\"123\"'}'", slackTeamId, slackChannel);
    }

    private String getMessageNullChannel() {
        return "{\"type\":\"message\", \"user\":\"123\"}";
    }

    private String getMessageNullUser() {
        return "{\"type\":\"message\"}";
    }

    private String getMessageEmptyText() {
        return MessageFormat.format("'{'\"type\":\"message\",\"text\":\"\",  \"team\":\"{0}\", \"channel\":\"{1}\", " +
                "\"user\":\"123\"'}'", slackTeamId, slackChannel);
    }

}
