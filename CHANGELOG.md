# Changelog

All notable changes for the Slack platform will be documented in this file.

Note that there is no changelog available for the initial release of the platform (2.0.0), you can find the release notes [here](https://github.com/xatkit-bot-platform/xatkit-slack-platform/releases).

The changelog format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/v2.0.0.html)

## Unreleased

### Added

- `xatkit.slack.listen_mentions_on_group_channels` configuration option to specify whether the bot should only listen to mentions (messages containing *@bot*) in group channels. This configuration option is **optional** and is set by default to **false**.
- The `slack` context now defines the `messageTs` and `threadTs`parameters corresponding to the timestamp of the message and the timestamp of its containing thread message (optional, `null` if the message is not contained in a thread).

### Fixed

- `SlackUtils#DEFAULT_IGNORE_FALLBACK_ON_GROUP_CHANNELS_KEY` has been renamed to `SlackUtils#DEFAULT_IGNORE_FALLBACK_ON_GROUP_CHANNELS` to conform to the format of other configuration keys.
- Reply action now posts message in its containing thread if it exists instead of in the channel top-level conversation

## [2.0.0] - 2019-08-20 

See the release notes [here](https://github.com/xatkit-bot-platform/xatkit-slack-platform/releases).
