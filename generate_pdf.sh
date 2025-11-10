#!/bin/bash

# Generate PDF User Guide from Markdown
# This script converts the Bunghole User Guide markdown to PDF format

echo "Bunghole User Guide PDF Generator"
echo "=================================="
echo ""

# Check if pandoc is installed
if ! command -v pandoc &> /dev/null; then
    echo "Error: pandoc is not installed"
    echo ""
    echo "To install pandoc on macOS:"
    echo "  brew install pandoc"
    echo ""
    echo "To install pandoc on Ubuntu/Debian:"
    echo "  sudo apt-get install pandoc texlive-latex-base texlive-fonts-recommended texlive-latex-extra"
    echo ""
    exit 1
fi

# Check if markdown file exists
if [ ! -f "BUNGHOLE_USER_GUIDE.md" ]; then
    echo "Error: BUNGHOLE_USER_GUIDE.md not found"
    exit 1
fi

echo "Converting BUNGHOLE_USER_GUIDE.md to PDF..."
echo ""

# Add MacTeX to PATH if it exists
if [ -d "/Library/TeX/texbin" ]; then
    export PATH="/Library/TeX/texbin:$PATH"
fi

# Create temporary file without emojis for LaTeX compatibility
echo "Removing emojis and special characters for LaTeX compatibility..."
# Remove ALL emojis and special unicode characters that LaTeX can't handle
# First remove variation selectors and zero-width joiners
sed -e 's/︎//g' -e 's/️//g' -e 's/‍//g' -e 's/📄//g' -e 's/✅/[YES]/g' -e 's/❌/[NO]/g' -e 's/☑/[x]/g' -e 's/☐/[ ]/g' -e 's/🟢/**/g' -e 's/🟡/**/g' -e 's/🔴/**/g' -e 's/🔵/**/g' -e 's/✂️//g' -e 's/✂//g' -e 's/🔗//g' -e 's/🏷️//g' -e 's/🏷//g' -e 's/🌐//g' -e 's/💡/TIP:/g' -e 's/⚠️//g' -e 's/⚠/WARNING:/g' -e 's/📧//g' -e 's/🚀//g' -e 's/🎯//g' -e 's/🐛//g' -e 's/🔄//g' -e 's/📦//g' -e 's/🧪//g' -e 's/📋//g' -e 's/📊//g' -e 's/📞//g' -e 's/🎉//g' -e 's/🔍//g' -e 's/🗂//g' -e 's/✓/[OK]/g' -e 's/✗/[X]/g' -e 's/→/->/g' -e 's/←/<-/g' -e 's/↑/^/g' -e 's/↓/v/g' -e 's/↕/|/g' -e 's/–/-/g' -e 's/—/-/g' -e 's/"/"/g' -e 's/"/"/g' -e "s/'/'/g" -e "s/'/'/g" -e 's/…/.../g' -e "s/´/'/g" -e 's/≥/>=/g' -e 's/≤/<=/g' -e 's/≠/!=/g' -e 's/≈/~=/g' -e 's/±/+-/g' -e 's/×/x/g' -e 's/÷/\//g' BUNGHOLE_USER_GUIDE.md > BUNGHOLE_USER_GUIDE_temp.md

# Generate PDF with pandoc
pandoc BUNGHOLE_USER_GUIDE_temp.md \
    -o bunghole_en.pdf \
    --pdf-engine=pdflatex \
    --variable geometry:margin=1in \
    --variable fontsize=11pt \
    --variable linkcolor=blue \
    --variable urlcolor=blue \
    --variable toccolor=black \
    --toc \
    --toc-depth=2 \
    --metadata title="Bunghole User Guide" \
    --metadata author="Håvard Nørjordet" \
    --metadata date="$(date '+%B %Y')" \
    2>&1

if [ $? -eq 0 ]; then
    echo "✓ Successfully generated bunghole_en.pdf"
    echo ""

    # Clean up temporary file
    rm -f BUNGHOLE_USER_GUIDE_temp.md

    # Backup old PDFs
    if [ -f "stingray_en.pdf" ]; then
        mv stingray_en.pdf stingray_en.pdf.backup
        echo "✓ Backed up old stingray_en.pdf"
    fi

    if [ -f "stingray_es.pdf" ]; then
        mv stingray_es.pdf stingray_es.pdf.backup
        echo "✓ Backed up old stingray_es.pdf"
    fi

    echo ""
    echo "Note: Spanish translation not yet available."
    echo "You can create bunghole_es.pdf by translating BUNGHOLE_USER_GUIDE.md to Spanish."
    echo ""
    echo "Done!"
else
    echo "✗ Error generating PDF"
    echo ""
    # Clean up temporary file on error too
    rm -f BUNGHOLE_USER_GUIDE_temp.md
    echo "If you see LaTeX errors, you may need to install additional packages:"
    echo "  brew install --cask mactex-no-gui  # macOS"
    echo "  sudo apt-get install texlive-full  # Ubuntu/Debian"
    exit 1
fi
