# Bunghole Documentation

This directory contains comprehensive documentation for Bunghole.

## User Guide

The main user guide is available in multiple formats:

### Markdown Format (Source)
- **BUNGHOLE_USER_GUIDE.md** - Complete user guide in markdown format
  - Easy to read on GitHub
  - Easy to edit and maintain
  - Can be converted to PDF, HTML, or other formats

### PDF Format
- **bunghole_en.pdf** - English PDF user guide (generated from markdown)
- **bunghole_es.pdf** - Spanish PDF user guide (translation needed)

## Generating PDF Documentation

To generate the PDF from the markdown source:

### Prerequisites

Install pandoc and LaTeX:

**macOS:**
```bash
brew install pandoc
brew install --cask basictex
```

**Ubuntu/Debian:**
```bash
sudo apt-get install pandoc texlive-latex-base texlive-fonts-recommended texlive-latex-extra
```

**Windows:**
- Download and install [pandoc](https://pandoc.org/installing.html)
- Download and install [MiKTeX](https://miktex.org/download)

### Generate PDF

Run the generation script:

```bash
./generate_pdf.sh
```

Or manually with pandoc:

```bash
pandoc BUNGHOLE_USER_GUIDE.md -o bunghole_en.pdf \
    --pdf-engine=pdflatex \
    --variable geometry:margin=1in \
    --toc \
    --toc-depth=2
```

## Documentation Structure

The user guide covers:

1. **Introduction** - Overview and key features
2. **What's New** - Recent improvements and enhancements
3. **Supported Formats** - File formats and platforms
4. **Getting Started** - Step-by-step tutorial
5. **AI-Powered Features** - Claude AI integration
6. **Keyboard Shortcuts** - Complete reference
7. **Advanced Features** - Hotkeys, confidence scores, navigation
8. **Program Settings** - Configuration options
9. **Security & Privacy** - Security best practices
10. **Troubleshooting** - Common issues and solutions
11. **Subscriptions** - Licensing information
12. **Licenses** - Open source components
13. **Glossary** - Technical terms and definitions

## Additional Documentation

- **README.md** - Project overview and build instructions
- **SECURITY.md** - Security policy and reporting
- **IMPROVEMENTS.md** - Recent improvements and roadmap
- **AI_ENHANCEMENT_README.md** - AI feature details
- **VERIFICATION_COMPLETE.md** - Testing documentation
- **MACOS_STYLING_GUIDE.md** - macOS UI guidelines
- **UI_IMPLEMENTATION_SUMMARY.md** - UI implementation notes
- **HUNALIGN_DEBUG.md** - Hunalign integration notes

## Updating the User Guide

### Edit the Markdown

1. Edit `BUNGHOLE_USER_GUIDE.md` with your favorite markdown editor
2. Preview changes (most IDEs have markdown preview)
3. Test that all links work
4. Check formatting and images

### Regenerate PDF

After editing the markdown:

```bash
./generate_pdf.sh
```

This will:
- Generate new PDF from updated markdown
- Backup old PDF files
- Create `bunghole_en.pdf`

### Internationalization

To create translations:

1. Copy `BUNGHOLE_USER_GUIDE.md` to `BUNGHOLE_USER_GUIDE_es.md` (or other language code)
2. Translate the content
3. Generate PDF:
   ```bash
   pandoc BUNGHOLE_USER_GUIDE_es.md -o bunghole_es.pdf --toc
   ```
4. Update the TypeScript code to reference the correct PDF for each language

## Legacy Documentation

Old Stingray PDFs are backed up with `.backup` extension:
- `stingray_en.pdf.backup`
- `stingray_es.pdf.backup`

These can be removed once the new documentation is verified.

## Help Menu Integration

The user guide PDF is opened when users press `F1` or select **Help → User Guide** from the menu.

The integration code is in `ts/app.ts`:

```typescript
static showHelp(): void {
    let filePath = this.path.join(app.getAppPath(), 'bunghole_' + Bunghole.appLang + '.pdf');
    let fileUrl: URL = new URL('file://' + filePath);
    shell.openExternal(fileUrl.href);
}
```

## PDF Distribution

### Building for Distribution

When building the application for distribution, ensure PDFs are included:

1. **Build Process**: PDFs are automatically included in the Electron build
2. **Location**: Root directory of the application
3. **Naming Convention**: `bunghole_[language].pdf`
   - `bunghole_en.pdf` - English
   - `bunghole_es.pdf` - Spanish (when available)
   - `bunghole_no.pdf` - Norwegian (if needed)

### Installer Configuration

The `package.json` already includes documentation in the build:

```json
"files": [
    "*.pdf",  // Includes all PDF files
    "html/**/*",
    // ... other files
]
```

## Maintaining Documentation Quality

### Before Committing

- [ ] Check all links are working
- [ ] Verify markdown formatting
- [ ] Test PDF generation
- [ ] Review for typos and clarity
- [ ] Update version number and date
- [ ] Ensure screenshots are up-to-date (if any)

### Periodic Reviews

- Review documentation quarterly
- Update for new features
- Address user feedback
- Keep screenshots current
- Update troubleshooting section based on support tickets

## Feedback

Documentation feedback is welcome!

- **Users**: Report documentation issues via support channels
- **Contributors**: Submit pull requests with improvements
- **Translators**: Contact for translation coordination

## Quick Reference

**Edit**: `BUNGHOLE_USER_GUIDE.md`
**Generate**: `./generate_pdf.sh`
**Output**: `bunghole_en.pdf`
**Old files**: `*.pdf.backup`

---

**Last Updated**: November 10, 2025
