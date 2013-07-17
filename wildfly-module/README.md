# Aerogear SimplePush Server WildFly module
This project is a module intended to be used with the [Netty Subsystem](https://github.com/danbev/netty-subsystem)

## Prerequisites 
This project depends on _aerogear-simple-push_ which needs to be installed manually as it is currently not available in any
maven repository. 
It also requires that [Netty Subsystem](https://github.com/danbev/netty-subsystem) be installed on the local system, as this
dependency is currently not available in a maven repository.

## Building
From the root folder of this project run the following command:

    mvn install

## Installing
Copy the module produced by ```mvn package``` to the _modules_ directory of the application server.

    cp -r wildfly-module/target/module/org $WILDFLY_HOME/modules
    
Make sure you have installed the [Netty Subsystem](https://github.com/danbev/netty-subsystem), and then add this module as 
a dependency to the Netty subsystem module (_modules/org/jboss/aerogear/netty/main/module.xml_):

    <dependencies>
        ...
        <module name="org.jboss.aerogear.simplepush"/>
    </dependencies>
    
## Configuring WildFly
This involves adding a _server_ element to the Netty subsystem.  
Open up your WildFly server's configuration xml file, for example standalone.xml, and add the following to the _netty_ subsystem:

    <subsystem xmlns="urn:org.jboss.aerogear.netty:1.0">
        <server name="simplepush-server" socket-binding="simplepush" factory-class="org.jboss.aerogear.simplepush.netty.SimplePushBootstrapFactory"/>
    </subsystem>
    
For details regarding the attirbutes of the _server_ element, please refer to [Netty Subsystem](https://github.com/danbev/netty-subsystem) .

### Add a socket-binding    
You need to add a _socket-binding_ for the server configured in the previous step. This is done by adding a _socket-binding_ element
to the _socket-binding-group_ element in _standalone.xml_:

    <socket-binding-group name="standard-sockets" default-interface="public" port-offset="${jboss.socket.binding.port-offset:0}">
        ...
        <socket-binding name="simplepush" port="7777"/>
    </socket-binding-group>  

## Start WildFly

    ./standalone.sh

If you inspect the server console output you'll see the following message:

    08:56:13,052 INFO  [org.jboss.aerogear.netty.extension.NettyService] (MSC service thread 1-3) NettyService [simplepush-server] binding to port [7777]    

    
    



    