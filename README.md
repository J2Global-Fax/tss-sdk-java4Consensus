# tss-sdk-java4Consensus


The '[tss-sdk-java4Consensus](https://github.com/J2Global-Fax/tss-sdk-java4Consensus)' is a Gradle plugin-in designed to connect to Consensus's [Thycotic Secret Server](https://laxsecretserver.j2global.com/secretserver/Login.aspx) (TSS) and retrieve secrets via REST API based on the ID of the secret on the Server.  The plugin can be found on Consensus's Maven Server at the URL
``` 
http://repository.j2.com/maven2/consensus-secret/consensus-secret.gradle.plugin/
http://source.j2.com/maven2/consensus-secret/consensus-secret.gradle.plugin/
```
The plugins above are dependent on the maven files at the URL
``` 
http://repository.j2.com/maven2/com/thycotic/secrets/tss-sdk-java4Consensus/
http://source.j2.com/maven2/com/thycotic/secrets/tss-sdk-java4Consensus/
```

This project is a derivative of Thycotic Secret Server '[tss-sdk-java](https://github.com/DelineaXPM/tss-sdk-java)' (now known as Delinea Secret Server).


## Install into your application

>The minimum JDK required for this project is JDK 8.

###### build.gradle values to set
You can use this plugin in your Gradle application by adding the following dependency in your build.gradle file:
```groovy
plugins {
    id "consensus-secret" version "1.0.1"
}
```

###### settings.gradle values to set

This plugin is only available on Consensus's Maven repository.  You will also need to add the following to the file 'settings.gradle' or 'build.gradle' if the pluginManagement closure exists.
```groovy 
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            url 'http://repository.j2.com/maven2'
            allowInsecureProtocol( true )
        }
        maven {
            url 'http://source.j2.com/maven2'
            allowInsecureProtocol (true)
        }
    }
}
```

###### gradle.properties settings in home directory
To Authenticate into the server, open or create the file 'gradle.properties' in your home directory of the file path
```
# for Windows users
C:\Users\{user name}\.gradle\gradle.properties

# for *nix users
/home/{user name}/.gradle/gradle.properties
```

Add the keys below with the respective information such as your username and password into the Thycotic Secret Server.
```properties
secret_server.oauth2.username = -- enter username here --
secret_server.oauth2.password = -- enter password here --
secret_server.oauth2.domain=j2global
```

Make sure you set the permissions to this file as Read-Only for you -- not to others or others in your group.

###### gradle.properties settings in parent of project directory
*if the parent directory of the project contains a gradle.properties file, this file will be read and included in the project's extra properties.  This will allow you to easily manage keys for development and keys for Deployment/Production.
*

```
root development directory
    |
    +--- gradle.properties (this could include secret keys to be read)
    |
    \--- GradleProject
        +--- build.gradle
        +--- src/main/java
        \--- gradle.properties

root deployment directory
    |
    +--- gradle.properties (this could include secret keys to be read)
    |
    \--- GradleProject
        +--- build.gradle
        +--- src/main/java
        \--- gradle.properties
```


For example, using the directory tree above, the key/value for development could contain
```properties
consensus.GradleProject.secret.key=1
...
```
...in the deployment directory, the key will likely have a different value. Place the same key in the gradle.properties in the root deployment directory with the production value...
```properties
consensus.GradleProject.secret.key=12345
...
```
###### extracting the Secret
This simple example expects the secret server object to be  created and placed in the project's extra properties with the key 'consensus-secret'. The instance (SecretServer) will be available for any task you create or extend.  In the example below the task 'obtainSecret' is arbitrarily named.
```groovy
import com.thycotic.secrets.server.spring.SecretServer
import com.thycotic.secrets.server.spring.Secret

task obtainSecret {
    doLast {
        Map<String, Object> map = project.
                getExtensions().
                extraProperties.
                getProperties()
        if (map.containsKey("consensus-secret")) {
            SecretServer secretServer = (SecretServer) map.get("consensus-secret")
            Secret secret = secretServer.getSecret(1) // 1 represents a particular key in TSS
            // do something with the secret.
        } else {
            throw new RuntimeException("Unable to interface with Thycotic Secret Server.  Check the log files for the reason.")
        }
    }
}
```

## Build locally

###### Find ID of secret

The ID can be obtained from logging into the Secret Server, for example using 'DOC Engineering/DEV Systems/DOC-J2Aura /ccaccountupdater-DEV and clicking 'ccaccounterupdater-DEV', the browser URL will be https://laxsecretserver.j2global.com/secretserver/app/#/secret/14888/general.  The number value (14888) before '/general' will be the ID for the secret.

###### Configure key IDs for system
The expected key can have 1 of 2 values -- 1) for Development and 2) for Production.  For this environment, the expected values for this project are:
``` 
# for production below
#ccaccountupdater.properties.key=14895

# for development below
ccaccountupdater.properties.key=14888
```
At this time, the current key used is for Development.  The hash (#) is used for commenting.

To keep development simple, the properties above for this can be in one of the following locations:
1. C:\\Users\\{user.name}\\.gradle\\gradle.properties (typically private information such as username and password)
2. ${projectDirectory}\\gradle.properties (typically project specific properties like name, version, description)
3. ${projectDirectory}\\..\\gradle.properties (added to set keys to particular secret IDs based on key value)

The 1st 2 items above are the standard locations for Gradle builds.  The 3rd item above is placing the gradle.properties file located at the parent of the project directory.  This will allow you to use certain directories for development and others for production builds.

###### setting up credentials
To setup credentials for the secret server from your home directory navigate to the .gradle directory (e.g. C:\Users\{user.name}\.gradle ).  Inside this directory add the file 'gradle.properties' (or edit it if it is already there) and add to the file the following properties:
```
secret_server.oauth2.username= put Consensus Thycotic Secret Server username here
secret_server.oauth2.password= put Consensus Thycotic Secret Server password here
secret_server.oauth2.domain=j2global
secret_server.api_root_url = https://laxsecretserver.j2global.com/SecretServer/api/v1
secret_server.oauth2.token_url = https://laxsecretserver.j2global.com/SecretServer/oauth2/token
```
Make sure the file is set as read-only to the logged in user of the system -- Not others or others in the user's group.

### Prerequisites

[Gradle](https://gradle.org/) is also required to build the SDK.

Gradle runs unit and integration tests during the build so the settings in
`src/main/resources/application.properties` must be configured before the build
will succeed.

After the SDK application settings are configured the jar can be built:

```bash
.\gradlew.bat clean build
```

-------------------------------------------
