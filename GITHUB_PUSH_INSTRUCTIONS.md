# Push AutoPhone to GitHub

> **Quick guide to push AutoPhone to GitHub for automated building**

---

## Why Push to GitHub?

Your system specs:
- **RAM**: 8 GB (minimum for Android builds)
- **CPU**: Intel Core i5-4210U (older generation)

**Building locally will likely:**
- ❌ Take 20-30+ minutes
- ❌ Use 6-8 GB RAM (90%+ of your available)
- ❌ Possibly fail due to memory constraints
- ❌ Make your laptop very slow/unresponsive

**Using GitHub Actions will:**
- ✅ Take 5-10 minutes
- ✅ Use GitHub's powerful servers
- ✅ Build reliably every time
- ✅ Keep your laptop free for other work
- ✅ **FREE for public repositories**

---

## Step-by-Step Instructions

### 1. Check Git Status
```bash
cd c:\Users\user\Documents\projects\forge-os\forge-autophone
git status
```

### 2. Initialize Git (if not already done)
```bash
git init
```

### 3. Add All Files
```bash
git add .
```

### 4. Commit Everything
```bash
git commit -m "Complete AutoPhone implementation - All 4 phases (78 tools)"
```

### 5. Create GitHub Repository

**Option A: Via GitHub Website (Easiest)**
1. Go to https://github.com/new
2. Repository name: `forge-autophone`
3. Description: "Forge OS Accessibility Layer - Android automation with ML-powered intelligence"
4. Choose **Public** (for free Actions) or **Private** (if you prefer)
5. **DO NOT** initialize with README, .gitignore, or license (we already have these)
6. Click "Create repository"

**Option B: Via GitHub CLI**
```bash
gh repo create forge-autophone --public --source=. --remote=origin
```

### 6. Add Remote Origin
```bash
# Replace YOUR_USERNAME with your actual GitHub username
git remote add origin https://github.com/YOUR_USERNAME/forge-autophone.git
```

### 7. Set Default Branch to Main
```bash
git branch -M main
```

### 8. Push to GitHub
```bash
git push -u origin main
```

**If prompted for credentials:**
- Username: Your GitHub username
- Password: Use a **Personal Access Token** (not your password)
  - Generate at: https://github.com/settings/tokens
  - Select: `repo` scope
  - Copy token and paste as password

---

## What Happens After Push?

### Automatic GitHub Actions Workflow

1. **GitHub detects the push**
2. **Workflow starts automatically** (see `.github/workflows/ci.yml`)
3. **Three parallel jobs run:**
   - **Lint**: Code quality checks
   - **Test**: Unit tests
   - **Build**: Compiles release AAR

### Timeline
- **Minutes 0-2**: Setup (checkout, install JDK, download dependencies)
- **Minutes 2-5**: Run lint and tests
- **Minutes 5-8**: Build release AAR
- **Minutes 8-10**: Upload artifacts

### View Progress
1. Go to your repository: `https://github.com/YOUR_USERNAME/forge-autophone`
2. Click **Actions** tab
3. Click on the running workflow
4. Watch real-time logs

---

## Download the Built AAR

### After Build Completes

1. **Go to Actions tab**
2. **Click on the successful workflow run** (green checkmark ✓)
3. **Scroll down to "Artifacts" section**
4. **Download**: `forge-autophone-release.aar`
5. **File will be**: `forge-autophone-release.zip`
6. **Extract** to get: `forge-autophone-release.aar`

### AAR Location
The downloaded file is your production-ready Android library!
```
forge-autophone-release.aar  (~45-50 MB)
```

---

## Verify the AAR

### Check Contents
```bash
# Windows (using 7-Zip or similar)
7z l forge-autophone-release.aar

# Linux/Mac
unzip -l forge-autophone-release.aar
```

### Expected Contents
- ✅ `AndroidManifest.xml`
- ✅ `classes.jar` (ProGuard optimized)
- ✅ `res/` directory
- ✅ `R.txt`
- ✅ ML Kit and OpenCV libraries

---

## Integration into Forge OS

### Add AAR to Main App

1. **Copy AAR to Forge OS**
   ```bash
   copy forge-autophone-release.aar C:\path\to\forge-os-main\app\libs\
   ```

2. **Update app/build.gradle**
   ```gradle
   dependencies {
       implementation files('libs/forge-autophone-release.aar')
       
       // AutoPhone dependencies (if not already included)
       implementation "com.google.mlkit:text-recognition:16.0.0"
       implementation "org.opencv:opencv-android:4.8.0"
       implementation "com.google.dagger:hilt-android:2.51.1"
   }
   ```

3. **Sync Gradle**

4. **Use AutoPhone**
   ```kotlin
   val service = AutoPhoneAccessibilityService.instance
   if (service != null) {
       val tools = AutoPhoneToolRegistry(service)
       // Use any of the 78 tools!
       tools.tap(540f, 1200f)
   }
   ```

---

## Future Workflow

### Making Changes

1. **Edit code locally**
2. **Commit changes**
   ```bash
   git add .
   git commit -m "Add new feature X"
   ```
3. **Push to GitHub**
   ```bash
   git push
   ```
4. **GitHub Actions rebuilds automatically**
5. **Download new AAR**

### Benefits
- ✅ Version control (git history)
- ✅ Automatic builds (no manual compile)
- ✅ Artifact history (14 days retention)
- ✅ CI/CD pipeline (test before build)
- ✅ Collaboration ready (if adding team members)

---

## Troubleshooting

### Push Fails: "Authentication Failed"
**Solution**: Use Personal Access Token
1. Go to https://github.com/settings/tokens
2. Generate new token (classic)
3. Select `repo` scope
4. Use token as password when pushing

### Push Fails: "Remote Already Exists"
**Solution**: Update remote URL
```bash
git remote set-url origin https://github.com/YOUR_USERNAME/forge-autophone.git
```

### Build Fails on GitHub
**Solution**: Check Actions logs
1. Go to Actions tab
2. Click failed run
3. Expand failed step
4. Read error message
5. Fix issue locally, commit, push again

### Can't Find AAR Artifact
**Solution**: Wait for build to complete
- Green checkmark (✓) = Success, artifacts available
- Yellow circle = Still running, wait
- Red X = Failed, check logs

---

## Summary

### The Plan
1. ✅ Push code to GitHub
2. ✅ Let GitHub Actions build (5-10 minutes)
3. ✅ Download AAR artifact
4. ✅ Integrate into Forge OS main app

### Why This Works Better
- **Faster**: GitHub servers are powerful
- **Reliable**: Consistent build environment
- **Convenient**: Build in background while you work
- **Free**: Public repos get unlimited Actions minutes
- **Professional**: Industry-standard CI/CD practice

---

## Ready to Push?

Run these commands in order:

```bash
# 1. Navigate to project
cd c:\Users\user\Documents\projects\forge-os\forge-autophone

# 2. Check status
git status

# 3. Add all files
git add .

# 4. Commit
git commit -m "Complete AutoPhone: All 4 phases implemented (78 tools)"

# 5. Create repo on GitHub (via website), then:
git remote add origin https://github.com/YOUR_USERNAME/forge-autophone.git

# 6. Push
git branch -M main
git push -u origin main
```

Then watch the magic happen on GitHub Actions! 🚀

---

**Next**: After AAR is built, integrate into forge-os-main app!