package com.speakit.app

import java.util.Locale

data class Lesson(val title: String, val hindiTitle: String, val picture: String, val prompt: String,
    val hindiPrompt: String, val example: String, val hint: String, val hindiHint: String)
val lessons = listOf(
    Lesson("Say hello", "अपना परिचय दें", "👋", "Introduce yourself in one sentence.", "एक वाक्य में अपना परिचय दें।", "My name is Asha.", "Use 'My name is' before your name.", "अपने नाम से पहले My name is कहें।"),
    Lesson("Yesterday", "बीता हुआ कल", "🏢", "Tell me where you went yesterday.", "बताइए, आप कल कहाँ गए थे?", "Yesterday, I went to the office.", "For a past action, use 'went', not 'go'. Say 'to the office'.", "बीते हुए काम के लिए go की जगह went कहें। दफ़्तर के लिए to the office कहें।"),
    Lesson("At the café", "कैफ़े में", "☕", "Ask politely for a cup of tea.", "विनम्रता से एक कप चाय माँगिए।", "Could I have a cup of tea, please?", "Use 'Could I have' to ask politely.", "विनम्रता से माँगने के लिए Could I have कहें।"),
    Lesson("Every day", "हर दिन", "📚", "Tell me what you do every day.", "बताइए, आप हर दिन क्या करते हैं?", "I read a book every day.", "Use the present tense for a daily habit.", "रोज़ की आदत के लिए वर्तमान काल का प्रयोग करें।"),
    Lesson("An interview", "नौकरी का साक्षात्कार", "💼", "Tell me about your experience.", "अपने काम के अनुभव के बारे में बताइए।", "I have two years of experience.", "Say 'two years of experience'.", "दो साल के अनुभव के लिए two years of experience कहें।"),
    Lesson("Ask for help", "मदद माँगें", "🤝", "Ask someone to speak more slowly.", "किसी से धीरे बोलने के लिए कहिए।", "Could you speak slowly, please?", "Use 'slowly' to describe how someone speaks.", "कैसे बोलना है, यह बताने के लिए slowly कहें।")
)
data class Meaning(val hindi: String, val english: String)
val dictionary = mapOf(
    "my" to Meaning("मेरा या मेरी", "Belonging to me."), "name" to Meaning("नाम", "What a person is called."),
    "is" to Meaning("है", "Used to say what someone or something is."), "asha" to Meaning("आशा, एक व्यक्ति का नाम", "A person's name."),
    "yesterday" to Meaning("बीता हुआ कल", "The day before today."), "i" to Meaning("मैं", "The person who is speaking."),
    "went" to Meaning("गया या गई", "The past form of go."), "go" to Meaning("जाना", "Move to a place."),
    "to" to Meaning("की ओर या तक; यहाँ दफ़्तर जाना", "Shows the destination here."), "the" to Meaning("किसी खास चीज़ के लिए; यहाँ वह दफ़्तर", "Points to a particular thing."),
    "office" to Meaning("दफ़्तर", "A place where people work."), "could" to Meaning("विनम्र अनुरोध करने का शब्द", "Used here to make a polite request."),
    "have" to Meaning("यहाँ: लेना; दूसरे वाक्य में: पास होना", "Here: receive something, or possess it."),
    "a" to Meaning("एक", "One; used before a singular noun."), "cup" to Meaning("कप या प्याला", "A small container for a drink."),
    "of" to Meaning("का, की या के; शब्दों को जोड़ता है", "Connects related things: a cup of tea."), "tea" to Meaning("चाय", "A drink made using tea leaves."),
    "please" to Meaning("कृपया", "Makes a request polite."), "read" to Meaning("पढ़ना", "Look at words and understand them."),
    "book" to Meaning("किताब", "Pages of writing bound together."), "every" to Meaning("हर", "Each one."), "day" to Meaning("दिन", "A period of 24 hours."),
    "two" to Meaning("दो", "The number 2."), "years" to Meaning("साल", "Periods of twelve months."), "experience" to Meaning("अनुभव", "Knowledge gained by doing something."),
    "you" to Meaning("आप या तुम", "The person being spoken to."), "speak" to Meaning("बोलना", "Say words aloud."), "slowly" to Meaning("धीरे", "At a gentle speed."),
    "every day" to Meaning("हर दिन", "Daily."), "could i have" to Meaning("क्या मुझे मिल सकता है?", "A polite way to ask for something."),
    "my name is" to Meaning("मेरा नाम है", "A way to introduce yourself."), "years of experience" to Meaning("सालों का अनुभव", "How long someone has done this work.")
)
fun words(text: String) = Regex("[A-Za-z]+(?:'[A-Za-z]+)?").findAll(text).map { it.value }.toList()
fun normalized(text: String) = words(text).joinToString(" ").lowercase(Locale.ROOT)

private fun practice(title: String, picture: String, prompt: String, example: String, hint: String) =
    Lesson(title, "", picture, prompt, "", example, hint, "")
private val intermediateLessons = listOf(
    practice("Explain a preference", "🚆", "Which do you prefer: a bus or a train? Give a reason.", "I prefer the train because it is more comfortable.", "Connect your preference and reason with because."),
    practice("A past experience", "🧳", "Describe a problem you had while travelling.", "I missed my train, so I had to wait for the next one.", "Use the past tense and explain what happened next."),
    practice("Make a plan", "📅", "Tell me what you will do if it rains tomorrow.", "If it rains tomorrow, I will stay at home.", "Use if for the condition and will for your plan."),
    practice("Compare choices", "🏙", "Compare living in a city with living in a village.", "A city is busier than a village, but it offers more jobs.", "Use than to compare and but to show a difference."),
    practice("Clarify a request", "💬", "Ask a colleague to explain a task more clearly.", "Could you explain which part of the report I should finish first?", "Ask a clear, polite question."),
    practice("Describe progress", "🌱", "Tell me about something you have learned recently.", "I have learned to speak more clearly by practising every day.", "Say what you learned and how you learned it.")
)
private val advancedLessons = listOf(
    practice("Weigh a trade-off", "⚖", "Explain one benefit and one drawback of working from home.", "Although working from home saves travel time, it can make collaboration harder.", "Balance two ideas using although."),
    practice("Disagree tactfully", "🤝", "Politely disagree with an unrealistic deadline and suggest a solution.", "I understand the urgency, but extending the deadline would allow us to check the results properly.", "Acknowledge the concern before offering an alternative."),
    practice("Reflect on a decision", "🧭", "Explain how you would change a past decision.", "If I had asked for feedback earlier, I would have avoided that mistake.", "Describe the imagined past condition and its result."),
    practice("Express uncertainty", "🔎", "Explain a possible reason for a delay without claiming certainty.", "The delay may have been caused by a missing document, although we still need to confirm that.", "Use may and explain what is still uncertain."),
    practice("Recommend an approach", "💡", "Recommend testing a new idea on a small scale first.", "I suggest starting with a small trial so that we can identify problems before expanding.", "Support your recommendation with a clear reason."),
    practice("Summarise a discussion", "📝", "Summarise a discussion where people agreed on a goal but not the timing.", "We agreed on the overall goal, but the schedule remains undecided because the estimates differ.", "Separate what was agreed from what remains open.")
)
fun lessonsForLevel(level: String): List<Lesson> = when(level) {
    "intermediate" -> intermediateLessons
    "advanced" -> advancedLessons
    else -> lessons
}
