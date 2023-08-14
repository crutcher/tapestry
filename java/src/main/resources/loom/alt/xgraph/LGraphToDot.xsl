<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:eg="http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd"
                exclude-result-prefixes="eg"
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
                    <xsl:otherwise/>
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
        <xsl:text>,"fontname"="helvetica"</xsl:text>
        <xsl:text>,"nodesep"="0.7"</xsl:text>
        <xsl:text>,"forcelabels"="true"</xsl:text>
        <xsl:text>]&#10;</xsl:text>

        <xsl:text>node [</xsl:text>
        <xsl:text>"margin"="0.1"</xsl:text>
        <xsl:text>,"fontname"="helvetica"</xsl:text>
        <xsl:text>"shape"="box"</xsl:text>
        <xsl:text>]&#10;</xsl:text>

        <xsl:text>edge [</xsl:text>
        <xsl:text>"fontname"="helvetica"</xsl:text>
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
        <font color="blue" point-size="12">
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
        <xsl:apply-templates mode="edge-label" select="."/>
    </xsl:template>

    <xsl:template match="*" mode="edge-label">
        <xsl:text>"label"="</xsl:text>
        <xsl:call-template name="GeneratePath">
            <xsl:with-param name="pathRoot" select="ancestor::*[@id][1]"/>
            <xsl:with-param name="this" select="."/>
            <xsl:with-param name="stackFormatParts" select="'yes'"/>
        </xsl:call-template>
        <xsl:text>"</xsl:text>
    </xsl:template>

    <xsl:template match="eg:ref[ancestor::eg:inputs or ancestor::eg:outputs]" mode="edge-label">
        <xsl:variable name="this" select="."/>

        <xsl:text>"label"="</xsl:text>
        <xsl:value-of select="local-name(ancestor::eg:item/parent::*)"/>
        <xsl:text>['</xsl:text>
        <xsl:value-of select="ancestor::eg:item/@key"/>
        <xsl:text>'][</xsl:text>
        <xsl:value-of select="count($this/preceding-sibling::eg:ref) + 1"/>
        <xsl:text>]"</xsl:text>
    </xsl:template>

</xsl:stylesheet>
