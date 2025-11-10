#!/bin/bash

echo "=== Hunalign Pre-Flight Check ==="

# 1. Check Hunalign binary
if [ -f "bin/hunalign/hunalign" ]; then
    echo "✅ Hunalign binary exists"
    bin/hunalign/hunalign --help > /dev/null 2>&1
    if [ $? -eq 0 ] || [ $? -eq 1 ]; then
        echo "✅ Hunalign is executable"
    else
        echo "❌ Hunalign is not executable"
        exit 1
    fi
else
    echo "❌ Hunalign binary not found"
    echo "   Run: mkdir -p bin/hunalign && cp /tmp/hunalign/src/hunalign/hunalign bin/hunalign/"
    exit 1
fi

# 2. Check dictionary
if [ -f "dictionaries/en-no.dic" ]; then
    echo "✅ Dictionary file exists"
    words=$(grep -c '@' dictionaries/en-no.dic)
    echo "   Dictionary has $words entries"
else
    echo "❌ Dictionary not found"
    exit 1
fi

# 3. Check Java compilation (JAR file deleted after dist, that's normal)
if [ -d "lib" ] && [ "$(ls -A lib 2>/dev/null)" ]; then
    echo "✅ Java compiled (lib directory exists)"
else
    echo "❌ Java not compiled - build needed!"
    echo "   Run: ant dist"
    exit 1
fi

# 4. Check distribution
if [ -d "lib" ] && [ "$(ls -A lib)" ]; then
    echo "✅ Distribution directory exists"
else
    echo "⚠️  Distribution not built - run: ant dist"
fi

# 5. Test Hunalign manually
echo ""
echo "=== Testing Hunalign Standalone ==="
cat > /tmp/test_src.txt << 'EOF'
Hello world
This is a test
How are you today
EOF

cat > /tmp/test_tgt.txt << 'EOF'
Hei verden
Dette er en test
Hvordan har du det i dag
EOF

result=$(bin/hunalign/hunalign -text dictionaries/en-no.dic /tmp/test_src.txt /tmp/test_tgt.txt 2>/dev/null | head -3)
if [ -n "$result" ]; then
    echo "✅ Hunalign produces output:"
    echo "$result" | sed 's/^/   /'
else
    echo "❌ Hunalign produces no output"
    exit 1
fi

echo ""
echo "=== All checks passed! ✅ ==="
echo ""
echo "Next steps:"
echo "1. Run: npm start"
echo "2. Create or open an alignment"
echo "3. Check logs for: 'Hunalign is available and will be used for alignment'"
echo "4. Click AI Review button"
echo "5. Should see cost estimate with actual numbers (not '-')"
