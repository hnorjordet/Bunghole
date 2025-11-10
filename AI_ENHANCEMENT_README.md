# 🤖 Bunghole AI-Enhanced Alignment

## Overview

This enhancement adds intelligent alignment capabilities to Bunghole using:
- **Gale-Church Algorithm**: Statistical sentence alignment based on length
- **Claude AI Integration**: AI-powered improvement of uncertain alignments
- **Cost Estimation**: Transparent cost calculation before AI usage
- **Confidence Scores**: Visual feedback on alignment quality

---

## 🆕 New Features

### 1. Gale-Church Statistical Alignment
- Replaces naive 1:1 index mapping
- Supports 1:1, 1:2, 2:1, and 2:2 alignments
- Automatic confidence scoring (0.0 - 1.0)
- Identifies uncertain alignments for AI review

### 2. Claude AI Improvement
- **Manual trigger** - User decides when to use AI
- **Cost-aware** - Shows estimated cost before running
- **Selective** - Only sends uncertain pairs (confidence < 0.75)
- **Fallback** - Uses Gale-Church if AI unavailable

### 3. Transparency
- Cost estimation before API calls
- Confidence scores displayed per segment
- AI review status tracked
- Statistics dashboard

---

## 📦 New Java Classes

### Core Alignment
```
src/com/maxprograms/stingray/alignment/
├── AlignmentPair.java      - Data structure for aligned pairs
├── AlignmentResult.java    - Result with statistics
├── AlignmentEngine.java    - Coordinates Gale-Church algorithm
└── GaleChurch.java         - Statistical alignment implementation
```

### AI Integration
```
src/com/maxprograms/stingray/ai/
├── ClaudeAIService.java    - HTTP client for Claude API
├── PromptBuilder.java      - Prompt engineering
└── CostEstimator.java      - Token & cost calculation
```

### Modified Files
```
src/com/maxprograms/stingray/
├── AlignmentService.java   - Added AI methods
└── BungholeServer.java     - Added API endpoints

ts/
└── preferences.ts          - Added claudeAPIKey field
```

---

## 🚀 Building

### Prerequisites
- Java 21+
- Ant
- Node.js & npm
- Anthropic API key (optional, for AI features)

### Build Steps

```bash
# 1. Build Java backend
ant clean
ant build

# 2. Build TypeScript frontend
npm install
npm run build

# 3. Run
npm start
```

---

## 🔧 Configuration

### Set Claude API Key

**Option 1: Environment Variable**
```bash
export ANTHROPIC_API_KEY="sk-ant-api-..."
npm start
```

**Option 2: Preferences (via TypeScript - UI needed)**
```typescript
// In preferences dialog (to be implemented in UI phase)
preferences.claudeAPIKey = "sk-ant-api-...";
preferences.enableAI = true;
```

**Option 3: Runtime API**
```bash
# Via HTTP endpoint
curl -X POST http://localhost:8040/setClaudeAPIKey \
  -H "Content-Type: application/json" \
  -d '{"apiKey": "sk-ant-api-..."}'
```

---

## 📡 New API Endpoints

### `/estimateAICost`
Get cost estimate for AI improvement
```json
Request: GET
Response: {
  "status": "Success",
  "needsReview": true,
  "pairsToReview": 15,
  "inputTokens": 2500,
  "outputTokens": 750,
  "estimatedCost": 0.0187,
  "formattedCost": "$0.019",
  "breakdown": "Detailed breakdown..."
}
```

### `/improveWithAI`
Improve uncertain alignments using Claude
```json
Request: POST
Response: {
  "status": "Success",
  "improved": 12,
  "remainingUncertain": 3,
  "overallConfidence": 0.92,
  "stats": { /* AlignmentResult JSON */ }
}
```

### `/getAlignmentStats`
Get current alignment statistics
```json
Response: {
  "totalPairs": 100,
  "confidentPairs": 85,
  "uncertainPairs": 15,
  "overallConfidence": 0.87,
  "aiReviewedCount": 0
}
```

### `/testClaudeConnection`
Test Claude API connection
```json
Response: {
  "status": "Success",
  "connected": true
}
```

---

## 🧪 Testing

### Test Gale-Church Algorithm
```bash
# Run with test files
java -cp "jars/*:lib/*" com.maxprograms.stingray.BungholeServer

# In another terminal, test alignment
curl -X POST http://localhost:8040/alignFiles \
  -H "Content-Type: application/json" \
  -d '{
    "sourceFile": "test/source.txt",
    "targetFile": "test/target.txt",
    "alignmentFile": "test/output.algn",
    "srcLang": "en",
    "tgtLang": "no",
    ...
  }'
```

### Test Claude AI Integration
```bash
# Set API key
export ANTHROPIC_API_KEY="sk-ant-..."

# Test connection
curl http://localhost:8040/testClaudeConnection

# Get cost estimate
curl http://localhost:8040/estimateAICost

# Improve with AI
curl -X POST http://localhost:8040/improveWithAI
```

---

## 💰 Cost Information

### Claude Sonnet 3.5 Pricing
- **Input**: $3.00 per million tokens
- **Output**: $15.00 per million tokens

### Typical Costs
```
100 segments, 10 uncertain:
- Cost: ~$0.01

1000 segments, 100 uncertain:
- Cost: ~$0.10

10000 segments, 500 uncertain:
- Cost: ~$0.50
```

### Cost Optimization
- ✅ Only uncertain pairs (< 75% confidence) sent to AI
- ✅ Cost estimate shown before API call
- ✅ User approval required
- ✅ No automatic AI calls

---

## 🔒 Security

### API Key Storage
- **Environment Variable**: Recommended for development
- **Preferences**: Encrypted storage (UI implementation needed)
- **Never**: Hardcoded in source code

### Best Practices
```bash
# Don't commit API keys
echo "ANTHROPIC_API_KEY=*" >> .gitignore

# Use environment-specific configs
.env.local (not committed)
.env.production (server-only)
```

---

## 📊 Workflow Example

### 1. Create Alignment (with Gale-Church)
```
User: File → New Alignment
System: Converts files to XLIFF
System: Runs Gale-Church algorithm
System: Shows results with confidence scores
```

### 2. Review Uncertain Pairs
```
UI shows: 15 uncertain alignments (confidence < 75%)
Confidence indicators: 🟢 High | 🟡 Medium | 🔴 Low
```

### 3. Improve with AI (Manual Trigger)
```
User: Click "Improve with AI" button
System: Shows cost estimate dialog
  "15 pairs will be reviewed
   Estimated cost: $0.019
   [Cancel] [Proceed]"

User: Clicks [Proceed]
System: Calls Claude API
System: Updates uncertain pairs
System: Shows results
  "12 pairs improved
   3 pairs still uncertain
   Overall confidence: 87% → 92%"
```

### 4. Manual Adjustments
```
User can still:
- Split segments
- Merge segments
- Move up/down
- Edit text
```

### 5. Export TMX
```
Final alignment saved with:
- High confidence scores
- AI-reviewed markers
- Ready for translation memory
```

---

## 🐛 Troubleshooting

### "Claude AI not available"
```bash
# Check API key
echo $ANTHROPIC_API_KEY

# Test connection
curl http://localhost:8040/testClaudeConnection

# Set via API
curl -X POST http://localhost:8040/setClaudeAPIKey \
  -d '{"apiKey": "sk-ant-..."}'
```

### Build Errors
```bash
# Clean and rebuild
ant clean
ant build

# Check Java version
java -version  # Should be 21+

# Check dependencies
ls jars/  # Should include all JARs
```

### High Costs
```bash
# Check cost before running
curl http://localhost:8040/estimateAICost

# Reduce uncertain pairs by:
- Better source/target file preparation
- Pre-alignment manual review
- Adjusting confidence threshold
```

---

## 🔮 Future Enhancements (Phase 2)

### UI Components (Not Yet Implemented)
- [ ] "Improve with AI" button in main window
- [ ] Cost confirmation dialog
- [ ] Progress bar for AI processing
- [ ] Confidence score visual indicators
- [ ] API key settings in preferences dialog
- [ ] Statistics dashboard

### Additional Features
- [ ] Batch AI processing
- [ ] Custom confidence thresholds
- [ ] Learning from user corrections
- [ ] Multi-language pair support (beyond en-no)
- [ ] Export confidence report

---

## 📚 References

### Gale-Church Algorithm
- Paper: "A Program for Aligning Sentences in Bilingual Corpora" (1993)
- [Link](https://aclanthology.org/J93-1004.pdf)

### Claude API
- Documentation: https://docs.anthropic.com/
- Pricing: https://www.anthropic.com/pricing

---

## 🤝 Contributing

When adding UI components, remember:
1. Cost estimation must be shown before AI calls
2. User approval required for API usage
3. Fallback to Gale-Church if AI fails
4. Display confidence scores visually
5. Keep API key secure

---

## ✅ Implementation Status

### ✅ Completed (Backend)
- [x] Gale-Church algorithm
- [x] Claude AI integration
- [x] Cost estimation
- [x] API endpoints
- [x] Preferences structure

### ⏳ Pending (Frontend - Phase 2)
- [ ] "Improve with AI" button
- [ ] Cost confirmation dialog
- [ ] Progress tracking UI
- [ ] Confidence indicators
- [ ] API key settings UI
- [ ] Statistics display

---

**Ready for UI implementation! 🎨**

Pass this file to the UI developer to continue with the frontend integration.
