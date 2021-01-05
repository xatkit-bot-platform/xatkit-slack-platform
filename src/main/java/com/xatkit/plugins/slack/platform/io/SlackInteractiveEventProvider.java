package com.xatkit.plugins.slack.platform.io;

import com.github.seratch.jslack.api.model.block.element.ButtonElement;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.xatkit.core.platform.io.WebhookEventProvider;
import com.xatkit.core.server.RestHandler;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.IntentFactory;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.slack.SlackUtils;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.xatkit.dsl.DSL.event;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An event provider matching requests from Slack interactive components.
 * <p>
 * This provider supports payloads representing interactions with buttons ({@link #ButtonClicked}) and checkboxes
 * ({@link #CheckboxesChanged}). See the documentation of the corresponding events for more information.
 * <p>
 * This provider requires some additional configuration in the settings of the Slack app corresponding to your bot:
 * you need to enable interactivity in the <i>Interactivity & Shortcuts</i> tab, and specify the URL of the endpoint
 * to send the payloads to. Note that the URL associated to this provider has to end with {@code /slack-interactive},
 * e.g. {@code https://my.xatkit.bot/slack-interactive}.
 */
public class SlackInteractiveEventProvider extends WebhookEventProvider<SlackPlatform, RestHandler<JsonElement>> {

    /**
     * The URI of the endpoint that receives payloads from Slack interactive elements.
     */
    private final static String ENDPOINT_URI = "/slack-interactive";

    /**
     * Constructs the provider and sets its containing {@link SlackPlatform}.
     *
     * @param slackPlatform the containing {@link SlackPlatform}
     */
    public SlackInteractiveEventProvider(SlackPlatform slackPlatform) {
        super(slackPlatform);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEndpointURI() {
        return ENDPOINT_URI;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RestHandler<JsonElement> createRestHandler() {
        return new SlackInteractiveMessageRestHandler();
    }

    /**
     * A {@link RestHandler} receiving requests from Slack interactive components.
     * <p>
     * The request parsing requirements are detailed in the
     * <a href="https://api.slack.com/interactivity/handling#payloads">Slack documentation</a>
     * . These requirements force us to define a new {@link RestHandler} implementation: the
     * {@link com.xatkit.core.server.JsonRestHandler} cannot handle non-JSON input requiring a pre-processing.
     */
    private class SlackInteractiveMessageRestHandler extends RestHandler<JsonElement> {

        /**
         * {@inheritDoc}
         * <p>
         * The <a href="https://api.slack.com/interactivity/handling#payloads">Slack documentation</a>
         * details the type of the received request ({@code application/x-www-form-urlencoded}. See
         * {@link #parseContent(Object)} for more information.
         *
         * @param contentType the content type to check
         * @return {@code true} if the provided {@code contentType} is {@code application/x-www-form-urlencoded},
         * {@code false} otherwise
         */
        @Override
        public boolean acceptContentType(String contentType) {
            return ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType);
        }

        /**
         * Parses the received {@link Object} and creates a {@link JsonElement}.
         * <p>
         * This method implements the parsing requirements detailed in the
         * <a href="https://api.slack.com/interactivity/handling#payloads">Slack documentation</a>. These requirements
         * force us to define a new {@link RestHandler} implementation: the
         * {@link com.xatkit.core.server.JsonRestHandler} cannot handle non-JSON input requiring a pre-processing.
         *
         * @param o the received payload
         * @return the parsed {@link JsonElement}
         * @throws IllegalArgumentException if the provided {@code o} is not a {@link String} or if it does not
         *                                  contain a {@code payload} attribute.
         */
        @Nullable
        @Override
        protected JsonElement parseContent(@Nullable Object o) {
            if (o instanceof String) {
                /*
                 * From the Slack documentation: the received payload is an application/x-www-form-urlencoded with a
                 * payload attribute. The following lines use HttpCore utility classes to extract the value of the
                 * payload attribute.
                 */
                List<NameValuePair> pairs = URLEncodedUtils.parse((String) o, StandardCharsets.UTF_8);
                NameValuePair payloadPair = pairs.stream().filter(p -> p.getName().equals("payload")).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("Cannot parse the "
                                + "received "
                                + "String: cannot find the 'payload' attribute from {0}", o)));
                return new JsonParser().parse(payloadPair.getValue());
            } else {
                throw new IllegalArgumentException(MessageFormat.format("Cannot parse the received payload: expected a "
                        + "String, found {0}", isNull(o) ? "null" : o.getClass().getSimpleName()));
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * This handler supports payloads representing interactions with buttons and checkboxes. Other interactions
         * are not supported yet and throw an {@link IllegalArgumentException}.
         *
         * @return {@code null}
         * @throws IllegalArgumentException if the {@code jsonElement} describes an action that is not supported by
         *                                  the Slack platform
         * @see #ButtonClicked
         * @see #CheckboxesChanged
         */
        @Nullable
        @Override
        protected Object handleParsedContent(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> parameters,
                                             @Nullable JsonElement jsonElement) {
            if (nonNull(jsonElement)) {
                Log.debug("Received the following payload from Slack:\n{0}",
                        new GsonBuilder().setPrettyPrinting().create().toJson(jsonElement));
                JsonElement typeElement = jsonElement.getAsJsonObject().get("type");
                if (nonNull(typeElement)) {
                    String type = typeElement.getAsString();
                    if (type.equals("block_actions")) {
                        JsonArray actions = jsonElement.getAsJsonObject().get("actions").getAsJsonArray();
                        String actionType = actions.get(0).getAsJsonObject().get("type").getAsString();
                        EventInstance eventInstance;
                        switch (actionType) {
                            case "button":
                                eventInstance = createButtonClickedEvent(actions);
                                break;
                            case "checkboxes":
                                eventInstance = createCheckboxesChangedEvent(actions);
                                break;
                            default:
                                throw new IllegalArgumentException(MessageFormat.format("Action {0} is not supported",
                                        actionType));
                        }
                        String team =
                                jsonElement.getAsJsonObject().get("team").getAsJsonObject().get("id").getAsString();
                        String channel =
                                jsonElement.getAsJsonObject().get("channel").getAsJsonObject().get("id").getAsString();
                        eventInstance.getPlatformData().put(ChatUtils.CHAT_CHANNEL_CONTEXT_KEY, channel);
                        eventInstance.getPlatformData().put(SlackUtils.SLACK_TEAM_CONTEXT_KEY, team);
                        StateContext context =
                                SlackInteractiveEventProvider.this.getRuntimePlatform().createSessionFromChannel(team,
                                        channel);
                        SlackInteractiveEventProvider.this.sendEventInstance(eventInstance, context);
                    } else {
                        System.out.println("Unsupported type " + type);
                    }
                } else {
                    /*
                     * The payload does not contain a type, ignore it.
                     */
                }
            }
            return null;
        }

        /**
         * Creates the {@link EventInstance} representing an interactive button clicked by a user.
         * <p>
         * The created {@link EventInstance}'s definition is set with {@link #ButtonClicked}, and contains a {@code
         * value} parameter holding the value of the clicked button.
         *
         * @param actionsArray the {@link JsonArray} containing the {@code actions} of the received payload
         * @return the created {@link EventInstance}
         * @throws NullPointerException     if the provided {@code actionsArray} is {@code null}
         * @throws IllegalArgumentException if the provided {@code actionsArray} does not contain a {@code value}
         *                                  field for the clicked button
         * @see #ButtonClicked
         */
        private @NonNull EventInstance createButtonClickedEvent(@NonNull JsonArray actionsArray) {
            EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
            eventInstance.setDefinition(ButtonClicked);
            /*
             * We assume here that the actions array in the payload contains a single element. This seems to be always
             * true but it is not properly documented. We print a log asking to contact the developers if there is zero
             * or more than one element in the array.
             */
            if (actionsArray.size() != 1) {
                Log.warn("Found {0} element(s) in the 'actions' array while expecting 1, please notify the developers "
                        + "about it.");
            }
            JsonElement valueElement = actionsArray.get(0).getAsJsonObject().get("value");
            if (isNull(valueElement)) {
                throw new IllegalArgumentException(MessageFormat.format("The received payload for {0} does not "
                        + "contain a \"value\" field. Make sure you specified it in the corresponding {1} of your "
                        + "bot", ButtonClicked.getName(), ButtonElement.class.getSimpleName()));
            }
            ContextParameterValue contextParameterValue = IntentFactory.eINSTANCE.createContextParameterValue();
            contextParameterValue.setValue(valueElement.getAsString());
            contextParameterValue.setContextParameter(ButtonClicked.getParameter("value"));
            eventInstance.getValues().add(contextParameterValue);
            return eventInstance;
        }

        /**
         * Creates the {@link EventInstance} representing an interactive checkbox element selected by a user.
         * <p>
         * <b>Note</b>: a {@link EventInstance} is created for each checkbox (un)selected by the user. It is the bot
         * developer's responsibility to handle these events properly.
         * <p>
         * The created {@link EventInstance}'s definition is set with {@link #CheckboxesChanged}, and contains a
         * {@code selected_options} parameter holding a list of {@link String}s representing the selected options in
         * the checkbox group. Note that the {@link String} elements contain the <i>values</i> of the checkboxes, not
         * their text.
         *
         * @param actionsArray the {@link JsonArray} containing the {@code actions} of the received payload
         * @return the created {@link EventInstance}
         * @throws NullPointerException if the provided {@code actionsArray} is {@code null}
         * @see #CheckboxesChanged
         */
        private @NonNull EventInstance createCheckboxesChangedEvent(@NonNull JsonArray actionsArray) {
            EventInstance eventInstance = IntentFactory.eINSTANCE.createEventInstance();
            eventInstance.setDefinition(CheckboxesChanged);
            /*
             * We assume here that the actions array in the payload contains a single element. This seems to be always
             * true but it is not properly documented. We print a log asking to contact the developers if there is zero
             * or more than one element in the array.
             */
            if (actionsArray.size() != 1) {
                Log.warn("Found {0} element(s) in the 'actions' array while expecting 1, please notify the developers "
                        + "about it.");
            }
            JsonArray selectedOptions = actionsArray.get(0).getAsJsonObject().get("selected_options").getAsJsonArray();
            List<String> selectedOptionValues = StreamSupport.stream(selectedOptions.spliterator(), false)
                    .map(o -> o.getAsJsonObject().get("value").getAsString())
                    .collect(Collectors.toList());
            ContextParameterValue contextParameterValue = IntentFactory.eINSTANCE.createContextParameterValue();
            contextParameterValue.setValue(selectedOptionValues);
            contextParameterValue.setContextParameter(CheckboxesChanged.getParameter("selected_options"));
            eventInstance.getValues().add(contextParameterValue);
            return eventInstance;
        }
    }

    /**
     * An event sent when an interactive button is clicked by a user.
     * <p>
     * This event contains a {@code value} parameter holding the value of the clicked button.
     */
    public static EventDefinition ButtonClicked = event("ButtonClicked")
            .parameter("value")
            .getEventDefinition();

    /**
     * An event sent when an interactive checkbox element is selected by a user.
     * <p>
     * This event contains a {@code selected_options} parameter holding a list of {@link String}s representing the
     * selected options in the checkbox group. Note that the {@link String} elements contain the <i>values</i> of the
     * checkboxes, not their text.
     */
    public static EventDefinition CheckboxesChanged = event("CheckboxesChanged")
            .parameter("selected_options")
            .getEventDefinition();
}
