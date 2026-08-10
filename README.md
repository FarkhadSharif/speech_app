# Сөйле

**Сөйле** — Kazakh-language Android learning app that helps children practise listening, vocabulary, sound awareness, sentence building, comprehension, and storytelling together with an adult.

The app is educational. It does not diagnose or treat speech or language disorders and does not replace a paediatrician, audiologist, or speech-language professional.

## Complete course

The course contains 6 sequential levels, 18 activities, and 80 rounds. A new level opens only after all three activities in the preceding level are complete.

| Level | Focus | Examples |
| --- | --- | --- |
| 1. Тыңдаймын және айтамын | listening, first words, 1–2 syllables | listen and choose, repeat a word, syllable drum |
| 2. Сөздерді топтаймын | vocabulary, categories, one-step directions, emotions | odd-one-out, attributes, social situations |
| 3. Дыбыстарды байқаймын | phonological awareness | initial sounds, rhymes, 3–4 syllables |
| 4. Сөйлем құраймын | 3–5 word sentences and questions | word ordering, who/what/where/why, full phrases |
| 5. Оқиға айтамын | sequencing and comprehension | three-part stories, cause and prediction, descriptions |
| 6. Шебер сөйлеуші | 2–3 step directions and independent language | 5–7 word sentences, four-part stories, creative retelling |

## Child experience

- Spoken Kazakh prompts through Android Text-to-Speech, with a replay button on every round
- Five exercise formats: listen-and-choose, syllable tapping, word ordering, story ordering, and voice practice
- Optional Kazakh speech recognition with an adult-confirmation fallback; microphone access is not required
- Supportive feedback that shows the correct model without punishing mistakes
- Stars, best scores, level locks, activity history, and a visible learning path
- A 24-word illustrated listening library for short everyday practice
- A break reminder after three activities to encourage off-screen conversation and play

## Parent experience

- Real local progress dashboard and per-activity statistics
- Practical prompts for responsive back-and-forth talk, expanding the child's words, shared reading, and sound play
- Guidance for multilingual families and signs that merit professional or hearing assessment
- Clear explanation that app scores are engagement indicators, not clinical measures

## Privacy

Progress is stored only in Android `SharedPreferences` on the device, and Android backup is disabled. The app has no account, ads, analytics, backend, or `INTERNET` permission. Audio is never saved by the app. If voice recognition is used, processing depends on the speech service installed on the device and that service may use a network connection. The parent-assisted option works without the microphone.

## Open and run

1. Open Android Studio.
2. Choose **Open** and select this `speech_app` folder (not its parent folder).
3. Wait for Gradle sync to finish.
4. Start an Android emulator or connect a phone with USB debugging enabled.
5. Select the device and click **Run app**.
6. Grant microphone access only if you want automatic voice matching.

The app requires Android 7.0 (API 24) or newer. A Kazakh TTS voice installed in Android gives the best listening experience; the app falls back to the device language if it is unavailable.

## Build and verify

Use Java 17 and Android SDK 34:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat lintDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

- `model/Curriculum.kt` — all levels, activities, rounds, and voice-match scoring
- `data/ProgressRepository.kt` — persistent progress, stars, attempts, active days, and unlock logic
- `audio/SpeechAudio.kt` — Text-to-Speech and short feedback sounds
- `ui/GameScreens.kt` — reusable exercise engine and the five exercise types
- `ui/SpeechApp.kt` — child path, parent area, navigation, statistics, guide, and word library

## Evidence-informed design

The activity design follows broadly accepted practices: responsive interaction, language in meaningful context, modelling a slightly richer phrase, story conversation, and phonological-awareness play. It avoids presenting automatic speech matching as a diagnosis or pronunciation assessment.

- [NIDCD: Speech and Language Developmental Milestones](https://www.nidcd.nih.gov/health/speech-and-language)
- [ASHA: Communication Milestones, 3–4 Years](https://www.asha.org/public/developmental-milestones/communication-milestones-3-to-4-years/)
- [ASHA: Communication Milestones, 4–5 Years](https://www.asha.org/public/developmental-milestones/communication-milestones-4-to-5-years/)
- [ASHA: Spoken Language Disorders](https://www.asha.org/practice-portal/clinical-topics/spoken-language-disorders/)
- [ASHA: Speech Sound Disorders](https://www.asha.org/practice-portal/clinical-topics/articulation-and-phonology/)
- [Harvard Center on the Developing Child: Serve and Return](https://developingchild.harvard.edu/key-concept/serve-and-return/)
- [American Academy of Pediatrics: Co-viewing](https://www.healthychildren.org/English/family-life/Media/Pages/why-co-viewing-is-important-tips-to-share-screen-time-with-your-kids.aspx)
