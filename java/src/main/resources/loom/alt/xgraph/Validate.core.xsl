<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="eg:operation">
        <xsl:comment>
            Validate that all operation inputs and outputs are tensors.
        </xsl:comment>
        <xsl:for-each select="(eg:inputs | eg:outputs)/eg:item/eg:ref">
            <xsl:variable name="target" select="//*[@id=current()/@target]"/>

            <xsl:if test="name($target) != 'eg:tensor'">
                <xsl:message terminate="yes">
                    <xsl:text>Operation </xsl:text>
                    <xsl:value-of select="local-name(../..)"/>
                    <xsl:text disable-output-escaping="yes"> item is not an &lt;eg:tensor/&gt;::</xsl:text>

                    <xsl:text>&#10;&#10;</xsl:text>
                    <xsl:text>Target path: "./</xsl:text>
                    <xsl:value-of select="name(../..)"/>
                    <xsl:text>/</xsl:text>
                    <xsl:value-of select="name(..)"/>
                    <xsl:text>[@name='</xsl:text>
                    <xsl:value-of select="../@name"/>
                    <xsl:text>']/[</xsl:text>
                    <xsl:value-of select="position()"/>
                    <xsl:text>]"</xsl:text>

                    <xsl:text>&#10;&#10;</xsl:text>
                    <xsl:text>Operation:&#10;</xsl:text>
                    <xsl:copy-of select="ancestor::eg:operation"/>

                    <xsl:text>&#10;&#10;</xsl:text>
                    <xsl:text>Ref:&#10;</xsl:text>
                    <xsl:copy-of select="."/>

                    <xsl:text>&#10;&#10;</xsl:text>
                    <xsl:text>Target:&#10;</xsl:text>
                    <xsl:copy-of select="$target"/>
                </xsl:message>
            </xsl:if>
        </xsl:for-each>

    </xsl:template>

</xsl:stylesheet>