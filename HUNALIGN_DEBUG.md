# 🐛 Hunalign Debugging & Testing Guide

## Problemet som ble funnet

**Symptom:** AI Review knappen viser bare `-` i cost-dialogen, og ingenting skjer.

**Root Cause funnet:**
1. ❌ `ant dist` sletter `bin/` mappen og gjenoppretter den
2. ❌ Dette slettet `bin/hunalign/hunalign` binæren
3. ❌ Java koden fant ikke Hunalign → falt tilbake til Gale-Church
4. ✅ **FIKSET:** build.xml bevarer nå hunalign under dist-bygging

---

## ✅ Pre-Flight Sjekkliste

Før du tester, sjekk at alt er på plass:

### 1. Hunalign Binary
```bash
ls -lh bin/hunalign/hunalign
# Should show: -rwxr-xr-x ... 366K ... hunalign
```

Hvis den mangler:
```bash
mkdir -p bin/hunalign
cp /tmp/hunalign/src/hunalign/hunalign bin/hunalign/
chmod +x bin/hunalign/hunalign
```

### 2. Dictionary File
```bash
ls -lh dictionaries/en-no.dic
# Should show: -rw-r--r-- ... 2.5K ... en-no.dic

head -5 dictionaries/en-no.dic
# Should show:
# # English-Norwegian dictionary for Hunalign
# # Format: english_word @ norwegian_word @ probability
# the @ det @ 1
# and @ og @ 1
# is @ er @ 1
```

### 3. Java Compilation
```bash
ls -lh jars/stingray.jar
unzip -l jars/stingray.jar | grep HunalignService
# Should show: HunalignService.class
```

### 4. Distribution Built
```bash
ls -lh lib/*.jar | wc -l
# Should show: 20 (or similar number)
```

---

## 🧪 Manual Test: Hunalign Standalone

Test at Hunalign faktisk virker:

```bash
# Create test files
cat > test_source.txt << 'EOF'
Hello world
This is a test
How are you today
EOF

cat > test_target.txt << 'EOF'
Hei verden
Dette er en test
Hvordan har du det i dag
EOF

# Run Hunalign
bin/hunalign/hunalign -text dictionaries/en-no.dic test_source.txt test_target.txt

# Expected output (approximately):
# Hello world	Hei verden	0.27
# This is a test	Dette er en test	0.85
# How are you today	Hvordan har du det i dag	0.2
```

**✅ Hvis du ser output med TAB-separerte verdier → Hunalign virker!**

---

## 🔍 Runtime Debugging

### Se logger når appen kjører:

Når du starter `npm start`, se etter disse meldingene:

**✅ GOOD - Hunalign er tilgjengelig:**
```
INFO: Hunalign is available and will be used for alignment
```

**❌ BAD - Hunalign ikke funnet:**
```
INFO: Hunalign not available, falling back to Gale-Church
```

### Når du lager alignment:

**✅ GOOD:**
```
INFO: Using Hunalign for alignment...
INFO: Running Hunalign: /path/to/hunalign -text -utf -realign ...
INFO: Hunalign produced 50 alignment pairs
INFO: Analysis complete: 50 pairs, 85.0% confidence, 5 uncertain
```

**❌ BAD:**
```
INFO: Using Gale-Church for alignment...
INFO: Gale-Church complete: ...
```

---

## 🎯 Test AI Review Flow

### Step 1: Åpne eller lag alignment
```
1. New Alignment → velg filer
2. ELLER Open Alignment → velg .algn fil
```

### Step 2: Sjekk loggen
Du skal se:
```
INFO: Hunalign is available and will be used for alignment
INFO: Using Hunalign for alignment...
INFO: Hunalign produced X alignment pairs
INFO: Analysis complete: X pairs, Y.Y% confidence, Z uncertain
```

### Step 3: Klikk AI Review knappen

**✅ GOOD - Cost dialog viser:**
```
Alignment pairs to review:    5
Input tokens:                 2,500
Estimated output tokens:      750
Estimated cost:               $0.019
```

**❌ BAD - Cost dialog viser:**
```
Alignment pairs to review:    -
Input tokens:                 -
Estimated output tokens:      -
Estimated cost:               -
```

**Hvis BAD:** Sjekk at `currentAlignmentResult` ikke er null. Dette betyr Hunalign ikke kjørte.

---

## 🔧 Fikser hvis noe er galt

### Problem 1: "Hunalign not available"

**Årsak:** Hunalign binary ikke funnet eller ikke kjørbar

**Løsning:**
```bash
# Sjekk at binæren finnes
ls -lh bin/hunalign/hunalign

# Sjekk at den er kjørbar
chmod +x bin/hunalign/hunalign

# Test manuelt
bin/hunalign/hunalign --help
```

### Problem 2: Cost dialog viser bare "-"

**Årsak:** `currentAlignmentResult` er null (alignment kjørte ikke)

**Løsning:**
1. Sjekk loggen - kjørte Hunalign eller Gale-Church?
2. Hvis Gale-Church → Hunalign-problemer, se Problem 1
3. Hvis Hunalign kjørte men ingen result → Java exception? Sjekk full logg

### Problem 3: "Proceed with AI Review" gjør ingenting

**Årsak:** Multiple mulige:
- Claude API key ikke satt
- currentAlignmentResult.uncertainPairs er tom
- Network error

**Løsning:**
```bash
# Test API key
curl -X POST http://localhost:8040/testClaudeConnection

# Test cost estimate
curl http://localhost:8040/estimateAICost

# Sjekk full logg for errors
```

### Problem 4: Hunalign slettes ved rebuild

**Årsak:** build.xml slettet bin/ uten å bevare hunalign

**Løsning:** ✅ ALLEREDE FIKSET i build.xml!

Men hvis du bygger uten den nye build.xml:
```bash
# Etter hver 'ant dist', kjør:
mkdir -p bin/hunalign
cp /tmp/hunalign/src/hunalign/hunalign bin/hunalign/
```

---

## 📊 Forvented Output for En God Alignment

Med Hunalign skal du se:

### I loggen:
```
INFO: Hunalign is available and will be used for alignment
INFO: Using Hunalign for alignment...
INFO: Hunalign produced 100 alignment pairs
INFO: Analysis complete: 100 pairs, 87.5% confidence, 12 uncertain
```

### I Cost Dialog:
```
Alignment pairs to review:    12
Input tokens:                 3,200
Estimated output tokens:      600
Estimated cost:               $0.024

Pairs to review: 12
Input tokens: 3,200 (~$0.0096)
Output tokens: 600 (~$0.0090)
Total estimated cost: $0.024
```

### Etter AI Review:
```
AI Review Complete!

Improved pairs: 10
Remaining uncertain: 2
Overall confidence: 92.0%
```

---

## 🧪 Full Test Script

Kjør dette for å teste hele flyten:

```bash
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

# 3. Check Java compilation
if [ -f "jars/stingray.jar" ]; then
    echo "✅ JAR file exists"
    if unzip -l jars/stingray.jar | grep -q HunalignService; then
        echo "✅ HunalignService is in JAR"
    else
        echo "❌ HunalignService not in JAR - rebuild needed!"
        exit 1
    fi
else
    echo "❌ JAR file not found - compile needed!"
    exit 1
fi

# 4. Test Hunalign manually
echo ""
echo "=== Testing Hunalign Standalone ==="
cat > /tmp/test_src.txt << 'EOF'
Hello world
This is a test
EOF

cat > /tmp/test_tgt.txt << 'EOF'
Hei verden
Dette er en test
EOF

result=$(bin/hunalign/hunalign -text dictionaries/en-no.dic /tmp/test_src.txt /tmp/test_tgt.txt 2>/dev/null | head -2)
if [ -n "$result" ]; then
    echo "✅ Hunalign produces output:"
    echo "$result"
else
    echo "❌ Hunalign produces no output"
    exit 1
fi

echo ""
echo "=== All checks passed! ==="
echo "You can now run: npm start"
```

Lagre som `test_hunalign.sh` og kjør:
```bash
chmod +x test_hunalign.sh
./test_hunalign.sh
```

---

## 📝 Kjente Problemer og Løsninger

### 1. Dictionary format
- ✅ Format: `english_word @ norwegian_word @ probability`
- ⚠️ Hunalign er case-sensitive
- ⚠️ En entry per linje

### 2. Hunalign output parsing
- ✅ Format: `source_idx	target_idx	confidence`
- ✅ Multi-mappings: `1-2	3	0.5` (means source[1,2] → target[3])
- ✅ 1-based indexing → konvertert til 0-based i Java

### 3. Confidence threshold
- Hunalign confidence: 0.0 - 1.0
- Threshold for "uncertain": < 0.75
- Multi-mappings får automatisk max 0.70 (for å trigge AI review)

---

## 🎯 Neste Steg for Testing i Morgen

1. **Start fresh:**
   ```bash
   ant dist
   npm start
   ```

2. **Test med dine filer:**
   - Lag ny alignment med English/Norwegian filer
   - Sjekk loggen for "Using Hunalign"
   - Noter hvor mange "uncertain" pairs

3. **Test AI Review:**
   - Klikk AI Review knappen
   - Skal vise cost estimate
   - Klikk "Proceed"
   - Skal forbedre usikre pairs

4. **Rapporter resultater:**
   - Hvor mange pairs totalt?
   - Hvor mange uncertain før AI?
   - Hvor mange improved etter AI?
   - Overall confidence før/etter?

---

**Alt er klart for testing! 🚀**
