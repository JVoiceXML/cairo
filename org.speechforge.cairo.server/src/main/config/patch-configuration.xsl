<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">
    <xsl:param name="synthesizer" />
    <xsl:param name="recordDir" />
    <xsl:param name="grammarDir" />
    <xsl:param name="voice" />
    <xsl:param name="promptDir" />
    <xsl:param name="ipAddress" />

    <xsl:template match="speechSynthesizer">
        <xsl:copy>
            <xsl:apply-templates select="@*" /><xsl:value-of select="replace(text(), '@@SYNTHESIZER@@', $synthesizer)" />
            <!-- Keep current settings -->
            <xsl:apply-templates select="@*|*|comment()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="baseRecordingDir">
        <xsl:copy>
            <xsl:apply-templates select="@*" /><xsl:value-of select="replace(text(), '@@RECORD_DIR@@', $recordDir)" />
            <!-- Keep current settings -->
            <xsl:apply-templates select="@*|*|comment()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="baseGrammarDir">
        <xsl:copy>
            <xsl:apply-templates select="@*" /><xsl:value-of select="replace(text(), '@@GRAMMAR_DIR@@', $grammarDir)" />
            <!-- Keep current settings -->
            <xsl:apply-templates select="@*|*|comment()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="voiceName">
        <xsl:copy>
            <xsl:apply-templates select="@*" /><xsl:value-of select="replace(text(), '@@VOICE@@', $voice)" />
            <!-- Keep current settings -->
            <xsl:apply-templates select="@*|*|comment()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="basePromptDir">
        <xsl:copy>
            <xsl:apply-templates select="@*" /><xsl:value-of select="replace(text(), '@@PROMPT_DIR@@', $promptDir)" />
            <!-- Keep current settings -->
            <xsl:apply-templates select="@*|*|comment()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="ipAddress">
        <xsl:copy>
            <xsl:apply-templates select="@*" /><xsl:value-of select="replace(text(), '@@IP@@', $ipAddress)" />
            <!-- Keep current settings -->
            <xsl:apply-templates select="@*|*|comment()" />
        </xsl:copy>
    </xsl:template>
    
    <!-- This template passes anything unmatched -->
    <xsl:template match="@*|*|text()|comment()">
        <xsl:copy>
            <xsl:apply-templates select="@*|*|text()|comment()" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
