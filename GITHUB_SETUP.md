# GitHub Setup & Auto-Updates Guide

## 🎯 Goal
Set up Bunghole on GitHub with automatic update notifications for your beta testers.

---

## Part 1: GitHub Repository Setup

### 1. Create GitHub Repository

1. Go to https://github.com/new
2. Repository name: `Bunghole`
3. Description: `Document alignment tool with AI-powered enhancement`
4. Visibility:
   - **Private** (recommended for beta) - Only you and invited collaborators
   - **Public** - Anyone can see (fine if you're comfortable)
5. ✅ Initialize with README (unchecked - we already have one)
6. Click **Create repository**

### 2. Update package.json

Replace the placeholder values in [package.json](package.json):

```json
"author": {
    "name": "Håvard Nørjordet",
    "email": "your-actual-email@example.com",
    "url": "https://github.com/YOUR_USERNAME"
},
"repository": {
    "type": "git",
    "url": "https://github.com/YOUR_USERNAME/Bunghole.git"
}
```

Replace `YOUR_USERNAME` with your actual GitHub username.

### 3. Push to GitHub

```bash
# Initialize git (if not already done)
cd /Users/havardnorjordet/Stingray

# Add remote
git remote add origin https://github.com/YOUR_USERNAME/Bunghole.git

# Push everything
git push -u origin master
```

---

## Part 2: Auto-Update Configuration

### 1. Install electron-updater

```bash
npm install electron-updater --save
```

### 2. Update package.json - Add publish config

Add this to your `package.json` inside the `"build"` section:

```json
"build": {
    "appId": "com.bunghole.app",
    "productName": "Bunghole",
    "publish": {
        "provider": "github",
        "owner": "YOUR_USERNAME",
        "repo": "Bunghole"
    },
    "mac": {
        "category": "public.app-category.productivity",
        "target": ["zip"],
        ...
    }
}
```

### 3. Create Update Check Code

Create a new file: `ts/updater.ts`

```typescript
/*******************************************************************************
 * Copyright (c) 2008 - 2025 Håvard Nørjordet.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Håvard Nørjordet - initial API and implementation
 *******************************************************************************/

import { dialog } from 'electron';
import { autoUpdater } from 'electron-updater';

export class Updater {

    static checkForUpdates() {
        // Configure auto-updater
        autoUpdater.autoDownload = false;
        autoUpdater.autoInstallOnAppQuit = true;

        // Check for updates
        autoUpdater.checkForUpdates();

        // Update available
        autoUpdater.on('update-available', (info) => {
            dialog.showMessageBox({
                type: 'info',
                title: 'Update Available',
                message: `Version ${info.version} is available!`,
                detail: 'Would you like to download it now?',
                buttons: ['Download', 'Later']
            }).then((result) => {
                if (result.response === 0) {
                    autoUpdater.downloadUpdate();
                }
            });
        });

        // Update not available
        autoUpdater.on('update-not-available', () => {
            console.log('App is up to date');
        });

        // Update downloaded
        autoUpdater.on('update-downloaded', () => {
            dialog.showMessageBox({
                type: 'info',
                title: 'Update Ready',
                message: 'Update downloaded. Restart to install?',
                buttons: ['Restart', 'Later']
            }).then((result) => {
                if (result.response === 0) {
                    autoUpdater.quitAndInstall();
                }
            });
        });

        // Error handling
        autoUpdater.on('error', (err) => {
            console.error('Update error:', err);
        });
    }
}
```

### 4. Add Update Check to Main App

In `ts/main.ts`, add update check when app starts:

```typescript
import { Updater } from './updater';

// In your app.whenReady() or similar:
app.whenReady().then(() => {
    // ... your existing code ...

    // Check for updates (after a short delay to not slow down startup)
    setTimeout(() => {
        Updater.checkForUpdates();
    }, 3000);
});
```

### 5. Add Manual Update Check

In your menu (probably in `app.ts`), add a "Check for Updates" menu item:

```typescript
{
    label: 'Check for Updates',
    click: () => {
        Updater.checkForUpdates();
    }
}
```

---

## Part 3: Creating Releases

### When You Want to Release an Update:

#### 1. Update Version Number

Edit `package.json`:
```json
"version": "2.11.1"  // or whatever the new version is
```

#### 2. Commit Changes

```bash
git add .
git commit -m "Release v2.11.1 - [brief description of changes]"
git push
```

#### 3. Build Distribution

```bash
# Build for macOS
npm run build
ant dist
npm run dist:mac
```

This creates: `dist/Bunghole-2.11.1-mac.zip`

#### 4. Create GitHub Release

**Via GitHub Web Interface:**

1. Go to your repo: `https://github.com/YOUR_USERNAME/Bunghole`
2. Click **Releases** → **Create a new release**
3. **Tag version**: `v2.11.1` (must start with 'v')
4. **Release title**: `Bunghole v2.11.1`
5. **Description**:
   ```markdown
   ## What's New
   - Added OpenAI support
   - Fixed bug with...
   - Improved performance for...

   ## Installation
   Download the .zip file below and extract to Applications folder.
   ```
6. **Attach files**: Drag `Bunghole-2.11.1-mac.zip` to upload
7. Check **Set as the latest release**
8. Click **Publish release**

**Via Command Line (Alternative):**

```bash
# Install GitHub CLI if not already
brew install gh

# Create release with file
gh release create v2.11.1 \
  dist/Bunghole-2.11.1-mac.zip \
  --title "Bunghole v2.11.1" \
  --notes "Release notes here"
```

---

## Part 4: How Users Get Updates

### Automatic Check
- App checks for updates on startup (after 3 seconds)
- If newer version exists, shows dialog: "Update available!"
- User clicks "Download"
- Update downloads in background
- When complete: "Update ready. Restart to install?"
- User restarts, update installs automatically

### Manual Check
- User goes to **Help → Check for Updates**
- Same flow as above

### User Experience:

```
┌─────────────────────────────────────┐
│         Update Available            │
├─────────────────────────────────────┤
│ Version 2.11.1 is available!        │
│                                     │
│ Would you like to download it now?  │
│                                     │
│  [Download]           [Later]       │
└─────────────────────────────────────┘
```

---

## Part 5: .gitignore Configuration

Make sure these are in your `.gitignore`:

```gitignore
# Build artifacts
dist/
out/
build/
*.app
*.dmg
*.zip

# Dependencies
node_modules/

# Configuration (sensitive)
config.properties

# macOS
.DS_Store

# IDE
.vscode/
.idea/

# Logs
*.log
```

---

## Part 6: First Release Checklist

Before pushing to GitHub:

- [ ] Update `package.json` with your GitHub username
- [ ] Add `.gitignore` with sensitive files
- [ ] Remove any API keys from code
- [ ] Generate PDF documentation
- [ ] Test build locally
- [ ] Create GitHub repository
- [ ] Push code to GitHub
- [ ] Create first release (v2.11.0)
- [ ] Test download from GitHub
- [ ] Send release link to beta tester

---

## Part 7: Beta Tester Setup

### For Your Colleague:

**First Time:**
1. Download from: `https://github.com/YOUR_USERNAME/Bunghole/releases/latest`
2. Extract and install normally
3. On next app launch, will automatically check for updates

**When Updates Are Available:**
1. App shows notification automatically
2. Click "Download"
3. Wait for download (shows in background)
4. Click "Restart" when prompted
5. Done! New version installed

**Manual Check:**
- Help → Check for Updates

---

## Part 8: Release Workflow Example

### Typical Update Cycle:

```bash
# 1. Make changes to code
# ... edit files ...

# 2. Test locally
npm run build
npm start

# 3. Update version
# Edit package.json: "version": "2.11.1"

# 4. Commit
git add .
git commit -m "Add new feature: multi-language support"
git push

# 5. Build distribution
npm run build && ant dist && npm run dist:mac

# 6. Create release on GitHub
gh release create v2.11.1 \
  dist/Bunghole-2.11.1-mac.zip \
  --title "Bunghole v2.11.1" \
  --notes "Added multi-language support"

# 7. Done! Users will be notified
```

---

## Part 9: Troubleshooting

### "Update check failed"
- Check internet connection
- Verify GitHub repository is public or user has access
- Check `package.json` repository URL is correct

### "Download failed"
- Release file might be missing
- File name doesn't match expected pattern
- GitHub API rate limit (rare)

### "Update doesn't appear"
- Version number must be higher than current
- Must create GitHub Release (not just git tag)
- Release must be published (not draft)

### User Not Getting Notifications
- Check if auto-updater code is running (look for console logs)
- Verify `publish` config in package.json
- Ensure app is not running in development mode

---

## Part 10: Advanced Options

### Private Repository
If using private repo:
1. Generate GitHub Personal Access Token
2. Add to environment: `GH_TOKEN=your_token`
3. electron-updater will use it for authentication

### Beta Releases
For testing before wide release:
```bash
gh release create v2.11.1-beta \
  dist/Bunghole-2.11.1-mac.zip \
  --prerelease \
  --title "Bunghole v2.11.1 Beta"
```

Users won't be auto-notified of pre-releases.

### Release Notes from CHANGELOG
```bash
gh release create v2.11.1 \
  dist/Bunghole-2.11.1-mac.zip \
  --title "Bunghole v2.11.1" \
  --notes-file CHANGELOG.md
```

---

## Benefits Summary

### For You:
- ✅ No more emailing ZIP files
- ✅ Version history tracked automatically
- ✅ Easy to roll back if issues
- ✅ Professional distribution workflow
- ✅ Issue tracking built-in

### For Your Colleague:
- ✅ Automatic update notifications
- ✅ One-click updates
- ✅ Always knows when new version available
- ✅ Can report issues via GitHub Issues
- ✅ Can see what changed in each version

---

## Security Notes

### Don't Commit:
- ❌ API keys
- ❌ config.properties with real keys
- ❌ Personal email addresses in code
- ❌ Sensitive test data

### Do Commit:
- ✅ config.properties.example (no real keys)
- ✅ Source code
- ✅ Documentation
- ✅ Build scripts

---

## Quick Start Commands

```bash
# First time setup
git remote add origin https://github.com/YOUR_USERNAME/Bunghole.git
git push -u origin master

# Regular release workflow
npm run build && ant dist && npm run dist:mac
git add . && git commit -m "Release v2.11.1" && git push
gh release create v2.11.1 dist/Bunghole-*.zip --title "v2.11.1"

# Quick update check
gh release list
```

---

**Ready to go!** Would you like me to help set up the auto-updater code now?
