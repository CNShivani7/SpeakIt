package com.speakit.app

import org.junit.Assert.*
import org.junit.Test

class CorrectionTest {
    private val spoken = "Yesterday I go to office"
    @Test fun acceptsMarkdownLabels() {
        assertEquals("Yesterday I went to the office.", Correction.sentence("**Corrected sentence:** Yesterday I went to the office.\n**Tip:** Use went for yesterday.", spoken))
    }
    @Test fun acceptsPlainSentenceOnlyForSecondAttempt() {
        assertNull(Correction.sentence("Yesterday I went to the office.", spoken))
        assertEquals("Yesterday I went to the office.", Correction.sentence("Yesterday I went to the office.", spoken, true))
    }
    @Test fun rejectsUnrelatedOutput() { assertNull(Correction.sentence("Correct: The cat sleeps on a mat.", spoken)) }
    @Test fun rejectsExplanationInsteadOfSentence() { assertNull(Correction.sentence("You should use went for the past.", spoken, true)) }
    @Test fun rejectsPlaceholderAndThinkingOutput() {
        assertNull(Correction.sentence("Correct: <one English sentence>", spoken))
        assertNull(Correction.sentence("<think>Check tense.</think>\nCorrect: Yesterday I went to the office.", spoken))
    }
    @Test fun handlesSameLineTip() { assertEquals("Yesterday I went to the office.", Correction.sentence("Correct: Yesterday I went to the office. Tip: Use went.", spoken)) }
    @Test fun keepsCorrectIntroductionFeedbackSimple() {
        assertEquals("Your sentence is already correct. Listen and say it again.", Correction.tip("Tip: Asha is a proper noun.", "my name is Asha", "My name is Asha."))
    }
}
