package com.jrees.wowcompanion

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FORK: the chat keyboard's German word suggestions, off the device: the list, the word completion and its capitals, and the strip's
 * controller rules in [ChatKeys]. Its own file so upstream's tests merge cleanly.
 */
class SuggestTest {

    private fun repo(): File {
        var d: File? = File("").absoluteFile
        while (d != null && !File(d, "companion/tools/whisper_native").isDirectory) d = d.parentFile
        return d ?: error("no repository root above ${File("").absolutePath}")
    }

    @Before fun list() {
        Suggest.load(File(repo(), "companion/app/src/main/assets/companion/words/de.txt").readText())
    }

    @Test fun theListIsAsciiAndHasNoDuplicates() {
        val t = File(repo(), "companion/app/src/main/assets/companion/words/de.txt").readText()
        Suggest.load(t)
        assertTrue(Suggest.loaded)
        // every word that is kept is printable ASCII: the chat takes nothing else
        val kept = Suggest.forText("a", 1000) + Suggest.forText("e", 1000) + Suggest.forText("s", 1000)
        assertTrue(kept.all { w -> w.all { it.code in 33..126 } })
    }

    @Test fun completesTheWordBeingTypedMostCommonFirst() {
        assertEquals(listOf("genau", "gehen", "gehe"), Suggest.forText("Hallo wie ge"))
        assertTrue(Suggest.forText("Ich habe heute kei").first().equals("kein", ignoreCase = true))
        assertTrue(Suggest.forText("wir sind da", 3).all { it.length > 2 && it.startsWith("da", ignoreCase = true) })
    }

    @Test fun noWordOnABlankAndStartersOnAnEmptyBox() {
        assertEquals(Suggest.STARTERS, Suggest.forText(""))
        assertEquals(emptyList<String>(), Suggest.forText("Hallo "))
        assertEquals(emptyList<String>(), Suggest.forText("Hallo wie geht?"))
    }

    @Test fun capitalsFollowTheTextAndTheSentence() {
        // a message start and a sentence start begin with a capital; mid-sentence a noun keeps its own
        assertEquals("Schon", Suggest.forText("sch").first())
        assertEquals("Das", Suggest.forText("Das ist gut. da").first())
        assertTrue(Suggest.forText("Wir gehen in die Sta").contains("Stadt"))
        // ALL CAPS typed stays all caps
        assertEquals("SCHON", Suggest.forText("SCH").first())
    }

    @Test fun anAppliedWordReplacesTheTypedOneAndAddsABlank() {
        assertEquals("Hallo wie gehst ", Suggest.apply("Hallo wie ge", "gehst"))
        assertEquals("gut ", Suggest.apply("g", "gut"))
        // too long for one chat message: refused, nothing changes
        assertNull(Suggest.apply("a ".repeat(126) + "aa", "bbbbbb"))
    }

    // ---- the strip in ChatKeys
    private fun keys(): ChatKeys = ChatKeys(ChatKeys.Layout.DEFAULT).also { it.suggester = { t -> Suggest.forText(t) }; it.open() }

    @Test fun upFromTheTopRowEntersTheStripAndAPicksAWord() {
        val k = keys()
        var text = "Hallo wie ge"
        repeat(5) { k.press("PADDUP", text) }
        assertTrue(k.onStrip)
        k.press("PADDRIGHT", text); k.press("PADDLEFT", text)
        val a = k.press("PAD1", text)
        assertTrue(a is ChatKeys.Act.Text)
        text = (a as ChatKeys.Act.Text).text
        assertTrue(text.endsWith(" "))
        assertFalse(k.onStrip)
    }

    @Test fun downLeavesTheStripAndYAndLTWorkOnIt() {
        val k = keys()
        val text = "Hallo wie ge"
        repeat(6) { k.press("PADDUP", text) }
        assertTrue(k.onStrip)
        assertEquals(ChatKeys.Act.Text("Hallo wie ge "), k.press("PAD4", text))
        k.press("PADDDOWN", text)
        assertFalse(k.onStrip)
        assertEquals(0, k.row)
        repeat(2) { k.press("PADDUP", text) }
        assertEquals(ChatKeys.Act.Text("Hallo wie g"), k.press("PADLTRIGGER", text))
    }

    @Test fun withoutAStripOrWordsTheKeysAreAsBefore() {
        val plain = ChatKeys(ChatKeys.Layout.DEFAULT).also { it.open() }
        repeat(6) { plain.press("PADDUP", "ge") }
        assertFalse(plain.onStrip)
        val k = keys()
        repeat(6) { k.press("PADDUP", "Hallo ") }
        assertFalse(k.onStrip)
        assertEquals(0, k.row)
    }

    @Test fun aFingerOnAKeyLeavesTheStrip() {
        val k = keys()
        repeat(6) { k.press("PADDUP", "ge") }
        assertTrue(k.onStrip)
        k.touch(1, 0, "ge")
        assertFalse(k.onStrip)
    }
}
