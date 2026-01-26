# Readwise Quotes V2 Improvements

## Feature Roadmap

Organized list of planned improvements based on codebase review (2025-12-30).

---

### Already Implemented

| ID | Feature | Status | Notes |
|----|---------|--------|-------|
| ~N2~ | Back button in settings | Done | Working - uses "‹" symbol |
| ~N3~ | Tag selection for "By Tag" filter | Done | Dialog with checkboxes, validation |
| ~F2~ | Match mode (ANY/ALL) | Done | Global toggle in settings |

---

### UI/UX Improvements

| ID | Feature | Status | Priority | Notes |
|----|---------|--------|----------|-------|
| ~U1~ | Update aesthetic design - cleaner, smaller font | Done | | 4 themes, elegant typography, text size setting |
| ~U2~ | Make settings menu cleaner and more aesthetic | Done | | Left rail nav, card-based content, TV-native focus |
| ~U3~ | Show highlight tags in UI | Done | | Bottom-center, toggle in settings |
| U4 | Option to show book cover art | Pending | | Data synced, UI not implemented |
| ~U5~ | Quotes getting cut off / resize issues | Done | | Reduced fonts, wider margins, markdown stripping |
| ~U6~ | Show highlight notes | Done | | Below source, toggle in settings |

---

### Navigation & Controls

| ID | Feature | Status | Priority | Notes |
|----|---------|--------|----------|-------|
| ~N1~ | D-pad left/right cycles quotes (forward/back) | Done | | Random on load, sequential nav after |

---

### New Features

| ID | Feature | Status | Priority | Notes |
|----|---------|--------|----------|-------|
| ~F1~ | QR code linking to original document/Readwise | Done | | Bottom-right, toggle + link type setting |
| ~F3~ | Per-group match mode (ANY/ALL) | Done | | Each group has own toggle |

---

### Settings & Configuration

| ID | Feature | Status | Priority | Notes |
|----|---------|--------|----------|-------|
| ~S1~ | API token entry without manual keyboard | Done | | Local web server on phone |
| ~S2~ | Show/hide password toggle for API token | Done | | Eye icon toggle |
| ~S3~ | Funny note about one-time token entry | Done | | Shows only when no token saved |
| S4 | Optional sync methods | Pending | | Need to clarify scope |

---

### Development & Testing

| ID | Feature | Status | Priority | Notes |
|----|---------|--------|----------|-------|
| D1 | Switch to Android Studio emulator | Pending | | Document setup |
| D2 | Document current emulation setup | Pending | | |

---

## Remaining Features Summary

**To Implement (2 items):**

1. **U4** - Show book cover art option
2. **S4** - Optional sync methods

---

## Implementation Log

### 2025-01-01: Setup via Phone token entry (S1)
- Local web server (NanoHTTPD) for receiving token
- TV shows IP address, user visits on phone
- Beautiful styled HTML form for pasting token
- Auto-syncs after token received
- Commit: `7fbc480`

### 2025-01-01: Playful token helper text (S3)
- Shows friendly message only when no token is saved
- "We know, typing with a remote is painful..." + URL
- Hidden once token is entered
- Commit: `400f25f`

### 2025-01-01: Password visibility toggle (S2)
- Eye icon button next to API token input
- Toggles between 👁 (hidden) and 🙈 (visible)
- D-pad navigation between input and toggle
- Commit: `b649162`

### 2025-01-01: D-pad quote navigation (N1)
- LEFT/RIGHT arrows navigate through quotes manually
- Faster fade animation (300ms) for manual vs auto (1s)
- Timer resets after manual navigation
- Quotes shuffled on load, sequential nav through shuffled list
- Commit: `4fed260`

### 2025-01-20: Aesthetic redesign & overflow fixes (U1, U5)
- 4 visual themes: Minimal, Ambient, Editorial, Stoic
- Reduced font sizes for elegant, minimal look
- Smooth quote transitions (no flicker on size change)
- Adjustable text size setting (Small/Medium/Large)
- Wider horizontal margins for elegant text blocks
- Strip markdown formatting from quotes
- Commits: `1636332`, `9dc0806`, `06c4f04`, `67c314f`, `2597e9d`, `4dd3f7b`, `a742c9f`

### 2025-12-30: Per-group match mode (F3)
- Added `matchMode` field to TagGroup model
- Each group row now has ANY/ALL toggle button
- Removed global match mode toggle
- Query: `(Group A tags with its mode) OR (Group B tags with its mode)`
- Commit: `8d5904e`

### 2025-01-20: Tags display (U3)
- Display tags at bottom-center of quote screen
- Subtle styling: 10sp font, 40% opacity
- "Show Tags" toggle in Display settings
- Commits: `022501a`, `9dd0d7e`

### 2025-01-20: Notes display (U6)
- Sync `note` field from Readwise API
- Display notes below source text (italic, muted)
- "Show Notes" toggle in Display settings
- Database version bump (v1 → v2) with destructive migration
- Commit: `bd7ab1d`

### 2025-01-20: Full Sync button
- Clears last sync time to re-download all quotes
- Added next to "Sync Now" in Account section
- Commit: `9dd0d7e`

### 2025-01-20: Padding adjustments
- Reduced horizontal padding for better text display
- Minimal/Ambient: 60dp, Editorial/Stoic: 80dp
- Commit: `9c86caa`

### 2025-01-20: QR code feature (F1)
- QR code in bottom-right corner (44dp, 25% opacity)
- "Show QR Code" toggle in Display settings
- "QR Code Links To" setting (Readwise / Original Source)
- Sync sourceUrl from Readwise API
- Falls back to Readwise URL if source unavailable
- ZXing library for QR generation
- Commit: `deb0d6c`

### 2025-01-25: Settings menu redesign (U2)
- Left rail navigation pattern (TV-native UX)
- 4 categories: Account, Filters, Display, Sync
- Card-based content areas with rounded corners (12dp)
- Icons next to category labels
- Gold accent color (`#c9a962`)
- Focus states: rail focused (accent bg), selected (left bar), content focused (glow border)
- D-pad: UP/DOWN navigates rail, RIGHT enters content, LEFT returns to rail
- New color scheme: darker surfaces, subtle borders, gold accent highlights
- Rounded dropdown popups (16dp) with proper alignment
- Files: `activity_settings_v2.xml`, `settings_content_*.xml`, `rail_item_*.xml`, `settings_*.xml`
- Commits: `04037aa` (initial), `5f721ed` (gold accent), `f741b8a` (dropdown alignment)

