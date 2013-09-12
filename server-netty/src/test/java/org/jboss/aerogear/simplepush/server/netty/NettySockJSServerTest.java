package org.jboss.aerogear.simplepush.server.netty;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;

import org.jboss.aerogear.simplepush.server.netty.NettySockJSServer.Option;
import org.jboss.aerogear.simplepush.server.netty.NettySockJSServer.Options;
import org.jboss.aerogear.simplepush.server.netty.NettySockJSServer.Options.Args;
import org.junit.Test;

public class NettySockJSServerTest {

    @Test
    public void hostDomainName() {
        final String[] args = {"-host=domain-1.com"};
        final Map<Args, Option<?>> options = NettySockJSServer.Options.options(args);
        assertThat(options.get(Options.Args.HOST).value().toString(), equalTo("domain-1.com"));
    }

    @Test
    public void hostIpAdress() {
        final String[] args = {"-host=127.0.0.1"};
        final Map<Args, Option<?>> options = NettySockJSServer.Options.options(args);
        assertThat(options.get(Options.Args.HOST).value().toString(), equalTo("127.0.0.1"));
    }

}
