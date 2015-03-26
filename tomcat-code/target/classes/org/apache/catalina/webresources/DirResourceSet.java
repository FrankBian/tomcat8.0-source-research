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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.util.IOTools;
import org.apache.catalina.util.ResourceSet;

/**
 * Represents a {@link org.apache.catalina.WebResourceSet} based on a directory.
 */
public class DirResourceSet extends AbstractFileResourceSet {

    /**
     * A no argument constructor is required for this to work with the digester.
     */
    public DirResourceSet() {
        super("/");
    }

    /**
     * Creates a new {@link org.apache.catalina.WebResourceSet} based on a
     * directory.
     *
     * @param root          The {@link WebResourceRoot} this new
     *                          {@link org.apache.catalina.WebResourceSet} will
     *                          be added to.
     * @param webAppMount   The path within the web application at which this
     *                          {@link org.apache.catalina.WebResourceSet} will
     *                          be mounted. For example, to add a directory of
     *                          JARs to a web application, the directory would
     *                          be mounted at "/WEB-INF/lib/"
     * @param base          The absolute path to the directory on the file
     *                          system from which the resources will be served.
     * @param internalPath  The path within this new {@link
     *                          org.apache.catalina.WebResourceSet} where
     *                          resources will be served from.
     */
    public DirResourceSet(WebResourceRoot root, String webAppMount, String base,
            String internalPath) {
        super(internalPath);
        setRoot(root);
        setWebAppMount(webAppMount);
        setBase(base);

        if (root.getContext().getAddWebinfClassesResources()) {
            File f = new File(base, internalPath);
            f = new File(f, "/WEB-INF/classes/META-INF/resources");

            if (f.isDirectory()) {
                root.createWebResourceSet(ResourceSetType.RESOURCE_JAR, "/",
                         f.getAbsolutePath(), null, "/");
            }
        }

        if (getRoot().getState().isAvailable()) {
            try {
                start();
            } catch (LifecycleException e) {
                throw new IllegalStateException(e);
            }
        }
    }


    @Override
    public WebResource getResource(String path) {
        checkPath(path);
        String webAppMount = getWebAppMount();
        WebResourceRoot root = getRoot();
        if (path.startsWith(webAppMount)) {
            File f = file(path.substring(webAppMount.length()), false);
            if (f == null) {
                return new EmptyResource(root, path);
            }
            if (!f.exists()) {
                return new EmptyResource(root, path, f);
            }
            if (f.isDirectory() && path.charAt(path.length() - 1) != '/') {
                path = path + '/';
            }
            return new FileResource(root, path, f, isReadOnly());
        } else {
            return new EmptyResource(root, path);
        }
    }

    @Override
    public String[] list(String path) {
        checkPath(path);
        String webAppMount = getWebAppMount();
        if (path.startsWith(webAppMount)) {
            File f = file(path.substring(webAppMount.length()), true);
            if (f == null) {
                return EMPTY_STRING_ARRAY;
            }
            String[] result = f.list();
            if (result == null) {
                return EMPTY_STRING_ARRAY;
            } else {
                return result;
            }
        } else {
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            if (webAppMount.startsWith(path)) {
                int i = webAppMount.indexOf('/', path.length());
                if (i == -1) {
                    return new String[] {webAppMount.substring(path.length())};
                } else {
                    return new String[] {
                            webAppMount.substring(path.length(), i)};
                }
            }
            return EMPTY_STRING_ARRAY;
        }
    }

    @Override
    public Set<String> listWebAppPaths(String path) {
        checkPath(path);
        String webAppMount = getWebAppMount();
        ResourceSet<String> result = new ResourceSet<>();
        if (path.startsWith(webAppMount)) {
            File f = file(path.substring(webAppMount.length()), true);
            if (f != null) {
                File[] list = f.listFiles();
                if (list != null) {
                    for (File entry : list) {
                        StringBuilder sb = new StringBuilder(path);
                        if (path.charAt(path.length() - 1) != '/') {
                            sb.append('/');
                        }
                        sb.append(entry.getName());
                        if (entry.isDirectory()) {
                            sb.append('/');
                        }
                        result.add(sb.toString());
                    }
                }
            }
        } else {
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            if (webAppMount.startsWith(path)) {
                int i = webAppMount.indexOf('/', path.length());
                if (i == -1) {
                    result.add(webAppMount + "/");
                } else {
                    result.add(webAppMount.substring(0, i + 1));
                }
            }
        }
        result.setLocked(true);
        return result;
    }

    @Override
    public boolean mkdir(String path) {
        checkPath(path);
        if (isReadOnly()) {
            return false;
        }
        String webAppMount = getWebAppMount();
        if (path.startsWith(webAppMount)) {
            File f = file(path.substring(webAppMount.length()), false);
            if (f == null) {
                return false;
            }
            return f.mkdir();
        } else {
            return false;
        }
    }

    @Override
    public boolean write(String path, InputStream is, boolean overwrite) {
        checkPath(path);

        if (is == null) {
            throw new NullPointerException(
                    sm.getString("dirResourceSet.writeNpe"));
        }

        if (isReadOnly()) {
            return false;
        }

        File dest = null;
        String webAppMount = getWebAppMount();
        if (path.startsWith(webAppMount)) {
            dest = file(path.substring(webAppMount.length()), false);
            if (dest == null) {
                return false;
            }
        } else {
            return false;
        }

        if (dest.exists()) {
            if (overwrite) {
                if (!dest.delete()) {
                    return false;
                }
            } else {
                return false;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(dest)) {
            IOTools.flow(is, fos);
        } catch (IOException ioe) {
            return false;
        }

        return true;
    }

    @Override
    protected void checkType(File file) {
        if (file.isDirectory() == false) {
            throw new IllegalArgumentException(sm.getString("dirResourceSet.notDirectory",
                    getBase(), File.separator, getInternalPath()));
        }
    }
}
