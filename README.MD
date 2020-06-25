Hermes
======

A Discord bot originally designed to announce stream updates in TCD.

It supports joining and announcing of roles.
Particularly useful for announcement roles (updates, ...)

## Commands

Hermes has multiple commands, 
which can be called by starting the message by mentioning him, then adding the command name

| Command  | Aliases                               | Syntax                                                                    | Description                                                                                                                                             |
|----------|---------------------------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| announce |                                       | announce <role_name> \[ &#124; <channel_mention> ] &#124; <announce_text> | Creates a new announcement. Only users with configured announcer role can create announcements.                                                         |
| config   | configure                             | See config output                                                         | Shows the current configuration and allows to add/remove announcement/announcer roles as well as enable/disable users from subscribing to roles via bot |
| help     |                                       |                                                                           | Lists all available commands                                                                                                                            |
| mention  | ping                                  | mention <role_name>                                                       | Only mentions the given role (useful to draw attention to above messages).  Only users with configured announcer role can use this command.             |
| sub      | subscribe, unsub, unsubscribe, toggle | sub <role_name> \[<role_name> ...]                                        | Join/leave the given role(s). Has to be enabled via config first and only works on announcement roles.                                                  |

## Running Hermes

Hermes supports 2 Methods of supplying the bootup config: Via environment variables or via config.json file.

### Using the config.json file

To use the config.json file, just create a file named `config.json` based on `config-template.json` next to the jar.

The configProvider section is optional and is defined in the [ConfigProvider section](#configproviders)

### Using Environment variables

These Environment variables are supported:

| Variable | Required | Default | Description   |
|----------|:--------:|---------|---------------|
| token    | x        | -       | The Bot token |
| configprovider     |          | com.kantenkugel.hermes.guildConfig.JSONGuildConfigProvider | The ConfigProvider to use |
| configproviderargs |          | {Empty String} | Args to use with the ConfigProvider |

The configprovider variables are explained in [the next section](#configproviders)

## ConfigProviders

Hermes uses a system called ConfigProviders to determine how to store per-guild configs.

It comes with 2 pre-defined ConfigProviders, but custom ConfigProviders can be written as well
(Need to implement the `IGuildConfigProvider` interface)

### JSON ConfigProvider

The JSONGuildConfigProvider uses a very simple json file in order to store its guild configs.

Its Argument (defaults to `guildConfig.json`) defines the path to the json file.

To use the JSONGuildConfigProvider, provide `com.kantenkugel.hermes.guildConfig.JSONGuildConfigProvider` in the config.

### PostgreSQL ConfigProvider

The PGGuildConfigProvider uses a PostgreSQL database (table `guildConfigs`) to store the configs.

Its Argument (defaults to `jdbc:postgresql://localhost/hermes?user=hermes&password=hermespw`)
defines the jdbc connect string to use

To use the PGGuildConfigProvider, provide `com.kantenkugel.hermes.guildConfig.PGGuildConfigProvider` in the config.

### Migrating between ConfigProviders

TODO

## Building Hermes

To build Hermes, simply use the provided gradle wrapper (`gradlew`).
A "fat" jar (with dependencies) can be obtained by running the `fatJar` task (provided by shadow plugin).

Additionally, a Dockerfile is provided

### Building via Dockerfile

The provided Dockerfile has 2 Modes:

- Build the jar during `docker build`. This is the `builder` mode and the default (`--build-arg buildtype=builder`)
- Use a previously built jar. This is the `custom` mode and can be enabled via the `buildtype` build argument:
`--build-arg buildmode=custom`. When using this mode, make sure a `Hermes-*-all.jar` is provided at the build context root.

**Note:** Since this Dockerfile uses different build stages that are controlled by the build argument, 
it is **strongly** recommended to use [Docker Buildkit](https://docs.docker.com/develop/develop-images/build_enhancements/) to improve performance.