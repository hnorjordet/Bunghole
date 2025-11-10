# Bunghole - Shipping Readiness Checklist

**Version**: 2.11.0
**Date**: November 10, 2025
**Target**: macOS Beta Testing

---

## ✅ Completed Items

### Core Functionality
- [x] Project renamed from Stingray to Bunghole
- [x] All Java packages updated (`com.maxprograms.bunghole`)
- [x] All TypeScript files updated
- [x] All HTML/CSS references updated
- [x] JAR file renamed to `bunghole.jar`
- [x] Copyright notices updated to "Håvard Nørjordet"
- [x] Compilation successful (no errors)
- [x] Distribution build successful (`ant dist`)

### AI Provider System
- [x] Multi-provider support (Claude + OpenAI)
- [x] AIProvider interface implemented
- [x] OpenAI service fully functional
- [x] Claude adapter working
- [x] Cost estimation for both providers
- [x] Configuration system for API keys
- [x] Environment variable support

### Security
- [x] Localhost-only binding (127.0.0.1)
- [x] Input validation framework (SecurityUtils)
- [x] Externalized configuration
- [x] .gitignore updated for sensitive files
- [x] Security documentation (SECURITY.md)

### Documentation
- [x] User guide created (BUNGHOLE_USER_GUIDE.md)
- [x] AI providers guide (AI_PROVIDERS.md)
- [x] Technical implementation docs (MULTI_AI_PROVIDER_IMPLEMENTATION.md)
- [x] Configuration example (config.properties.example)
- [x] Security documentation
- [x] README updated

---

## ⚠️ Items Needing Attention Before Beta

### 1. PDF Documentation (REQUIRED)
The app expects `bunghole_en.pdf` but only `stingray_en.pdf` exists.

**Action Required:**
```bash
# Generate PDF from markdown
./generate_pdf.sh
```

This will create:
- `bunghole_en.pdf` - New user guide
- Backup old PDFs automatically

**Alternative (Manual):**
If pandoc is not installed:
```bash
# On macOS
brew install pandoc
brew install --cask mactex-no-gui

# Then generate
./generate_pdf.sh
```

---

### 2. Package.json Updates (COMPLETED)
✅ Author updated to "Håvard Nørjordet"
⚠️ Email and URL are placeholders - update these:

**Current:**
```json
"author": {
    "name": "Håvard Nørjordet",
    "email": "your-email@example.com",
    "url": "https://github.com/yourusername"
}
```

**Update to your actual details:**
- Your email address
- Your GitHub username/URL (or personal website)

---

### 3. Configuration File Setup
✅ `config.properties.example` exists

**For Beta Tester:**
Your friend will need to create `config.properties` with either:

**Option A: Claude (Recommended)**
```properties
# AI Provider
ai.provider=claude

# Claude API Key
claude.apiKey=sk-ant-your-key-here
claude.model=claude-sonnet-4-20250514
```

**Option B: OpenAI**
```properties
# AI Provider
ai.provider=openai

# OpenAI API Key
openai.apiKey=sk-your-key-here
openai.model=gpt-4-turbo-preview
```

---

### 4. Icon/Assets (OPTIONAL but NICE)
Current status: Old Stingray icon may still be in use

**Check:**
```bash
ls -la icons/
```

**If needed:** Replace icon files with Bunghole-branded icons before packaging.

---

## 📦 Creating macOS Distribution

### Prerequisites
```bash
# Install Node.js dependencies
npm install

# Ensure TypeScript is compiled
npm run build

# Ensure Java is built
ant dist
```

### Build macOS App
```bash
# Build for macOS (creates .app bundle)
npm run dist:mac
```

This creates:
- `dist/Bunghole-2.11.0-mac.zip` - Distributable app

### What Gets Packaged
- Electron app wrapper
- All JavaScript/TypeScript (compiled)
- HTML/CSS files
- Java JAR files
- Native binaries (hunalign)
- Dictionaries
- PDF documentation

---

## 🧪 Testing Checklist (Before Giving to Friend)

### Basic Functionality
- [ ] App launches without errors
- [ ] Main window appears correctly
- [ ] Can open/create alignment files
- [ ] Can load source and target files
- [ ] Manual alignment works (move, split, merge)
- [ ] Save/export TMX works

### AI Features
- [ ] Preferences dialog opens
- [ ] Can enter API key
- [ ] Can select AI provider (Claude/OpenAI)
- [ ] Cost estimation dialog works
- [ ] AI enhancement processes segments
- [ ] Confidence scores display correctly
- [ ] Error handling for invalid API keys

### Documentation
- [ ] Help menu opens PDF correctly
- [ ] About dialog shows correct info
- [ ] PDF guide is readable and complete

### macOS Specific
- [ ] App icon displays correctly
- [ ] File associations work (if any)
- [ ] No permission errors
- [ ] Keyboard shortcuts work (Cmd+O, Cmd+S, etc.)

---

## 📋 Beta Tester Instructions (Draft)

### Installation
1. Download `Bunghole-2.11.0-mac.zip`
2. Unzip the file
3. Drag `Bunghole.app` to Applications folder
4. First launch: Right-click → Open (bypass Gatekeeper)

### First-Time Setup
1. Open Preferences (Cmd+,)
2. Choose AI Provider:
   - **Claude**: Get key from https://console.anthropic.com/
   - **OpenAI**: Get key from https://platform.openai.com/
3. Paste API key
4. Save preferences

### Basic Usage
1. **File → New** - Create new alignment
2. Load source document (File → Add Source)
3. Load target document (File → Add Target)
4. **Alignment → Auto-align** - Initial alignment
5. **AI → Estimate Cost** - Check cost before AI enhancement
6. **AI → Improve Alignment** - Let AI analyze uncertain segments
7. Review and manually adjust as needed
8. **File → Export TMX** - Save translation memory

### Getting Help
- **Help → User Guide** - Full documentation
- **Help → About** - Version info
- Report issues to: [your contact method]

---

## 🚀 Ready to Ship When:

### Must Have
- [ ] PDF documentation generated (`bunghole_en.pdf` exists)
- [ ] package.json author info updated (email, URL)
- [ ] Basic testing completed (app launches, core features work)
- [ ] At least one AI provider tested with real API key

### Should Have
- [ ] Icon updated (if desired)
- [ ] Beta tester instructions written
- [ ] Known issues documented
- [ ] Support contact info provided

### Nice to Have
- [ ] Spanish documentation (if needed)
- [ ] Video walkthrough/demo
- [ ] Example files for testing
- [ ] Troubleshooting guide

---

## 🐛 Known Issues/Limitations

### Security
- API keys stored in plain text (use environment variables for added security)
- No automatic key rotation
- No built-in usage limits

### AI Features
- Cost estimation is approximate (actual costs may vary)
- No caching (duplicate requests cost full price)
- No batch processing optimization yet
- Single provider per session (restart required to switch)

### macOS Packaging
- Not code-signed (will show "unidentified developer" warning)
- Not notarized (requires right-click → Open first time)
- No auto-update mechanism

---

## 📞 Support Preparation

### What Your Friend Might Ask

**Q: "It won't open / shows security warning"**
A: Right-click the app → Open → Open anyway. This is normal for unsigned apps.

**Q: "Where do I get an API key?"**
A:
- Claude: https://console.anthropic.com/ → API Keys → Create Key
- OpenAI: https://platform.openai.com/ → Settings → API Keys → Create

**Q: "How much will this cost?"**
A:
- Always use "Estimate Cost" first (Cmd+R)
- Claude: ~$1-2 per 1000 segments
- OpenAI GPT-4: ~$3-4 per 1000 segments
- OpenAI GPT-3.5: ~$0.20 per 1000 segments

**Q: "The AI made mistakes"**
A: AI suggestions are not perfect - always review and manually correct as needed.

**Q: "Can I use it offline?"**
A: Manual alignment yes, AI features no (require API connection).

---

## 🔄 After Beta Testing

Collect feedback on:
- Installation experience
- Feature usability
- AI quality and cost
- Documentation clarity
- Bugs/crashes
- Feature requests

---

## ✅ Final Pre-Ship Command

```bash
# 1. Generate PDF
./generate_pdf.sh

# 2. Update package.json with your info
# (edit email and URL manually)

# 3. Clean build
npm run build
ant dist

# 4. Package for macOS
npm run dist:mac

# 5. Test the packaged app
open dist/mac/Bunghole.app

# 6. If all looks good, zip and share!
```

---

**Notes:**
- This is for personal/beta use, not commercial distribution
- macOS only for now (code is cross-platform ready)
- Keep API keys secure and never commit them
- Monitor API usage to avoid unexpected charges

**Last Updated**: November 10, 2025
