# AeroGear SimplePush Server
__Disclaimer__  
This project is a Java implementation of the server side that follows the [SimplePush Protocol](https://wiki.mozilla.org/WebAPI/SimplePush/Protocol).  
This version uses an in-memory data store and will loose all registrations upon shutdown restart. 
A persistent data store will be added with [AGPUSH-18](https://issues.jboss.org/browse/AGPUSH-18).

### Prerequisites
This version uses SockJS in combination with Netty 4. This support currently not availble in any release of Netty and 
you have to build the following branch manually:

    git clone https://github.com/danbev/netty/tree/sockjs
    cd netty
    mvn install -DskipTests=true

## Usage

### Build the SimplePush Server

    mvn install

### Start the SimplePush Server

    mvn exec:java
    
This will start the server listening localhost using port 7777. To toggle these arguments you can
specify overrides on the command line:  

    mvn exec:java -Dexec.args="-host=localhost -port=8888 -tls=false -ack_interval=10000 -useragent_reaper_timeout=60000"
    
__host__  
The host that the server will bind to.

__port__  
The port that the server will bind to.

__tls__  
Whether to use transport layer security or not.
The server will use a system property named ```simplepush.keystore.path``` which should point to 
a keystore available on the servers classpath. If the keystore is password protected then the system property 
```simplepush.keystore.password``` can be used to specify the password for the keystore.

When running the ```mvn exec:java``` command a sample keystore is used that contains a self signed certificate for testing. 
The above mentioned system variables are set in the pom.xml file.

__ack_interval__ 
How often the acknowledge job will run to re-send unacknowledged notifications.

__useragent_reaper_timeout__ 
How often the UserAgent reaper job will run to clean up inactive user agents.
    
### Access the demo html page

#### Setting up TLS/SSL
This SimplePush Server uses SockJS with transport layer security and therefore requires a certificate to be accepted by 
the client. The server can be enabled with TLS by changing the _tls_ setting in pom.xml, but the browser also needs to 
import the certificate.  

For some broswers is will be enough to access ```https://localhost:7777``` once, and then accept the certificate.  For other
systems it might be required to import the certificate through the browser preferences/settings page. For this case we
have exported the certificate and it can be found in ```src/main/resources/simplepush.crt```.

#### Mac WebServer

Serve _src/main/resources/netty/socket.html_ from a local webserver. One way to do this is to create a symbolic link
to _src/main/resources/netty_, for example:

    cd /Library/WebServer/Documents/
    sudo ln -s /path/to/push/aerogear-simplepush-server/src/main/resources/netty/ netty
    
Now you should be able to point your browser to ```http://localhost/netty/websocket.html```
The path to your documents directory and the port that the web server is listening to might differ. For httpd the look
in _/etc/apache2/httpd.conf_ for this information.

#### Python WebServer

In case you are not running a mac, there is a simple HTTP server, that comes with Python. Simple navigate to ```src/main/resources/netty``` and issue:

    python -m SimpleHTTPServer 5555

Now you should be able to point your browser to ```http://localhost:5555/websocket.html```



### Register a channel
You will automatically be registered to receive push notifications for mail and foo. The endpoint channelID's will be displayed in the results textarea.

### Send a notification

Use one of the above mentioned IDs in the following ```curl``` command:

    curl -i --header "Accept: application/x-www-form-urlencoded" -X PUT -d "version=1" "https://localhost:7777/endpoint/{ChannelID}"

A push notification stating the version will be displayed in the textarea of the _websocket.html_ page that has registerd for that channel.

## Protocol

### Hello Handshake
Is sent by the UserAgent to the SimplePush Server:

![Hello Message](https://raw.github.com/aerogear/aerogear-simple-push-server/master/server-netty/src/etc/images/hello-message.png)  

The SimplePush Server will ignore any additional Hello Messages after the first one on the web socket connection. 

#### Request format

    {
       "messageType": "hello",
       "uaid": "fd52438f-1c49-41e0-a2e4-98e49833cc9c",
       "channelIDs": ["431b4391-c78f-429a-a134-f890b5adc0bb", "a7695fa0-9623-4890-9c08-cce0231e4b36"]
    } 

__uaid__  
The UserAgent Identifier is optional and if not specified a UAID will be created on by the SimplePush Server. This can 
be used as a way of reseting. The ```channeldIDs``` are also optional and when specified these channels will be registered
during the handshake. This can be useful if the UserAgent has stored the ```uaid``` and for some reason has disconnected, and
then reconnected. By passing both a ```uaid``` and the ```channelIDs``` it can restore the channels that were registered at 
the point when the UserAgent disconnected.

__channelIDs__  
The optional channelIds passed in are identifiers created on the client side and will be associated with the ```UAID```. In the case
of a Hello Message the _channelIDs_ represent channels that the UserAgent want to have registered.


#### Response format

    {
       "messageType": "hello",
       "uaid": "fd52438f-1c49-41e0-a2e4-98e49833cc9c",
    } 


### Register
Register is used to register a ```channelId``` with the SimplePush server and enables the the client to be notified when the version 
for this channel is updated.

![Register Channel](https://raw.github.com/aerogear/aerogear-simple-push-server/master/server-netty/src/etc/images/register-channel.png)  
Notice that the ```UAID``` is absent from this message. This is because we have already performed hello message handshake and the current 
web socket connection is for the current UserAgent (identified by the UAID).

#### Request format

    {
      "messageType": "register",
      "channelID": "d9b74644-4f97-46aa-b8fa-9393985cd6cd"
    }  
    
#### Response format

    {
      "messageType": "register",
      "channelID": "d9b74644-4f97-46aa-b8fa-9393985cd6cd",
      "status": 200,
      "pushEndpoint": "/endpoint/d9b74644-4f97-46aa-b8fa-9393985cd6cd"
    }  
    
__status__  

* 200 "OK"   
* 409 "Conflict"
The chosen channelId is already in use and not associated with this user agent. UserAgent should retry with a different
channelId.
* 500 "Internal Server error"
 
### Notification
A notification is triggered by sending a ```PUT``` request to the ```pushEndpoint```.

![Notification](https://raw.github.com/aerogear/aerogear-simple-push-server/master/server-netty/src/etc/images/notification.png)  

#### Request PUT format

    PUT http://server/simplepush-server//endpoint/d9b74644-4f97-46aa-b8fa-9393985cd6cd
    ContentType: application/x-www-form-urlencoded
    
    version=N
    
#### Response (PUT) format

    HTTP/1.1 200 OK
    
#### Notification Request format
    
    {
      "messageType": "notification",
      "updates": [{"channelID": "d9b74644-4f97-46aa-b8fa-9393985cd6cd", "version" 2}, {"channeID": "a9b74644-4f97-46aa-b8fa-9393985dd688", "version": 10}]"
    }  
    
#### Notification Response format

    {
      "messageType": "ack",
      "updates": [{"channelID": "d9b74644-4f97-46aa-b8fa-9393985cd6cd", "version" 2}]"
    }  
The ```updates``` are the channels that the UserAgent acknowledges that it has processed.   
The SimplePush server will try to will resend the the un-acknowledged notifications every 60 seconds. 

### Unregister

![Unregister](https://raw.github.com/aerogear/aerogear-simple-push-server/master/server-netty/src/etc/images/unregister-channel.png)  

#### Request format

    {
      "messageType": "unregister",
      "channelID": "d9b74644-4f97-46aa-b8fa-9393985cd6cd"
    }  
    
#### Response format

    {
      "messageType": "unregister",
      "channelID": "d9b74644-4f97-46aa-b8fa-9393985cd6cd"
      "status": 200
    }  

## Deploying to OpenShift
Deploying the SimplePush server to OpenShift involves creating a new application of type AS 7.
After this has been done you need to clone this application.  
Next, remove the pom.xml, src, and deployments/ROOT.war files from git:

    git rm -r pom.xml src deployments/ROOT.war
    git commit -m "removing src files"
    
### Add the modules for the Netty subsystem and SimplePush
We are going to add two modules to the AS7 instance which is done by adding the modules to the _.openshift/config/modules_
directory.

You need to bulid the [Netty subystem](https://github.com/danbev/netty-subsystem) locally first:

    git clone https://github.com/danbev/netty-subsystem
    cd netty-subsystem
    mvn install
Next, copy the module produced by the above build (your path to the simplepush openshift clone might differ):

    cp -r subsystem/target/module/org ~/work/openshift/simplepush/.openshift/config/modules
 
We also need the module for the SimplePush server which is build by the wildfly-module in the current project:

    cp -r wildfly-module/target/module/org ~/work/openshift/simplepush/.openshift/config/modules
    
Currently, we also need to add the SimplePush module as a dependency to Netty subsystem so that Netty can
find classes and resources in the SimplePush module. This will later be fixed and the module reference will be
part of the subsystem configuration([AGPUSH-129](https://issues.jboss.org/browse/AGPUSH-129)
Edit .openshift/config/modules/org/jboss/aerogear/netty/main/modules.xml, and add the following dependency:

    <dependencies>
        ...
        <module name="org.jboss.aerogear.simplepush"/>
    </dependencies>
      
### Adding the subsystem to WildFly
The Netty subsystem can be added to any of the configurations that are shipped with WildFly. 
As an example, add the following elements to _.openshift/standalone.xml_.


#### Add the extension

    <extensions>
        ...
        <extension module="org.jboss.aerogear.netty"/>
    <extensions>
    
    
#### Add a socket-binding    

    
    <socket-binding-group name="standard-sockets" default-interface="public" port-offset="${jboss.socket.binding.port-offset:0}">
        <socket-binding name="http" port="8099"/>
        ...
        <socket-binding name="simplepush" port="8080"/>
    </socket-binding-group>  
We are changing the _http_ binding so that we can have the SimplePush server be accessed externally.    
    
#### Add the Netty subsystem

    <profile>
        ...
        <subsystem xmlns="urn:org.jboss.aerogear.netty:1.0">
            <netty>
                <server name="simplepush-server" socket-binding="simplepush" factoryClass="org.jboss.aerogear.simplepush.netty.SimplePushBootstrapFactory" />
            </netty>
        </subsystem>
    </profile>    
    
Add all the changes by using git add and commit:
    
    git add .openshift
    git commit -m 'adding Netty Subsystem and SimplePush module'
    git push origin master
      
#### How do I know that it works?
You can hit the SockJS info page and verify that it returns the expected result

    http://simplepush-danbev.rhcloud.com/simplepush/info
    
Which should return:
    
    {"websocket": true, "origins": ["*:*"], "cookie_needed": true, "entropy": 1625422556}    
    
#### Registering a Web Variant
When registering a Web Variant you have to give a url to the service that handles the notifications, which is often referred
to as the endpoint url. This url should look like this:

    http://simplepush-danbev.rhcloud.com/endpoint/
    
#### Use the OpenShift WebSocket port 8000 to connect
OpenShift as the external port 8000 available for WebSockets and the client must use this port to succeed. Depending
on the client this might look different but the url should look like the following example:

    http://sockjs-danbev.rhcloud.com:8000/simplepush
    
    
#### Known issues
When SockJS tries to estabilish a WebSocket connection the following error is raised:

    WebSocket connection to 'ws://simplepush-danbev.rhcloud.com/simplepush/933/5plrvach/websocket' failed: WebSocket is closed before the connection is established.

This issue is covered by [AGPUSH-120](https://issues.jboss.org/browse/AGPUSH-120).
    
    