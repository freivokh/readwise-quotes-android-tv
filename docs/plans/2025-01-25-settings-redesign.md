# Settings Menu Redesign (U2)

## Overview

Redesign the settings screen with a left rail navigation pattern for better TV navigation and visual polish.

## Design Decisions

### Navigation Pattern: Left Rail + Content

```
┌─────────────────────────────────────────────────────────────┐
│  ‹  Settings                                                │
├───────────────┬─────────────────────────────────────────────┤
│               │                                             │
│  👤 Account   │      [ Content area for selected           │
│               │        category loads here ]                │
│  🏷️ Filters   │                                             │
│               │                                             │
│  🎨 Display   │                                             │
│               │                                             │
│  🔄 Sync      │                                             │
│               │                                             │
└───────────────┴─────────────────────────────────────────────┘
```

**Why this pattern:**
- Predictable focus zones (rail vs content)
- TV-native pattern (Android TV settings, Netflix, YouTube)
- Categories always visible
- Scales gracefully

### Focus Behavior

- D-pad DOWN/UP: Navigate rail items
- D-pad RIGHT: Move focus to content area
- D-pad LEFT: Return to rail from content
- Content area has its own vertical navigation

### Visual Style

Blend of app theme matching + modern polish:

| Element | Color | Notes |
|---------|-------|-------|
| Background | `#0D0D0D` | Darker base |
| Rail surface | `#1A1A1A` | Subtle distinction |
| Content cards | `#1E1E1E` | Slight elevation |
| Card border | `#2A2A2A` | Very subtle, 1dp |
| Text primary | `#F5F5F5` | Warm white |
| Text secondary | `#888888` | Labels, hints |
| Accent | Existing | Selected states |

### Focus States

- **Rail item focused:** Accent background at 20%, accent text
- **Rail item selected (not focused):** Left accent bar (3dp)
- **Content item focused:** Soft glow border, slight scale (1.02x)

### Typography

- Category labels: 14sp medium
- Content headers: 13sp secondary, letter-spacing 0.1
- Option labels: 15sp primary
- Option values: 14sp secondary

### Corners & Spacing

- Rail items: 8dp radius
- Content cards: 12dp radius
- Rail item padding: 16dp vertical, 20dp horizontal
- Content card padding: 20dp
- Card gap: 16dp

## Category Content

### 👤 Account
- API Token input + visibility toggle
- "Setup via Phone" button (conditional)
- Sync Now + Full Sync buttons
- Sync status text

### 🏷️ Filters
- Quote filter dropdown
- Tag groups section (conditional)
- Create Group button
- Active filter summary

### 🎨 Display
- Visual Style dropdown
- Text Size dropdown
- Show Tags toggle
- Show Notes toggle
- Show QR Code toggle
- QR Link Type dropdown (conditional)
- Quote Duration slider

### 🔄 Sync
- Sync Interval dropdown
- Last synced timestamp
- Quote count

## Implementation Plan

1. Create new color resources for surfaces
2. Create rail item layout and drawable states
3. Create content card layout
4. Build new activity_settings.xml with horizontal split
5. Create separate layout files for each category's content
6. Update SettingsActivity.kt with rail navigation logic
7. Implement focus handling between rail and content
8. Test D-pad navigation thoroughly

## Files to Modify/Create

**New files:**
- `res/layout/activity_settings_v2.xml` - Main layout with rail
- `res/layout/settings_content_account.xml`
- `res/layout/settings_content_filters.xml`
- `res/layout/settings_content_display.xml`
- `res/layout/settings_content_sync.xml`
- `res/drawable/rail_item_background.xml`
- `res/drawable/content_card_background.xml`

**Modified files:**
- `res/values/colors.xml` - New surface colors
- `SettingsActivity.kt` - Rail navigation logic
