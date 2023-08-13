<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
                exclude-result-prefixes="eg"
>
    <!--
    There are XSLT extensions which would permit us to directly pass a document or node-set;
    but debugging them with Java is a pain, so we'll just pass a URI and use a temp file.
    This will parse the temp file as XML, and make it available as a node-set.

    <NodeAliases>
        <node id="..." alias="..."/>
        ...
    </NodeAliases>
     -->
    <xsl:param name="NodeAliasesURI"/>
    <xsl:variable name="NodeAliases" select="document($NodeAliasesURI)"/>

    <xsl:template name="node-id-to-alias">
        <xsl:param name="id"/>
        <xsl:text>#</xsl:text>
        <xsl:value-of select="$NodeAliases/descendant::*[@id=$id]/@*[local-name() = 'alias']"/>
    </xsl:template>

    <xsl:output method="xml" omit-xml-declaration="yes"/>

    <xsl:template name="graph-defaults">
        <xsl:text>graph [</xsl:text>
        <xsl:text>"rankdir"="RL"</xsl:text>
        <xsl:text>,"nodesep"="0.7"</xsl:text>
        <xsl:text>,"forcelabels"="true"</xsl:text>
        <xsl:text>]&#10;</xsl:text>

        <xsl:text>node [</xsl:text>
        <xsl:text>"margin"="0.1",</xsl:text>
        <xsl:text>"shape"="box"</xsl:text>
        <xsl:text>]&#10;</xsl:text>
    </xsl:template>

    <xsl:template match="/eg:graph">
        <xsl:text>digraph G {&#10;</xsl:text>

        <xsl:call-template name="graph-defaults"/>

        <xsl:for-each select="eg:nodes/*">
            <xsl:call-template name="node-item"/>
            <xsl:apply-templates mode="node-edges" select="*"/>
        </xsl:for-each>
        <xsl:text>}</xsl:text>
    </xsl:template>

    <xsl:template name="node-item">
        <xsl:text>"</xsl:text>
        <xsl:value-of select="@id"/>
        <xsl:text>" [</xsl:text>
        <xsl:apply-templates mode="node-attrs" select="."/>
        <xsl:text disable-output-escaping="yes">,"xlabel"=&lt;</xsl:text>
        <font color="red" point-size="12">
            <xsl:call-template name="node-id-to-alias">
                <xsl:with-param name="id" select="@id"/>
            </xsl:call-template>
        </font>
        <xsl:text disable-output-escaping="yes">&gt;</xsl:text>
        <xsl:text>];&#10;</xsl:text>

        <xsl:if test="@target">
            <xsl:text>"</xsl:text>
            <xsl:value-of select="@id"/>
            <xsl:text disable-output-escaping="yes">" -&gt; "</xsl:text>
            <xsl:value-of select="@target"/>
            <xsl:text>" [</xsl:text>
            <xsl:text>"style"="dashed"</xsl:text>
            <xsl:text>];&#10;</xsl:text>
        </xsl:if>
    </xsl:template>

    <xsl:template match="*" mode="node-attrs">
        <xsl:text disable-output-escaping="yes">"label"=&lt;</xsl:text>
        <xsl:value-of select="local-name()"/>
        <xsl:text disable-output-escaping="yes">&gt;</xsl:text>
    </xsl:template>

    <xsl:template match="*" mode="node-edges">
        <xsl:variable name="self" select="../@id"/>

        <xsl:variable name="reverse">
            <xsl:apply-templates mode="reverse-edge" select="."/>
        </xsl:variable>

        <xsl:for-each select="descendant::eg:ref">
            <xsl:variable name="target" select="@target"/>

            <xsl:variable name="to">
                <xsl:choose>
                    <xsl:when test="$reverse = 'yes'">
                        <xsl:value-of select="$self"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$target"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:variable name="from">
                <xsl:choose>
                    <xsl:when test="$reverse = 'yes'">
                        <xsl:value-of select="$target"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$self"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:text>"</xsl:text>
            <xsl:value-of select="$from"/>
            <xsl:text disable-output-escaping="yes">" -&gt; </xsl:text>
            <xsl:text>"</xsl:text>
            <xsl:value-of select="$to"/>
            <xsl:text>" [&#10;</xsl:text>
            <xsl:apply-templates mode="edge-attrs" select="."/>
            <xsl:if test="$reverse = 'yes'">
                <xsl:text>,"dir"="both"</xsl:text>
                <xsl:text>,"arrowhead"="normal"</xsl:text>
                <xsl:text>,"arrowtail"="empty"</xsl:text>
            </xsl:if>
            <xsl:text>];&#10;</xsl:text>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="*" mode="reverse-edge">
        <xsl:text>no</xsl:text>
    </xsl:template>

    <xsl:template match="eg:outputs" mode="reverse-edge">
        <xsl:text>yes</xsl:text>
    </xsl:template>

    <xsl:template match="*" mode="edge-attrs">
        <xsl:call-template name="edge-label"/>
    </xsl:template>

    <xsl:template name="edge-label">
        <xsl:text>"label"="</xsl:text>
        <xsl:call-template name="generate-path">
            <xsl:with-param name="ancestorNode" select="ancestor::*[@id][1]"/>
            <xsl:with-param name="descendantNode" select="."/>
            <xsl:with-param name="stack" select="'yes'"/>
        </xsl:call-template>
        <xsl:text>"</xsl:text>
    </xsl:template>

    <xsl:template name="generate-path">
        <xsl:param name="ancestorNode"/>
        <xsl:param name="descendantNode"/>
        <xsl:param name="stack" select="'no'"/>

        <!-- If the descendant node is neither the ancestor node nor the root, proceed -->
        <xsl:if test="not($descendantNode = $ancestorNode)">
            <xsl:call-template name="generate-path">
                <xsl:with-param name="ancestorNode" select="$ancestorNode"/>
                <xsl:with-param name="descendantNode" select="$descendantNode/parent::*"/>
                <xsl:with-param name="stack" select="$stack"/>
            </xsl:call-template>
        </xsl:if>

        <!-- Check if the current descendantNode is NOT the ancestorNode, then output its name and position -->
        <xsl:if test="not($descendantNode = $ancestorNode)">
            <!-- Check if we're at the first node after the ancestor; if not, prepend with a slash -->
            <xsl:if test="not($descendantNode/parent::* = $ancestorNode)">
                <xsl:if test="$stack = 'yes'">
                    <xsl:text>&#10;</xsl:text>
                </xsl:if>
                <xsl:text>/</xsl:text>
            </xsl:if>

            <xsl:value-of select="name($descendantNode)"/>

            <!-- Output the positional selector conditionally -->
            <xsl:choose>
                <!-- If there's more than one sibling of the same name, include the position -->
                <xsl:when test="count($descendantNode/../child::*[name() = name($descendantNode)]) > 1">
                    <xsl:value-of
                            select="concat('[', count($descendantNode/preceding-sibling::*[name() = name($descendantNode)]) + 1, ']')"/>
                </xsl:when>
                <!-- Otherwise, don't output anything for the position -->
                <xsl:otherwise/>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
