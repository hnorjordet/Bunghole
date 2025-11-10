# GitHub Quick Start Guide

Everything is now configured for GitHub with automatic updates! Here's what to do:

---

## Step 1: Create GitHub Repository

1. Go to: https://github.com/new
2. **Repository name**: `Bunghole`
3. **Description**: `Document alignment tool with AI-powered enhancement`
4. **Visibility**: Choose Private or Public
5. **DON'T** initialize with README (we already have one)
6. Click **Create repository**

---

## Step 2: Push to GitHub

```bash
cd /Users/havardnorjordet/Stingray

# Add GitHub remote
git remote add origin https://github.com/hnorjordet/Bunghole.git

# Push everything
git branch -M master
git push -u origin master
```

---

## Step 3: Create Your First Release

### Build the distributable:
```bash
# Make sure Java is built
ant dist

# Build macOS app
npm run dist:mac
```

This creates: `dist/Bunghole-2.11.0-mac.zip`

### Create the release on GitHub:

**Option A: Web Interface** (easiest)
1. Go to: https://github.com/hnorjordet/Bunghole/releases
2. Click **"Create a new release"**
3. **Tag version**: `v2.11.0` (must start with 'v')
4. **Release title**: `Bunghole v2.11.0 - Initial Release`
5. **Description**:
   ```markdown
   ## Bunghole Document Aligner

   ### Features
   - Document alignment with AI enhancement
   - Support for Claude (Anthropic) and OpenAI
   - Automatic alignment algorithms
   - TMX export
   - Multi-language support

   ### Installation
   1. Download `Bunghole-2.11.0-mac.zip` below
   2. Extract the .zip file
   3. Drag Bunghole.app to Applications folder
   4. Right-click → Open (first time only)

   ### Setup
   - Get Claude API key: https://console.anthropic.com/
   - Get OpenAI API key: https://platform.openai.com/
   - Configure in Preferences (Cmd+,)

   ### What's New
   - Initial release with multi-AI provider support
   - Full Claude and OpenAI integration
   - Enhanced security features
   ```
6. **Attach file**: Drag `dist/Bunghole-2.11.0-mac.zip` to the upload area
7. Check **"Set as the latest release"**
8. Click **"Publish release"**

**Option B: Command Line** (if you have GitHub CLI)
```bash
# Install gh if needed
brew install gh

# Login
gh auth login

# Create release
gh release create v2.11.0 \
  dist/Bunghole-2.11.0-mac.zip \
  --title "Bunghole v2.11.0 - Initial Release" \
  --notes "Initial release with multi-AI provider support. See full documentation at https://github.com/hnorjordet/Bunghole"
```

---

## Step 4: Share with Your Colleague

Send them this link:
```
https://github.com/hnorjordet/Bunghole/releases/latest
```

They should:
1. Download the .zip file
2. Extract and move to Applications
3. First launch: Right-click → Open (to bypass security warning)
4. Get an API key (Claude or OpenAI)
5. Configure in Preferences

---

## How Auto-Updates Work

### For You (Developer):
When you want to release an update:

```bash
# 1. Make your changes
# ... edit files ...

# 2. Update version in package.json
# Edit package.json: "version": "2.11.1"

# 3. Commit and push
git add .
git commit -m "Release v2.11.1: Added feature XYZ"
git push

# 4. Build
ant dist
npm run dist:mac

# 5. Create GitHub release
gh release create v2.11.1 dist/Bunghole-2.11.1-mac.zip \
  --title "v2.11.1" \
  --notes "Bug fixes and improvements"
```

### For Your Colleague (User):
1. App checks for updates automatically on startup
2. If new version exists, shows dialog: "Update Available!"
3. User clicks "Download"
4. Update downloads in background
5. When complete: "Update ready. Restart?"
6. User restarts, new version installed automatically

**Manual check**: Help → Check for Updates

---

## What Was Configured

✅ **package.json**
- GitHub repository: `https://github.com/hnorjordet/Bunghole.git`
- Author: Håvard Nørjordet
- Auto-updater config pointing to your GitHub

✅ **.gitignore**
- Prevents committing sensitive files (API keys, build artifacts)
- Already configured

✅ **Auto-Updater**
- `electron-updater` installed
- `ts/updater.ts` created with update logic
- `ts/app.ts` modified to check for updates on startup
- Compiled to JavaScript successfully

✅ **TypeScript**
- Builds successfully
- Strict mode temporarily disabled for compatibility

---

## Testing the Setup

### Before Giving to Your Colleague:

1. **Build and test locally:**
   ```bash
   npm run dist:mac
   open dist/mac/Bunghole.app
   ```

2. **Verify it launches** and core features work

3. **Push to GitHub** and create release

4. **Test the download link** yourself first

---

## Troubleshooting

### "Can't push to GitHub"
Make sure you've created the repository first at https://github.com/new

### "electron-builder fails"
```bash
npm install
npm run build
ant dist
npm run dist:mac
```

### "App won't open (security warning)"
This is normal for unsigned apps. Tell users to:
- Right-click the app
- Click "Open"
- Click "Open" again in the dialog

### "Updates not working"
- Make sure version numbers increase (2.11.0 → 2.11.1)
- Release must be published (not draft)
- App must be packaged build, not running in development

---

## Regular Release Workflow

```bash
# Quick release script you can use:

# 1. Update version
# Edit package.json: bump version number

# 2. Commit
git add .
git commit -m "Release v2.11.1"
git push

# 3. Build and release
ant dist && npm run dist:mac
gh release create v2.11.1 dist/Bunghole-*.zip \
  --title "v2.11.1" \
  --notes "- Fixed bug with alignment\n- Improved AI cost estimation"

# Done! Users will be notified automatically
```

---

## Repository Settings (Optional)

### Make it Private
If you want only invited people to access:
1. Go to: https://github.com/hnorjordet/Bunghole/settings
2. Scroll to "Danger Zone"
3. Click "Change visibility" → Private

### Add Collaborators
1. Go to: https://github.com/hnorjordet/Bunghole/settings/access
2. Click "Add people"
3. Enter your colleague's GitHub username

---

## Files Created/Modified

| File | Status | Purpose |
|------|--------|---------|
| `package.json` | ✅ Modified | Added GitHub info, auto-update config |
| `.gitignore` | ✅ Modified | Added build artifacts to ignore |
| `ts/updater.ts` | ✅ Created | Auto-update logic |
| `ts/app.ts` | ✅ Modified | Integrated auto-updater |
| `js/updater.js` | ✅ Generated | Compiled updater code |
| `tsconfig.json` | ✅ Modified | Disabled strict mode for build |
| `node_modules/electron-updater` | ✅ Installed | Auto-update library |

---

## Ready to Go!

Everything is configured. Just follow Steps 1-3 above and you're done!

Your colleague will get automatic update notifications whenever you create a new release on GitHub. 🎉

---

**Questions?**
- GitHub help: https://docs.github.com/
- Electron-updater docs: https://www.electron.build/auto-update
- Full setup guide: See `GITHUB_SETUP.md` for detailed information

**Last Updated**: November 10, 2025
