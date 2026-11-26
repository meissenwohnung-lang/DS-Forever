package com.jrees.wowcompanion

import android.content.Context

/**
 * FORK: WORD SUGGESTIONS FOR THE CHAT KEYBOARD (optional, off by default: Settings > General > Voice typing > Suggestions).
 *
 * Pure Kotlin like [ChatKeys] (tested off the device): the word being typed (the letters at the end of the text) is completed from a
 * list of common words, most common first. The chat carries printable ASCII only, so the German list is written the way it can be
 * typed: ae oe ue for the umlauts, ss for the sharp s ("Schoen", "Gruesse", "Strasse"). The list keeps the words' own capital
 * (German nouns); a word typed with a capital, or one that starts a message or a sentence, is capitalised too.
 *
 * The list is the text asset companion/words/de.txt: words separated by blanks or lines, most common first. Nothing is learned and
 * nothing typed is stored or sent anywhere; the list is read once and kept in memory.
 */
object Suggest {
    /** Settings.keySuggest's value for German */
    const val DE = "de"
    /** how many words the strip shows */
    const val MAX = 3
    /** the asset the German list is read from */
    const val ASSET = "companion/words/de.txt"
    /** a message start with nothing typed yet: these three */
    val STARTERS = listOf("Hallo", "Danke", "Moin")

    @Volatile private var words: List<String> = emptyList()
    @Volatile private var lower: List<String> = emptyList()

    /** the list is in memory */
    val loaded: Boolean get() = words.isNotEmpty()

    /** the list from its text (blank / line separated, most common first); duplicates keep their first place */
    fun load(text: String) {
        val seen = HashSet<String>()
        val w = ArrayList<String>()
        for (t in text.split(Regex("\\s+"))) {
            if (t.isEmpty() || !t.all { it.code in 33..126 } || !seen.add(t.lowercase())) continue
            w += t
        }
        words = w
        lower = w.map { it.lowercase() }
    }

    /** reads the German list from the app's assets once (a missing asset leaves the strip empty); true = there is a list */
    fun ensure(ctx: Context): Boolean {
        if (loaded) return true
        runCatching { ctx.assets.open(ASSET).use { load(it.readBytes().toString(Charsets.UTF_8)) } }
        return loaded
    }

    /** Settings says German suggestions are on */
    fun on(): Boolean = Settings.keySuggest == DE

    /** the letters at the end of [text] ("Hallo wie ge" -> "ge") */
    fun prefixOf(text: String): String = text.takeLastWhile { it in 'a'..'z' || it in 'A'..'Z' }

    /** the three (at most) words to offer for [text]: completions of the word being typed, else the starters on an empty box */
    fun forText(text: String, max: Int = MAX): List<String> {
        if (words.isEmpty()) return emptyList()
        if (text.isEmpty()) return STARTERS.take(max)
        val p = prefixOf(text)
        if (p.isEmpty()) return emptyList()
        val key = p.lowercase()
        val out = ArrayList<String>(max)
        for (i in words.indices) {
            val l = lower[i]
            if (l.length > key.length && l.startsWith(key)) {
                out += styled(words[i], p, text)
                if (out.size >= max) break
            }
        }
        return out
    }

    /** [w] in the capitals the place wants: ALL CAPS typed -> all caps; a capital typed, or a message / sentence start -> first letter */
    private fun styled(w: String, typed: String, text: String): String {
        if (typed.length >= 2 && typed.all { it in 'A'..'Z' }) return w.uppercase()
        val before = text.dropLast(typed.length)
        val starts = before.isBlank() || before.trimEnd().let { it.endsWith(".") || it.endsWith("!") || it.endsWith("?") } && before.endsWith(" ")
        return if (typed[0] in 'A'..'Z' || starts) w.replaceFirstChar { it.uppercaseChar() } else w
    }

    /** [text] with its last word replaced by [word] and a blank after it; null when it would not fit in [max] characters */
    fun apply(text: String, word: String, max: Int = ChatKeys.MAX): String? {
        val r = text.dropLast(prefixOf(text).length) + word + " "
        return if (r.length > max) null else r
    }
}
