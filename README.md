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

- Bundled native Kazakh speech for every course phrase, with offline playback and a replay button on every round
- Face Map 2.0 articulation mirror with target lip guides, deviation colors, mouth metrics, two-second pose hold, and sound-specific exercises; camera frames are not stored or uploaded
- Express speech assessment with four Kazakh prompts, Face Map exercises, and a clear non-medical speech profile
- Color-coded sound map that links assessment results to matching course words and exercises
- Personalized daily route that prioritizes sounds, words, games, and a reassessment date from the latest assessment
- Five exercise formats: listen-and-choose, syllable tapping, word ordering, story ordering, and voice practice
- Optional Kazakh speech recognition with an adult-confirmation fallback; microphone access is not required
- Supportive feedback that shows the correct model without punishing mistakes
- Stars, best scores, level locks, activity history, and a visible learning path
- A 24-word illustrated listening library for short everyday practice
- A break reminder after three activities to encourage off-screen conversation and play

## Parent experience

- Persistent parent accounts through Firebase Authentication with session restoration
- Email verification, safe password recovery, current-device sign-out, and all-device session revocation
- Real local progress dashboard and per-activity statistics
- Practical prompts for responsive back-and-forth talk, expanding the child's words, shared reading, and sound play
- Guidance for multilingual families and signs that merit professional or hearing assessment
- Clear explanation that app scores are engagement indicators, not clinical measures

## Privacy

Parent accounts are managed by Firebase Authentication. The child name, age, and learned-word identifiers can be stored in Cloud Firestore for the signed-in parent; detailed activity progress currently remains in Android `SharedPreferences` on that device. Android backup is disabled. The app has no ads or analytics, and microphone audio is never saved by the app. If voice recognition is used, processing depends on the speech service installed on the device and that service may use a network connection. The parent-assisted option works without the microphone.

## Open and run

1. Open Android Studio.
2. Choose **Open** and select this `speech_app` folder (not its parent folder).
3. Wait for Gradle sync to finish.
4. Start an Android emulator or connect a phone with USB debugging enabled.
5. Select the device and click **Run app**.
6. Grant microphone access only if you want automatic voice matching.

The app requires Android 7.0 (API 24) or newer. All current course prompts are bundled in the APK and work without internet access or an installed Android text-to-speech voice.

For presentations and offline evaluation, the sign-in screen includes a clearly labelled local demo mode. It unlocks the learning path for navigation but does not fabricate progress or write guest data to Firebase.

Firebase Authentication features require an internet connection. In the Firebase console, enable the **Email/Password** sign-in provider and configure the verification and password-reset email templates for the production domain.

## Bundled Kazakh speech

The 127 normalized course and Face Map phrases are generated with the high-quality `kk_KZ-issai-high` Piper voice trained on the ISSAI KazakhTTS/KazakhTTS2 datasets. The bundled voice uses speaker `ISSAI_KazakhTTS_F1_Raya`. Dataset attribution and the CC BY 4.0 license link are included in `app/src/main/assets/speech/ATTRIBUTION.txt`.

## Build and verify

Use Java 17 and Android SDK 34:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat lintDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Firebase backend

The callable Cloud Function for **sign out on all devices** is prepared in `backend/functions`. It accepts only authenticated callable requests, obtains the parent identifier from the verified Firebase Authentication context, and never accepts a user identifier from Android. The Android app clears its local session only after the function succeeds.

The function has not been deployed. Before this feature can work in an installed APK, select the same Firebase project used by `app/google-services.json`, then follow `backend/functions/README.md`. Do not add a project-specific `.firebaserc` until the deployment target has been confirmed.

## Architecture

- `model/Curriculum.kt` — all levels, activities, rounds, and voice-match scoring
- `data/ProgressRepository.kt` — persistent progress, stars, attempts, active days, and unlock logic
- `data/AuthRepository.kt` — testable parent-authentication contract and domain errors
- `ui/AuthViewModel.kt` — unified authentication state, validation, and session restoration
- `backend/functions` — tested Admin SDK callable function for revoking the current parent's refresh tokens
- `audio/SpeechAudio.kt` — bundled offline speech playback, device TTS fallback, and short feedback sounds
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
