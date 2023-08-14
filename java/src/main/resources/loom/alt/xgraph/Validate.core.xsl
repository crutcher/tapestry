<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
                xmlns:xalan="http://xml.apache.org/xalan"
                exclude-result-prefixes="eg xalan"
>
    <!-- ===================================== -->
    <!-- Work out importing utility sheets -->


    <xsl:template name="GeneratePath">
        <xsl:param name="this"/>
        <xsl:param name="pathRoot"/>

        <xsl:param name="stackFormatParts" select="'no'"/>

        <xsl:param name="pathRootIsDocumentRoot" select="not($pathRoot/parent::node())"/>

        <xsl:param name="parent" select="$this/parent::*"/>
        <xsl:variable name="isDirectChild" select="$pathRoot = $parent"/>

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
                <xsl:call-template name="GeneratePath">
                    <xsl:with-param name="pathRoot" select="$pathRoot"/>
                    <xsl:with-param name="this" select="$parent"/>
                    <xsl:with-param name="stackFormatParts" select="$stackFormatParts"/>
                </xsl:call-template>

                <xsl:choose>
                    <xsl:when test="$isDirectChild and $pathRootIsDocumentRoot">
                        <xsl:text>/</xsl:text>
                    </xsl:when>
                    <xsl:when test="$isDirectChild"/>
                    <xsl:otherwise>
                        <xsl:if test="$stackFormatParts = 'yes'">
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

    </xsl:template>
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
        <xsl:param name="expectedType"/>

        <xsl:variable name="targetId" select="$ref/@target"/>
        <xsl:variable name="target" select="//*[@id=$targetId]"/>
        <xsl:variable name="actualType" select="name($target)"/>

        <xsl:variable name="path">
            <xsl:call-template name="GeneratePath">
                <xsl:with-param name="pathRoot" select="/"/>
                <xsl:with-param name="this" select="$ref"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="summary">
            <xsl:text>expected "</xsl:text>
            <xsl:value-of select="$expectedType"/>
            <xsl:text>", but found "</xsl:text>
            <xsl:value-of select="$actualType"/>
            <xsl:text>"</xsl:text>
        </xsl:variable>

        <xsl:if test="$actualType != $expectedType">
            <error type="RefTargetType" path="{$path}">
                <summary>
                    <xsl:text>Invalid eg:ref target type: </xsl:text>
                    <xsl:value-of select="$summary"/>
                </summary>
                <details>
                    <target>
                        <xsl:value-of select="$targetId"/>
                    </target>
                    <expectedType>
                        <xsl:value-of select="$expectedType"/>
                    </expectedType>
                    <actualType>
                        <xsl:value-of select="$actualType"/>
                    </actualType>
                </details>

                <message>
                    <xsl:if test="$context">
                        <xsl:value-of select="$context"/>
                        <xsl:text>:: </xsl:text>
                    </xsl:if>

                    <xsl:text>Invalid </xsl:text>

                    <xsl:text disable-output-escaping="yes">&lt;</xsl:text>
                    <xsl:value-of select="name($ref)"/>
                    <xsl:text disable-output-escaping="yes">/&gt;</xsl:text>

                    <xsl:text> @target type, </xsl:text>
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
                        <xsl:with-param name="expectedType" select="'eg:tensor'"/>
                    </xsl:call-template>

                </xsl:for-each>
            </xsl:for-each>
        </xsl:for-each>


    </xsl:template>

</xsl:stylesheet>