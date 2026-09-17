# Twit AI Android Companion

This companion app is designed for the native X Android app.

## Intended flow

X Android app
→ select tweet text
→ Android Accessibility Service detects the selection
→ Twit AI floating action appears
→ Twit AI overlay opens
→ settings
→ https://twitai.app/api/generate
→ Firebase ID token authentication
→ OpenAI remains server-side
→ replies appear in the overlay
→ user copies and manually posts

## Important limitation

Android and X can change how text selection events are exposed. This app does not modify X's native selection toolbar. It uses an AccessibilityService and an accessibility overlay.

Therefore:
- it does not require X API access
- it does not store X passwords/cookies
- it does not automatically post
- it does not click Reply, Like, Repost, or Follow
- it only sends selected text to Twit AI after the user taps the Twit AI action

If a particular X app version does not expose the selected text through accessibility events, this method may not receive the text. In that case a native text-selection ACTION_PROCESS_TEXT integration or another Android-side mechanism may be needed, but X must expose the action.

## Firebase / Google setup

The project uses the existing Twit AI Firebase project configuration and the existing web OAuth client ID in `Config.kt`.

For Google Credential Manager on Android, create an Android OAuth client in Google Cloud for:

Package:
app.twitai.companion

Add the SHA-1 certificate fingerprint for the signing key used to build/install the app.

Keep the existing web client ID as the `serverClientId` in `Config.kt`.

## Build

Open the project in Android Studio and let Gradle sync.

Build:
./gradlew assembleDebug

Install the resulting APK.

## First launch

1. Sign in with Google.
2. Tap "Enable X text helper".
3. Android Settings → Accessibility → Twit AI → enable.
4. Open X.
5. Select text in a tweet.
6. Tap "Send to Twit AI".
7. Generate.
8. Copy a reply and paste it into X manually.

## Backend

The app calls:

POST https://twitai.app/api/generate

with:
Authorization: Bearer <Firebase ID token>

and the same request fields used by the existing Twit AI web app:
tweet, replyCount, minWords, maxWords, tone, tag, language.

The OpenAI API key is never included in this Android project.
