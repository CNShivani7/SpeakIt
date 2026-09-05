# Speak It

A phone-first English speaking coach prototype: listen, speak, receive a correction, and repeat. Built with Kotlin and Jetpack Compose.

## Status — read before demonstrating

Version 1.0 compiled and ran on the user’s Galaxy Z Fold6. The user reported a successful offline correction/retry and word-saving test; intermittent correction-format failure was also reported. Version 1.8 includes real email OTP authentication and the UI updates below; it still needs a fresh Android build and phone test. The public repository and download release have not been published yet.

### Implemented in source

- Six short lessons with emoji picture prompts: introductions, yesterday, café, daily habits, interview, asking for help.
- Actual local LLM integration: Qwen2.5-0.5B-Instruct Q4_K_M through llama.cpp Android binding.
- Explicit model download (~491 MB), SHA-256 verification, import alternative, loading/error states.
- Short grammar correction and replay target generated locally; no cloud inference fallback. Version 1.1 accepts common formatting variations and automatically retries once with a sentence-only prompt when needed. This does not guarantee semantic correctness.
- Listen at normal/slow speed; one-tap Repeat starts listening immediately, with no second record button. Compare recognised words after the user stops speaking.
- Tap example, corrected or recognised words for Hindi/English meanings. Selected multiword expressions included.
- Built-in dictionary for lesson vocabulary; local Qwen for other meanings.
- English-first screens and teaching. Choose Hindi/English only for optional word meanings; change this preference later without losing progress. Hindi is the initial optional meaning preference for the 1.1 migration.
- Saved word notebook, attempt count, lesson retry completion persisted on device.
- Android on-device speech recognition only; offline TTS voices only.

### Limits

- Speech service and TTS engine are supplied by Android/device vendors and are not necessarily open-source. The **LLM weights and inference runtime** are openly licensed. If the rules require every speech component to be open-source too, replace these with bundled Whisper/Vosk/sherpa models before submission.
- The package installs on API 26+, but this prototype's microphone path requires API 31+ and a supported on-device recognition service with downloaded English data. There is no network recognizer fallback.
- English and Hindi voices must be installed separately through the device's TTS settings; availability varies by engine.
- Word matching is **not phoneme-level pronunciation assessment**. It never assigns a pronunciation score.
- Tiny model outputs can be incorrect; malformed correction responses are rejected rather than silently substituted with a canned AI answer.
- Hindi translations produced by the tiny model require human evaluation. Lesson dictionary meanings are authored, not model-generated at runtime.
- Pictures are local emoji prompts, not custom illustrations. Multiple unknown words can be saved one at a time.
- No streaks, native-language speech-to-English feature, recording comparison, broad language packs, or public hosting yet.
- First setup requires internet. Download must be completed with the app open; interrupted downloads restart.
- Background operation is not implemented. Keep the app foregrounded during model download and inference.

## Fast start on the existing laptop project

1. Make a backup of your original project.
2. Close the project in Android Studio.
3. Copy the contents of this `SpeakIt` folder into your existing `C:\Users\User\AndroidStudioProjects\SpeakIt` folder and replace matching files. The ZIP excludes generated caches and `local.properties`; your existing SDK location and downloaded caches stay local.
4. Open the existing project again. Let Gradle sync the new `dev.ffmpegkit-maintained:llama-android:0.1.1` dependency.
5. Connect the Fold6 via USB with Developer options → USB debugging enabled. Accept the phone's debugging prompt only for your own laptop.
6. Select the phone in Android Studio and click Run.
7. Follow app Setup: local model → English listening → offline voices → lessons.

Local AI needs no Claude key or cloud inference. Email registration uses the configured Supabase project and its Brevo SMTP service; sign-in requires internet.

## Exact model

- Publisher: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF
- File: `qwen2.5-0.5b-instruct-q4_k_m.gguf`
- Download: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf
- SHA-256: `74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db`
- Model weights licence: Apache-2.0. Model files are downloaded from the publisher and are not in this repository.
- Runtime: https://github.com/ffmpegkit-maintained/llama-android (MIT), bundling llama.cpp (MIT).

## Build

Keep the template versions that successfully synced on the development laptop: Android Gradle Plugin 9.4.0, Gradle 9.6.0, compile/target SDK 37, Compose BOM 2026.02.01. Use Android Studio's bundled Gradle JDK.

Windows terminal from this folder:

```
.\gradlew.bat :app:assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`.
This debug APK is for demonstration; store publishing and release signing are separate work.

## Public GitHub submission

1. Create a GitHub repository named `speak-it`, with visibility **Public**.
2. Add the source from this folder. Do not upload the ZIP as a substitute for browsable source.
3. Exclude `.gradle`, `.idea`, `local.properties`, build folders, APKs, model weights, API keys and recordings (see `.gitignore`).
4. Upload a successfully tested APK to a GitHub Release as `SpeakIt-demo.apk`.
5. The public repository URL and Release page are shareable URLs. The release URL lets a learner download the Android app; it is not a browser version of the app.
6. Check both URLs in a signed-out browser before submission.
7. If judges explicitly require a browser-interactive product, a download page alone does not satisfy that requirement.

See `docs/DEMO-CHECKLIST.md` for the acceptance test. Record actual outcomes before making offline claims.

## Version 1.1 acceptance checks

- Open a lesson with Hindi selected under optional meanings: all prompts/corrections must remain English.
- Tap went: English definition appears. Tap Meaning in my language: Hindi definition appears and plays if its offline voice is available. Close: lesson remains English.
- Tap Repeat once: the same button says Listening; speak immediately. No Repeat aloud button should appear.
- Say a correct introduction: feedback should say it is already correct, without a proper-noun lecture.
- Retry Yesterday offline, including an original sentence different from the example. The parser may retry once but must never fabricate a canned correction.
- Confirm model files and saved words survived the update; do not uninstall.

## Version 1.2 — inline spoken meanings (current)

This supersedes the two-step translation popup described for version 1.1.
Tap a word or phrase: show its meaning directly below that selected chip, automatically speak it in the chosen native language, and offer Save word. No modal or extra translation action. Replay and Hide meaning are optional controls. All lesson screens and correction feedback stay English. Select Hindi in Setup for Hindi meanings. The notebook also shows meanings inline. Model identity and saved-word storage keys are preserved. Android compilation and phone testing of this update remain pending.

Acceptance: select Hindi in Setup, open Yesterday, tap went. Hindi must appear under went and play without any second tap. Save word should change to Saved, without dismissing the meaning. Tap a word in another card: only that occurrence should expand. Return to speaking: inline help closes and speech playback stops.

## Version 1.3 — focused lessons and bilingual inline meanings (current)

Supersedes version 1.2 meaning display. English definition is shown first, selected native-language definition beneath it, with automatic native-language audio and Save word in the same inline panel. English and Hindi are the current supported choices. No popup.

One exercise per screen: collapsed optional example and recognised transcript; a single speaking/repeat action; bottom-right Next unlocked by a successful fresh repeat. Last exercise leads to a session completion screen. Uses Compose animations for progress, inline expansion, a listening pulse and completion reveal. Visual direction uses short lessons and clear rounded controls inspired by language-learning apps, without Duolingo artwork or branding.

Device verification for 1.3 is pending. The previous working model download and saved-word keys are preserved.

Quick acceptance:
1. Keep Hindi selected for meanings. Lessons stay English.
2. Expand Need an example, tap a word: see English then Hindi; hear Hindi automatically; Save word remains inline.
3. Practise example starts microphone on one tap. Repeat the sentence; Words matched appears; Next at bottom-right becomes enabled.
4. Next shows exactly the next exercise, with clean state. No double microphone action.
5. Test grammar correction and a bilingual word meaning again in airplane mode with Wi-Fi disabled.
6. If Hindi voice is missing, English/Hindi text remains visible and the setup message asks for an offline voice; no cloud voice is used.

## Version 1.4 — autoplay lifecycle and animated colour treatment (current)

- Autoplay now runs in a Compose effect after the meaning is visible and the TTS engine has initialized. Each word tap has a unique request ID, including repeated taps of the same word. It selects an offline voice explicitly, observes playback callbacks, and retries a rejected request once. A startup watchdog retries if no start callback arrives. Navigating away cancels pending autoplay.
- English and Hindi text remain stacked inline, with Save word. Hear again is optional. Autoplay errors are visible below the meaning.
- Purple, teal, orange, green, pink and blue lesson accents; soft gradients; an original Canvas-drawn animated speech buddy with blinking, floating and celebration particles. No external images, downloads or Duolingo art used.
- Header displays 1.4 to verify deployment. App identity, model storage and saved-word keys remain unchanged.

Important: previous auto-audio failure was reported on the phone; its exact engine cause was not diagnosed remotely. This patch must be tested on the Fold6. Do not claim autoplay is fixed until a single word tap plays Hindi without touching Hear again. Check two consecutive different words and a repeated tap of the same word; then verify in airplane mode. Missing Hindi voice data still requires setup. This environment has not compiled or visually tested 1.4.

## Version 1.5 — language and level onboarding (current)

First screen: English (UK English / US English), Native language (Hindi only currently). Next screen: Beginner / Intermediate / Advanced, each with a short description. Choices persist. Existing users see this flow once after upgrading, without deleting their model or notebook.

The English locale controls the recognition request and voice selection (en-GB or en-US), and is included in the correction prompt. Exact offline voice region is required rather than silently playing another accent. Prepare the corresponding recognition pack and voice in Setup. The old en-IN recognition pack may not cover these locales. The Qwen model does not need downloading again.

The selected level chooses one of three authored six-exercise sets; intermediate prompts cover experiences/reasons/plans, advanced prompts cover trade-offs, tactful disagreement, reflection and uncertainty. This is self-selection, not a proficiency assessment. Newly added vocabulary outside the existing dictionary uses local Qwen; translation quality needs validation. Level and voice may be changed in Setup.

Version 1.5 requires Android compilation and phone tests: first-run screen order, choice persistence, no loss of words/model, correct exercise set, actual UK/US playback and recognition (including airplane mode), and automatic Hindi meaning audio.

## Version 1.6 — local profile and settings (current)

Top-right avatar button opens Profile; gear opens Settings. Profile supports name editing, four local avatar choices, selected level/languages, speaking-attempt count, matched-exercise count, saved-word count and a notebook link. Legacy numeric completion keys are normalized to beginner-level keys to avoid double-counting.

Settings include offline English voice variants (up to four installed choices for the selected UK/US locale), slow/normal/fast speech speed, standard/large text scaling that preserves the system font-scale preference, English locale, Hindi native language, level and existing offline setup controls. Choices persist on-device.

Log out stops speech/listening and returns to a local welcome screen. Continue as [name] restores access; this is explicitly NOT password-based authentication or secure account separation. It does not delete model files, preferences, progress or saved words. No email, password, online profile, account deletion or server session has been introduced.

Acceptance checks pending: compile/install 1.6; open top-right Profile/Settings; edit name/avatar and relaunch; test speed and font-size controls; test UK/US voice selection; log out and relaunch (welcome should remain); Continue restores profile and saved words; then repeat the offline speech and automatic Hindi playback tests.

## Version 1.7 — email verification

- Real Supabase email OTP send and verification, followed by name/avatar, language and level setup. No simulated codes or bypass button. SMS is not implemented.
- Requires custom SMTP and the Magic Link email template containing `{{ .Token }}`. The project URL and publishable key are public client configuration in EmailAuth.kt. Never add SMTP keys, service-role keys or database passwords to the app or repository.
- Sessions are encrypted with an Android Keystore AES-GCM key. Practice preferences are separate per Supabase user ID. Existing anonymous practice can be explicitly imported once during account setup. Model downloads remain shared on this phone.
- A previously verified local session permits offline practice; this is not a fresh server validation of account status. No cloud practice-data sync is implemented. Logging out clears local credentials and attempts server session revocation; signing back in requires internet and a fresh code.
- Email delivery, OTP verification, Keystore restoration and logout have not been tested against the live project from this workspace. No test emails were sent here. This version must be compiled and exercised on the phone.

### Required phone checks

1. Install over the existing app, without uninstalling. Send a code to your own email and verify it. Check spam if needed.
2. Enter an incorrect code first: it must remain on the verification screen. Then enter the newest valid code.
3. Complete account setup; optionally import your earlier practice. Check lessons and saved words.
4. Close and reopen in airplane mode: a previously verified account should retain offline practice access.
5. Log out offline, then reopen: it must require sign-in. Reconnect and verify the same email: its progress should return. Another email must start separately.
6. Confirm one-tap Repeat and automatic inline Hindi word audio on the device.

## Version 1.8 — automatic review cards and offline voice choice

Every tapped word or phrase is automatically kept as a review card, even if its meaning cannot yet be prepared. Meanings are cached when available. There is no Save word button. Existing saved words enter the review queue automatically. Failed meanings must be prepared before grading; error text is not stored as the answer.

Review reveals English/Hindi meanings and context before self-grading. Again returns in 1 minute; Hard starts at 10 minutes, Good at 1 day, Easy at 3 days. Subsequent Hard/Good/Easy intervals multiply by 1.2/2/3, capped at 365 days. Looking up a word again does not reset its schedule. Due times and intervals persist per account. This is a simple integrated spaced-repetition scheduler, not Anki integration or FSRS.

Automatic audio prioritizes engine-reported offline voice quality. Both English and Hindi offer installed voice previews and selection. No neural voice model is bundled; humanlike quality depends on installed TTS voices. The app still rejects network-only voices.

Validation: scheduler test cases cover initial intervals, interval growth, lapse reset, maximum interval and invalid ratings, but could not be executed here because the Java compiler is unavailable. XML and ZIP checks passed. The complete Android app still needs a build and phone test. Test automatic saving, reveal/grade, due-time persistence after restart, repeated taps preserving schedules and English/Hindi voice playback.

## Version 1.9 — Profile progress

Profile now shows overall and per-level exercise progress bars, speaking attempts, flashcard totals, due cards, unique cards reviewed and total review actions. Counts use the signed-in account’s existing local data. Repeating an exercise does not double-count completion. These are activity counts, not language proficiency scores. Android build and device validation remain pending.

## Version 1.10 — all-word Review list
Review opens a scrollable alphabetical list of every saved word, including cards not yet due. Select any word to reveal and grade its flashcard. All words returns without grading; grading saves its schedule and returns to the list. Existing words and schedules are preserved. Android build/device testing remains pending.
