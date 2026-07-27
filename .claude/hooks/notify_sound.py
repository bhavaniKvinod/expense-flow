"""Play a short sound when Claude Code emits a Notification event.

Wired as a `Notification` hook in .claude/settings.local.json. Uses the
built-in `winsound` module (Windows only, no dependencies). Hook input JSON
is read from stdin and ignored — we only care that the event fired.
"""

import os
import sys
import winsound


def main() -> None:
    # Drain stdin so the hook's piped JSON never triggers a broken pipe.
    try:
        sys.stdin.read()
    except Exception:
        pass

    wav = os.path.join(os.environ.get("SystemRoot", r"C:\Windows"), "Media", "Ring01.wav")
    try:
        if os.path.isfile(wav):
            winsound.PlaySound(wav, winsound.SND_FILENAME)
        else:
            winsound.MessageBeep(winsound.MB_ICONASTERISK)
    except Exception:
        # A sound failure must never surface as a hook error.
        try:
            winsound.MessageBeep(winsound.MB_ICONASTERISK)
        except Exception:
            pass


if __name__ == "__main__":
    main()
