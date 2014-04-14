# AeroGear SimplePush Server
This project is a Java implementation of the server side that follows the [SimplePush Protocol](https://wiki.mozilla.org/WebAPI/SimplePush/Protocol)
and uses [vert.x](http://vertx.io/).

__Disclaimer__ This version uses an in-memory data store and will loose all registrations upon shutdown restart.   

## Prerequisites 
[Vert.x](http://vertx.io/downloads.html) is required to be availble and usable from the command line. 
Note also that Vert.x requires Java 7.

## Usage

### Build the SimplePush Server

    mvn install

### Start the SimplePush Server

    cd target
    vertx runmod aerogear~simplepush~<version>
    
You can also start the server with a different configuration:

    vertx runmod aerogear~simplepush~<version> -conf classes/config.json

The config file can be named anything file you like but the ```classes/config.json``` file above contains an example of the configuration 
options and their default values.    

## Configuration
The SimplePush Server vert.x module can be configured using a json configuration file.  
The following configuration options are available:

    {
      "host" : "localhost",
      "port" : 7777,
      "reaperTimeout" : 300000,
      "ackInterval" : 60000,
      "password" : "yourRandomToken"
    }
    
__host__    
The host that the server should bind to.
    
__port__  
The port that the server should bind to.

__reaperTimeout__  
This is a scheduled job that will clean up UserAgent that have been inactive for the specified amount of time.

__ackInterval__  
The time, in milliseconds, that a scheduled job will try to resend unacknowledged notifications.    

__password__
This should be a password that will be used to generate the server private key which is used for  encryption/decryption
of the endpoint URLs that are returned to clients upon successful channel registration.

## Deploy on OpenShift

Vert.x applications can now be easily deployed on OpenShift using the dedicated cartdridge. This way you can deploy your vert.x SimplePush Server on the cloud and fully benefit from the scalability features.

### Application Creation

Use the [vert.x cartdridge](https://openshift.redhat.com/app/console/application_type/cart!jboss-vertx-2.1) to create a new application.

Once it has been created, clone the application locally.

### Add the vert.x Simple Push Server Mod

From your ```server-vertx ``` folder, copy the mod folder ``` target/mods/aerogear~simplepush~[your_version] ``` to the local cloned OpenShift App in ``` /mods ```. 

### Add a configuration file

Create a file ``` config.json ``` and put it in the root folder of your cloned app. 


```
{
    "port" : 8443,
    "openshift": true,
    "tls": true,  
    "host" : "<your_name-useraccount>.rhcloud.com",
    "userAgentReaperTimeout" : 604800000,
    "ackInterval" : 60000,
    "endpointUrlPrefix" : "/update",
    "password" : "changeMe!"
}

```
Be sure to have these options set correctly : 

__port__ : 8443 for a secured connection or 8000 for an unsecured connection

__tls__ : be sure to have this one set on ``` true ``` if you have set the port to ``` 8443 ```

__openshift__ : Be sure this one is on ``` true ```

__host__ : Be sure to use the public domain that has been affected to your app.


### Configure the cartdridge 

Open the file ``` configuration/vertx.env ``` and edit it to have : 

```
export vertx_module=aerogear~simplepush~<your_version>
export vertx_run_options="-conf config.json"

```

Commit and Push to the remote, your Vert.x Simple Push Server is running on OpenShift ! 


