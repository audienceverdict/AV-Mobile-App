# Audience Verdict Android app

Native Android app scaffold using Kotlin, Jetpack Compose, Material 3, Retrofit, OkHttp, Coil, Navigation Compose, and encrypted token storage.

## Open and run

Open this directory in Android Studio with JDK 17 and Android SDK 36 installed. Allow Gradle sync. Install/run the `app` configuration on an emulator or device with internet access. The default API is `https://api.audienceverdict.in/api/v1/`.

To point Gradle at another environment, add `apiBaseUrl=https://your-existing-api-host/api/v1/` to `~/.gradle/gradle.properties`. A release deployment should use HTTPS and the deployed API URL.

## Splash screen

The launch screen stays visible for a short intro while the AV microphone, clapperboard, and ticket animate one at a time. If internet is unavailable, it remains on the splash and displays a retry message. A valid stored session is checked through `/auth/me`; expired tokens are cleared. Before sign-in, the app requires approximate Location permission. The email sign-in screen uses a cinematic collage background and sends OTP through the existing API. The supplied Audience Verdict logo is used on the splash, authentication screens, and as the launcher icon.

## Implemented API flows

- Email OTP request and verification use the documented `destination` and `otp` request fields. The returned access token is stored with Android Keystore-backed encrypted preferences, and `/api/v1/auth/me` is requested after login.
- Account creation verifies the mobile number through the registration WhatsApp OTP endpoints, then verifies the email using the mobile registration token. Account creation remains disabled until both proof tokens are present and submits them to `/auth/otp/register`.
- Catalog and movie details load from `/api/v1/movies`; filter choices use the documented `status` and `title` parameters.
- Poster fields accept direct image URLs or web pages with an Open Graph/Twitter image preview (including Wikipedia pages). Trailer links for YouTube and Google Drive use their embedded players; direct video URLs and other HTTP/HTTPS web links open inside the app WebView. Movie detail, showtime, seat, review, payment, and booking pages retain the bottom navigation with the related tab selected.
- Theatres, shows, show availability, reviews, and the authenticated booking list use their documented endpoints and response shapes.
- Seat availability is rendered from the returned layout. Booked, reserved, held, disabled, and non-clickable seats cannot be selected. Selected seats are sent to the server hold endpoint.

## Integration dependencies and current limits

- The documented backend has no payment initiation or checkout-session endpoint. The payment screen clearly reports that checkout is not configured; the app never claims that a payment or booking succeeded.
- Review submission/editing, booking creation/cancellation, payment verification, watchlist persistence, and notifications UI still need implementation against their documented contracts/provider configuration.
- The configured API base URL is `https://api.audienceverdict.in/api/v1/`; verify that this production service and its certificate are available before release.
- Movie images are loaded from API image URLs; missing URLs use a branded AV fallback.

## Release signing

Keep the upload keystore outside this project. The release build reads `key.properties` from the project root (this file is ignored by Git) or the `AV_RELEASE_STORE_FILE`, `AV_RELEASE_STORE_PASSWORD`, `AV_RELEASE_KEY_ALIAS`, and `AV_RELEASE_KEY_PASSWORD` environment variables. Create the keystore only with the actual publisher identity and back it up securely. Never commit the keystore or credentials. Without configured signing credentials, `bundleRelease` produces an unsigned bundle that cannot be uploaded.

In PowerShell, generate the upload key without putting passwords in command history:

```powershell
keytool -genkeypair -v -keystore "${env:USERPROFILE}\AndroidKeys\AudienceVerdict-release.jks" -alias audienceverdictkey -keyalg RSA -keysize 3072 -validity 10000
```

Keytool prompts locally for passwords and certificate details. Use the real publisher or legal-entity details. Then create the ignored root `key.properties` with `storeFile`, `storePassword`, `keyAlias=audienceverdictkey`, and `keyPassword`.

When your upload key is ready, create a local `key.properties` containing `storeFile` (absolute keystore path), `storePassword`, `keyAlias`, and `keyPassword`, then run `gradlew.bat bundleRelease`.

## Build artifact

Release validation tasks `:app:testReleaseUnitTest`, `:app:lintRelease`, and `:app:bundleRelease` completed successfully. No unit tests are currently present, so Gradle reports the test task as `NO-SOURCE`. The generated bundle is unsigned until a local upload keystore is configured; see Release signing. No emulator/device was attached, so runtime navigation and API sign-in were not exercised. A local unsigned validation bundle is in `release-output/AudienceVerdict-release-UNSIGNED.aab`.

The supplied OpenAPI document is available at the configured API host `/v3/api-docs`.







"# AV-Mobile-App" 
