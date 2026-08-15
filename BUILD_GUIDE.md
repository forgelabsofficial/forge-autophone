# AutoPhone Build Guide

> **Building the AutoPhone AAR library**

---

## Prerequisites

### System Requirements
- **RAM**: 8 GB minimum, 16 GB recommended
- **Java**: JDK 17
- **Android SDK**: API 26-35
- **Gradle**: 8.7 (auto-downloaded by wrapper)

### Software Setup
1. **Install Java JDK 17**
   - Windows: Download from [Adoptium](https://adoptium.net/)
   - Linux: `sudo apt install openjdk-17-jdk`
   - Mac: `brew install openjdk@17`

2. **Install Android SDK** (optional for local builds, required for development)
   - Download [Android Studio](https://developer.android.com/studio)
   - Or use command-line tools

3. **Set ANDROID_HOME** (if building locally)
   ```bash
   # Windows
   setx ANDROID_HOME "C:\Android\SDK"
   
   # Linux/Mac
   export ANDROID_HOME=$HOME/Android/Sdk
   ```

---

## Building Locally (If You Have 16+ GB RAM)

### Quick Build
```bash
# Windows
gradlew.bat assembleRelease

# Linux/Mac
./gradlew assembleRelease
```

### Full Build with Tests
```bash
# Windows
gradlew.bat clean test assembleRelease

# Linux/Mac
./gradlew clean test assembleRelease
```

### Build Output
AAR file will be in:
```
build/outputs/aar/forge-autophone-release.aar
```

---

## Building on GitHub Actions (Recommended)

**For systems with limited resources (< 16 GB RAM), use GitHub Actions.**

### Setup

1. **Create GitHub Repository**
   ```bash
   git init
   git add .
   git commit -m "Initial commit: AutoPhone complete implementation"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/forge-autophone.git
   git push -u origin main
   ```

2. **GitHub Actions Will Automatically**:
   - Run lint checks
   - Execute unit tests
   - Build release AAR
   - Upload artifacts

### Download Built AAR

1. Go to your repository on GitHub
2. Click **Actions** tab
3. Click on the latest successful workflow run
4. Scroll to **Artifacts** section
5. Download `forge-autophone-release.aar`

---

## Build Variants

### Debug Build
```bash
gradlew assembleDebug
```
- Includes debug symbols
- No ProGuard optimization
- Larger file size
- Good for development

### Release Build
```bash
gradlew assembleRelease
```
- ProGuard optimized
- Smaller file size
- Production-ready
- Symbols removed

---

## Troubleshooting

### Out of Memory Error
**Symptom**: `OutOfMemoryError: Java heap space`

**Solution 1**: Increase heap size in `gradle.properties`
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

**Solution 2**: Use GitHub Actions instead (recommended for < 16 GB RAM)

### Gradle Daemon Issues
**Symptom**: Build hangs or fails with daemon errors

**Solution**: Stop all Gradle daemons
```bash
gradlew --stop
```

### SDK Not Found
**Symptom**: `SDK location not found`

**Solution**: Set ANDROID_HOME or create `local.properties`:
```properties
sdk.dir=C\:\\Android\\SDK
```

### Dependency Download Failures
**Symptom**: `Could not resolve all dependencies`

**Solution**: Check internet connection and try:
```bash
gradlew clean --refresh-dependencies
```

### Build Too Slow
**Solution**: Enable parallel builds and caching (already configured in gradle.properties)
```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

---

## GitHub Actions CI/CD

### Workflow Features
- ✅ Automatic linting on every push
- ✅ Unit tests on every push
- ✅ Release AAR build on every push
- ✅ Instrumented tests on main branch
- ✅ Artifact retention for 14 days

### Workflow File
Located at: `.github/workflows/ci.yml`

### Triggering Builds
- **Push to main/develop**: Full CI pipeline
- **Pull request**: Lint + tests only
- **Manual trigger**: Use "Run workflow" in Actions tab

### Viewing Results
1. Go to **Actions** tab
2. Click on workflow run
3. View logs for each job
4. Download artifacts from successful runs

---

## Build Artifacts

### AAR Contents
```
forge-autophone-release.aar
├── AndroidManifest.xml          ← Service declarations
├── classes.jar                  ← Compiled code (ProGuarded)
├── res/                         ← Resources
│   ├── values/
│   └── xml/
├── proguard.txt                 ← ProGuard rules
└── libs/                        ← Dependencies
```

### Size Information
- **Debug AAR**: ~50-60 MB
- **Release AAR**: ~45-50 MB (ProGuard optimized)
- Includes:
  - ML Kit Text Recognition (~18 MB)
  - OpenCV Android (~23 MB)
  - AutoPhone code (~2-3 MB)

---

## Integration

### Adding to Your App

1. **Copy AAR to libs folder**
   ```
   YourApp/app/libs/forge-autophone-release.aar
   ```

2. **Add dependency in app/build.gradle**
   ```gradle
   dependencies {
       implementation files('libs/forge-autophone-release.aar')
   }
   ```

3. **Sync project**

---

## Development Builds

### Clean Build
```bash
gradlew clean
gradlew assembleDebug
```

### Quick Iterative Build
```bash
gradlew assembleDebug --no-daemon
```

### With Specific Tests
```bash
gradlew test --tests "*SelfHealingSelectorTest"
```

---

## Performance Tips

### Speed Up Builds
1. **Use Gradle daemon** (enabled by default)
2. **Enable parallel builds** (already configured)
3. **Use configuration cache** (already configured)
4. **Increase heap size** (if you have RAM)
5. **Exclude unnecessary tasks**
   ```bash
   gradlew assembleRelease -x lint -x test
   ```

### Reduce Memory Usage
1. **Use `--no-daemon`** for one-off builds
2. **Close IDE** during command-line builds
3. **Limit parallel workers**
   ```properties
   org.gradle.workers.max=2
   ```

---

## Recommended: GitHub Actions

**For most users**, especially with limited resources:

1. ✅ **Push to GitHub**
2. ✅ **Let Actions build automatically**
3. ✅ **Download AAR artifact**
4. ✅ **No local resource usage**
5. ✅ **Consistent environment**
6. ✅ **Free for public repos**

This approach is **faster**, **more reliable**, and **doesn't stress your local machine**.

---

## Build Commands Reference

| Command | Description | Time | Memory |
|---------|-------------|------|--------|
| `gradlew clean` | Clean build artifacts | 5s | Low |
| `gradlew assembleDebug` | Build debug AAR | 2-5 min | 4-6 GB |
| `gradlew assembleRelease` | Build release AAR | 3-6 min | 4-6 GB |
| `gradlew test` | Run unit tests | 1-2 min | 2-4 GB |
| `gradlew lint` | Run lint checks | 30s-1min | 2-3 GB |
| `gradlew build` | Full build + test | 5-10 min | 6-8 GB |

---

## Questions?

- Check [GitHub Issues](https://github.com/YOUR_USERNAME/forge-autophone/issues)
- Review workflow logs in Actions tab
- See [README.md](README.md) for project documentation

---

**Recommended**: Use GitHub Actions for reliable, resource-efficient builds! 🚀