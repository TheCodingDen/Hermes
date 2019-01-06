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