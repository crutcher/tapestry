<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:func="http://exslt.org/functions"
                extension-element-prefixes="func"
                exclude-result-prefixes="eg xalan"
>
    <!-- ===================================== -->
    <!-- Work out importing utility sheets -->

    <func:function name="eg:GenerateXPath">
        <xsl:param name="this"/>
        <xsl:param name="pathRoot" select="/"/>
        <xsl:param name="stack" select="'no'"/>

        <xsl:variable name="pathRootIsDocumentRoot" select="not($pathRoot/parent::node())"/>
        <xsl:variable name="parent" select="$this/parent::*"/>
        <xsl:variable name="isDirectChild" select="$pathRoot = $parent"/>

        <func:result>
            <xsl:choose>
                <xsl:when test="$this = $pathRoot">
                    <xsl:choose>
                        <xsl:when test="$pathRootIsDocumentRoot">
                            <xsl:text>/</xsl:text>
                            <xsl:value-of select="name($this)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>.</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <!-- Recurse for the prefix -->
                    <xsl:value-of select="eg:GenerateXPath($parent, $pathRoot, $stack)"/>

                    <xsl:choose>
                        <xsl:when test="$isDirectChild and $pathRootIsDocumentRoot">
                            <xsl:text>/</xsl:text>
                        </xsl:when>
                        <xsl:when test="$isDirectChild"/>
                        <xsl:otherwise>
                            <xsl:if test="$stack = 'yes'">
                                <xsl:text>&#10;</xsl:text>
                            </xsl:if>
                            <xsl:text>/</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>

                    <xsl:value-of select="name($this)"/>

                    <xsl:variable name="idAttribute"
                                  select="$this/@*[name() = 'id' or name() = 'key']"/>

                    <!-- Output the positional selector conditionally -->
                    <xsl:choose>
                        <!-- hardcoded notion of which are id attributes -->
                        <xsl:when test="boolean($idAttribute)">
                            <xsl:text>[@</xsl:text>
                            <xsl:value-of select="name($idAttribute)"/>
                            <xsl:text>='</xsl:text>
                            <xsl:value-of select="$idAttribute"/>
                            <xsl:text>']</xsl:text>
                        </xsl:when>

                        <!-- If there's more than one sibling of the same name, include the position -->
                        <xsl:when test="count($this/../child::*[name() = name($this)]) > 1">
                            <xsl:value-of
                                    select="concat('[', count($this/preceding-sibling::*[name() = name($this)]) + 1, ']')"/>
                        </xsl:when>
                        <!-- Otherwise, don't output anything for the position -->
                        <xsl:otherwise/>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </func:result>

    </func:function>

    <!-- ===================================== -->

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/eg:graph">
        <report>
            <xsl:for-each select="eg:nodes">
                <xsl:apply-templates mode="nodes"/>
            </xsl:for-each>
        </report>
    </xsl:template>


    <xsl:template name="CheckRefTargetType">
        <xsl:param name="ref" select="."/>
        <xsl:param name="context"/>

        <xsl:param name="expectedNs"/>
        <xsl:param name="expectedLocalName"/>

        <xsl:variable name="targetId" select="$ref/@target"/>
        <xsl:variable name="target" select="//*[@id=$targetId]"/>

        <xsl:variable name="ns" select="namespace-uri($target)"/>
        <xsl:variable name="localName" select="local-name($target)"/>

        <xsl:variable name="path" select="eg:GenerateXPath($ref)"/>

        <xsl:variable name="expectedType" select="concat('{', $expectedNs, '}', $expectedLocalName)"/>
        <xsl:variable name="actualType" select="concat('{', $ns, '}', $localName)"/>

        <xsl:variable name="summary">
            <xsl:text>expected "</xsl:text>
            <xsl:value-of select="$expectedType"/>
            <xsl:text>", but found "</xsl:text>
            <xsl:value-of select="$actualType"/>
            <xsl:text>"</xsl:text>
        </xsl:variable>

        <xsl:if test="$expectedType != $actualType">
            <error type="RefTargetType" path="{$path}">
                <summary>
                    <xsl:text>Invalid eg:ref target type: </xsl:text>
                    <xsl:value-of select="$summary"/>
                </summary>
                <details>
                    <target>
                        <xsl:value-of select="$targetId"/>
                    </target>
                    <expectedNs>
                        <xsl:value-of select="$expectedNs"/>
                    </expectedNs>
                    <expectedLocalName>
                        <xsl:value-of select="$expectedLocalName"/>
                    </expectedLocalName>
                    <ns>
                        <xsl:value-of select="$ns"/>
                    </ns>
                    <localName>
                        <xsl:value-of select="$localName"/>
                    </localName>
                </details>

                <message>
                    <xsl:if test="$context">
                        <xsl:value-of select="$context"/>
                        <xsl:text>:: </xsl:text>
                    </xsl:if>

                    <xsl:text>Invalid "</xsl:text>
                    <xsl:value-of select="name($ref)"/>

                    <xsl:text>" @target type, </xsl:text>
                    <xsl:value-of select="$summary"/>
                    <xsl:text>:</xsl:text>

                    <xsl:text>&#10;&#10;</xsl:text>
                    <xsl:text>Target Node:&#10; </xsl:text>
                    <xsl:copy-of select="$target"/>
                </message>
            </error>
            <xsl:text>Referenced node</xsl:text>
        </xsl:if>

    </xsl:template>

    <xsl:template match="*" mode="nodes"/>

    <xsl:template match="eg:tensor" mode="nodes"/>

    <xsl:template match="eg:operation" mode="nodes">
        <xsl:variable name="op" select="."/>


        <xsl:for-each select="(eg:inputs | eg:outputs)">
            <xsl:variable name="map" select="local-name()"/>
            <xsl:for-each select="eg:item">
                <xsl:variable name="key" select="@key"/>
                <xsl:for-each select="eg:ref">

                    <xsl:call-template name="CheckRefTargetType">
                        <xsl:with-param name="context">
                            <xsl:value-of select="name($op)"/>
                            <xsl:text>/</xsl:text>
                            <xsl:value-of select="$map"/>
                            <xsl:text>[@key="</xsl:text>
                            <xsl:value-of select="$key"/>
                            <xsl:text>"][</xsl:text>
                            <xsl:value-of select="position()"/>
                            <xsl:text>]</xsl:text>
                        </xsl:with-param>
                        <xsl:with-param name="expectedNs"
                                        select="'http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd'"/>
                        <xsl:with-param name="expectedLocalName" select="'tensor'"/>
                    </xsl:call-template>

                </xsl:for-each>
            </xsl:for-each>
        </xsl:for-each>


    </xsl:template>

</xsl:stylesheet>