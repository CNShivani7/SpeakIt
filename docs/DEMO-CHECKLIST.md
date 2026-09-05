# Demonstration checklist

Version 1.0: user confirmed build/install, model load, a working offline correction/retry and saved word. Voice/recognition worked in that trial. This is user-reported evidence, not a full audit. Version 1.1 needs the checks below repeated; other items remain unverified.

1. Android project compiles and installs on Galaxy Z Fold6.
2. First launch offers Hindi/English; selection persists after relaunch.
3. Exact Qwen download succeeds, hash verifies, local engine loads. Corrupt imports show an error.
4. English recognition model is downloaded. English and Hindi offline voices speak correctly.
5. Switch on airplane mode AND turn Wi-Fi off. Kill and reopen the app so previously displayed answers cannot masquerade as fresh inference.
6. Open Yesterday lesson. Say "Yesterday I go to office". Confirm the app transcribes, generates a local correction, and plays it aloud. Check it retains the original meaning.
7. Repeat the corrected sentence. Confirm a fresh recognition result and meaningful retry feedback.
8. Try a different original sentence to establish the correction is not just a fixed canned response.
9. Tap "yesterday", hear "बीता हुआ कल", save it, reopen My words. Hear word and meaning offline.
10. Change optional meaning language to Hindi. Confirm screens, lessons and corrections stay English. Tap a word, then Meaning in my language: only the requested meaning switches to Hindi. Confirm notebook/progress remain.
11. Deny microphone permission; check clear recovery. Try silence/noise; no fabricated transcript or pronunciation score should appear.
12. Fold/unfold and rotate the phone; no duplicate inference or lost model state.
13. Explain the limits honestly: no phoneme scoring; pictures are emoji; other languages and spaced revision are future work.
14. Publish source as public GitHub repo and tested APK as Release asset; verify signed-out access.

For each performance trial record: device, Android version, exact model, approximate time from end of speech to spoken correction, whether airplane mode was on, and whether the correction was useful. No performance claims are pre-filled.
