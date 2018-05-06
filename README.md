## A simple Java Scrobble Client for Last.fm

This client uses [jkovac's](https://github.com/jkovacs) [lastfm-java](https://github.com/jkovacs/lastfm-java) library for calls of the official [Last.fm API](https://www.last.fm/api/intro) as well as the [lastfm-unscrobble-java](https://github.com/calne-ca/lastfm-unscrobble-java) library for unscrobbling and updating Scrobbles.

The point of this library is to simplify all interactions with Last.fm concerning scrobbles as well as providing extended features - relying on official and unofficial features of Last.fm - such as scrobble updating.

### Usage

**Initializing the Scrobble Client**

```java
LastfmAuthenticationDetails authenticationDetails = new LastfmAuthenticationDetails();
authenticationDetails.setApiKey("api-key");
authenticationDetails.setSharedSecret("shared-secret");
authenticationDetails.setUsername("username");
authenticationDetails.setPassword("password");

ScrobbleClient scrobbleClient = new ScrobbleClient();

try {
    scrobbleClient.login(authenticationDetails);
} catch(LastfmAuthenticationException e){
    System.err.println(e.getMessage());
}
```

Not all authentication details are mandatory depending on what you want to do. Certain operations require certain authentication details as shown in the table below:

|               | Scrobbling                | Fetching Scrobbles | Unscrobbling    | Updating Scrobbles    |
| ------------- | :-------------:           | :-------------:    | :-------------: | :-------------: |
| API key       | X                         | X                  |                 | X               |
| Shared Secret | X                         |                    |                 | X               |
| Username      | X                         | X                  | X               | X               |
| Password      | X                         |                    | X               | X               |

So if for example you only want to fetch scrobble data from a user then the username and your API key is sufficient.

**Fetching all Scrobbles**
```java
List<Scrobble> scrobbles = scrobbleClient.getAllScrobbles();
```

**Fetching Scrobbles from a specific time**
```java
Temporal since = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
List<Scrobble> scrobblesSince = scrobbleClient.getScrobblesSince(since);
```

**Fetching a certain amount of Scrobbles**
```java
List<Scrobble> scrobbles = scrobbleClient.getLastScrobbles(100);
```

**Scrobbling a Track**
```java
Scrobble scrobble = scrobbleClient.scrobble("LIQ","[un]INSOMNIA");
```

**Unscrobbling a Scrobble**
```java
Scrobble lastScrobble = scrobbles.get(0);
scrobbleClient.unscrobble(lastScrobble);
```

**Updating Scrobble data**
```java
scrobble.setArtist("LIQ feat. 結月ゆかり");
scrobbleClient.updateScrobble(scrobble);
```

### Maven Dependency
```xml
<dependency>
    <groupId>net.beardbot</groupId>
    <artifactId>lastfm-simple-scrobble-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

