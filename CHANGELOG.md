# Changelog

All notable changes for the Slack platform will be documented in this file.

Note that there is no changelog available for the initial release of the platform (2.0.0), you can find the release notes [here](https://github.com/xatkit-bot-platform/xatkit-slack-platform/releases).

The changelog format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/v2.0.0.html)

## Unreleased

### Added

- Erroring `SlackApiResponse` are now logged to ease debugging. If the error refers to OAuth scopes the required/provided scopes are logged.

## [3.0.0] - 2019-12-01

### Added
- `PostMessage` now accepts an optional parameter `threadTs` to specify the thread to post the message to.

### Changed
- `PostMessage` and `Reply` now return the timestamp of the posted message instead of `null`. This allows to reuse this timestamp to post in a given thread.
- Action parameters and return are now statically typed. **This change breaks the public API**: execution models relying on the generic `Object` type for parameter and return now need to cast values to the expected type. (e.g. `ChatPlatform.Reply(message)` now requires that `message` is a `String`, this can be fixed with the following syntax `ChatPlatform.Reply(message as String)`).  

### Fixed
- `PostAttachmentsMessage` now correctly retrieves the channel to post to when the provided `channel` parameter refers to a channel name of a user name.

## [2.1.0] - 2019-10-10

### Added

- `xatkit.slack.listen_mentions_on_group_channels` configuration option to specify whether the bot should only listen to mentions (messages containing *@bot*) in group channels. This configuration option is **optional** and is set by default to **false**.
- The `slack` context now defines the `messageTs` and `threadTs`parameters corresponding to the timestamp of the message and the timestamp of its containing thread message (optional, `null` if the message is not contained in a thread).
- Action `PostMessage(message, channel, threadTs)`. This action posts the provided *message* in the given *thread*. **Note**: this new action doesn't change the behavior of `PostMessage(message, channel)`.

### Changed

- `SlackIntentProvider` and `ChatProvider` now use the new intent provider hierarchy (see [xatkit-runtime/#221](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/221)). This doesn't change the public API, but requires to use the latest versions of [xatkit-runtime](https://github.com/xatkit-bot-platform/xatkit-runtime) and [xatkit-chat-platform](https://github.com/xatkit-bot-platform/xatkit-chat-platform).
- `PostMessage` and `Reply` actions now return the timestamp of the posted message instead of `null`. This allows to store the timestamp and reuse it to post messages in specific threads.

### Fixed

- `SlackUtils#DEFAULT_IGNORE_FALLBACK_ON_GROUP_CHANNELS_KEY` has been renamed to `SlackUtils#DEFAULT_IGNORE_FALLBACK_ON_GROUP_CHANNELS` to conform to the format of other configuration keys.
- Reply action now posts message in its containing thread if it exists instead of in the channel top-level conversation
- `PostAttachmentsMessage` now posts to the correct Slack channel when its `channel` attribute refers to an username or channel name.

## [2.0.0] - 2019-08-20 

See the release notes [here](https://github.com/xatkit-bot-platform/xatkit-slack-platform/releases).
