/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.webresources;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class WarURLStreamHandler extends URLStreamHandler {

    @Override
    protected void parseURL(URL u, String spec, int start, int limit) {
        // Need to make this look like a JAR URL for the WAR file
        // Assumes that the spec is absolute and starts war:file:/...

        // Only the path needs to be changed
        String path = "jar:" + spec.substring(4);
        if (path.contains("*/")) {
            path = path.replaceFirst("\\*/", "!/");
        } else {
            path = path.replaceFirst("\\^/", "!/");
        }

        setURL(u, u.getProtocol(), "", -1, null, null,
                path, null, null);
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new WarURLConnection(u);
    }
}
