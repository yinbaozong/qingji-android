# UI Redesign Plan

## Product Direction

The app should feel like a quiet personal capture tool: calm, private, lightweight, and suited to both just-woke dream capture and daytime notes. The interface should avoid dashboard density, loud gradients, and generic Material sample styling.

Current product name in the app is `瞬记`. The historical package and project names remain unchanged to avoid breaking Android installation and data paths.

## Principles

- The first useful action should always be obvious.
- Recording is the primary workflow; everything else supports reviewing and refining records.
- Detail pages should privilege reading and editing over controls.
- Settings should disclose complexity only when needed.
- Every async action should produce short feedback.
- No UI change may erase existing Room data, audio paths, API keys, provider config, tags, photos, todos, or export behavior.

## Phase 1: Documentation And Audit

Status: in progress.

Deliverables:

- `docs/ui-redesign-audit.md`
- `docs/ui-redesign-plan.md`
- `docs/design-system.md`
- `docs/navigation-and-user-flow.md`

## Phase 2: Theme And Shared Components

Scope:

- Introduce missing design tokens for spacing, shape, and state colors.
- Reduce page-local `Color(...)` usage.
- Add shared controls for icon/text actions, compact section headers, status messages, and confirmation dialogs.
- Keep business logic untouched.

Acceptance:

- App compiles.
- Light and dark mode retain contrast.
- Home, Detail, Calendar, and Mine use consistent spacing and shape.

## Phase 3: Home And Recording

Scope:

- Make Home less like a dashboard and more like a recording workspace.
- Keep day/night buttons, duration, and real amplitude bars.
- Improve permission, transcription, and failed-provider states.
- Consider a bottom recording panel with pause/resume/cancel later.

Acceptance:

- Recording day and night entries still works.
- Transcription errors preserve audio and record.
- Buttons remain reachable above system gesture area.

## Phase 4: Detail

Scope:

- Move toward read-first detail with explicit edit state.
- Keep title editing, body editing, photos, todos, tags, audio, transcription, AI, chat.
- Add delete confirmation.
- Replace basic audio buttons with a compact player.
- Keep save feedback as snackbar.

Acceptance:

- Saving updates title/content/tags/photos/todos.
- Manual transcription still works.
- AI analysis and chat still work.
- Delete requires confirmation.

## Phase 5: Calendar

Scope:

- Keep default expanded month grid and horizontal swipe.
- Add today shortcut.
- Tighten markers and daily records.
- Preserve year picker.

Acceptance:

- Selecting a day updates the record list.
- Month changes do not lose record markers.
- Empty state is clear and compact.

## Phase 6: Mine And Settings

Scope:

- Convert long settings into grouped sections.
- Move provider configuration into focused expandable sheets or sub-sections.
- Keep export center, tag management, dark mode, AI prompt, and provider tests.

Acceptance:

- API keys persist.
- Provider tests work.
- Export supports TXT and ZIP rules.
- Custom tags appear in Detail.

## Phase 7: Verification

Run:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 assembleDebug
```

Manual checks:

- Home empty state
- Home recording state
- Transcription progress and failure
- Detail edit/save
- Detail photos and todos
- AI analysis and chat
- Calendar swipe/year/day selection
- Mine export and provider settings
- Light/dark mode

