# Vinja

## Testing

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
