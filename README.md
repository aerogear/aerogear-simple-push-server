# Aerogear SimplePush Server
This project is a Java server side implementation of the [SimplePush Protocol](https://wiki.mozilla.org/WebAPI/SimplePush/Protocol) 
specification from Mozilla that describes a JavaScript API and a protocol which allows backend/application developers to 
send notification messages to their web applications. Originally SimplePush was introduced for Firefox OS but there are 
plans to establish the API on the desktop and mobile browsers as well.

Firefox OS v1.1 uses SimplePush as its Push Notification technology. Firefox OS Push Notifications are designed for one 
thing – waking up apps. They do not deal with data, desktop notifications and other features, since there are other 
Web APIs that provide them. From the very beginning SimplePush was designed to explicitly not carry any payload. 
Instead a version number is sent to the client. Based on that version number the client can perfom an action, e.g. refresh a view of data.

Mozilla published a very detailed [article](https://hacks.mozilla.org/2013/07/dont-miss-out-on-the-real-time-fun-use-firefox-os-push-notifications/) 
that explains the API in depth.

AeroGear SimplePush consists of the following modules:  

* [protocol](https://github.com/aerogear/aerogear-simple-push-server/tree/master/protocol)  
The SimplePush Server Protocol provides interfaces for the protocol.

* [server-api](https://github.com/aerogear/aerogear-simple-push-server/tree/master/server-api)  
An API for AeroGear SimplePush Server

* [server-core](https://github.com/aerogear/aerogear-simple-push-server/tree/master/server-core)  
An implementation of AeroGear SimplePush Server API.

* [server-netty](https://github.com/aerogear/aerogear-simple-push-server/tree/master/server-netty)  
The SimplePush Server implementation that uses Netty 4.x.

* [server-vertx](https://github.com/aerogear/aerogear-simple-push-server/tree/master/server-vertx)  
The SimplePush Server implementation that uses vert.x.

* [wildfly-module](https://github.com/aerogear/aerogear-simple-push-server/tree/master/wildfly-module)  
A WildFly/AS7 module for the SimplePush Server.

Please refer to the above modules documentation for more information.

#### How to generate Javadocs?

Your Javadocs are generated while building the project itself. Execute in the root dir:

```mvn clean install```

When you want them to be aggregated (meaning all docs in one, not by modules), please execute the following command after the first one:

```mvn javadoc:aggregate```

When you have [GraphViz](https://code.google.com/p/apiviz/) installed locally on your host, your Javadocs will be enriched by class model embedded directly into them as images. They are placed in ```target/site/apidocs```
