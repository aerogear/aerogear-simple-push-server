/**
 * JBoss, Home of Professional Open Source Copyright Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.jboss.aerogear.simplepush.server.netty.performance;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Just a very simple utility methods for parsing command line
 * arguments for the performance tests.
 */
public final class ArgsUtil {

    private ArgsUtil() {
    }

    public static URI firstArg(final String[] args, final String defaultUri) throws URISyntaxException {
        return args.length >= 1 ? new URI(args[0]) : new URI(defaultUri);
    }

    public static int secondArg(final String[] args, final int defaultThreads) {
        return args.length >= 2 ? Integer.parseInt(args[1]) : defaultThreads;
    }

    public static int thirdArg(final String[] args, final int defaultOperations) {
        return args.length >= 3 ? Integer.parseInt(args[2]) : defaultOperations;
    }

    public static int fourthArg(final String[] args, final int defaultOperations) {
        return args.length == 4 ? Integer.parseInt(args[3]) : defaultOperations;
    }

}
