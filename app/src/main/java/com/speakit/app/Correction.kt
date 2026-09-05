package com.speakit.app

/** Parses presentation variations without accepting an arbitrary model explanation as a sentence. */
object Correction {
    private val label = Regex("(?im)^\\s*(?:[-*]\\s*)?(?:correct(?:ed)?(?:\\s+sentence)?|correction)\\s*[:：]\\s*")
    private val tipLabel = Regex("(?im)\\b(?:tip|explanation)\\s*[:：]\\s*")
    fun sentence(raw: String, original: String, allowPlain: Boolean = false): String? {
        val text = raw.replace("**", "").replace("```", "").trim()
        if (text.contains("<think>", true)) return null
        val match = label.find(text)
        val candidate = if(match != null) text.substring(match.range.last + 1)
            else if(allowPlain) text else return null
        val first = candidate.split(tipLabel, limit = 2).first().trim().lineSequence().firstOrNull()?.trim()?.trim('"', '“', '”') ?: return null
        if(first.length !in 2..350 || first.contains(':') || first.contains('：') || first.contains('<') || first.contains('>')) return null
        if(Regex("(?i)^(here|the correct|the sentence|you should|tip|sorry|i cannot|i can't|as an ai)\\b").containsMatchIn(first)) return null
        val before = words(original).map { it.lowercase() }.toSet()
        val after = words(first).map { it.lowercase() }.toSet()
        if(after.isEmpty() || after.size > before.size * 2 + 5) return null
        if(before.intersect(after).size.toDouble() / before.size.coerceAtLeast(1) < 0.35) return null
        return first
    }
    fun tip(raw: String, original: String, corrected: String): String {
        if(normalized(original) == normalized(corrected)) return "Your sentence is already correct. Listen and say it again."
        val cleaned = raw.replace("**", "")
        val match = tipLabel.find(cleaned)
        val tip = match?.let { cleaned.substring(it.range.last + 1).lineSequence().first().trim().take(220) }.orEmpty()
        if(tip.isNotBlank() && !Regex("(?i)proper noun|past participle|syntactic|morpholog|grammatical|subject.verb|unchanged").containsMatchIn(tip)) return tip
        return "Listen to the corrected sentence. Notice the words that changed, then say it again."
    }
}
