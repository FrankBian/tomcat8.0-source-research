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
package org.apache.coyote.lite;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.realm.RealmBase;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.ContextConfig;

// This class is here for compat with Tomcat6.

// TODO: lazy init for the temp dir - only when a JSP is compiled or
// get temp dir is called we need to create it. This will avoid the
// need for the baseDir

// TODO: allow contexts without a base dir - i.e.
// only programmatic. This would disable the default servlet.

/**
 * Minimal tomcat starter for embedding/unit tests.
 *
 * Tomcat supports multiple styles of configuration and
 * startup - the most common and stable is server.xml-based,
 * implemented in org.apache.catalina.startup.Bootstrap.
 *
 * This class is for use in apps that embed tomcat.
 * Requirements:
 *
 * - all tomcat classes and possibly servlets are in the classpath.
 * ( for example all is in one big jar, or in eclipse CP, or in any other
 * combination )
 *
 * - we need one temporary directory for work files
 *
 * - no config file is required. This class provides methods to
 * use if you have a webapp with a web.xml file, but it is
 * optional - you can use your own servlets.
 *
 * This class provides a main() and few simple CLI arguments,
 * see setters for doc. It can be used for simple tests and
 * demo.
 *
 * @see TestTomcat for examples on how to use this
 * @author Costin Manolache
 */
public class Tomcat {
    // Single engine, service, server, connector - few cases need more,
    // they can use server.xml
    protected StandardServer server;
    protected StandardService service;
    protected StandardEngine engine;
    protected Connector connector; // for more - customize the classes

    boolean started = false;
    // To make it a bit easier to config for the common case
    // ( one host, one context ).
    protected StandardHost host;

    // TODO: it's easy to add support for more hosts - but is it
    // really needed ?

    // TODO: allow use of in-memory connector

    protected int port = 8080;
    protected String hostname = "localhost";
    protected String basedir;

    // Default in-memory realm, will be set by default on
    // created contexts. Can be replaced with setRealm() on
    // the context.
    protected Realm defaultRealm;
    private Map<String, String> userPass = new HashMap<String, String>();
    private Map<String, List<String>> userRoles =
            new HashMap<String, List<String>>();
    private Map<String, Principal> userPrincipals = new HashMap<String, Principal>();

    public Tomcat() {
        // NOOP
    }

    /**
     * Tomcat needs a directory for temp files. This should be the
     * first method called.
     *
     * By default, if this method is not called, we use:
     *  - system properties - catalina.base, catalina.home
     *  - $HOME/tomcat.$PORT
     * ( /tmp doesn't seem a good choice for security ).
     *
     *
     * TODO: better default ? Maybe current dir ?
     * TODO: disable work dir if not needed ( no jsp, etc ).
     */
    public void setBaseDir(String basedir) {
        this.basedir = basedir;
    }

    /**
     * Set the port for the default connector. Must
     * be called before start().
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * The the hostname of the default host, default is
     * 'localhost'.
     */
    public void setHostname(String s) {
        hostname = s;
    }

    /**
     * Add a webapp using normal WEB-INF/web.xml if found.
     *
     * @param contextPath
     * @param baseDir
     * @return new StandardContext
     * @throws ServletException
     */
    public StandardContext addWebapp(String contextPath,
                                     String baseDir) throws ServletException {

        return addWebapp(getHost(), contextPath, baseDir);
    }


    /**
     * Add a context - programmatic mode, no web.xml used.
     *
     * API calls equivalent with web.xml:
     *
     * context-param
     *  ctx.addParameter("name", "value");
     *
     *
     * error-page
     *    ErrorPage ep = new ErrorPage();
     *    ep.setErrorCode(500);
     *    ep.setLocation("/error.html");
     *    ctx.addErrorPage(ep);
     *
     * ctx.addMimeMapping("ext", "type");
     *
     * Note: If you reload the Context, all your configuration will be lost. If
     * you need reload support, consider using a LifecycleListener to provide
     * your configuration.
     *
     * TODO: add the rest
     *
     *  @param contextPath "/" for root context.
     *  @param baseDir base dir for the context, for static files. Must exist,
     *  relative to the server home
     */
    public StandardContext addContext(String contextPath,
                                      String baseDir) {
        return addContext(getHost(), contextPath, baseDir);
    }

    /**
     * Equivalent with
     *  <servlet><servlet-name><servlet-class>.
     *
     * In general it is better/faster to use the method that takes a
     * Servlet as param - this one can be used if the servlet is not
     * commonly used, and want to avoid loading all deps.
     * ( for example: jsp servlet )
     *
     * You can customize the returned servlet, ex:
     *
     *    wrapper.addInitParameter("name", "value");
     *
     * @param contextPath   Context to add Servlet to
     * @param servletName   Servlet name (used in mappings)
     * @param servletClass  The class to be used for the Servlet
     * @return The wrapper for the servlet
     */
    public StandardWrapper addServlet(String contextPath,
            String servletName,
            String servletClass) {
        Container ctx = getHost().findChild(contextPath);
        return addServlet((StandardContext) ctx,
                servletName, servletClass);
    }

    /**
     * Static version of {@link #addServlet(String, String, String)}
     * @param ctx           Context to add Servlet to
     * @param servletName   Servlet name (used in mappings)
     * @param servletClass  The class to be used for the Servlet
     * @return The wrapper for the servlet
     */
    public static StandardWrapper addServlet(StandardContext ctx,
                                      String servletName,
                                      String servletClass) {
        // will do class for name and set init params
        StandardWrapper sw = (StandardWrapper)ctx.createWrapper();
        sw.setServletClass(servletClass);
        sw.setName(servletName);
        ctx.addChild(sw);

        return sw;
    }

    /**
     * Add an existing Servlet to the context with no class.forName or
     * initialisation.
     * @param contextPath   Context to add Servlet to
     * @param servletName   Servlet name (used in mappings)
     * @param servlet       The Servlet to add
     * @return The wrapper for the servlet
     */
    public StandardWrapper addServlet(String contextPath,
            String servletName,
            Servlet servlet) {
        Container ctx = getHost().findChild(contextPath);
        return addServlet((StandardContext) ctx,
                servletName, servlet);
    }

    /**
     * Static version of {@link #addServlet(String, String, Servlet)}.
     * @param ctx           Context to add Servlet to
     * @param servletName   Servlet name (used in mappings)
     * @param servlet       The Servlet to add
     * @return The wrapper for the servlet
     */
    public static StandardWrapper addServlet(StandardContext ctx,
                                      String servletName,
                                      Servlet servlet) {
        // will do class for name and set init params
        StandardWrapper sw = new ExistingStandardWrapper(servlet);
        sw.setName(servletName);
        ctx.addChild(sw);

        return sw;
    }


    /**
     * Initialize and start the server.
     * @throws LifecycleException
     */
    public void start() throws LifecycleException {
        if (started) {
            return;
        }
        started = true;
        getServer();
        getConnector();
        server.start();
    }

    /**
     * Stop the server.
     * @throws LifecycleException
     */
    public void stop() throws LifecycleException {
        getServer().stop();
    }


    /**
     * Add a user for the in-memory realm. All created apps use this
     * by default, can be replaced using setRealm().
     *
     */
    public void addUser(String user, String pass) {
        userPass.put(user, pass);
    }

    /**
     * @see #addUser(String, String)
     */
    public void addRole(String user, String role) {
        List<String> roles = userRoles.get(user);
        if (roles == null) {
            roles = new ArrayList<String>();
            userRoles.put(user, roles);
        }
        roles.add(role);
    }

    // ------- Extra customization -------
    // You can tune individual tomcat objects, using internal APIs

    /**
     * Get the default http connector. You can set more
     * parameters - the port is already initialized.
     *
     * Alternatively, you can construct a Connector and set any params,
     * then call addConnector(Connector)
     *
     * @return A connector object that can be customized
     */
    public Connector getConnector() {
        getServer();
        if (connector != null) {
            return connector;
        }
        // This will load Apr connector if available,
        // default to nio. I'm having strange problems with apr
        // and for the use case the speed benefit wouldn't matter.

        //connector = new Connector("HTTP/1.1");
        try {
            connector = new Connector("org.apache.coyote.http11.Http11Protocol");
            connector.setPort(port);
            service.addConnector( connector );
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    /**
     * Get the service object. Can be used to add more
     * connectors and few other global settings.
     */
    public StandardService getService() {
        getServer();
        return service;
    }

    /**
     * Sets the current host - all future webapps will
     * be added to this host. When tomcat starts, the
     * host will be the default host.
     *
     * @param host
     */
    public void setHost(StandardHost host) {
        this.host = host;
    }

    public StandardHost getHost() {
        if (host == null) {
            host = new StandardHost();
            host.setName(hostname);

            getEngine().addChild( host );
        }
        return host;
    }

    /**
     * Set a custom realm for auth. If not called, a simple
     * default will be used, using an internal map.
     *
     * Must be called before adding a context.
     */
    public void setDefaultRealm(Realm realm) {
        defaultRealm = realm;
    }


    /**
     * Access to the engine, for further customization.
     */
    public StandardEngine getEngine() {
        if(engine == null ) {
            getServer();
            engine = new StandardEngine();
            engine.setName( "Tomcat" );
            engine.setDefaultHost(hostname);
            service.setContainer(engine);
        }
        return engine;
    }

    /**
     * Get the server object. You can add listeners and few more
     * customizations. JNDI is disabled by default.
     */
    public StandardServer getServer() {

        if (server != null) {
            return server;
        }

        initBaseDir();

        System.setProperty("catalina.useNaming", "false");

        server = new StandardServer();
        server.setPort( -1 );

        service = new StandardService();
        service.setName("Tomcat");
        server.addService( service );
        return server;
    }

    public StandardContext addContext(StandardHost host,
                                      String contextPath,
                                      String dir) {
        silence(contextPath);
        StandardContext ctx = new StandardContext();
        ctx.setPath( contextPath );
        ctx.setDocBase(dir);
        ctx.addLifecycleListener(new FixContextListener());

        if (host == null) {
            getHost().addChild(ctx);
        } else {
            host.addChild(ctx);
        }
        return ctx;
    }

    public StandardContext addWebapp(StandardHost host,
                                     String url, String path) {
        silence(url);

        StandardContext ctx = new StandardContext();
        ctx.setPath( url );
        ctx.setDocBase(path);
        if (defaultRealm == null) {
            initSimpleAuth();
        }
        ctx.setRealm(defaultRealm);
        ctx.addLifecycleListener(new DefaultWebXmlListener());

        ContextConfig ctxCfg = new ContextConfig();
        ctx.addLifecycleListener( ctxCfg );
        // prevent it from looking ( if it finds one - it'll have dup error )
        ctxCfg.setDefaultWebXml("org/apache/catalin/startup/NO_DEFAULT_XML");

        if (host == null) {
            getHost().addChild(ctx);
        } else {
            host.addChild(ctx);
        }

        return ctx;
    }


    // ---------- Helper methods and classes -------------------

    /**
     * Initialize an in-memory realm. You can replace it
     * for contexts with a real one.
     */
    protected void initSimpleAuth() {
        defaultRealm = new RealmBase() {
            @Override
            protected String getName() {
                return "Simple";
            }

            @Override
            protected String getPassword(String username) {
                return userPass.get(username);
            }

            @Override
            protected Principal getPrincipal(final String username) {
                Principal p = userPrincipals.get(username);
                if (p == null) {
                    String pass = userPass.get(username);
                    if (pass != null) {
                        p = new Principal() {

                            @Override
                            public String getName() {
                                return username;
                            }

                        };
                    }
                }
                return p;
            }

        };
    }

    protected void initBaseDir() {
        if (basedir == null) {
            basedir = System.getProperty("catalina.base");
        }
        if (basedir == null) {
            basedir = System.getProperty("catalina.home");
        }
        if (basedir == null) {
            // Create a temp dir.
            basedir = System.getProperty("user.dir") +
                "/tomcat." + port;
            File home = new File(basedir);
            home.mkdir();
            if (!home.isAbsolute()) {
                try {
                    basedir = home.getCanonicalPath();
                } catch (IOException e) {
                    basedir = home.getAbsolutePath();
                }
            }
        }
        System.setProperty("catalina.home", basedir);
        System.setProperty("catalina.base", basedir);
    }

    static String[] silences = new String[] {
        "org.apache.coyote.http11.Http11Protocol",
        "org.apache.catalina.core.StandardService",
        "org.apache.catalina.core.StandardEngine",
        "org.apache.catalina.startup.ContextConfig",
        "org.apache.catalina.core.ApplicationContext",
        "org.apache.catalina.core.AprLifecycleListener"
    };

    /**
     * Controls if the loggers will be silenced or not.
     * @param silent    <code>true</code> sets the log level to WARN for the
     *                  loggers that log information on Tomcat start up. This
     *                  prevents the usual startup information being logged.
     *                  <code>false</code> sets the log level to the default
     *                  level of INFO.
     */
    public void setSilent(boolean silent) {
        for (String s : silences) {
            if (silent) {
                Logger.getLogger(s).setLevel(Level.WARNING);
            } else {
                Logger.getLogger(s).setLevel(Level.INFO);
            }
        }
    }

    private void silence(String ctx) {
        String base = "org.apache.catalina.core.ContainerBase.[default].[";
        base += getHost().getName();
        base += "].[";
        base += ctx;
        base += "]";
        Logger.getLogger(base).setLevel(Level.WARNING);
    }

    /**
     * Enables JNDI naming which is disabled by default.
     */
    public void enableNaming() {
        // Make sure getServer() has been called as that is where naming is
        // disabled
        getServer();

        System.setProperty("catalina.useNaming", "true");
        String value = "org.apache.naming";
        String oldValue =
            System.getProperty(javax.naming.Context.URL_PKG_PREFIXES);
        if (oldValue != null) {
            value = value + ":" + oldValue;
        }
        System.setProperty(javax.naming.Context.URL_PKG_PREFIXES, value);
        value = System.getProperty
            (javax.naming.Context.INITIAL_CONTEXT_FACTORY);
        if (value == null) {
            System.setProperty
                (javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                 "org.apache.naming.java.javaURLContextFactory");
        }
    }

    /**
     * Provide default configuration for a context. This is the programmatic
     * equivalent of the default web.xml.
     *
     *  TODO: in normal tomcat, if default-web.xml is not found, use this
     *  method
     *
     * @param contextPath   The context to set the defaults for
     */
    public void initWebappDefaults(String contextPath) {
        Container ctx = getHost().findChild(contextPath);
        initWebappDefaults((StandardContext) ctx);
    }

    /**
     * Static version of {@link #initWebappDefaults(String)}
     * @param ctx   The context to set the defaults for
     */
    public static void initWebappDefaults(StandardContext ctx) {
        // Default servlet
        StandardWrapper servlet = addServlet(
                ctx, "default", "org.apache.catalina.servlets.DefaultServlet");
        servlet.setLoadOnStartup(1);

        // JSP servlet (by class name - to avoid loading all deps)
        servlet = addServlet(
                ctx, "jsp", "org.apache.jasper.servlet.JspServlet");
        servlet.addInitParameter("fork", "false");
        servlet.setLoadOnStartup(3);

        // Servlet mappings
        ctx.addServletMapping("/", "default");
        ctx.addServletMapping("*.jsp", "jsp");
        ctx.addServletMapping("*.jspx", "jsp");

        // Sessions
        ctx.setManager( new StandardManager());
        ctx.setSessionTimeout(30);

        // MIME mappings
        for (int i = 0; i < DEFAULT_MIME_MAPPINGS.length; ) {
            ctx.addMimeMapping(DEFAULT_MIME_MAPPINGS[i++],
                    DEFAULT_MIME_MAPPINGS[i++]);
        }

        // Welcome files
        ctx.addWelcomeFile("index.html");
        ctx.addWelcomeFile("index.htm");
        ctx.addWelcomeFile("index.jsp");
    }


    /**
     * Fix startup sequence - required if you don't use web.xml.
     *
     * The start() method in context will set 'configured' to false - and
     * expects a listener to set it back to true.
     */
    public static class FixContextListener implements LifecycleListener {

        public void lifecycleEvent(LifecycleEvent event) {
            try {
                Context context = (Context) event.getLifecycle();
                if (event.getType().equals(Lifecycle.START_EVENT)) {
                    context.setConfigured(true);
                }
            } catch (ClassCastException e) {
                return;
            }
        }

    }


    /**
     * Fix reload - required if reloading and using programmatic configuration.
     * When a context is reloaded, any programmatic configuration is lost. This
     * listener sets the equivalent of conf/web.xml when the context starts. The
     * context needs to be an instance of StandardContext for this listener to
     * have any effect.
     */
    public static class DefaultWebXmlListener implements LifecycleListener {
        public void lifecycleEvent(LifecycleEvent event) {
            if (Lifecycle.BEFORE_START_EVENT.equals(event.getType()) &&
                    event.getLifecycle() instanceof StandardContext) {
                initWebappDefaults((StandardContext) event.getLifecycle());
            }
        }
    }


    /**
     * Helper class for wrapping existing servlets. This disables servlet
     * lifecycle and normal reloading, but also reduces overhead and provide
     * more direct control over the servlet.
     */
    public static class ExistingStandardWrapper extends StandardWrapper {
        private Servlet existing;
        boolean init = false;

        public ExistingStandardWrapper( Servlet existing ) {
            this.existing = existing;
        }
        @Override
        public synchronized Servlet loadServlet() throws ServletException {
            if (!init) {
                existing.init(facade);
                init = true;
            }
            return existing;

        }
        @Override
        public long getAvailable() {
            return 0;
        }
        @Override
        public boolean isUnavailable() {
            return false;
        }
    }

    /**
     * TODO: would a properties resource be better ? Or just parsing
     * /etc/mime.types ?
     * This is needed because we don't use the default web.xml, where this
     * is encoded.
     */
    public static final String[] DEFAULT_MIME_MAPPINGS = {
        "abs", "audio/x-mpeg",
        "ai", "application/postscript",
        "aif", "audio/x-aiff",
        "aifc", "audio/x-aiff",
        "aiff", "audio/x-aiff",
        "aim", "application/x-aim",
        "art", "image/x-jg",
        "asf", "video/x-ms-asf",
        "asx", "video/x-ms-asf",
        "au", "audio/basic",
        "avi", "video/x-msvideo",
        "avx", "video/x-rad-screenplay",
        "bcpio", "application/x-bcpio",
        "bin", "application/octet-stream",
        "bmp", "image/bmp",
        "body", "text/html",
        "cdf", "application/x-cdf",
        "cer", "application/pkix-cert",
        "class", "application/java",
        "cpio", "application/x-cpio",
        "csh", "application/x-csh",
        "css", "text/css",
        "dib", "image/bmp",
        "doc", "application/msword",
        "dtd", "application/xml-dtd",
        "dv", "video/x-dv",
        "dvi", "application/x-dvi",
        "eps", "application/postscript",
        "etx", "text/x-setext",
        "exe", "application/octet-stream",
        "gif", "image/gif",
        "gtar", "application/x-gtar",
        "gz", "application/x-gzip",
        "hdf", "application/x-hdf",
        "hqx", "application/mac-binhex40",
        "htc", "text/x-component",
        "htm", "text/html",
        "html", "text/html",
        "ief", "image/ief",
        "jad", "text/vnd.sun.j2me.app-descriptor",
        "jar", "application/java-archive",
        "java", "text/x-java-source",
        "jnlp", "application/x-java-jnlp-file",
        "jpe", "image/jpeg",
        "jpeg", "image/jpeg",
        "jpg", "image/jpeg",
        "js", "application/javascript",
        "jsf", "text/plain",
        "jspf", "text/plain",
        "kar", "audio/midi",
        "latex", "application/x-latex",
        "m3u", "audio/x-mpegurl",
        "mac", "image/x-macpaint",
        "man", "text/troff",
        "mathml", "application/mathml+xml",
        "me", "text/troff",
        "mid", "audio/midi",
        "midi", "audio/midi",
        "mif", "application/x-mif",
        "mov", "video/quicktime",
        "movie", "video/x-sgi-movie",
        "mp1", "audio/mpeg",
        "mp2", "audio/mpeg",
        "mp3", "audio/mpeg",
        "mp4", "video/mp4",
        "mpa", "audio/mpeg",
        "mpe", "video/mpeg",
        "mpeg", "video/mpeg",
        "mpega", "audio/x-mpeg",
        "mpg", "video/mpeg",
        "mpv2", "video/mpeg2",
        "nc", "application/x-netcdf",
        "oda", "application/oda",
        "odb", "application/vnd.oasis.opendocument.database",
        "odc", "application/vnd.oasis.opendocument.chart",
        "odf", "application/vnd.oasis.opendocument.formula",
        "odg", "application/vnd.oasis.opendocument.graphics",
        "odi", "application/vnd.oasis.opendocument.image",
        "odm", "application/vnd.oasis.opendocument.text-master",
        "odp", "application/vnd.oasis.opendocument.presentation",
        "ods", "application/vnd.oasis.opendocument.spreadsheet",
        "odt", "application/vnd.oasis.opendocument.text",
        "otg", "application/vnd.oasis.opendocument.graphics-template",
        "oth", "application/vnd.oasis.opendocument.text-web",
        "otp", "application/vnd.oasis.opendocument.presentation-template",
        "ots", "application/vnd.oasis.opendocument.spreadsheet-template ",
        "ott", "application/vnd.oasis.opendocument.text-template",
        "ogx", "application/ogg",
        "ogv", "video/ogg",
        "oga", "audio/ogg",
        "ogg", "audio/ogg",
        "spx", "audio/ogg",
        "flac", "audio/flac",
        "anx", "application/annodex",
        "axa", "audio/annodex",
        "axv", "video/annodex",
        "xspf", "application/xspf+xml",
        "pbm", "image/x-portable-bitmap",
        "pct", "image/pict",
        "pdf", "application/pdf",
        "pgm", "image/x-portable-graymap",
        "pic", "image/pict",
        "pict", "image/pict",
        "pls", "audio/x-scpls",
        "png", "image/png",
        "pnm", "image/x-portable-anymap",
        "pnt", "image/x-macpaint",
        "ppm", "image/x-portable-pixmap",
        "ppt", "application/vnd.ms-powerpoint",
        "pps", "application/vnd.ms-powerpoint",
        "ps", "application/postscript",
        "psd", "image/vnd.adobe.photoshop",
        "qt", "video/quicktime",
        "qti", "image/x-quicktime",
        "qtif", "image/x-quicktime",
        "ras", "image/x-cmu-raster",
        "rdf", "application/rdf+xml",
        "rgb", "image/x-rgb",
        "rm", "application/vnd.rn-realmedia",
        "roff", "text/troff",
        "rtf", "application/rtf",
        "rtx", "text/richtext",
        "sh", "application/x-sh",
        "shar", "application/x-shar",
        /*"shtml", "text/x-server-parsed-html",*/
        "sit", "application/x-stuffit",
        "snd", "audio/basic",
        "src", "application/x-wais-source",
        "sv4cpio", "application/x-sv4cpio",
        "sv4crc", "application/x-sv4crc",
        "svg", "image/svg+xml",
        "svgz", "image/svg+xml",
        "swf", "application/x-shockwave-flash",
        "t", "text/troff",
        "tar", "application/x-tar",
        "tcl", "application/x-tcl",
        "tex", "application/x-tex",
        "texi", "application/x-texinfo",
        "texinfo", "application/x-texinfo",
        "tif", "image/tiff",
        "tiff", "image/tiff",
        "tr", "text/troff",
        "tsv", "text/tab-separated-values",
        "txt", "text/plain",
        "ulw", "audio/basic",
        "ustar", "application/x-ustar",
        "vxml", "application/voicexml+xml",
        "xbm", "image/x-xbitmap",
        "xht", "application/xhtml+xml",
        "xhtml", "application/xhtml+xml",
        "xls", "application/vnd.ms-excel",
        "xml", "application/xml",
        "xpm", "image/x-xpixmap",
        "xsl", "application/xml",
        "xslt", "application/xslt+xml",
        "xul", "application/vnd.mozilla.xul+xml",
        "xwd", "image/x-xwindowdump",
        "vsd", "application/vnd.visio",
        "wav", "audio/x-wav",
        "wbmp", "image/vnd.wap.wbmp",
        "wml", "text/vnd.wap.wml",
        "wmlc", "application/vnd.wap.wmlc",
        "wmls", "text/vnd.wap.wmlsc",
        "wmlscriptc", "application/vnd.wap.wmlscriptc",
        "wmv", "video/x-ms-wmv",
        "wrl", "model/vrml",
        "wspolicy", "application/wspolicy+xml",
        "Z", "application/x-compress",
        "z", "application/x-compress",
        "zip", "application/zip"
    };
}
