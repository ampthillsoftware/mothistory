# Agent Instructions — MOT History Android App

## Session Start — ALWAYS do this first

Before writing any code, read these files in order:

1. `memory-bank/projectbrief.md`
2. `memory-bank/systempatterns.md`
3. `memory-bank/activecontext.md`
4. `memory-bank/progress.md`
5. `memory-bank/current.md`
6. The file pointed to by `latest_log` in `memory-bank/current.md`
7. 

Once read, output a short paragraph confirming:
- The current task / active focus
- Any known blockers or open questions
- The single next action you will take

Do not write any code until this confirmation is complete.

---

## Session End — ALWAYS do this last

Before finishing, update these files:

**Always update:**
- `memory-bank/activecontext.md` — keep concise; revise Current Focus,
  Current Decisions (latest only), blockers, and Next Action. Update `_Last updated_`.
- `memory-bank/progress.md` — keep concise; update In Progress, Up Next,
  Deferred/Out of Scope.
- `memory-bank/logs/YYYY-MM-DD.md` — append today’s detailed session notes and decisions.
- `memory-bank/current.md` — set `latest_log` to today’s log and refresh `updated_at`.
- `memory-bank/logs/INDEX.md` — ensure today’s log is listed near top.

**Update only if architecture or conventions changed:**
- `memory-bank/systempatterns.md`

**Never modify without explicit user instruction:**
- `memory-bank/projectbrief.md`

Finish with a brief "Session Summary" stating what was built/changed and
which memory-bank files were updated.

---

## Task Completion — update memory continuously

After completing any meaningful task (not only at session close), update
memory-bank files as appropriate so project state does not drift. At minimum,
reflect user-visible decisions/progress in `activecontext.md`, `progress.md`,
and today’s log when those details changed.

---

## General Rules

- Never hardcode secrets. API credentials go in `local.properties` (gitignored).
- Never store the DVSA client secret in the app binary or shared prefs in plaintext.
- Always handle 429 rate-limit responses gracefully — show the user a clear message.
- All network calls must be off the main thread (use Retrofit + OkHttp with a
  background executor or ViewModel + LiveData).
- UK registration input should be uppercased and stripped of spaces before
  sending to the API.
