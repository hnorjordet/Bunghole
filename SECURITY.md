# Security Policy

## Security Improvements Implemented

### Version 2.11.0+

The following security improvements have been implemented:

#### 1. **Network Security**
- HTTP server now binds to `127.0.0.1` (localhost only) by default
- Prevents exposure of the API to network access
- Can be configured via `server.bindAddress` in config.properties

#### 2. **Input Validation**
- Added `SecurityUtils` class for path validation
- Protection against path traversal attacks
- File extension validation
- Canonical path resolution to prevent symbolic link attacks

#### 3. **Configuration Management**
- Externalized configuration to `config.properties`
- Support for environment variables
- No hardcoded sensitive values

#### 4. **TypeScript Strict Mode**
- Enabled strict type checking
- Added null safety checks
- Improved code quality and reduced runtime errors

## Security Best Practices

### API Key Management

**IMPORTANT**: Never commit API keys to version control!

1. Set your Claude API key via the application preferences UI
2. The key is stored in your application data directory
3. Consider implementing OS-level keychain integration for production use

### File Access

The application validates all file paths to prevent:
- Path traversal attacks (../)
- Access to system directories
- Invalid file extensions

### Network Configuration

By default, the internal HTTP server binds to `127.0.0.1:8040`, making it accessible only from the local machine. This prevents remote access to your alignment data.

## Known Limitations

### Current Security Considerations

1. **API Key Storage**: Currently stored in plain text in preferences. For production use, consider implementing OS keychain integration.

2. **No Authentication**: The HTTP server does not require authentication. This is acceptable for local-only access but should be addressed if exposing the server externally.

3. **No Rate Limiting**: The API endpoints do not have rate limiting. Consider implementing this for production deployments.

## Reporting Security Issues

If you discover a security vulnerability, please email: tech@maxprograms.com

**Do not** open a public issue for security vulnerabilities.

Please include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

## Security Checklist for Developers

When contributing code, ensure:

- [ ] All file paths are validated using `SecurityUtils.validateFilePath()`
- [ ] No sensitive data in logs
- [ ] No hardcoded credentials or API keys
- [ ] Input validation for all user-provided data
- [ ] Proper error handling without information leakage
- [ ] No SQL injection vulnerabilities (if adding database)
- [ ] XSS prevention in UI code
- [ ] CSRF protection for state-changing operations

## Dependency Security

Run regular security audits:

```bash
# For Node.js dependencies
npm audit

# For Java dependencies (when migrated to Maven)
mvn dependency-check:check
```

## Security Updates

Security updates will be released as patch versions and documented in this file.

### Update History

- **2025-01**: Initial security hardening
  - Localhost-only binding
  - Input validation framework
  - Configuration externalization
  - TypeScript strict mode
