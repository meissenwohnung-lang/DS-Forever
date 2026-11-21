package com.jrees.wowcompanion

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FORK: German voice typing, off the device. The voice language setting (Auto / Deutsch / English), the recognizer's language tag and
 * the umlauts, which chat can only carry as ASCII. Kept in its own file so upstream's VoiceTest merges cleanly.
 */
class VoiceGermanTest {

    @Test fun autoFollowsTheAppsLanguage() {
        assertEquals("en", Voice.speechLang("auto", "en"))
        assertEquals("fr", Voice.speechLang("auto", "fr"))
        assertEquals("es", Voice.speechLang(null, "es"))
        assertEquals("en", Voice.speechLang("anything else", "en"))
    }

    @Test fun anExplicitChoiceWinsOverTheAppsLanguage() {
        assertEquals("de", Voice.speechLang("de", "en"))
        assertEquals("de", Voice.speechLang("de", "fr"))
        assertEquals("en", Voice.speechLang("en", "es"))
    }

    @Test fun germanHasItsOwnRecognizerTag() {
        assertEquals("de-DE", Voice.language("de"))
        assertEquals("en-US", Voice.language("en"))
    }

    @Test fun umlautsAreWrittenTheGermanWay() {
        assertEquals("Wir muessen den Boss toeten", Voice.umlauts("Wir müssen den Boss töten"))
        assertEquals("Aerzte Oel Ueberfall", Voice.umlauts("Ärzte Öl Überfall"))
        assertEquals("schaeden fuer alle", Voice.umlauts("schäden für alle"))
    }

    @Test fun textWithoutUmlautsIsUntouched() {
        assertEquals("Let's go kill the boss", Voice.umlauts("Let's go kill the boss"))
        assertEquals("", Voice.umlauts(""))
    }

    @Test fun theSharpSIsStillFoldedByTheExistingRule() {
        assertEquals("Strasse", Voice.fold("Straße"))
        // clean() = fold + ASCII: an umlaut that was written out first survives as letters, one that was not loses its dots
        assertEquals("fuer", Voice.clean(Voice.umlauts("für"), 20))
        assertEquals("fur", Voice.clean("für", 20))
    }
}
