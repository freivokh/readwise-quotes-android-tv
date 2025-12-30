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
| U1 | Update aesthetic design - cleaner, smaller font | Pending | | Use frontend-design skill |
| U2 | Make settings menu cleaner and more aesthetic | Pending | | |
| U3 | Show highlight tags in UI (bottom right corner) | Pending | | Data exists, just need to display |
| U4 | Option to show book cover art | Pending | | Data synced, UI not implemented |
| U5 | Quotes getting cut off / resize issues | Pending | | Has adaptive sizing but no truncation |

---

### Navigation & Controls

| ID | Feature | Status | Priority | Notes |
|----|---------|--------|----------|-------|
| N1 | D-pad left/right cycles quotes (forward/back) | Pending | | Random on load, sequential nav after |

---

### New Features

| ID | Feature | Status | Priority | Notes |
|----|---------|--------|----------|-------|
| F1 | QR code linking to original document/Readwise | Pending | | Optional, bottom right |
| ~F3~ | Per-group match mode (ANY/ALL) | Done | | Each group has own toggle |

---

### Settings & Configuration

| ID | Feature | Status | Priority | Notes |
|----|---------|--------|----------|-------|
| S1 | API token entry without manual keyboard | Pending | | QR code? Deep link? |
| S2 | Show/hide password toggle for API token | Pending | | Currently always masked |
| S3 | Funny note about one-time token entry | Pending | | UX improvement |
| S4 | Optional sync methods | Pending | | Need to clarify scope |

---

### Development & Testing

| ID | Feature | Status | Priority | Notes |
|----|---------|--------|----------|-------|
| D1 | Switch to Android Studio emulator | Pending | | Document setup |
| D2 | Document current emulation setup | Pending | | |

---

## Remaining Features Summary

**To Implement (11 items):**

1. **U1** - Aesthetic redesign (fonts, cleaner look)
2. **U2** - Settings menu visual refresh
3. **U3** - Display tags on quote screen
4. **U4** - Show book cover art option
5. **U5** - Fix quote text overflow/sizing
6. **N1** - D-pad left/right quote navigation
7. **F1** - QR code for source link
8. **S1** - Alternative token entry method
9. **S2** - Password visibility toggle
10. **S3** - One-time entry note
11. **S4** - Optional sync methods

---

## Implementation Log

### 2025-12-30: Per-group match mode (F3)
- Added `matchMode` field to TagGroup model
- Each group row now has ANY/ALL toggle button
- Removed global match mode toggle
- Query: `(Group A tags with its mode) OR (Group B tags with its mode)`
- Commit: `8d5904e`

