Xatkit Slack Platform
=====

[![License Badge](https://img.shields.io/badge/license-EPL%202.0-brightgreen.svg)](https://opensource.org/licenses/EPL-2.0)
[![Build Status](https://travis-ci.com/xatkit-bot-platform/xatkit-slack-platform.svg?branch=master)](https://travis-ci.com/xatkit-bot-platform/xatkit-slack-platform)

Receive and send messages from [Slack](https://slack.com). This platform is bundled with the [Xatkit release](https://github.com/xatkit-bot-platform/xatkit-releases/releases).

The Slack platform is a concrete implementation of the [*ChatPlatform*](https://github.com/xatkit-bot-platform/xatkit-chat-platform).

## Providers

The chat platform defines the following providers:

| Provider                   | Type  | Context Parameters | Description                                                  |
| -------------------------- | ----- | ------------------ | ------------------------------------------------------------ |
| ChatProvider | Intent | - `chat.channel`: the identifier of the channel that sent the message<br/> - `chat.username`: the name of the user that sent the message<br/> - `chat.rawMessage`: the raw message sent by the user (before NLP processing) | The chat intent provider receives messages from a communication channel and translate them into Xatkit-compatible intents (*inherited from [ChatPlatform](https://github.com/xatkit-bot-platform/xatkit-chat-platform)*) |
| SlackIntentProvider | Intent | - `slack.channel`: the identifier of the Slack channel that sent the message<br/> - `slack.username`: the name of the Slack user that sent the message<br/> - `slack.rawMessage`: the raw message sent by the user (before NLP processing)<br/> - `userId`: the Slack unique identifier of the user that sent the message<br/> - `userEmail`: the email address of the Slack user that sent the message | The Slack intent provider receives messages from Slack and translates them into Xatkit-compatible intents. Note that `slack.channel`, `slack.username`, and `slack.rawMessage` contain the same values as `chat.channel`, `chat.username`, and `chat.rawMessage` |

## Actions

| Action | Parameters                                                   | Return                         | Return Type | Description                                                 |
| ------ | ------------------------------------------------------------ | ------------------------------ | ----------- | ----------------------------------------------------------- |
| PostMessage | - `message`(**String**): the message to post<br/> - `channel` (**String**): the name or raw identfier of the Slack  channel to post the message to (direct messages can be sent using the target username as channel name) | The posted message | String | Posts the provided `message` to the given Slack `channel` (*inherited from [ChatPlatform](https://github.com/xatkit-bot-platform/xatkit-chat-platform)*) |
| Reply | - `message` (**String**): the message to post as a reply | The posted message | String | Posts the provided `message` as a reply to a received message (*inherited from [ChatPlatform](https://github.com/xatkit-bot-platform/xatkit-chat-platform)*) |
| PostFileMessage | - `message` (**String**): the message to post with the file<br/> - `file` ([**File**](https://docs.oracle.com/javase/7/docs/api/java/io/File.html)): the file to post<br/> - `channel` (**String**): the name or raw identfier of the Slack  channel to post the message to (direct messages can be sent using the target username as channel name) | `null` | `null` | Post the provided `file` with the provided `message` to the given Slack `channel` (the file title is automatically set with the name of the provided `file`) |
| PostFileMessage | - `title` (**String**): the associated to the file to post<br/> - `message` (**String**): the message to post with the file<br/> - `content` (**String**): the raw content of the file to post<br/> - `channel` (**String**): the name or raw identfier of the Slack  channel to post the message to (direct messages can be sent using the target username as channel name) | `null` | `null` | Post a file with the provided `content` and `title` to the given Slack `channel` |
| ReplyFileMessage | - `message` (**String**): the message to post with the file<br/> - `file` ([**File**](https://docs.oracle.com/javase/7/docs/api/java/io/File.html)): the file to post<br/> | `null` | `null` | Posts the provided `file` as a reply to a received message |
| PostAttachmentsMessage | - `attachments` ([**List\<Attachment\>**](https://github.com/seratch/jslack): the attachments to set in the message<br/> - `channel` (**String**): the name or raw identfier of the Slack  channel to post the message to (direct messages can be sent using the target username as channel name) | `null` | `null` | Post a message with the given `attachments` to the provided Slack `channel` |
| PostAttachmentsMessage | - `pretex` (**String**): the text to display before the attachment<br/> - `title` (**String**): the title of the attachment<br/> - `text` (**String**): the text of the attachment<br/> - `attchColor` (**String**): the color of the attachment (in HEX format)<br/> - `timestamp` (**String**): the timestamp associated to the attachment<br/> - `channel` (**String**): the name or raw identfier of the Slack  channel to post the message to (direct messages can be sent using the target username as channel name) | `null` | `null` | Post a message containing the given `pretext` with an attachment containing the provided `title`, `text`, `attachColor`, and `timestamp` to the given Slack `channel` |
| PostAttachmentMessage | - `pretex` (**String**): the text to display before the attachment<br/> - `title` (**String**): the title of the attachment<br/> - `text` (**String**): the text of the attachment<br/> - `attchColor` (**String**): the color of the attachment (in HEX format)<br/> - `channel` (**String**): the name or raw identfier of the Slack  channel to post the message to (direct messages can be sent using the target username as channel name) | `null` | `null` | Post a message containing the given `pretext` with an attachment containing the provided `title`, `text`, and `attachColor` to the given Slack `channel` (the attachment `timestamp` is automatically set to the current date) |
| ReplyAttachmentMessage | - `attachments` ([**List\<Attachment\>**](https://github.com/seratch/jslack): the attachments to set in the message | `null` | `null` | Posts a message with the given `attachments` as a reply to a received message |
| ReplyAttachmentMessage | - `pretex` (**String**): the text to display before the attachment<br/> - `title` (**String**): the title of the attachment<br/> - `text` (**String**): the text of the attachment<br/> - `attchColor` (**String**): the color of the attachment (in HEX format)<br/> - `timestamp` (**String**): the timestamp associated to the attachment<br/> | `null` | `null` | Post a message containing the given `pretext` with an attachment containing the provided `title`, `text`, `attachColor`, and `timestamp` as a reply to a received message |
| ReplyAttachmentsMessage | `pretex` (**String**): the text to display before the attachment<br/> - `title` (**String**): the title of the attachment<br/> - `text` (**String**): the text of the attachment<br/> - `attchColor` (**String**): the color of the attachment (in HEX format) | `null` | `null` | Post a message containing the given `pretext` with an attachment containing the provided `title`, `text`, and `attachColor` as a reply to a received message (the attachment `timestamp` is automatically set to the current date) |
| ItemizeList | - `list` ([**List**](https://docs.oracle.com/javase/7/docs/api/java/util/List.html)): the list to itemize | A String presenting the provided `list` as a set of items | String | Creates a set of items from the provided `list`. This actions relies on `Object.toString()` to print each item's content |
| ItemizeList | - `list` ([**List**](https://docs.oracle.com/javase/7/docs/api/java/util/List.html)): the list to itemize<br/> - `formatter` ([**Formatter**](https://xatkit-bot-platform.github.io/xatkit-runtime-docs/releases/snapshot/doc/com/xatkit/core/platform/Formatter.html) the formatter used to print each item | A String presenting the provided `list` as a set of items formatted with the given `formatter` | String | Creates a set of items from the provided `list`. This action relies on the provided `formatter` to print each item's content |
| EnumerateList | - `list` ([**List**](https://docs.oracle.com/javase/7/docs/api/java/util/List.html)): the list to enumerate | A String presenting the provided `list` as an enumeration | String | Creates an enumeration from the provided `list`. This actions relies on `Object.toString()` to print each item's content |
| EnumerateList | - `list` ([**List**](https://docs.oracle.com/javase/7/docs/api/java/util/List.html)): the list to enumerate<br/> - `formatter` ([**Formatter**](https://xatkit-bot-platform.github.io/xatkit-runtime-docs/releases/snapshot/doc/com/xatkit/core/platform/Formatter.html) the formatter used to print each item | A String presenting the provided `list` as an enumeration formatted with the given `formatter` | String | Creates an enumeration from the provided `list`. This action relies on the provided `formatter` to print each item's content |

## Options

The chat platform supports the following configuration options

| Key                  | Values | Description                                                  | Constraint    |
| -------------------- | ------ | ------------------------------------------------------------ | ------------- |
| `xatkit.slack.token` | String | The [Slack token](https://api.slack.com/) used by Xatkit to deploy the bot | **Mandatory** |

**Note**: if the Slack platform is used as a concrete implementation of the [*ChatPlatform*](https://github.com/xatkit-bot-platform/xatkit-chat-platform) the following property must be set in the Xatkit configuration:

```properties
xatkit.platforms.abstract.ChatPlatform = com.xatkit.plugins.slack.platform.SlackPlatform
```
