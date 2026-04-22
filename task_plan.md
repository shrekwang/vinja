# Task Plan: Support Python 3.13 for Vinja

## Goal
Migrate the Vinja vim plugin's Python codebase to be compatible with Python 3.13, while maintaining backward compatibility with Python 3.9+.

## Project Overview
Vinja is a Vim plugin for Java development (and general editing) that uses Python 3 (via `py3` / `py3file` Vim commands) to implement features like:
- Java code completion, compilation, and debugging (`jde.py`)
- A built-in shell (`shext.py`)
- Project tree navigation (`tree.py`)
- Database query UI (`dbext.py`)
- File/class locator (`locate.py`)
- Javadoc browser (`javadoc.py`)

All Python files live in `vinja/python/` and are loaded via Vim's embedded Python 3 interpreter.

---

## Phases

### Phase 1: Remove `distutils` usage (CRITICAL - removed in Python 3.12)
- **Status:** `pending`
- **File:** `vinja/python/common.py`
- **Issue:** Lines 13-14 import `from distutils import dir_util` and `from distutils import file_util`
  - `dir_util.copy_tree()` is used in `FileUtil.fileOrDirCp()`
  - `file_util.copy_file()` is used in `FileUtil.fileOrDirCp()`
- **Fix:** Replace with `shutil` equivalents:
  - `dir_util.copy_tree(src, dst)` -> `shutil.copytree(src, dst, dirs_exist_ok=True)` (Python 3.8+)
  - `file_util.copy_file(src, dst)` -> `shutil.copy2(src, dst)`
- **Note:** `shutil` is already imported in `common.py`, making this straightforward.

### Phase 2: Update `pyparsing` imports in `jde.py`
- **Status:** `pending`
- **File:** `vinja/python/jde.py`
- **Issue:** Line 14 uses `from pyparsing import *`. Modern `pyparsing` (3.x) has reorganized its API.
  - `oneOf` -> still available but deprecated import path
  - `Optional`, `OneOrMore`, `Forward`, `Suppress`, `delimitedList`, `Word`, `Keyword`, `Group`, `Combine`, `restOfLine`, `lineEnd`, `alphas`, `alphanums`, `javaStyleComment` are all used
- **Fix:** 
  - Verify `pyparsing` 3.x compatibility (star import still works for backward compat)
  - If using `pyparsing` >= 3.0, the `from pyparsing import *` still works but some names moved
  - Ensure `pyparsing` is installed in the Vim Python environment: `pip install pyparsing`
  - Test that Java parsing still works correctly
- **Risk:** Low - pyparsing 3.x maintains backward compatibility for star imports

### Phase 3: Fix `javadoc.py` - BeautifulSoup v3 -> v4
- **Status:** `pending`
- **File:** `vinja/python/javadoc.py`
- **Issue:** Line 1 uses `from BeautifulSoup import *` (BeautifulSoup v3, which is Python 2 era and unmaintained)
- **Fix:**
  - Change `from BeautifulSoup import *` to `from bs4 import BeautifulSoup, Comment`
  - Change `ICantBelieveItsBeautifulSoup(page)` to `BeautifulSoup(page, 'html.parser')`
  - Update `node.__next__` to `next(node)` or use bs4's navigation methods
  - Update `node.nextSibling` to `node.next_sibling` (bs4 naming convention)
  - Install `beautifulsoup4`: `pip install beautifulsoup4`
- **Risk:** Medium - BeautifulSoup API changed significantly between v3 and v4

### Phase 4: Fix `getscript.py` - Python 2 code
- **Status:** `pending`
- **File:** `vinja/util/getscript.py`
- **Issue:** This file is pure Python 2 code:
  - Line 1: `import urllib2` (Python 2 only)
  - Line 2: `from BeautifulSoup import BeautifulSoup` (BS v3)
  - Line 47: `print "downlading %s" % script_name` (print statement, not function)
- **Fix:**
  - Replace `urllib2` with `urllib.request`
  - Replace BS3 with BS4
  - Fix print statements to function calls
- **Risk:** Low - this is a standalone utility script, not loaded by Vim

### Phase 5: Audit `optparse` usage in `shext.py`
- **Status:** `pending`
- **File:** `vinja/python/shext.py`
- **Issue:** Line 4 uses `from optparse import OptionParser`. `optparse` is soft-deprecated in favor of `argparse`, but NOT removed in Python 3.13.
- **Fix:** No immediate action needed. `optparse` still works in Python 3.13. Consider migrating to `argparse` as a future improvement.
- **Risk:** None for 3.13 compatibility

### Phase 6: Check `OptionParser` in `shext.py` (INFORMATIONAL)
- **Status:** `pending`  
- **Note:** The custom `ShextOptionParser` subclass overrides `exit()` and `error()` methods. This pattern works fine in Python 3.13.

### Phase 7: Verify Python 3.13 Vim support
- **Status:** `pending`
- **Issue:** The plugin requires Vim compiled with Python 3 support (`+python3`). Need to verify:
  - Vim 9.x supports Python 3.13 embedding
  - NeoVim (if applicable) supports Python 3.13 via `pynvim`
- **Fix:** Document minimum Vim version requirement for Python 3.13 support
- **Risk:** External dependency - not in our control

### Phase 8: Audit cross-file import mechanism
- **Status:** `pending`
- **Files:** All Python files
- **Issue:** All inter-module imports use bare names (`from common import ...`, `from jde import ...`, etc.) without an `__init__.py` package marker. This works because `common.py:initVinja()` calls `sys.path.append(VinjaConf.getAppHome())` to add the `vinja/python/` directory to `sys.path`.
- **Import chain:**
  - `locate.py` -> `from shext import LocateCmd`, `from common import VimUtil`
  - `tree.py` -> `from common import ...`, `from jde import ...`
  - `dbtree.py` -> `from common import ...`, `from jde import ...`, `from tree import ...`
  - `jde.py` -> `from common import ...` (also `from tree import get_all_trees` inside a function)
- **Analysis:** These are NOT implicit relative imports (the Python 2 feature removed in Python 3). They are absolute imports resolved via `sys.path`. This pattern is valid in Python 3.13.
- **Risk:** Low. The mechanism works, but depends on load order (`common.py` must be loaded first to set up `sys.path`). Vim's `vinja.vim` handles this correctly (line 401-402 loads `common.py` then `tree.py`).
- **Optional future fix:** Add `__init__.py` and convert to explicit relative imports. Not required for Python 3.13 compatibility.

### Phase 9: Audit remaining stdlib usage for 3.13 removals
- **Status:** `pending`
- **Files:** All Python files
- **Modules removed in Python 3.13 (PEP 594):** `aifc`, `audioop`, `cgi`, `cgitb`, `chunk`, `crypt`, `imghdr`, `mailcap`, `msilib`, `nis`, `nntplib`, `ossaudiodev`, `pipes`, `sndhdr`, `spwd`, `sunau`, `telnetlib`, `uu`, `xdrlib`
- **Modules removed in Python 3.12:** `distutils` (already covered in Phase 1), `imp`, `asynchat`, `asyncore`, `smtpd`
- **Check:** None of the PEP 594 modules appear to be used in the codebase. Confirmed clean.
- **Risk:** None

### Phase 10: Test and document
- **Status:** `pending`
- **Tasks:**
  - Create a compatibility note in README
  - Document Python version requirements
  - Document required pip packages (`pyparsing`, `beautifulsoup4`)

---

## Summary of Breaking Changes

| Priority | File | Issue | Fix Difficulty |
|----------|------|-------|----------------|
| CRITICAL | `common.py` | `distutils` removed in 3.12 | Easy |
| HIGH | `javadoc.py` | BeautifulSoup v3 obsolete | Medium |
| HIGH | `getscript.py` | Pure Python 2 code | Easy (standalone) |
| LOW | `jde.py` | `pyparsing` star import | Verify only |
| NONE | `shext.py` | `optparse` soft-deprecated | No action needed |

## Dependencies to Install
```
pip install pyparsing beautifulsoup4
```

## Backward Compatibility Target
- Python 3.9+ (for `shutil.copytree(dirs_exist_ok=True)` introduced in 3.8)
- Vim 9.0+ with `+python3` compiled against Python 3.13
