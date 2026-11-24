# Vinja

## Testing

Run all Python tests with `python3 -m unittest discover -s vinja/test -v`.
Run the SQLite table-listing regression tests with:

```sh
python3 -m unittest discover -s vinja/test -p 'test_dbext_sqlite.py' -v
```

These integration tests use a real in-memory SQLite database and stub Vim UI
calls. They cover rendered table listings, name filtering, quoted names, and
empty matches. They require only the Python standard library; no configuration,
secrets, or external database are needed.

Run the saved project-tree root tests from the repository root:

```sh
python3 -m unittest discover -s vinja/test -p 'test_ptree_roots.py' -v
```

Run a category by adding `-k VimSystem`, `-k CommandTests`, or `-k ConfigTests`.

The standard-library suite covers configuration preservation, aliases, path
identity, atomic-write failures, current-tab/displayed-root selection, and empty
selector results. Python integration tests stub Vim and unrelated Java/shell
modules. All test homes and writable configuration live under `/tmp`; the user's
actual configuration is not modified. No secrets or `.env` are required.

The system test loads the real plugin in headless Vim and exercises both Ex
commands, C/U/B navigation, hidden panels, tab isolation, and the root selector.
It requires the plugin's Python dependencies (`pyparsing`, `beautifulsoup4`) and
Vim with usable Python 3 support. It prefers `mvim`, then `vim`, and skips when
Python 3 support is unavailable. Set `VINJA_TEST_VIM` to choose another executable.
For MacVim needing an explicit Python library, run:

```sh
VINJA_TEST_PYTHON_DLL=/Library/Frameworks/Python.framework/Versions/3.13/Python \
  python3 -m unittest discover -s vinja/test -p 'test_ptree_roots.py' -v
```

Adjust the library path to the installed Python runtime. Existing configuration
readers may emit `ResourceWarning` messages for unclosed files; those are outside
this feature's changes.

Run the tab-selector regression tests with:

```sh
python3 -m unittest discover -s vinja/test -p 'test_locate_tabs.py' -v
```

These tests stub Vim and verify that opening the selector preserves the original
current-tab label for display and filtering. They require only the Python
standard library and no configuration or secrets.

Run the asynchronous project-tree search regressions with:

```sh
python3 -m unittest discover -s vinja/test -p 'test_tree_search.py' -v
```

Add `-k VimSearchSystem` for the headless Vim lifecycle test or `-k SearchTests`
for backend tests. Backend integration tests require `rg` on PATH. The system
case also requires Vim jobs/channels/timers, Python 3, and the plugin dependencies
listed above; the same `VINJA_TEST_VIM` and `VINJA_TEST_PYTHON_DLL` overrides apply.
Missing prerequisites cause explicit skips. Tests use only temporary homes and
fixtures under `/tmp`, with no secrets or `.env`.

Coverage includes f/F entry points, asynchronous cancellation/replacement,
quickfix auto-opening for nonempty completion, isolation across tabs, freed
quickfix lists, no-match/error states,
result/display limits, literal and regex searches, encoding, special filenames,
hidden and workset filters, symlink loops, binary/archive/large-file exclusion,
and FIFOs. All are included in the standard full-suite discovery command.

Run the project-tree status/shutdown regressions with:

```sh
python3 -m unittest discover -s vinja/test -p 'test_tree_status.py' -v
```

Add `-k VimStatusSystem` for the real headless Vim test or `-k StatusTests`
for Python integration tests. The system test uses the same Vim/Python overrides
and prerequisites described above. It checks multi-tab shutdown with 60 extra
loaded buffers, one save per live tree (including hidden panels), no shutdown
edit-flag synchronization or rendering, preserved file history, cancelled quits,
and idempotent, buffer-owner-aware panel unload hooks. Integration tests check
UI-free state serialization, lazy-directory preservation, exclusion of closed
tabs, and continuation after a state-file write failure. Fixtures live under
`/tmp`; no secrets or user configuration are needed.

Run the tree-loading performance regressions with:

```sh
python3 -m unittest discover -s vinja/test -p 'test_tree_loading.py' -v
```

These standard-library tests verify lazy nested-project discovery, ancestor
symlink-cycle prevention without blocking external directory links, linear
child-reuse comparisons during refresh, and file-to-directory replacement.
They use temporary fixtures under `/tmp` and stub Vim and SDK discovery.
The status system test also covers real-plugin cycle handling and `TabClosed`
registry cleanup while preserving hidden trees in surviving tabs. The search
suite checks visibility caching across batches (including rejected files), fresh
filters for each search, and parser release on completion, failure, truncation,
cancellation, replacement, and freed quickfix lists. The same Vim prerequisites
and environment overrides apply; no secrets or `.env` are required.

Run the tree-restoration regressions with:

```sh
python3 -m unittest discover -s vinja/test -p 'test_tree_restore.py' -v
```

Add `-k VimRestoreSystem` for the headless Vim test or `-k RestoreTests`
for Python integration tests. The system test checks a single final render when
restoring ten saved files, preserved edit flags/history and cursor selection,
and reopening panels without replaying stale disk state or closed buffers.
Integration tests cover rendering after tree-type restoration and resuming normal
edit rendering after a restoration error. The same Vim prerequisites and
`VINJA_TEST_VIM`/`VINJA_TEST_PYTHON_DLL` overrides apply. Fixtures and temporary
homes live under `/tmp`; no secrets or user configuration are needed.

Run the file-opening regressions with:

```sh
python3 -m unittest discover -s vinja/test -p 'test_tree_open.py' -v
```

Add `-k VimOpenSystem` for the headless Vim test or `-k OpenTests` for Python
integration tests. Coverage includes one redraw for a fresh file, no redraw for
unchanged edit flags, MRU history, failed edits, Enter/preview/split/tab focus,
tree cursor preservation, directory toggles, and unloading/reopening files.
The system test uses the same Vim prerequisites and environment overrides above.
Fixtures live under `/tmp`; no secrets or user configuration are needed.
