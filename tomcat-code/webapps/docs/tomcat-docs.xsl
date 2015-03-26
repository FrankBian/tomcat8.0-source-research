<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!-- Content Stylesheet for "tomcat-docs" Documentation -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="3.0">


  <!-- Output method -->
  <xsl:output method="html"
              html-version="5.0"
              encoding="UTF-8"
              indent="no"
              doctype-system="about:legacy-compat"/>


  <!-- Defined parameters (overrideable) -->
  <xsl:param    name="home-name"           select="'The Tomcat Project'"/>
  <xsl:param    name="home-href"           select="'http://tomcat.apache.org/'"/>
  <xsl:param    name="home-logo"           select="'/images/tomcat.png'"/>
  <xsl:param    name="home-stylesheet"     select="'/images/docs-stylesheet.css'"/>
  <xsl:param    name="apache-logo"         select="'/images/asf-feather.png'"/>
  <xsl:param    name="subdir"              select="''"/>
  <xsl:param    name="relative-path"       select="'.'"/>
  <xsl:param    name="version"             select="'8.0.x'"/>
  <xsl:param    name="majorversion"        select="'8'"/>
  <xsl:param    name="majorminorversion"   select="'8.0'"/>
  <xsl:param    name="build-date"          select="'MMM d yyyy'"/>
  <xsl:param    name="build-date-iso-8601" select="'yyyy-dd-MM'"/>
  <xsl:param    name="year"                select="'yyyy'"/>
  <xsl:param    name="buglink"             select="'http://bz.apache.org/bugzilla/show_bug.cgi?id='"/>
  <xsl:param    name="revlink"             select="'http://svn.apache.org/viewvc?view=rev&amp;rev='"/>
  <xsl:param    name="doclink"             select="'http://tomcat.apache.org/tomcat-8.0-doc'"/>
  <xsl:param    name="sylink"              select="'http://tomcat.apache.org/security-8.html'"/>
  <xsl:param    name="dllink"              select="'http://tomcat.apache.org/download-80.cgi'"/>
  <xsl:param    name="sitedir"             select="''"/>
  <xsl:param    name="filename"            select="'-'"/>

  <!-- Defined variables (non-overrideable) -->
  <xsl:variable name="commentslink"><xsl:value-of select="$relative-path"/>/comments.html</xsl:variable>
  <xsl:variable name="project-xml-filename"><xsl:value-of select="$subdir"/>project.xml</xsl:variable>
  <xsl:variable name="project"
              select="document($project-xml-filename)/project"/>

  <!-- Process an entire document into an HTML page -->
  <xsl:template match="document">
<html lang="en">
<head>
  <!-- Note: XLST seems to always output a
       <META http-equiv="Content-Type" content="text/html; charset=UTF-8">
       when method="html",
       therefore we can't use
       <meta charset="UTF-8"/>.

       In XHTML, this is not needed as the encoding will be
       specified in the XML declaration.
  -->

  <xsl:variable name="css-src">
    <xsl:value-of select="$relative-path"/><xsl:value-of select="$home-stylesheet"/>
  </xsl:variable>
  <link href="{$css-src}" rel="stylesheet" type="text/css"/>

  <title><xsl:value-of select="$project/title"/> (<xsl:value-of select="$version"/>) - <xsl:value-of select="properties/title"/></title>
  <xsl:for-each select="properties/author">
    <xsl:variable name="name">
      <xsl:value-of select="."/>
    </xsl:variable>
    <!--
      <xsl:variable name="email">
        <xsl:value-of select="@email"/>
      </xsl:variable>
    -->
    <meta name="author" content="{$name}"/>
  </xsl:for-each>

  <!-- Script for ASF Comments System. -->
  <!--
    Use data-* attributes for retrieval of XSLT-generated data
    in JavaScript.
    Use this approach rather than directly inserting text
    in a JS string literal as that would cause
    problems when the string contains special characters
    like ", ', \n etc.
  -->
  <xsl:variable name="comments-identifier">
    <xsl:value-of select="$sitedir"/><xsl:value-of select="$subdir"/><xsl:value-of select="substring($filename,1,string-length($filename)-4)"/>
  </xsl:variable>
  <xsl:if test="not(properties/no-comments)">
  <script type="application/javascript"
      data-comments-identifier="{$comments-identifier}"><![CDATA[
    "use strict"; // Enable strict mode

    (function() {
      var thisScript = document.currentScript;
      if (!thisScript) { // Workaround for IE <= 11
        var scripts = document.getElementsByTagName("script");
        thisScript = scripts[scripts.length - 1];
      }
      document.addEventListener("DOMContentLoaded", (function() {
        var commentsDiv = document.getElementById("comments_thread");
        var commentsShortname = "tomcat";
        var commentsIdentifier = "http://tomcat.apache.org/" +
          thisScript.getAttribute("data-comments-identifier") + ".html";

        (function(w, d) {
          if (w.location.hostname.toLowerCase() == "tomcat.apache.org") {
            var s = d.createElement("script");
            s.type = "application/javascript";
            s.async = true;
            s.src = "https://comments.apache.org/show_comments.lua?site=" +
              encodeURIComponent(commentsShortname) +
              "&page=" + encodeURIComponent(commentsIdentifier);
            d.head.appendChild(s);
          } else {
            commentsDiv.appendChild(d.createTextNode("Comments are disabled for this page at the moment."));
          }
        })(window, document);
      }), false);
    })();
  ]]></script>
  </xsl:if>
  </head>

  <body>
  <div id="wrapper">
  <!-- Header -->
  <header><div id="header">
    <div>
      <div>
        <xsl:if test="$project/logo">
          <xsl:variable name="src">
            <xsl:value-of select="$relative-path"/><xsl:value-of select="$home-logo"/>
          </xsl:variable>
          <div class="logo noPrint">
            <a href="{$project/@href}"><img alt="Tomcat Home" src="{$src}"/></a>
          </div>
        </xsl:if>

        <div style="height: 1px;"/>
        <xsl:variable name="src">
          <xsl:value-of select="$relative-path"/><xsl:value-of select="$apache-logo"/>
        </xsl:variable>
        <div class="asfLogo noPrint">
          <a href="http://www.apache.org/" target="_blank"><img src="{$src}" alt="The Apache Software Foundation" style="width: 266px; height: 83px;"/></a>
        </div>
        <h1><xsl:value-of select="$project/title"/></h1>
        <div class="versionInfo">
          Version <xsl:value-of select="$version"/>,
          <time datetime="{$build-date-iso-8601}"><xsl:value-of select="$build-date"/></time>
        </div>
        <div style="height: 1px;"/>
        <div style="clear: left;"/>
      </div>
    </div>
  </div></header>

  <div id="middle">
    <div>
      <div id="mainLeft" class="noprint">
        <div>
          <!-- Navigation -->
          <nav>
            <xsl:apply-templates select="$project/body/menu"/>
          </nav>
        </div>
      </div>
      <div id="mainRight">
        <div id="content">
          <!-- Main Part -->
            <h2><xsl:value-of select="properties/title"/></h2>
            <xsl:apply-templates select="body/section"/>

            <!-- Comments Section -->
            <xsl:if test="not(properties/no-comments)">
              <div class="noprint">
                <h3 id="comments_section">
                  Comments
                </h3>

                <div class="text">
                  <p class="notice">
                    <strong>Notice: </strong>This comments section collects your suggestions
                    on improving documentation for Apache Tomcat.<br/><br/>
                    If you have trouble and need help, read
                    <a href="http://tomcat.apache.org/findhelp.html">Find Help</a> page
                    and ask your question on the tomcat-users
                    <a href="http://tomcat.apache.org/lists.html">mailing list</a>.
                    Do not ask such questions here. This is not a Q&amp;A section.<br/><br/>
                    The Apache Comments System is explained <a href="{$commentslink}">here</a>.
                    Comments may be removed by our moderators if they are either
                    implemented or considered invalid/off-topic.
                  </p>
                  <div id="comments_thread"/>
                </div>
              </div>
            </xsl:if>

        </div>
      </div>
    </div>
  </div>

  <!-- Footer -->
  <footer><div id="footer">
    Copyright © 1999-<xsl:value-of select="$year"/>, The Apache Software Foundation
  </div></footer>
</div>
</body>
</html>


  </xsl:template>


  <!-- Process a menu for the navigation bar -->
  <xsl:template match="menu">
  <div>
    <h2><xsl:value-of select="@name"/></h2>
    <ul>
      <xsl:apply-templates select="item"/>
    </ul>
  </div>
  </xsl:template>


  <!-- Process a menu item for the navigation bar -->
  <xsl:template match="item">
    <xsl:variable name="href">
      <xsl:value-of select="@href"/>
    </xsl:variable>
    <li><a href="{$href}"><xsl:value-of select="@name"/></a></li>
  </xsl:template>


  <!-- Process a documentation section -->
  <xsl:template match="section">
    <xsl:variable name="name2">
      <xsl:choose>
        <xsl:when test="@anchor">
          <xsl:value-of select="@anchor" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="name">
      <xsl:value-of select="translate($name2, ' #', '__')"/>
    </xsl:variable>


    <!-- Section heading -->
    <h3 id="{$name}">
      <xsl:if test="@rtext">
        <!-- Additional right-aligned text cell in section heading. It is used by changelog.xml -->
        <span style="float: right;">
          <xsl:value-of select="@rtext"/>
        </span><xsl:text>&#x20;</xsl:text> <!-- Ensure a space follows after </span> -->
      </xsl:if>
      <xsl:value-of select="@name"/>
    </h3>
    <!-- Section body -->
    <div class="text">
      <xsl:apply-templates/>
    </div>

  </xsl:template>


  <!-- Process a documentation subsection -->
  <xsl:template match="subsection">
    <xsl:variable name="name2">
      <xsl:choose>
        <xsl:when test="@anchor">
          <xsl:value-of select="@anchor" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:if test="
              count(//*[self::section or self::subsection][@name=current()/@name]) &gt; 1
              ">
            <xsl:value-of select="concat(parent::*[self::section or self::subsection]/@name, '/')"/>
          </xsl:if>
          <xsl:value-of select="@name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="name">
      <xsl:value-of select="translate($name2, ' #', '__')"/>
    </xsl:variable>

    <div class="subsection">
      <!-- Subsection heading -->
      <!-- TODO: When a <subsection> is nested in another <subsection>,
           the output should be <h5>, not <h4>. Same with <h6>. -->
      <h4 id="{$name}">
        <xsl:value-of select="@name"/>
      </h4>
      <!-- Subsection body -->
      <div class="text">
        <xsl:apply-templates/>
      </div>
    </div>

  </xsl:template>


  <!-- Generate table of contents -->
  <xsl:template match="toc">
    <ul><xsl:apply-templates mode="toc" select="following::section"/></ul>
  </xsl:template>

  <xsl:template mode="toc" match="section|subsection">
    <xsl:variable name="name2">
      <xsl:choose>
        <xsl:when test="@anchor">
          <xsl:value-of select="@anchor" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:if test="local-name()='subsection' and
              count(//*[self::section or self::subsection][@name=current()/@name]) &gt; 1
              ">
            <xsl:value-of select="concat(parent::*[self::section or self::subsection]/@name, '/')"/>
          </xsl:if>
          <xsl:value-of select="@name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="name">
      <xsl:value-of select="translate($name2, ' #', '__')"/>
    </xsl:variable>
    <li><a href="#{$name}"><xsl:value-of select="@name"/></a>
    <xsl:if test="subsection">
      <ol><xsl:apply-templates mode="toc" select="subsection"/></ol>
    </xsl:if>
    </li>
  </xsl:template>


  <!-- Process a source code example -->
  <xsl:template match="source">
  <div class="codeBox">
    <pre>
      <xsl:if test="@wrapped='true'">
        <xsl:attribute name="class">wrap</xsl:attribute>
      </xsl:if>
      <code><xsl:apply-templates/></code>
    </pre>
  </div>
  </xsl:template>

  <!-- Process an attributes list with nested attribute elements -->
  <xsl:template match="attributes">
    <table class="defaultTable">
      <tr>
        <th style="width: 15%;">
          Attribute
        </th>
        <th style="width: 85%;">
          Description
        </th>
      </tr>
      <xsl:for-each select="attribute">
        <tr>
          <td>
            <xsl:if test="@required = 'true'">
              <strong><code class="attributeName"><xsl:value-of select="@name"/></code></strong>
            </xsl:if>
            <xsl:if test="@required != 'true'">
              <code class="attributeName"><xsl:value-of select="@name"/></code>
            </xsl:if>
          </td>
          <td>
            <xsl:apply-templates/>
          </td>
        </tr>
      </xsl:for-each>
    </table>
  </xsl:template>

  <!-- Process a properties list with nested property elements -->
  <xsl:template match="properties">
    <table class="defaultTable">
      <tr>
        <th style="width: 15%;">
          Property
        </th>
        <th style="width: 85%;">
          Description
        </th>
      </tr>
      <xsl:for-each select="property">
        <tr>
          <td>
            <code class="propertyName"><xsl:value-of select="@name"/></code>
          </td>
          <td>
            <xsl:apply-templates/>
          </td>
        </tr>
      </xsl:for-each>
    </table>
  </xsl:template>

  <!-- Changelog related tags -->
  <xsl:template match="changelog">
    <ul class="changelog">
      <xsl:apply-templates/>
    </ul>
  </xsl:template>

  <xsl:template match="changelog/add">
    <xsl:variable name="src"><xsl:value-of select="$relative-path"/>/images/add.gif</xsl:variable>
    <li>
    <img alt="Add: " class="icon" src="{$src}"/><xsl:apply-templates/>
  </li>
  </xsl:template>

  <xsl:template match="changelog/update">
    <xsl:variable name="src"><xsl:value-of select="$relative-path"/>/images/update.gif</xsl:variable>
    <li>
    <img alt="Update: " class="icon" src="{$src}"/><xsl:apply-templates/>
  </li>
  </xsl:template>

  <xsl:template match="changelog/design">
    <xsl:variable name="src"><xsl:value-of select="$relative-path"/>/images/design.gif</xsl:variable>
    <li>
    <img alt="Design: " class="icon" src="{$src}"/><xsl:apply-templates/>
  </li>
  </xsl:template>

  <xsl:template match="changelog/docs">
    <xsl:variable name="src"><xsl:value-of select="$relative-path"/>/images/docs.gif</xsl:variable>
    <li>
    <img alt="Docs: " class="icon" src="{$src}"/><xsl:apply-templates/>
  </li>
  </xsl:template>

  <xsl:template match="changelog/fix">
    <xsl:variable name="src"><xsl:value-of select="$relative-path"/>/images/fix.gif</xsl:variable>
    <li>
    <img alt="Fix: " class="icon" src="{$src}"/><xsl:apply-templates/>
  </li>
  </xsl:template>

  <xsl:template match="changelog/scode">
    <xsl:variable name="src"><xsl:value-of select="$relative-path"/>/images/code.gif</xsl:variable>
    <li>
    <img alt="Code: " class="icon" src="{$src}"/><xsl:apply-templates/>
  </li>
  </xsl:template>

  <!-- Link to a bug report -->
  <xsl:template match="bug">
      <xsl:variable name="link"><xsl:value-of select="$buglink"/><xsl:value-of select="text()"/></xsl:variable>
      <a href="{$link}"><xsl:apply-templates/></a>
  </xsl:template>

  <!-- Link to a SVN revision report -->
  <xsl:template match="rev">
      <xsl:variable name="link"><xsl:value-of select="$revlink"/><xsl:value-of select="text()"/></xsl:variable>
      <a href="{$link}">r<xsl:apply-templates/></a>
  </xsl:template>

  <!-- Link to online docs -->
  <xsl:template match="doc">
      <xsl:variable name="link"><xsl:value-of select="$doclink"/><xsl:value-of select="@path"/></xsl:variable>
      <a href="{$link}"><xsl:apply-templates/></a>
  </xsl:template>

  <!-- Link to security page -->
  <xsl:template match="security">
      <xsl:variable name="link"><xsl:value-of select="$sylink"/></xsl:variable>
      <a href="{$link}"><xsl:apply-templates/></a>
  </xsl:template>

  <!-- Link to download page -->
  <xsl:template match="download">
      <xsl:variable name="link"><xsl:value-of select="$dllink"/></xsl:variable>
      <a href="{$link}"><xsl:apply-templates/></a>
  </xsl:template>

  <!-- Version numbers -->
  <xsl:template match="version-major-minor">
    <xsl:value-of select="$majorminorversion"/>
  </xsl:template>
  <xsl:template match="version-major">
    <xsl:value-of select="$majorversion"/>
  </xsl:template>

  <!-- Process everything else by just passing it through -->
  <xsl:template match="*|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|text()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
