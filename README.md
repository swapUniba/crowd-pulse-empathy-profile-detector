# crowd-pulse-empathy-profile-detector
This plugin detects the user empathy from all input messages. Empathy value is in the [0, 1] range.

Plugin configuration example:
```json
"empathyDetector": {
  "plugin": "empathy-profile-detector",
  "config": {
    "profilesDatabaseName": "profiles",
    "username": "abcUserName"
  }
}
```
- **profilesDatabaseName**: the profiles database name to connect and retrieve the user profile entity;
- **username**: the username used to get the user profile from database.

Example of usage:
```json
{
  "process": {
    "name": "empathy-tester",
    "logs": "/opt/crowd-pulse/logs"
  },
  "nodes": {
    "fetch": {
      "plugin": "message-fetch",
      "config": {
        "db": "test"
      }
    },
    "empathyDetector": {
      "plugin": "empathy-profile-detector",
      "config": {
        "profilesDatabaseName": "profiles",
        "username": "{{dbName}}"
      }
    }
  },
  "edges": {
    "fetch": [
      "empathyDetector"
    ]
  }
}
```

**IMPORTANT**: the EmpathyDetector plugin has its own persistence mechanism, so you don't need the ProfilePersister
plugin.