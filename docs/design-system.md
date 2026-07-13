# Design System

## Visual Thesis

Quiet personal technology: soft surfaces, restrained color, clear typography, and low-pressure interactions. The UI should feel private and useful, not decorative or dashboard-like.

## Color Tokens

Existing tokens:

- `NightBackground`
- `NightSurface`
- `NightSurfaceRaised`
- `NightGlass`
- `DawnBackground`
- `DawnSurface`
- `DawnSurfaceRaised`
- `DawnGlass`
- `MoonBlue`
- `MoonBlueLight`
- `MoonGlow`
- `DreamMint`
- `DreamPink`
- `Ink`
- `InkSoft`
- `NightInk`
- `NightInkSoft`
- `Danger`

Recommended additions:

- `Success`
- `Warning`
- `Divider`
- `RecordingActive`
- `TextMuted`
- `SurfaceSecondary`

Rules:

- Avoid large purple/blue gradients.
- Avoid pure black dark mode.
- Use color as state support, not the only state signal.
- Keep day/dream markers small and simple.

## Type

Use system fonts for Chinese stability.

Hierarchy:

- Page title: strong but compact.
- Section title: smaller than page title, clear weight.
- Body: comfortable reading size.
- Metadata: smaller but readable.
- Button labels: short, direct Chinese.

Rules:

- No viewport-scaled type.
- No negative letter spacing.
- Avoid long labels inside tight buttons.

## Shape

Current app uses many 24-30dp radii. Next pass should reduce shape levels:

- Small controls: 8dp
- Inputs/buttons: 12dp
- Repeated cards: 16dp
- Recording panel: 20-24dp

Rules:

- Do not nest cards inside cards.
- Use cards only where an item or tool needs a frame.
- Prefer unframed sections for page layout.

## Spacing

Preferred scale:

- 4dp
- 8dp
- 12dp
- 16dp
- 20dp
- 24dp
- 32dp

Rules:

- Avoid arbitrary 13/17/19dp spacing.
- Keep narrow-screen density in mind.
- Preserve system safe areas.

## Components To Standardize

- App top bar
- Bottom navigation
- Primary recording button
- Recording status panel
- Entry list item
- Compact action row
- Audio player
- AI analysis section
- Error card
- Snackbar host
- Settings section
- Provider status row
- Calendar grid
- Export range picker

## Feedback

- Save success: snackbar.
- Transcription success/failure: snackbar or inline error.
- AI analysis success/failure: snackbar plus retained content.
- Delete: confirmation dialog.
- Export success: visible path and share/open action later.

## Accessibility

- Tap targets at least 48dp.
- Important icons need `contentDescription`.
- Error text must name next action.
- Controls must work with large font sizes.
- Dialogs and sheets must close normally.

