<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/eg:graph">
        <report>
            <xsl:for-each select="eg:nodes">
                <xsl:apply-templates mode="nodes"/>
            </xsl:for-each>
        </report>
    </xsl:template>

    <xsl:template match="*" mode="nodes"/>

    <xsl:template match="eg:tensor" mode="nodes"/>

    <xsl:template match="eg:operation" mode="nodes">
        <xsl:variable name="op" select="."/>

        <xsl:for-each select="(eg:inputs | eg:outputs)">
            <xsl:variable name="map" select="local-name()"/>
            <xsl:for-each select="eg:item">
                <xsl:variable name="key" select="@name"/>
                <xsl:for-each select="eg:ref">
                    <xsl:variable name="ref" select="."/>
                    <xsl:variable name="target" select="//*[@id=current()/@target]"/>

                    <xsl:if test="name($target) != 'eg:tensor'">
                        <error target="{$op/@id}">
                            <message>
                                <xsl:text>Operation </xsl:text>
                                <xsl:value-of select="local-name(../..)"/>
                                <xsl:text> item is not an eg:tensor::</xsl:text>

                                <xsl:text>&#10;&#10;</xsl:text>
                                <xsl:text>Target path: "./</xsl:text>
                                <xsl:value-of select="$map"/>
                                <xsl:text>/</xsl:text>
                                <xsl:value-of select="name(..)"/>
                                <xsl:text>[@name='</xsl:text>
                                <xsl:value-of select="$key"/>
                                <xsl:text>']/[</xsl:text>
                                <xsl:value-of select="position()"/>
                                <xsl:text>]"</xsl:text>

                                <xsl:text>&#10;&#10;</xsl:text>
                                <xsl:text>Operation:&#10;</xsl:text>
                                <xsl:copy-of select="$op"/>

                                <xsl:text>&#10;&#10;</xsl:text>
                                <xsl:text>Ref:&#10;</xsl:text>
                                <xsl:copy-of select="$ref"/>

                                <xsl:text>&#10;&#10;</xsl:text>
                                <xsl:text>Target:&#10;</xsl:text>
                                <xsl:copy-of select="$target"/>
                            </message>
                        </error>
                    </xsl:if>
                </xsl:for-each>
            </xsl:for-each>
        </xsl:for-each>

    </xsl:template>

</xsl:stylesheet>