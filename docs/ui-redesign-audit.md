# UI Redesign Audit

## Current Scope

Project: `E:\opai\DreamJournalApp`

Stack retained:

- Android native
- Kotlin
- Jetpack Compose
- Material 3
- Room
- ViewModel
- Repository
- Navigation Compose
- DataStore
- Local audio recording
- Replaceable speech-to-text providers
- Replaceable AI analysis providers

This audit is based on the current worktree after the `1.3.2` build. The app has evolved from a dream-only recorder into a lightweight personal recorder with day records, night/dream records, photos, todos, AI analysis, calendar lookup, settings, and export.

## Current Pages

- Home: quick day/night recording, recording status, recent records.
- Detail: editable title, body, tags, photos, todos, audio playback, transcription, AI analysis, AI chat, delete.
- Calendar: month pager with day/dream markers and daily record list.
- Mine/Settings: stats, dark mode, custom tags, daily dream summary, export, provider configuration, version.

## Navigation

- Bottom navigation has three destinations: Home, Calendar, Mine.
- Detail is opened from Home and Calendar with `detail/{entryId}`.
- Recording completion creates a record and navigates into Detail.
- System speech recognition returns text and creates a record.

## Main Tasks Per Page

- Home: start recording quickly and review recent entries.
- Detail: read, edit, enrich, analyze, chat, and manage one record.
- Calendar: find records by day and identify days with day or dream entries.
- Mine: configure services, manage tags, export data, review stats, change appearance.

## Current Layout Observations

- Home uses a compact brand header, recent list, and fixed bottom recording buttons.
- Detail is now closer to an editor, but the toolbar still mixes content actions and record metadata.
- Calendar now opens directly to the month grid, which better matches user expectation.
- Mine still carries many settings in one scroll; advanced provider settings are folded, but the page remains dense.

## Reusable Components

- `DreamBrandMark`
- `DreamBrandTitle`
- Home record buttons and entry cards
- Calendar day cell and entry card
- Settings cards, provider cards, stats cards, range picker

These are useful but not yet a coherent component system. Shape, spacing, and buttons still vary too much between pages.

## Theme And Color

- Current theme uses dawn/night color schemes in `Color.kt` and `Theme.kt`.
- The visual direction is calm, private, and soft, but some pages still lean into large rounded cards.
- There are still direct `Color.White` and `Color.Black` usages in page files for special states.

## Typography

- System fonts are used, which is correct for Chinese compatibility.
- Current hierarchy works, but page sections often use similar title sizes, so scan priority can blur.
- Detail body should become more reading-oriented and less input-box-like over time.

## Recording Flow

- Home offers two modes: day and night.
- Recording button shows elapsed seconds and audio level bars.
- Stop creates a record, saves audio locally, attempts transcription, then navigates to Detail.
- Errors use snackbars and do not show `null`.

## Transcription Flow

- Automatic transcription runs after recording when a cloud provider is selected.
- Manual transcription exists in Detail.
- Providers include mock/system/Baidu/Aliyun/OpenAI-compatible.
- Failed transcription retains the record and audio.

## AI Analysis Flow

- AI provider can be configured in Mine.
- Custom analysis prompt is stored in DataStore.
- Detail shows a single AI analysis result rather than many small fields.
- AI chat remains available as a secondary section.

## Calendar Interaction

- Calendar displays current month by default.
- Horizontal swipe changes month.
- Year picker is available from the year text.
- Day records and dream records use two small markers.
- Daily records show below the calendar and can scroll.

## Settings Flow

- Mine contains stats, custom tags, export, appearance, provider setup, and AI prompt configuration.
- Advanced settings are folded by default.
- Provider fields are still long and should eventually move to focused sheets or sub-pages.

## Export Flow

- Export is centralized in Mine.
- Single, range, and all export paths are supported.
- Text-only export produces TXT.
- Entries with audio/photos export as ZIP.
- Export includes text, audio, photos, and todos.

## UI To Preserve

- Fixed bottom recording entry on Home.
- Clear day/night recording split.
- Calendar month swipe.
- Tag management.
- Export center in Mine.
- Dark mode switch.
- Manual transcription in Detail.
- Single AI analysis box.

## UI To Refactor Further

- Home header can be lighter and less card-like.
- Detail needs a true read/edit rhythm instead of always-on input fields.
- Detail audio should become a small player with progress.
- Mine should become grouped settings with sheets for provider configuration.
- Delete should use confirmation.
- Buttons should use a consistent hierarchy and fewer rounded pill shapes.

## Maturity Issues

- Some pages still feel like stacked form cards.
- Settings remains dense for non-technical users.
- Detail actions are compact but not yet icon/tool-bar polished.
- There are no Compose previews for the target states.
- No emulator screenshot verification has been captured in this repo.

## Top Usability Problems

- Provider configuration is still intimidating.
- Detail editing is functional but visually busy on narrow screens.
- Audio playback lacks duration/progress.
- Delete is easy to tap without confirmation.
- Export path is visible, but opening/sharing export is not yet streamlined.

## Top Visual Problems

- Cards use large radii repeatedly.
- Surface hierarchy is similar across many sections.
- Some controls use text buttons where icons plus short labels would scan better.
- Settings has too many same-weight sections.
- Home still uses a brand card that consumes first-screen space.

## Refactor Risks

- Room migrations must remain additive and non-destructive.
- Provider settings and API keys must not be reset.
- Recording and transcription state must keep navigation behavior intact.
- Export must keep local file paths valid.
- Detail changes must not discard unsaved edits.

## Recommended Order

1. Stabilize design tokens and shared controls.
2. Refine Home recording as the visual standard.
3. Refine Detail read/edit/audio/delete.
4. Refine Calendar density and empty states.
5. Split Mine into clearer settings groups.
6. Add Compose previews and emulator screenshots.
7. Run full regression on recording, transcription, AI, export, and database migration.

