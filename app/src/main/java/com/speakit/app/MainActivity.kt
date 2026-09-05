package com.speakit.app

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Locale

class MainActivity : ComponentActivity(), RecognitionListener {
    private lateinit var auth: EmailAuth
    private var session by mutableStateOf<EmailSession?>(null)
    private val prefs get() = getSharedPreferences("speakit_user_${session?.userId ?: "signedout"}", MODE_PRIVATE)
    private var authBusy by mutableStateOf(false)
    private var authNotice by mutableStateOf("")
    private lateinit var coach: LocalCoach
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var nativeLanguage by mutableStateOf("hi")
    private var englishLocale by mutableStateOf("en-GB")
    private var level by mutableStateOf("beginner")
    private var registrationStep by mutableIntStateOf(0)
    private val currentLessons get() = lessonsForLevel(level)
    private val englishName get() = if(englishLocale == "en-GB") "UK English" else "US English"
    private var meaningLanguage by mutableStateOf("en")
    private var meaningContext by mutableStateOf("")
    private var meaningValid by mutableStateOf(false)
    private var meaningRequest by mutableIntStateOf(0)
    private var lastAutoRequest = -1
    private var meaningAudioStatus by mutableStateOf("")
    private var registered by mutableStateOf(false)
    private var displayName by mutableStateOf("Learner")
    private var avatar by mutableStateOf("😊")
    private var voiceSpeed by mutableFloatStateOf(0.9f)
    private var textScale by mutableFloatStateOf(1f)
    private var englishVoice by mutableStateOf("")
    private var hindiVoice by mutableStateOf("")
    private var page by mutableStateOf("Learn")
    private var lessonIndex by mutableIntStateOf(0)
    private var busy by mutableStateOf(false)
    private var listening by mutableStateOf(false)
    private var modelReady by mutableStateOf(false)
    private var modelDownloaded by mutableStateOf(false)
    private var voiceReady by mutableStateOf(false)
    private var message by mutableStateOf("")
    private var transcript by mutableStateOf("")
    private var feedback by mutableStateOf("")
    private var retryTarget by mutableStateOf("")
    private var retryMode by mutableStateOf(false)
    private var selectedWord by mutableStateOf("")
    private var selectedSlot by mutableStateOf("")
    private var definition by mutableStateOf("")
    private var englishDefinition by mutableStateOf("")
    private var exerciseDone by mutableStateOf(false)
    private var showExample by mutableStateOf(false)
    private var showTranscript by mutableStateOf(false)
    private var saved by mutableStateOf(setOf<String>())
    private var completed by mutableStateOf(setOf<String>())
    private var attempts by mutableIntStateOf(0)
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else message = "Microphone permission is needed to practise."
    }
    private val modelPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch {
            busy = true; message = "Checking model file…"
            try { coach.importModel(uri); modelDownloaded = true; loadCoach() }
            catch (e: Exception) { message = e.message ?: "Import failed" }
            finally { busy = false }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = EmailAuth(applicationContext)
        session = auth.restore()
        loadProfile()
        coach = LocalCoach(applicationContext)
        modelDownloaded = coach.downloaded
        tts = TextToSpeech(this) { status -> voiceReady = status == TextToSpeech.SUCCESS }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { runOnUiThread {
                if(id == "meaning_$meaningRequest") meaningAudioStatus = "Playing ${if(meaningLanguage == "hi") "Hindi" else "English"}…"
            } }
            override fun onDone(id: String?) { runOnUiThread {
                if(id == "meaning_$meaningRequest") meaningAudioStatus = ""
            } }
            @Deprecated("Legacy TTS callback")
            override fun onError(id: String?) { runOnUiThread {
                if(id == "meaning_$meaningRequest") meaningAudioStatus = "Voice playback failed. Check your offline voice in Setup."
            } }
        })
        setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity.density, deviceDensity.fontScale * textScale)) {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF156E5C), secondary = Color(0xFFCF6B38), background = Color(0xFFF6F3FF))) { App() }
            }
        }
        if (modelDownloaded) lifecycleScope.launch { busy = true; try { loadCoach() } finally { busy = false } }
    }
    private fun loadProfile() {
        displayName = prefs.getString("displayName", "Learner") ?: "Learner"
        avatar = prefs.getString("avatar", "😊") ?: "😊"
        voiceSpeed = prefs.getFloat("voiceSpeed", 0.9f)
        textScale = prefs.getFloat("textScale", 1f)
        englishVoice = prefs.getString("englishVoice", "") ?: ""
        hindiVoice = prefs.getString("hindiVoice", "") ?: ""
        nativeLanguage = "hi"
        englishLocale = prefs.getString("englishLocale", "en-GB") ?: "en-GB"
        level = prefs.getString("level", "beginner") ?: "beginner"
        registered = session != null && prefs.getBoolean("registered", false) && prefs.getBoolean("onboardingV2", false)
        saved = prefs.getStringSet("words", emptySet())?.toSet() ?: emptySet()
        completed = prefs.getStringSet("completed", emptySet())?.map { if(it.all(Char::isDigit)) "beginner:$it" else it }?.toSet() ?: emptySet()
        attempts = prefs.getInt("attempts", 0)
        registrationStep = if(prefs.getBoolean("accountSetup", false)) 0 else -1
        page = "Learn"
    }
    private suspend fun loadCoach() {
        message = "Loading Qwen on this phone…"
        try { coach.load(); modelReady = true; message = "Local AI loaded. Test your voices next." }
        catch (e: Exception) { message = "Model could not load: ${e.message}" }
        catch (e: LinkageError) { message = "AI engine could not start on this device: ${e.message}" }
    }
    private fun offlineSpeechAvailable() = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
    private fun speak(text: String, lang: String = "en", slow: Boolean = false, utteranceId: String = "lesson"): Boolean {
        if (listening) return false
        val engine = tts ?: return false
        if (!voiceReady) { message = "Voice engine is starting. Try again."; return false }
        val availableVoices = engine.voices?.filter { it.locale.language == lang && !it.isNetworkConnectionRequired && (lang != "en" || it.locale.country == (if(englishLocale == "en-GB") "GB" else "US")) }
            ?.sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }.thenBy { it.latency }).orEmpty()
        val voice = availableVoices.firstOrNull { it.name == (if(lang == "en") englishVoice else hindiVoice) } ?: availableVoices.firstOrNull()
        if (voice == null) { message = "Download an offline ${if(lang == "hi") "Hindi" else englishName} voice in your phone's text-to-speech settings."; return false }
        engine.setLanguage(voice.locale)
        if(engine.setVoice(voice) == TextToSpeech.ERROR) {
            message = "Could not activate the offline voice. Check voice settings."
            return false
        }
        engine.setSpeechRate(if (slow) 0.65f else voiceSpeed)
        val accepted = engine.speak(text.take(1500), TextToSpeech.QUEUE_FLUSH, null, utteranceId) != TextToSpeech.ERROR
        if(!accepted) message = "Voice playback failed. Check voice settings."
        return accepted
    }
    private fun speechIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, englishLocale)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    private fun startListening() {
        if (busy || listening) return
        if (!modelReady && !retryMode) { page = "Setup"; message = "Set up the local AI before starting a conversation."; return }
        if (!offlineSpeechAvailable()) { message = "On-device speech recognition is unavailable. This prototype needs Android 12+ with an on-device speech service. No cloud fallback is used."; page = "Setup"; return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { permission.launch(Manifest.permission.RECORD_AUDIO); return }
        selectedSlot = ""
        exerciseDone = false
        tts?.stop()
        recognizer?.destroy()
        try {
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this).also { it.setRecognitionListener(this) }
            listening = true; transcript = ""; feedback = ""; message = "Listening… speak one short sentence."
            recognizer?.startListening(speechIntent())
        } catch (e: Exception) { listening = false; message = "Could not start microphone: ${e.message}" }
    }
    private fun evaluate(spoken: String) {
        transcript = spoken
        if (spoken.isBlank()) { message = "I did not hear a sentence. Please try again."; return }
        attempts++; prefs.edit().putInt("attempts", attempts).apply()
        if (retryMode) {
            if (normalized(spoken) == normalized(retryTarget)) {
                exerciseDone = true
                feedback = "The recognised words match. Now try the sentence in a conversation."
                completed = completed + "$level:$lessonIndex"
                prefs.edit().putStringSet("completed", completed).apply()
            } else {
                feedback = "I heard different words. Listen to the example and try again. This may also be a listening error."
            }
            message = "Word matching is not a pronunciation score."
            speak(feedback)
            return
        }
        val lesson = currentLessons[lessonIndex]
        busy = true; message = "Thinking on your phone…"
        lifecycleScope.launch {
            try {
                val result = coach.ask("Fix this spoken English sentence. Keep the speaker's meaning and name. Use $englishName. The practice level is $level. Do not change a correct sentence. Use everyday words, not grammar terms. Return two lines, for example:\nCorrect: Yesterday I went to the office.\nTip: Use went when talking about yesterday.\nQuestion: ${lesson.prompt}\nSentence to fix: ${spoken.take(500)}")
                var corrected = Correction.sentence(result, spoken)
                if (corrected == null) {
                    message = "Checking the correction…"
                    val second = coach.ask("Fix only the English in the sentence below. Keep its meaning and name. Return ONLY the fixed English sentence on one line. No explanation, labels or quotation marks.\nSentence: ${spoken.take(500)}")
                    corrected = Correction.sentence(second, spoken, allowPlain = true)
                }
                if (corrected == null) {
                    feedback = "I couldn't check this sentence reliably. Please try a shorter sentence."
                    retryTarget = ""
                } else {
                    retryTarget = corrected
                    feedback = Correction.tip(result, spoken, corrected)
                    speak(corrected)
                }
                message = "Correction from local Qwen. AI can make mistakes."
            } catch (e: Exception) { feedback = ""; message = "Correction failed: ${e.message}" }
            finally { busy = false }
        }
    }
    private fun showMeaning(word: String, context: String, slot: String) {
        val cardWord = word.lowercase(Locale.ROOT).trim()
        saved = saved + cardWord
        prefs.edit().putStringSet("words", saved).putString("context_$cardWord", context).apply()
        meaningRequest++
        meaningAudioStatus = "Preparing audio…"
        selectedSlot = slot
        selectedWord = cardWord
        meaningLanguage = nativeLanguage
        meaningContext = context
        definition = ""; englishDefinition = ""; meaningValid = false
        val entry = dictionary[word.lowercase(Locale.ROOT)]
        if (entry != null) {
            englishDefinition = entry.english
            definition = if(meaningLanguage == "hi") entry.hindi else entry.english
            meaningValid = true
            cacheCardMeaning()
            return
        }
        if (!modelReady) { definition = "Set up the AI for words outside the lesson dictionary."; englishDefinition = definition; return }
        busy = true
        lifecycleScope.launch {
            try {
                englishDefinition = coach.ask("Explain the word or phrase '${word.take(80)}' as used in '${context.take(350)}' in simple English. Give one short meaning only. If it is a name, say so.")
                definition = if(meaningLanguage == "en") englishDefinition else coach.ask("Explain the word or phrase '${word.take(80)}' as used in '${context.take(350)}' in ${if(meaningLanguage == "hi") "Hindi" else "simple English"}. One brief meaning only. If it is a name, say so.")
                meaningValid = englishDefinition.isNotBlank() && definition.isNotBlank()
                if(meaningValid) cacheCardMeaning()
                } catch (e: Exception) { definition = "Meaning unavailable. Please try again."; if(englishDefinition.isBlank()) englishDefinition = definition }
            finally { busy = false }
        }
    }
    private fun cacheCardMeaning() {
        prefs.edit().putString("meaning_en_$selectedWord", englishDefinition)
            .putString("meaning_${meaningLanguage}_$selectedWord", definition).apply()
    }
    @Composable private fun App() {
        LaunchedEffect(meaningRequest, selectedSlot, meaningValid, voiceReady, listening) {
            if(selectedSlot.isNotBlank() && meaningValid && voiceReady && !listening && lastAutoRequest != meaningRequest) {
                val request = meaningRequest
                delay(250) // Let the inline meaning appear and the voice service settle.
                if(request == meaningRequest && selectedSlot.isNotBlank()) {
                    lastAutoRequest = request
                    var accepted = speak(definition, meaningLanguage, utteranceId = "meaning_$request")
                    if(!accepted) {
                        delay(450)
                        accepted = speak(definition, meaningLanguage, utteranceId = "meaning_$request")
                    }
                    if(!accepted) meaningAudioStatus = message
                    else {
                        delay(1500)
                        if(request == meaningRequest && selectedSlot.isNotBlank() && meaningAudioStatus == "Preparing audio…") {
                            meaningAudioStatus = "Starting audio…"
                            if(!speak(definition, meaningLanguage, utteranceId = "meaning_$request")) meaningAudioStatus = message
                        }
                    }
                }
            }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Speak It ✦", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7153D6), modifier = Modifier.weight(1f))
                    if(registered && session != null) {
                        IconButton(enabled = !busy && !listening, modifier = Modifier.semantics { contentDescription = "Profile" },
                            onClick = { page = "Profile"; selectedSlot = ""; tts?.stop() }) { Text(avatar, fontSize = 25.sp) }
                        IconButton(enabled = !busy && !listening, modifier = Modifier.semantics { contentDescription = "Settings" },
                            onClick = { page = "Setup"; selectedSlot = ""; tts?.stop() }) { Text("⚙", fontSize = 27.sp, color = Color(0xFF7153D6)) }
                    }
                }
                if(session == null) { LoginScreen() } else if (!registered) {
                    if(registrationStep < 0) AccountSetup() else if(registrationStep == 0) Welcome() else LevelScreen()
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Learn" to "Learn", "Words" to "Review").forEach { (key, label) ->
                            TextButton(onClick = { page = key; selectedSlot = ""; tts?.stop() }, enabled = !busy && !listening) { Text(label, color = when(key) { "Learn" -> Color(0xFF7153D6); "Words" -> Color(0xFFC45624); else -> Color(0xFF007C91) }, fontWeight = if(page == key) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (message.isNotBlank() && (page == "Setup" || Regex("(?i)fail|couldn.t|unavailable|permission|download an|open phone|could not").containsMatchIn(message))) Text(message, fontSize = 13.sp, color = Color(0xFF67503D), modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEEDC), RoundedCornerShape(12.dp)).padding(12.dp))
                        if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("One moment…", fontSize = 12.sp) }
                        when(page) { "Profile" -> Profile(); "Setup" -> Setup(); "Words" -> ReviewCards(); "Done" -> Finished(); else -> LessonScreen() }
                        Spacer(Modifier.height(12.dp))
                    }
                    if(page == "Learn") ExerciseFooter()
                }
            }
        }
    }

    @Composable private fun Welcome() {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SpeechBuddy(Color(0xFF7153D6), false, false)
            Text("Make it yours", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Choose how you want to learn.")
            Text("English", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            EnglishButtons()
            Text("Native language", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            LanguageButtons()
            Text("Teaching stays in English. Tap a word for English and Hindi meanings.", fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { registrationStep = 1 }, modifier = Modifier.height(54.dp)) { Text("Next →") }
            }
        }
    }
    @Composable private fun EnglishButtons() {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("en-GB" to "UK English", "en-US" to "US English").forEach { (code, label) ->
                FilterChip(selected = englishLocale == code, enabled = !busy && !listening,
                    onClick = { englishLocale = code; englishVoice = ""; prefs.edit().putString("englishVoice", "").apply(); tts?.stop(); selectedSlot = ""; prefs.edit().putString("englishLocale", code).apply() }, label = { Text(label) })
            }
        }
    }
    @Composable private fun LanguageButtons() {
        FilterChip(selected = true, onClick = { nativeLanguage = "hi"; prefs.edit().putString("nativeLanguage", "hi").apply() }, label = { Text("Hindi · हिन्दी") })
    }
    @Composable private fun LevelScreen() {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Choose your level", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Pick a comfortable starting point. You can change it later.")
            listOf(
                Triple("beginner", "🌱 Beginner", "Short sentences and everyday words"),
                Triple("intermediate", "🌿 Intermediate", "Explain experiences, reasons and plans"),
                Triple("advanced", "🌳 Advanced", "Discuss ideas, trade-offs and opinions")
            ).forEachIndexed { index, (code, title, description) ->
                OutlinedButton(onClick = { level = code }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if(level == code) lessonColors[index].copy(alpha = .13f) else Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title + if(level == code) "  ✓" else "", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text(description, fontSize = 14.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { registrationStep = 0 }) { Text("‹ Back") }
                Button(onClick = {
                    nativeLanguage = "hi"
                    prefs.edit().putBoolean("registered", true).putBoolean("onboardingV2", true)
                        .putString("englishLocale", englishLocale).putString("nativeLanguage", "hi").putString("level", level).apply()
                    goToLesson(0); registered = true; page = "Setup"
                }, modifier = Modifier.height(54.dp)) { Text("Let's begin →") }
            }
        }
    }
    @Composable private fun LoginScreen() {
        var email by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var sent by remember { mutableStateOf(false) }
        var cooldown by remember { mutableIntStateOf(0) }
        LaunchedEffect(cooldown) { if(cooldown > 0) { delay(1000); cooldown-- } }
        fun send() {
            authBusy = true; authNotice = ""
            lifecycleScope.launch {
                try { auth.sendCode(email.trim()); sent = true; cooldown = 60; authNotice = "Code sent. Check your inbox and spam folder." }
                catch(e: Exception) { authNotice = e.message ?: "Could not send code. Check your internet connection." }
                finally { authBusy = false }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SpeechBuddy(Color(0xFF7153D6), false, false)
            Text("Welcome to Speak It", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Sign in or create an account with an email code.")
            OutlinedTextField(value = email, onValueChange = { email = it.take(254) }, enabled = !sent && !authBusy,
                label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            if(sent) {
                OutlinedTextField(value = code, onValueChange = { code = it.filter(Char::isDigit).take(10) }, enabled = !authBusy,
                    label = { Text("Verification code") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                Button(enabled = !authBusy && code.length >= 6, onClick = {
                    authBusy = true; authNotice = ""
                    lifecycleScope.launch {
                        try { session = auth.verify(email.trim(), code); code = ""; loadProfile(); authNotice = "" }
                        catch(e: Exception) { authNotice = e.message ?: "Could not verify. Check your connection and code." }
                        finally { authBusy = false }
                    }
                }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Verify and continue →") }
                TextButton(enabled = !authBusy && cooldown == 0, onClick = { send() }) { Text(if(cooldown > 0) "Resend in ${cooldown}s" else "Resend code") }
                TextButton(enabled = !authBusy, onClick = { sent = false; code = ""; authNotice = "" }) { Text("Change email") }
            } else {
                Button(enabled = !authBusy && cooldown == 0 && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches(), onClick = { send() }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Send verification code") }
            }
            if(authBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(authNotice.isNotBlank()) Text(authNotice, fontSize = 14.sp)
            Text("Internet is needed to sign in. After setup, practise offline on this phone. Email sign-in is available now; SMS is not included.", fontSize = 13.sp)
        }
    }
    @Composable private fun AccountSetup() {
        var name by remember { mutableStateOf("") }
        var importEarlier by remember { mutableStateOf(false) }
        val legacy = getSharedPreferences("speakit", MODE_PRIVATE)
        val migration = getSharedPreferences("speakit_migration", MODE_PRIVATE)
        val canImport = legacy.contains("registered") && !migration.contains("owner")
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Email verified: ${session?.email.orEmpty()}")
            OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row {
                listOf("😊", "🦊", "🐼", "🚀").forEach { choice -> FilterChip(selected = avatar == choice, onClick = { avatar = choice }, label = { Text(choice) }) }
            }
            if(canImport) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = importEarlier, onCheckedChange = { importEarlier = it })
                Text("Bring my earlier practice on this phone into this account", modifier = Modifier.weight(1f))
            }
            Button(enabled = name.isNotBlank(), onClick = {
                if(importEarlier && canImport) {
                    val editor = prefs.edit()
                    legacy.all.forEach { (key, value) ->
                        if(key == "words" || key == "completed" || key == "attempts" || key.startsWith("meaning_") || key.startsWith("context_")) {
                            when(value) {
                                is String -> editor.putString(key, value)
                                is Int -> editor.putInt(key, value)
                                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                            }
                        }
                    }
                    editor.commit(); migration.edit().putString("owner", session?.userId).commit()
                }
                prefs.edit().putString("displayName", name.trim()).putString("avatar", avatar).putBoolean("accountSetup", true).apply()
                loadProfile(); registrationStep = 0
            }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Next →") }
        }
    }
    @Composable private fun Profile() {
        var nameDraft by remember(displayName) { mutableStateOf(displayName) }
        Text("Your profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(session?.email.orEmpty(), fontSize = 14.sp)
        CardBlock {
            Text(avatar, fontSize = 52.sp)
            OutlinedTextField(value = nameDraft, onValueChange = { nameDraft = it.take(40) }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = nameDraft.isNotBlank(), onClick = {
                displayName = nameDraft.trim(); prefs.edit().putString("displayName", displayName).apply()
            }) { Text("Save name") }
            Text("Choose your avatar", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("😊", "🦊", "🐼", "🚀").forEach { choice ->
                    FilterChip(selected = avatar == choice, onClick = { avatar = choice; prefs.edit().putString("avatar", choice).apply() }, label = { Text(choice, fontSize = 23.sp) })
                }
            }
        }
        CardBlock {
            Text("Your learning", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("$englishName  ·  Hindi meanings")
            Text("Level: ${level.replaceFirstChar { it.uppercase() }}")
            Text("Your progress", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            val levels = listOf("beginner", "intermediate", "advanced")
            val totalExercises = levels.sumOf { lessonsForLevel(it).size }
            val finishedExercises = levels.sumOf { code -> lessonsForLevel(code).indices.count { "$code:$it" in completed } }
            Text("$finishedExercises of $totalExercises exercises completed")
            val overallProgress by animateFloatAsState(targetValue = finishedExercises.toFloat() / totalExercises.coerceAtLeast(1), label = "overall progress")
            LinearProgressIndicator(progress = { overallProgress }, modifier = Modifier.fillMaxWidth().height(10.dp), color = Color(0xFF7153D6))
            levels.forEachIndexed { index, code ->
                val total = lessonsForLevel(code).size
                val done = lessonsForLevel(code).indices.count { "$code:$it" in completed }
                Text("${code.replaceFirstChar { it.uppercase() }} · $done / $total", fontSize = 14.sp)
                LinearProgressIndicator(progress = { done.toFloat() / total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth(), color = lessonColors[index])
            }
            Text("$attempts speaking attempts", fontWeight = FontWeight.Bold)
            val reviewCount = saved.sumOf { prefs.getInt("reviews_$it", 0).toLong() }
            val dueCount = saved.count { prefs.getLong("due_$it", 0) <= System.currentTimeMillis() }
            val reviewedCards = saved.count { prefs.getInt("reviews_$it", 0) > 0 }
            Text("Flashcards", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${saved.size} cards · $dueCount due now")
            Text("$reviewedCards cards reviewed · $reviewCount total reviews")
            Text("Exercise completion counts matching words in a repeat. It is not a fluency or pronunciation score.", fontSize = 12.sp)
            Row {
                TextButton(onClick = { page = "Words" }) { Text("Review →") }
                TextButton(onClick = { page = "Setup" }) { Text("Learning settings →") }
            }
        }
        CardBlock {
            Text("About your profile", fontWeight = FontWeight.Bold)
            Text("Your email is verified by Supabase. Your practice and saved words stay on this phone, separately for each account. They are not synced to other phones.", fontSize = 14.sp)
            Text("Speak It 1.10 · local Qwen AI", fontSize = 12.sp)
        }
        OutlinedButton(enabled = !busy && !listening, modifier = Modifier.fillMaxWidth(), onClick = {
            selectedSlot = ""; selectedWord = ""; tts?.stop(); recognizer?.cancel()
            goToLesson(0)
            val previous = session
            auth.clear(); session = null; loadProfile(); authBusy = true
            lifecycleScope.launch {
                val revoked = previous?.let { auth.logout(it) } ?: true
                authNotice = if(revoked) "Logged out. Enter your email to sign in again." else "Logged out on this phone. Server sign-out could not be confirmed; check your connection."
                authBusy = false
            }
        }) { Text("Log out") }
    }
    @Composable private fun VoiceAndTextSettings() {
        CardBlock {
            Text("Voice speed", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.7f to "Slow", 0.9f to "Normal", 1.1f to "Fast").forEach { (value, label) ->
                    FilterChip(selected = voiceSpeed == value, onClick = { voiceSpeed = value; prefs.edit().putFloat("voiceSpeed", value).apply() }, label = { Text(label) })
                }
            }
            Text("Text size", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1f to "Standard", 1.15f to "Large").forEach { (value, label) ->
                    FilterChip(selected = textScale == value, onClick = { textScale = value; prefs.edit().putFloat("textScale", value).apply() }, label = { Text(label) })
                }
            }
            Text("English voice", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val voices = if(voiceReady) tts?.voices?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired && it.locale.country == (if(englishLocale == "en-GB") "GB" else "US") }?.sortedByDescending { it.quality }.orEmpty() else emptyList()
            if(voices.isEmpty()) Text("Install an offline $englishName voice below.", fontSize = 13.sp)
            voices.take(4).forEachIndexed { index, voice ->
                FilterChip(selected = englishVoice == voice.name || (englishVoice.isBlank() && index == 0),
                    onClick = { englishVoice = voice.name; prefs.edit().putString("englishVoice", voice.name).apply(); speak("Hello. This is your English practice voice.") },
                    label = { Text("$englishName · Voice ${index + 1}") })
            }
            Text("Voice quality", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Automatic uses the highest-quality installed offline voice. Preview voices to choose the one that sounds best to you.", fontSize = 13.sp)
            TextButton(onClick = { englishVoice = ""; hindiVoice = ""; prefs.edit().remove("englishVoice").remove("hindiVoice").apply(); speak("Hello! Let's practise a little English together.") }) { Text("Use best available voices") }
            Text("Hindi voice", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val hindiVoices = if(voiceReady) tts?.voices?.filter { it.locale.language == "hi" && !it.isNetworkConnectionRequired }?.sortedByDescending { it.quality }.orEmpty() else emptyList()
            hindiVoices.take(4).forEachIndexed { index, voice ->
                FilterChip(selected = hindiVoice == voice.name || (hindiVoice.isBlank() && index == 0), onClick = {
                    hindiVoice = voice.name; prefs.edit().putString("hindiVoice", voice.name).apply()
                    speak("नमस्ते। आइए मिलकर सीखें।", "hi")
                }, label = { Text("Hindi · Voice ${index + 1}") })
            }
            Text("For a more natural sound, install a higher-quality offline voice through Open voice settings below. Voice quality depends on the phone's speech engine.", fontSize = 13.sp)
            TextButton(onClick = { speak("Listen and repeat at your own pace.") }) { Text("▶ Test voice") }
        }
    }
    @Composable private fun Setup() {
        Text("Settings · 1.10", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        VoiceAndTextSettings()
        Text("English voice and listening"); EnglishButtons()
        Text("Native language"); LanguageButtons()
        Text("Level: ${level.replaceFirstChar { it.uppercase() }}")
        TextButton(enabled = !busy && !listening, onClick = { registered = false; registrationStep = 1 }) { Text("Change level") }
        Text("Changing UK/US English may need a matching offline voice and listening download.", fontSize = 12.sp)
        Text("All teaching stays in English. Hindi meanings are available on request; more native languages are planned.", fontSize = 12.sp)
        CardBlock {
            Text("1  ·  Local AI", fontWeight = FontWeight.Bold)
            Text(if(modelReady) "Qwen is loaded on this phone." else "Download once: Qwen2.5 0.5B · approximately 491 MB. Keep this screen open during download.")
            Text("Apache-2.0 model weights · llama.cpp engine · no API key", fontSize = 12.sp)
            Button(enabled = !busy && !modelReady, onClick = {
                lifecycleScope.launch {
                    busy = true
                    try {
                        if (!coach.downloaded) coach.download { pct -> runOnUiThread { message = if(pct >= 0) "Downloading AI: $pct%" else "Downloading AI: ${-pct} MB" } }
                        modelDownloaded = true; loadCoach()
                    } catch (e: Exception) { message = "Download failed: ${e.message}. You can import the downloaded model instead." }
                    finally { busy = false }
                }
            }) { Text(if(modelReady) "AI loaded" else if(modelDownloaded) "Load AI" else "Download AI") }
            TextButton(enabled = !busy && !modelReady, onClick = { modelPicker.launch(arrayOf("*/*")) }) { Text("Or import the exact Qwen GGUF file") }
        }
        CardBlock {
            Text("2  ·  Offline listening", fontWeight = FontWeight.Bold)
            Text(if(offlineSpeechAvailable()) "On-device speech service detected. English still needs an airplane-mode test." else "No on-device speech service detected. Android 12+ with a supported service is needed for this prototype.")
            Button(enabled = !busy, onClick = {
                if (Build.VERSION.SDK_INT >= 33 && offlineSpeechAvailable()) {
                    try { recognizer?.destroy(); recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this@MainActivity); recognizer?.triggerModelDownload(speechIntent()); message = "${englishName} speech download requested. Your phone manages it; test listening after it completes." }
                    catch(e: Exception) { message = "Open your phone's speech settings to download English. ${e.message ?: ""}" }
                } else message = "Download English in your phone's offline speech settings."
            }) { Text("Prepare $englishName listening") }
        }
        CardBlock {
            Text("3  ·  Offline voices", fontWeight = FontWeight.Bold)
            Text("Download English and Hindi voice data using your phone's text-to-speech settings. Only voices marked as offline are used.")
            Row { TextButton(onClick = { speak("Hello. Let us practise English.") }) { Text("Test $englishName") }; TextButton(onClick = { speak("नमस्ते। आइए अंग्रेज़ी का अभ्यास करें।", "hi") }) { Text("हिन्दी सुनें") } }
            TextButton(onClick = { try { startActivity(Intent("com.android.settings.TTS_SETTINGS")) } catch(e: Exception) { message = "Open phone Settings and search for Text-to-speech." } }) { Text("Open voice settings") }
        }
        Text("Final check: enable airplane mode, turn Wi-Fi off, and complete a spoken correction and retry. Setup checks alone do not prove offline readiness.", fontSize = 13.sp)
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = { page = "Learn"; message = "" }) { Text("Open lessons") }
    }
    private fun goToLesson(index: Int) {
        tts?.stop(); selectedSlot = ""; lessonIndex = index
        retryMode = false; retryTarget = ""; transcript = ""; feedback = ""; message = ""
        exerciseDone = false; showExample = false; showTranscript = false
    }
    @Composable private fun ExerciseFooter() {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = lessonIndex > 0 && !busy && !listening, onClick = { goToLesson(lessonIndex - 1) }) { Text("‹ Back") }
            Button(enabled = exerciseDone && !busy && !listening,
                modifier = Modifier.height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7153D6)),
                onClick = { if(lessonIndex == currentLessons.lastIndex) { page = "Done"; tts?.stop() } else goToLesson(lessonIndex + 1) }) {
                Text(if(lessonIndex == currentLessons.lastIndex) "Finish ✓" else "Next →", fontSize = 18.sp)
            }
        }
    }
    @Composable private fun Finished() {
        CardBlock {
            SpeechBuddy(Color(0xFF7153D6), false, true)
            Text("Practice complete", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("You finished this speaking session. Your saved words are ready to revisit.")
            Button(onClick = { page = "Words" }) { Text("Review my words") }
            TextButton(onClick = { goToLesson(0); page = "Learn" }) { Text("Practise again") }
        }
    }
    @Composable private fun LessonScreen() {
        val lesson = currentLessons[lessonIndex]
        val accent = lessonColors[lessonIndex % lessonColors.size]
        val progress by animateFloatAsState(targetValue = (lessonIndex + if(exerciseDone) 1 else 0) / currentLessons.size.toFloat(), label = "lesson progress")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${level.replaceFirstChar { it.uppercase() }} · ${lessonIndex + 1}/${currentLessons.size}", fontSize = 13.sp, color = Color(0xFF56675B))
            Text(lesson.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp), color = accent, trackColor = accent.copy(alpha = 0.12f))
        CardBlock {
            Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(accent.copy(alpha = .12f), Color(0xFFFFF2CC))), RoundedCornerShape(20.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpeechBuddy(accent, listening, exerciseDone)
                Text("${lesson.picture} ${lesson.prompt}", fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            if(retryTarget.isBlank()) {
                TextButton(enabled = !listening && !busy, onClick = { speak(lesson.prompt) }) { Text("▶ Hear question") }
            } else {
                Text(if(exerciseDone) "Sentence practised" else "Listen, then repeat", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                WordButtons(retryTarget, "correction")
                Row {
                    TextButton(enabled = !listening && !busy, onClick = { speak(retryTarget) }) { Text("▶ Listen") }
                    TextButton(enabled = !listening && !busy, onClick = { speak(retryTarget, slow = true) }) { Text("▶ Slowly") }
                }
            }
            if(feedback.isNotBlank() && !exerciseDone) {
                Text(feedback, fontSize = 15.sp)
                TextButton(enabled = !listening && !busy, onClick = { speak(feedback) }) { Text("▶ Hear feedback") }
            }
            if(transcript.isNotBlank()) {
                TextButton(onClick = { showTranscript = !showTranscript }, enabled = !listening && !busy) { Text(if(showTranscript) "Hide what I heard" else "What did you hear?") }
                AnimatedVisibility(visible = showTranscript, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column { WordButtons(transcript, "heard") }
                }
            }
        }
        AnimatedVisibility(visible = exerciseDone, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Row(Modifier.fillMaxWidth().background(Color(0xFFE0F1DF), RoundedCornerShape(16.dp)).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("✓", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Column { Text("Words matched", fontWeight = FontWeight.Bold); Text("Ready for the next exercise.", fontSize = 14.sp) }
            }
        }
        if(!exerciseDone) {
            var scale = 1f
            if(listening) {
                val pulse = rememberInfiniteTransition(label = "listening pulse")
                val animated by pulse.animateFloat(initialValue = 1f, targetValue = 1.025f,
                    animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "microphone pulse")
                scale = animated
            }
            Button(enabled = !busy && !listening, modifier = Modifier.fillMaxWidth().height(60.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, disabledContainerColor = if(listening) accent else Color(0xFFCAD6CC), disabledContentColor = Color.White),
                onClick = { retryMode = retryTarget.isNotBlank(); startListening() }) {
                Text(if(listening) "Listening… speak now" else if(retryTarget.isNotBlank()) "● Repeat" else "● Tap and speak", fontSize = 19.sp)
            }
        }
        if(retryTarget.isBlank()) {
            TextButton(enabled = !busy && !listening, onClick = { showExample = !showExample }) { Text(if(showExample) "Hide example" else "Need an example?") }
            AnimatedVisibility(visible = showExample, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                CardBlock {
                    WordButtons(lesson.example, "example")
                    Row { TextButton(onClick = { speak(lesson.example) }, enabled = !busy && !listening) { Text("▶ Listen") }
                        TextButton(onClick = { speak(lesson.example, slow = true) }, enabled = !busy && !listening) { Text("▶ Slowly") } }
                    Button(enabled = !busy && !listening, onClick = { retryTarget = lesson.example; retryMode = true; showExample = false; startListening() }) { Text("Practise example") }
                }
            }
        }
        Text("Tap a word for English + ${if(nativeLanguage == "hi") "Hindi" else "spoken"} meanings.", fontSize = 12.sp, color = Color(0xFF56675B))
        if(retryTarget.isNotBlank()) TextButton(enabled = !busy && !listening, onClick = { goToLesson(lessonIndex) }) { Text("Try a different answer") }
    }
    @Composable private fun InlineMeaning() {
        Column(
            Modifier.widthIn(max = 270.dp).animateContentSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF1CD), Color(0xFFFFE7EE))), RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(selectedWord, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("English", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(englishDefinition.ifBlank { "Preparing meaning…" }, fontSize = 16.sp)
            if(meaningLanguage != "en") {
                Text("Hindi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(definition.ifBlank { "Preparing Hindi meaning…" }, fontSize = 18.sp)
            }
            if(busy && !meaningValid) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(meaningAudioStatus.isNotBlank()) Text(meaningAudioStatus, fontSize = 12.sp, color = Color(0xFF7153D6))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Added to Review ✓", fontSize = 12.sp)
                TextButton(enabled = meaningValid && !busy && !listening, onClick = { speak(definition, meaningLanguage) }) { Text("↻ Hear again") }
            }
            TextButton(enabled = !busy, onClick = { selectedSlot = ""; selectedWord = ""; tts?.stop() }) { Text("Hide meaning") }
        }
    }
    @OptIn(ExperimentalLayoutApi::class)
    @Composable private fun WordButtons(sentence: String, source: String) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            words(sentence).forEachIndexed { index, word ->
                val slot = "$source:word:$index"
                Column {
                    AssistChip(enabled = !busy && !listening,
                        onClick = { showMeaning(word, context = sentence, slot = slot) },
                        label = { Text(word, fontSize = 14.sp) })
                    if(selectedSlot == slot) InlineMeaning()
                }
            }
        }
        val phrases = dictionary.keys.filter { it.contains(" ") && normalized(sentence).contains(it) }
        if(phrases.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            phrases.forEach { phrase ->
                val slot = "$source:phrase:$phrase"
                Column {
                    AssistChip(enabled = !busy && !listening,
                        onClick = { showMeaning(phrase, context = sentence, slot = slot) },
                        label = { Text(phrase) })
                    if(selectedSlot == slot) InlineMeaning()
                }
            }
        }
    }
    @Composable private fun ReviewCards() {
        var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var revealed by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { while(true) { delay(1000); tick = System.currentTimeMillis() } }
        val due = saved.filter { prefs.getLong("due_$it", 0) <= tick }
            .sortedWith(compareBy<String> { prefs.getLong("due_$it", 0) }.thenBy { it })
        var chosenWord by remember { mutableStateOf<String?>(null) }
        val word = chosenWord?.takeIf { it in saved }
        LaunchedEffect(word) { revealed = false; selectedSlot = "" }
        Text("Review", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("${due.size} due · ${saved.size} cards", fontSize = 14.sp)
        if(word == null) {
            Text(if(saved.isEmpty()) "Tap a word in a lesson to add it automatically." else "Your words · tap any card to practise")
            saved.sorted().forEach { item ->
                val dueAt = prefs.getLong("due_$item", 0)
                OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), onClick = {
                    chosenWord = item; revealed = false; selectedSlot = ""; tts?.stop()
                }) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(if(dueAt <= tick) "Due now · Open →" else "Next: ${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(dueAt))} · Open →", fontSize = 12.sp)
                    }
                }
            }
        } else CardBlock {
            TextButton(enabled = !busy, onClick = { chosenWord = null; revealed = false; selectedSlot = ""; tts?.stop() }) { Text("‹ All words") }
            Text(word, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("What does this mean? Try saying a sentence with it.")
            TextButton(onClick = { speak(word) }) { Text("▶ Hear word") }
            if(!revealed) Button(onClick = { revealed = true }) { Text("Show answer") }
            else {
                val entry = dictionary[word.lowercase(Locale.ROOT)]
                val en = prefs.getString("meaning_en_$word", null) ?: entry?.english
                val hi = prefs.getString("meaning_hi_$word", null) ?: entry?.hindi
                val ready = !en.isNullOrBlank() && !hi.isNullOrBlank()
                Text(en ?: "Meaning not prepared yet.")
                Text(hi ?: "Hindi meaning not prepared yet.")
                val context = prefs.getString("context_$word", "").orEmpty()
                if(context.isNotBlank()) Text(context, fontSize = 14.sp)
                if(!ready) TextButton(enabled = !busy, onClick = { showMeaning(word, context, "review:$word") }) { Text("Prepare meaning") }
                else {
                    TextButton(onClick = { speak(hi.orEmpty(), "hi") }) { Text("▶ Hear Hindi meaning") }
                    Text("How well did you remember?", fontWeight = FontWeight.Bold)
                    listOf("Again", "Hard", "Good", "Easy").chunked(2).forEachIndexed { row, labels ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            labels.forEachIndexed { column, label ->
                                val rating = row * 2 + column
                                val minutes = ReviewSchedule.interval(prefs.getLong("interval_$word", 0), rating)
                                val intervalLabel = if(minutes < 60) "$minutes min" else if(minutes < 1440) "${minutes / 60} hr" else "${minutes / 1440} days"
                                OutlinedButton(enabled = !busy, modifier = Modifier.weight(1f), onClick = {
                                    prefs.edit().putLong("interval_$word", minutes).putLong("due_$word", System.currentTimeMillis() + minutes * 60000)
                                        .putInt("reviews_$word", prefs.getInt("reviews_$word", 0) + 1).apply()
                                    chosenWord = null; revealed = false; tick = System.currentTimeMillis(); selectedSlot = ""; tts?.stop()
                                }) { Text("$label · $intervalLabel") }
                            }
                        }
                    }
                }
            }
        }
        Text("Difficult words return sooner. Familiar words wait longer. Reviews work offline.", fontSize = 12.sp)
    }
    @Composable private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.animateContentSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { message = "Processing your words…" }
    override fun onError(error: Int) {
        listening = false
        message = when(error) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I couldn't hear clearly. Tap the microphone and try again."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Please allow microphone access in app settings."
            12, 13 -> "English recognition data is unavailable. Use Setup → Prepare English listening while online."
            else -> "Listening failed (code $error). Check offline speech setup and try again."
        }
    }
    override fun onResults(results: Bundle?) { listening = false; evaluate(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()) }
    override fun onPartialResults(partialResults: Bundle?) { transcript = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty() }
    override fun onEvent(eventType: Int, params: Bundle?) {}
    override fun onStop() { super.onStop(); recognizer?.cancel(); listening = false; tts?.stop() }
    override fun onDestroy() {
        recognizer?.destroy(); tts?.shutdown()
        // The coach mutex waits for any native generation before releasing its model.
        CoroutineScope(Dispatchers.IO).launch { coach.close() }
        super.onDestroy()
    }
}
