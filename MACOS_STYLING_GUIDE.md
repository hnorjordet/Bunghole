# macOS Styling Guide - Bunghole

## Oversikt

Bunghole har fått en moderne macOS-inspirert GUI basert på designprinsipper fra macOS Big Sur og Ventura.

## Hva er endret?

### ✨ Nye funksjoner

1. **Glassmorfisme-effekter**
   - Toolbars bruker nå `backdrop-filter: blur(20px)` for moderne frosted glass-look
   - Semi-transparente bakgrunner med 80% opacity
   - Smooth saturation på 180%

2. **Moderne fargepalett**
   - **Light mode**: Hvit (#ffffff) og lys grå (#f5f5f7) bakgrunner
   - **Dark mode**: Mørk grå (#1e1e1e, #2d2d2d) i stedet for pure black
   - **Accent color**: Apple's system blue (#007aff light / #0a84ff dark)

3. **Typografi**
   - SF Pro Display font stack: `-apple-system, BlinkMacSystemFont`
   - Antialiased font rendering for bedre lesbarhet
   - Større base font-size (15px)

4. **Spacing og layout**
   - Mer luftig spacing (12-16px padding)
   - 8px gap mellom elementer
   - Større border-radius (6-12px)

5. **Animasjoner**
   - Smooth 0.15-0.2s transitions
   - Subtle hover-effekter
   - Button lift-effekt med transform
   - Focus states med glow

6. **Scrollbars**
   - Tynne, moderne scrollbars (10px)
   - Transparent track
   - Avrundede thumbs med hover-effekt

7. **Tabeller**
   - Subtile borders i stedet for tykke linjer
   - Hover states på rader
   - Moderne selection states

## CSS-variabler

### Light mode
```css
--macos-bg-primary: #ffffff;
--macos-bg-secondary: #f5f5f7;
--macos-bg-tertiary: #e8e8ed;
--macos-text-primary: #1d1d1f;
--macos-text-secondary: #86868b;
--macos-accent: #007aff;
--macos-border: rgba(0, 0, 0, 0.1);
--macos-hover: rgba(0, 0, 0, 0.05);
```

### Dark mode
```css
--macos-bg-primary: #1e1e1e;
--macos-bg-secondary: #2d2d2d;
--macos-bg-tertiary: #3a3a3a;
--macos-text-primary: #f5f5f7;
--macos-text-secondary: #98989d;
--macos-accent: #0a84ff;
--macos-border: rgba(255, 255, 255, 0.1);
--macos-hover: rgba(255, 255, 255, 0.08);
```

## Brukte ressurser

Designet er basert på:
- **Puppertino Framework** - macOS UI framework
- **Glass UI** - Glassmorphism generator
- **Apple Human Interface Guidelines**

## Browser-kompatibilitet

- ✅ Chrome/Edge (Chromium)
- ✅ Safari
- ⚠️ Firefox (backdrop-filter krever `layout.css.backdrop-filter.enabled`)

## Videre utvikling

Mulige forbedringer:
- [ ] Window traffic lights (red/yellow/green buttons)
- [ ] Native-looking context menus
- [ ] Segmented controls for grouped buttons
- [ ] System font size preferences
- [ ] Reduce motion preferences support

## Testing

For å teste endringene:

```bash
npm run start
```

Bytt mellom light/dark mode i app preferences for å se begge temaer.

## Tilbakestilling

Originale CSS-filer er ikke sikkerhetskopiert. Hvis du vil gå tilbake:

```bash
git checkout css/light.css css/dark.css
```

---

**Laget:** 2025-10-22
**Versjon:** 2.11.0+
