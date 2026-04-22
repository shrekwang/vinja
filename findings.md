# Findings: Python 3.13 Compatibility Audit for Vinja

## Date: 2026-04-12

---

## Finding 1: `distutils` removed (CRITICAL)

**File:** `vinja/python/common.py:13-14`
```python
from distutils import dir_util
from distutils import file_util
```

**Usage locations:**
- `FileUtil.fileOrDirCp()` (line 99-104): Uses `dir_util.copy_tree()` and `file_util.copy_file()`

**Impact:** Plugin will crash immediately on Python 3.12+ when any file copy operation is attempted in the project tree or shell.

**Replacement:**
- `dir_util.copy_tree(src, dst)` -> `shutil.copytree(src, dst, dirs_exist_ok=True)`
- `file_util.copy_file(src, dst)` -> `shutil.copy2(src, dst)`

`shutil` is already imported at line 7, so no new dependency needed.

---

## Finding 2: BeautifulSoup v3 in `javadoc.py`

**File:** `vinja/python/javadoc.py:1`
```python
from BeautifulSoup import *
```

**Impact:** BeautifulSoup 3 (`BeautifulSoup` package) has been unmaintained since 2012. It won't install cleanly on Python 3.13. The replacement is `beautifulsoup4` (`bs4` package).

**API differences:**
- `ICantBelieveItsBeautifulSoup(page)` -> `BeautifulSoup(page, 'html.parser')`
- `node.__next__` -> use bs4 `.next_element`
- `node.nextSibling` -> `node.next_sibling`
- `Comment` class import changes

---

## Finding 3: Python 2 code in `getscript.py`

**File:** `vinja/util/getscript.py`
```python
import urllib2                              # Python 2 only
from BeautifulSoup import BeautifulSoup     # BS v3
print "downlading %s" % script_name        # print statement
```

**Impact:** This is a standalone utility script for downloading Vim scripts. It's not loaded by the Vim plugin at runtime. Low priority, but should be modernized or marked as legacy.

---

## Finding 4: `pyparsing` star import in `jde.py`

**File:** `vinja/python/jde.py:14`
```python
from pyparsing import *
```

**Used names from pyparsing:**
- `Forward`, `Optional`, `OneOrMore`, `Suppress`, `delimitedList`
- `Word`, `Keyword`, `Group`, `Combine`, `restOfLine`, `lineEnd`
- `alphas`, `alphanums`, `javaStyleComment`, `oneOf`

**Impact:** `pyparsing` 3.x (current) maintains backward compatibility for these names via star import. This should work fine on Python 3.13 as long as `pyparsing` is installed.

---

## Finding 5: `optparse` in `shext.py`

**File:** `vinja/python/shext.py:4`
```python
from optparse import OptionParser
```

**Impact:** `optparse` is still in Python 3.13 stdlib. It's "soft deprecated" (docs recommend `argparse`) but NOT removed. No action needed.

---

## Finding 6: PEP 594 module audit (Python 3.13 removals)

Searched all Python files for usage of any PEP 594 removed modules:
- `aifc`, `audioop`, `cgi`, `cgitb`, `chunk`, `crypt`, `imghdr`, `mailcap`, `msilib`, `nis`, `nntplib`, `ossaudiodev`, `pipes`, `sndhdr`, `spwd`, `sunau`, `telnetlib`, `uu`, `xdrlib`

**Result:** None of these modules are used anywhere in the codebase. Clean.

---

## Finding 7: `sqlite3` usage is fine

**Files:** `shext.py`, `javadoc.py`, `dbext.py`

`sqlite3` remains in the stdlib and is not affected by any Python 3.13 changes.

---

## Finding 8: No `setup.py` / `pyproject.toml`

The project has no Python packaging metadata (`setup.py`, `setup.cfg`, `pyproject.toml`). It's distributed as a Vim plugin with embedded Python files. This means:
- No automated dependency resolution
- Users must manually install `pyparsing` and `beautifulsoup4`
- Should document this in README

---

## Finding 9: Vim `py3` / `py3file` interface

The plugin uses Vim's embedded Python 3 via:
- `py3 <command>` in `.vim` files
- `py3file <path>` to source `.py` files
- `import vim` in Python code

This interface depends on Vim being compiled with `+python3` and linked against the specific Python version. Python 3.13 support requires Vim 9.1+ (check Vim changelog for exact version).

---

## Finding 10: Implicit relative imports via `sys.path` manipulation

**Critical mechanism:** The codebase uses bare module names for cross-file imports:
```python
# locate.py
from shext import LocateCmd
from common import VimUtil

# tree.py
from common import ZipUtil,FileUtil,VimUtil,PathUtil
from jde import ProjectManager,EditUtil

# dbtree.py
from tree import TreeNode, get_current_tree, ...

# jde.py
from common import output,VinjaConf,MiscUtil,VimUtil,BasicTalker,ZipUtil,PathUtil
```

**How it works today:** `common.py:initVinja()` (line 799) does `sys.path.append(VinjaConf.getAppHome())` which adds the `vinja/python/` directory to `sys.path`. After that, `from common import ...` resolves as an absolute import against that `sys.path` entry.

**Why this matters for Python 3.13:**
- Python 3 (since 3.0, PEP 328) requires explicit relative imports (`from .common import ...`) for intra-package imports. **However**, this codebase is NOT a Python package — there is no `__init__.py` in `vinja/python/`.
- Instead, the files are loaded individually via Vim's `py3file` command, and `sys.path` is manipulated to make the directory findable.
- This pattern **still works** in Python 3.13 because the bare names resolve as absolute imports against the `sys.path` entry. It's not using the removed "implicit relative import" mechanism (which was a Python 2 feature where `import foo` inside a package would find `foo.py` in the same directory before searching `sys.path`).

**Potential risk:** If Vim's `py3file` mechanism changes how the current working directory or `__file__` attribute is set, or if Python tightens `sys.path` behavior, these imports could break. But this is unlikely in Python 3.13.

**Load order dependency:** The files must be loaded in the right order:
1. `common.py` is loaded first (by `vinja.vim` line 401: `call RunSzPyfile("common.py")`)
2. `common.py`'s `initVinja()` runs at import time and adds the directory to `sys.path`
3. Then `tree.py` is loaded (line 402: `call RunSzPyfile("tree.py")`)
4. Other files (`jde.py`, `shext.py`, `locate.py`, etc.) are loaded on demand via their Vim functions

**Verdict:** The current import pattern works in Python 3.13, but it's fragile. There is no `__init__.py` file, so these are NOT package-relative imports — they are absolute imports resolved via `sys.path`. This is a valid pattern but worth documenting.

**Future improvement (optional):** Add an `__init__.py` to `vinja/python/` and convert to explicit relative imports (`from .common import ...`). This would make the import mechanism more robust and explicit. However, this is complex because `py3file` doesn't natively support package-relative imports.

---

## Finding 11: No type hints or modern Python features needed

The codebase uses Python 2-era idioms (classes inheriting from `object`, `type()` comparisons, etc.) but these all still work in Python 3.13. No forced modernization is needed beyond the critical fixes.
