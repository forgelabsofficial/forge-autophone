# Production App Signing Guide

This guide explains how to sign AutoPhone with your own keystore for production releases.

## Why Sign Your App?

- **Debug-signed APKs** (current setup) can be installed on any device but can't be published to Google Play
- **Release-signed APKs** use your private keystore and are required for:
  - Publishing to Google Play Store
  - Updating existing installed apps (must use same signature)
  - Establishing your app's identity

## Step 1: Generate a Keystore

Run this command to create a new keystore (one-time setup):

```bash
keytool -genkey -v -keystore autophone-release.keystore -alias autophone -keyalg RSA -keysize 2048 -validity 10000
```

You'll be prompted for:
- **Keystore password**: Choose a strong password (you'll need this to sign)
- **Key password**: Can be same as keystore password
- **Your details**: Name, organization, location (shown in certificate)

**IMPORTANT:** 
- Store the keystore file safely (backup to secure location)
- Never commit the keystore to git
- If you lose it, you can't update your app on Play Store

## Step 2: Create keystore.properties

Create a file `keystore.properties` in the project root (it's already in `.gitignore`):

```properties
storeFile=autophone-release.keystore
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=autophone
keyPassword=YOUR_KEY_PASSWORD
```

**Security Note:** This file contains secrets - never commit it to version control!

## Step 3: Update build.gradle.kts

Replace the current signing configuration with:

```kotlin
signingConfigs {
    create("release") {
        val keystorePropsFile = rootProject.file("keystore.properties")
        if (keystorePropsFile.exists()) {
            val keystoreProps = java.util.Properties()
            keystoreProps.load(java.io.FileInputStream(keystorePropsFile))
            
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        } else {
            // Fallback to debug for CI builds
            storeFile = signingConfigs.getByName("debug").storeFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
}

buildTypes {
    release {
        isMinifyEnabled = true  // Enable for production
        shrinkResources = true  // Remove unused resources
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
    }
}
```

## Step 4: Build Signed APK

### Local Build

```bash
# Windows
.\gradlew clean assembleRelease

# Linux/Mac
./gradlew clean assembleRelease
```

The signed APK will be at:
```
build/outputs/apk/release/forge-autophone-release.apk
```

### CI Build (GitHub Actions)

To sign on GitHub Actions:

1. **Encode your keystore to base64:**
   ```bash
   # Windows (PowerShell)
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("autophone-release.keystore")) | Out-File keystore.txt
   
   # Linux/Mac
   base64 -i autophone-release.keystore -o keystore.txt
   ```

2. **Add GitHub Secrets:**
   - Go to: Repository → Settings → Secrets and variables → Actions
   - Add these secrets:
     - `KEYSTORE_BASE64`: content of keystore.txt
     - `KEYSTORE_PASSWORD`: your keystore password
     - `KEY_ALIAS`: autophone
     - `KEY_PASSWORD`: your key password

3. **Update `.github/workflows/ci.yml`:**

```yaml
- name: Decode Keystore
  if: github.ref == 'refs/heads/main'
  run: |
    echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > autophone-release.keystore

- name: Create keystore.properties
  if: github.ref == 'refs/heads/main'
  run: |
    echo "storeFile=autophone-release.keystore" > keystore.properties
    echo "storePassword=${{ secrets.KEYSTORE_PASSWORD }}" >> keystore.properties
    echo "keyAlias=${{ secrets.KEY_ALIAS }}" >> keystore.properties
    echo "keyPassword=${{ secrets.KEY_PASSWORD }}" >> keystore.properties

- name: Build Release APK
  run: |
    chmod +x gradlew
    ./gradlew clean assembleRelease --no-daemon \
      -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m" \
      -Dkotlin.daemon.jvm.options="-Xmx1g"
```

## Step 5: Verify Signature

Check the signature of your APK:

```bash
# Windows
keytool -printcert -jarfile build\outputs\apk\release\forge-autophone-release.apk

# Linux/Mac
keytool -printcert -jarfile build/outputs/apk/release/forge-autophone-release.apk
```

Look for:
- **Owner**: Your details from Step 1
- **Certificate fingerprints**: SHA-256 hash (needed for Play Store)

## Step 6: Publishing to Google Play

1. **Get SHA-256 fingerprint:**
   ```bash
   keytool -list -v -keystore autophone-release.keystore -alias autophone
   ```

2. **Create Play Console account:**
   - Go to https://play.google.com/console
   - Pay one-time $25 registration fee

3. **Upload APK:**
   - Create new app
   - Upload `forge-autophone-release.apk`
   - Fill in store listing, screenshots, descriptions
   - Submit for review

## Pro Tips

### Use Android App Bundle (AAB)

For Play Store, use AAB instead of APK (smaller downloads):

```bash
./gradlew clean bundleRelease
```

Output: `build/outputs/bundle/release/forge-autophone-release.aab`

### Enable ProGuard

The updated config above enables:
- `isMinifyEnabled = true` - removes unused code
- `shrinkResources = true` - removes unused resources

This reduces APK size by ~30-50%.

### Version Management

Update version in `build.gradle.kts` before each release:

```kotlin
defaultConfig {
    versionCode = 2  // Increment for each release
    versionName = "1.1.0"  // Semantic versioning
}
```

### Security Checklist

- [ ] Keystore backed up to secure location
- [ ] `keystore.properties` in `.gitignore`
- [ ] Never share keystore password
- [ ] Use different keystores for different apps
- [ ] Store GitHub secrets securely
- [ ] Rotate keys every few years

## Troubleshooting

### "keystore was tampered with, or password was incorrect"
- Check password in `keystore.properties`
- Ensure keystore file is not corrupted

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
- App is signed with different key than installed version
- Uninstall old version first, or use same keystore

### CI build fails with signing error
- Verify all 4 GitHub secrets are set correctly
- Check base64 encoding has no line breaks
- Ensure `keystore.properties` is created before build step

## Current Status

**Current Setup:** Debug-signed for development
**Production Ready:** After completing Steps 1-3

The current debug signature allows:
✅ Install on any device
✅ Development and testing
✅ Side-loading to users
❌ Google Play Store publishing
❌ Updating existing installs (different signature)

---

**Next Steps:** Follow Steps 1-3 when ready to publish to Play Store.
