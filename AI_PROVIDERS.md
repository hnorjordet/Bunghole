# AI Providers for Bunghole

Bunghole supports multiple AI providers for intelligent alignment enhancement. This document explains how to configure and use different AI services.

## Supported Providers

### 1. Claude (Anthropic)
- **Default provider**
- **Models supported**: Claude Sonnet 4, Claude Opus, etc.
- **Best for**: High-quality translation analysis, nuanced understanding
- **Pricing**: ~$3/million input tokens, ~$15/million output tokens

### 2. OpenAI (ChatGPT)
- **Alternative provider**
- **Models supported**: GPT-4 Turbo, GPT-4, GPT-3.5 Turbo
- **Best for**: Fast processing, widely available API keys
- **Pricing**: ~$10/million input tokens, ~$30/million output tokens (GPT-4 Turbo)

## Configuration

### Option 1: Configuration File

Create or edit `config.properties` in the application directory:

```properties
# Choose your AI provider
ai.provider=claude  # or "openai"

# Claude Configuration
claude.apiKey=your-claude-api-key-here
claude.model=claude-sonnet-4-20250514
claude.inputPrice=3.0
claude.outputPrice=15.0

# OpenAI Configuration
openai.apiKey=your-openai-api-key-here
openai.model=gpt-4-turbo-preview
openai.inputPrice=10.0
openai.outputPrice=30.0
```

### Option 2: Environment Variables

Set environment variables (useful for CI/CD or security):

```bash
# Provider selection
export AI_PROVIDER=claude  # or openai

# Claude
export CLAUDE_API_KEY=your-claude-api-key
export CLAUDE_MODEL=claude-sonnet-4-20250514

# OpenAI
export OPENAI_API_KEY=your-openai-api-key
export OPENAI_MODEL=gpt-4-turbo-preview
```

### Option 3: UI Preferences

The Preferences dialog allows you to:
- Select AI provider from dropdown
- Enter API keys securely
- Choose specific models
- View current configuration

## Getting API Keys

### Claude API Key

1. Visit [Anthropic Console](https://console.anthropic.com/)
2. Sign up or log in
3. Navigate to **API Keys**
4. Click **Create Key**
5. Copy your key and store securely

**Free Tier**: Limited free credits for testing
**Paid**: Pay-as-you-go pricing

### OpenAI API Key

1. Visit [OpenAI Platform](https://platform.openai.com/)
2. Sign up or log in
3. Navigate to **API Keys** (under account settings)
4. Click **Create new secret key**
5. Copy your key immediately (shown only once)

**Free Tier**: $5 free credits for new accounts
**Paid**: Pay-as-you-go pricing

## Model Selection

### Claude Models

| Model | Best For | Speed | Cost | Quality |
|-------|----------|-------|------|---------|
| Claude Sonnet 4 | **Recommended** - Best balance | Fast | Low | Excellent |
| Claude Opus | Maximum quality | Slow | High | Outstanding |
| Claude Haiku | Speed-critical tasks | Very Fast | Very Low | Good |

**Recommendation**: Use `claude-sonnet-4-20250514` for alignment tasks.

### OpenAI Models

| Model | Best For | Speed | Cost | Quality |
|-------|----------|-------|------|---------|
| GPT-4 Turbo | **Recommended** - Latest | Fast | Medium | Excellent |
| GPT-4 | High quality | Medium | High | Excellent |
| GPT-3.5 Turbo | Budget-friendly | Very Fast | Very Low | Good |

**Recommendation**: Use `gpt-4-turbo-preview` for best results, or `gpt-3.5-turbo` for budget processing.

## Cost Comparison

### Example: 1000 Segment Pairs

**Estimated Usage**:
- Input: ~200,000 tokens
- Output: ~50,000 tokens

**Claude Sonnet 4**:
- Input cost: $0.60
- Output cost: $0.75
- **Total: ~$1.35**

**OpenAI GPT-4 Turbo**:
- Input cost: $2.00
- Output cost: $1.50
- **Total: ~$3.50**

**OpenAI GPT-3.5 Turbo**:
- Input cost: $0.10
- Output cost: $0.10
- **Total: ~$0.20**

💡 **Tip**: Always use the cost estimation dialog before processing to see actual costs.

## Switching Providers

### Method 1: Configuration File

Edit `config.properties`:

```properties
# Switch to OpenAI
ai.provider=openai
```

Restart the application.

### Method 2: Environment Variable

```bash
export AI_PROVIDER=openai
```

Restart the application.

### Method 3: Preferences Dialog

1. Open **Preferences** (`Cmd+,` on macOS)
2. Select **AI Provider** dropdown
3. Choose **Claude** or **OpenAI**
4. Enter appropriate API key
5. Click **Save**

Changes take effect immediately.

## Provider-Specific Features

### Claude Advantages

✅ **Strengths**:
- Better at nuanced language understanding
- More context-aware
- Lower cost per token
- Excellent for complex translations
- Better handling of cultural context

⚠️ **Considerations**:
- Newer API, less widely adopted
- Fewer model options
- Rate limits may be stricter

### OpenAI Advantages

✅ **Strengths**:
- Widely available API keys
- More model options (including budget options)
- Faster processing with GPT-3.5
- Well-documented API
- Mature ecosystem

⚠️ **Considerations**:
- Higher cost per token (GPT-4)
- May be less nuanced than Claude
- Potential for more generic responses

## Best Practices

### 1. Cost Management

- **Always estimate costs first** using `Cmd+R` / `Ctrl+R`
- **Process in batches** for large documents
- **Use cheaper models for drafts**, premium models for final
- **Monitor usage** via provider dashboards

### 2. Quality Optimization

- **Use Claude Sonnet 4** for best quality-to-cost ratio
- **Use GPT-4 Turbo** as alternative with good quality
- **Avoid GPT-3.5** for complex translations (lower quality)
- **Review AI suggestions** - don't blindly accept

### 3. Security

- **Never commit API keys** to version control
- **Use environment variables** in production
- **Rotate keys regularly** (every 90 days)
- **Set spending limits** in provider dashboards
- **Monitor for unusual activity**

### 4. Performance

- **Process 500-1000 segments** at a time for optimal performance
- **Use concurrent requests** when possible (future feature)
- **Cache results** locally to avoid reprocessing
- **Timeout appropriately** (30-60 seconds per request)

## Troubleshooting

### "API Key Not Configured"

**Solution**: Ensure API key is set in one of:
1. Preferences dialog
2. `config.properties` file
3. Environment variables (`CLAUDE_API_KEY` or `OPENAI_API_KEY`)

### "Rate Limit Exceeded"

**Solution**:
- Wait and retry (rate limits reset after time)
- Reduce batch size
- Upgrade to higher tier plan
- Switch to alternative provider temporarily

### "Invalid API Key"

**Solution**:
- Verify key is copied correctly (no extra spaces)
- Check key hasn't expired or been revoked
- Regenerate key from provider dashboard
- Ensure billing is active on provider account

### "Model Not Found"

**Solution**:
- Update model name in configuration
- Check [Claude models](https://docs.anthropic.com/claude/docs/models-overview) or [OpenAI models](https://platform.openai.com/docs/models)
- Ensure account has access to requested model
- Use recommended models from this guide

### "Connection Timeout"

**Solution**:
- Check internet connection
- Verify proxy settings if applicable
- Increase timeout in configuration
- Try alternative provider

### "Unexpected Response Format"

**Solution**:
- Check if API version is compatible
- Update Bunghole to latest version
- Report issue with provider response example
- Try alternative provider

## Provider Status

Check provider status:

- **Claude Status**: https://status.anthropic.com/
- **OpenAI Status**: https://status.openai.com/

## Pricing Updates

AI provider pricing changes frequently. Update your configuration:

```properties
# Check current pricing at:
# Claude: https://www.anthropic.com/pricing
# OpenAI: https://openai.com/pricing

# Update as needed
claude.inputPrice=3.0
claude.outputPrice=15.0
openai.inputPrice=10.0
openai.outputPrice=30.0
```

## Future Providers

Potential future support:

- **Google PaLM/Gemini** - Google's AI models
- **Cohere** - Enterprise-focused AI
- **Azure OpenAI** - Microsoft-hosted OpenAI
- **Amazon Bedrock** - AWS AI services
- **Local Models** - Ollama, LM Studio integration

Want to see support for another provider? [Request a feature](https://github.com/rmraya/Stingray/issues)!

## Technical Details

### API Implementation

Both providers use RESTful APIs with JSON:

**Claude**:
```
POST https://api.anthropic.com/v1/messages
Headers:
  - x-api-key: YOUR_KEY
  - anthropic-version: 2023-06-01
  - content-type: application/json
```

**OpenAI**:
```
POST https://api.openai.com/v1/chat/completions
Headers:
  - Authorization: Bearer YOUR_KEY
  - content-type: application/json
```

### Request Format

Both providers receive:
- Source and target segments
- Language codes
- Instructions for analysis

Both providers return:
- Confidence scores (0-100)
- Issue descriptions
- Uncertainty flags

### Caching

Future versions may cache:
- Identical segment pairs
- Provider responses
- Cost estimates

Currently, every request hits the API.

## FAQ

**Q: Which provider should I use?**
A: Claude Sonnet 4 for best quality-to-cost ratio, GPT-3.5 Turbo for budget.

**Q: Can I use both providers?**
A: Yes! Configure both API keys and switch as needed.

**Q: Do I need to pay for API access?**
A: Both providers require payment after free credits are exhausted.

**Q: Is my data sent to these providers?**
A: Yes, segment text is sent for analysis. Review provider privacy policies.

**Q: Can I use local/offline AI?**
A: Not yet, but local model support is planned for future versions.

**Q: Which is more accurate?**
A: Both are excellent. Claude may be slightly better at nuanced translations.

**Q: Can I set different providers per project?**
A: Currently global setting only. Per-project settings planned for future.

**Q: What about API quotas?**
A: Check your provider dashboard for quota limits and usage.

---

**Last Updated**: November 10, 2025
**Version**: 2.11.0+multi-provider
