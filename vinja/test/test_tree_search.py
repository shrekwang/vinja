import base64
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import types
import unittest
from unittest import mock


REPO = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("search_under_test", REPO / "vinja/python/tree_search.py")
search = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(search)


class VimSearchSystemTests(unittest.TestCase):
    def test_async_search_lifecycle(self):
        executable = os.environ.get("VINJA_TEST_VIM") or shutil.which("mvim") or shutil.which("vim")
        if not executable or not shutil.which("rg"):
            self.skipTest("Vim and ripgrep are required")
        with tempfile.TemporaryDirectory(dir="/tmp") as home:
            env = dict(os.environ, HOME=home, VINJA_TEST_REPO=str(REPO),
                       PYTHONDONTWRITEBYTECODE="1")
            result = subprocess.run(
                [executable, "-v", "-Nu", "NONE", "-i", "NONE", "-n", "-es", "-V1",
                 "-S", str(REPO / "vinja/test/tree_search.vim")],
                cwd=home, env=env, capture_output=True, text=True, timeout=40)
            if result.returncode == 77:
                self.skipTest("Vim has no usable Python 3 support or jobs")
            errors = Path(home, "errors.txt")
            detail = errors.read_text() if errors.exists() else result.stdout + result.stderr
            self.assertEqual(0, result.returncode, detail)


class SearchTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory(dir="/tmp")
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.node = types.SimpleNamespace(realpath=str(self.root), get_rel_path=lambda: "src")
        self.tree = types.SimpleNamespace(project_encoding="utf-8", hide_dot_files=True, work_path_set=[])

    def request(self, text="needle"):
        return search.make_request(self.node, self.tree, text)

    def run_search(self, request):
        if not shutil.which("rg"):
            self.skipTest("ripgrep is required")
        result = subprocess.run(request["argv"], capture_output=True, text=True, timeout=5)
        self.assertIn(result.returncode, (0, 1), result.stderr)
        return search.parse_matches(result.stdout.splitlines(), request)["items"]

    def test_real_search_filters_symlinks_binary_archives_and_large_files(self):
        (self.root / ".git").mkdir()
        (self.root / ".gitignore").write_text("ignored/\n")
        for name in ("ignored", "keep", "excluded", ".hidden", "CVS"):
            (self.root / name).mkdir()
            (self.root / name / "file").write_text("needle\n")
        (self.root / "keep/loop").symlink_to(self.root, target_is_directory=True)
        (self.root / "binary").write_bytes(b"\x00needle\n")
        (self.root / "archive.ZIP").write_text("needle\n")
        (self.root / "large").write_text("needle\n" + "x" * (2 * 1024 * 1024))
        os.mkfifo(self.root / "pipe")
        items = self.run_search(self.request())
        self.assertEqual({str(self.root / "keep/file"), str(self.root / "excluded/file")},
                         {item["filename"] for item in items})
        self.tree.work_path_set = ["src/keep", "src/ignored"]
        items = self.run_search(self.request())
        self.assertEqual([str(self.root / "keep/file")], [item["filename"] for item in items])
        self.assertEqual(1, items[0]["lnum"])

    def test_literal_regex_special_names_encoding_and_column(self):
        path = self.root / "space 'quote: \u9879\u76ee.txt"
        path.write_text("  caf\u00e9 a.b\n  caf\u00e9 axb\n", encoding="latin-1")
        self.tree.project_encoding = "latin1"
        literal = self.run_search(self.request("a.b"))
        self.assertEqual(1, len(literal))
        self.assertEqual(9, literal[0]["col"])
        self.assertEqual("  caf\u00e9 a.b", literal[0]["text"])
        self.assertEqual(str(path), literal[0]["filename"])
        self.assertEqual(2, len(self.run_search(self.request("/a.b/"))))
        self.assertEqual([], self.run_search(self.request("missing")))
        self.assertEqual([], self.run_search(self.request("-g *; $(touch nope)")))
        self.assertFalse((self.root / "nope").exists())

    def test_hidden_toggle_and_project_filters_on_ancestor_directories(self):
        (self.root / ".hidden").write_text("needle")
        self.assertEqual([], self.run_search(self.request()))
        self.tree.hide_dot_files = False
        self.assertEqual(1, len(self.run_search(self.request())))
        (self.root / "allowed").mkdir()
        (self.root / "allowed/keep.py").write_text("needle")
        (self.root / "allowed/drop.txt").write_text("needle")
        self.tree.work_path_set = ["src/allowed", "src/allowed/*.py"]
        items = self.run_search(self.request())
        self.assertEqual([str(self.root / "allowed/keep.py")], [item["filename"] for item in items])

    def test_validates_delimiters_without_compiling_python_regex(self):
        for pattern in ("/", "//", "/abc"):
            with self.subTest(pattern=pattern), self.assertRaises(ValueError):
                self.request(pattern)
        self.assertIn("(?:a+/gem)[^\\n]*", self.request("/a+/gem/")["argv"])
        # The backend reports regex syntax errors asynchronously.
        self.assertIn("(?:[)[^\\n]*", self.request("/[/")["argv"])

    def test_repetitive_line_does_not_amplify_json_submatches(self):
        if not shutil.which("rg"):
            self.skipTest("ripgrep is required")
        size = 1024 * 1024
        (self.root / "repeated").write_text("a" * size)
        for pattern in ("a", "/a/", "/a*/"):
            request = self.request(pattern)
            result = subprocess.run(request["argv"], capture_output=True, text=True, timeout=5)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertLess(len(result.stdout), size * 3)
            events = [json.loads(line) for line in result.stdout.splitlines()]
            match = next(event["data"] for event in events if event["type"] == "match")
            self.assertLessEqual(len(match["submatches"]), 2)
            items = search.parse_matches(result.stdout.splitlines(), request)["items"]
            self.assertEqual(1, len(items))
            self.assertEqual(1, items[0]["col"])

    def test_visibility_cached_across_batches_and_reset_for_new_search(self):
        self.tree.work_path_set = ['src/keep']
        request = self.request()
        parser = search.MatchParser(request)
        messages = [json.dumps({'type': 'match', 'data': {
            'path': {'text': str(self.root / name)}, 'line_number': 1,
            'lines': {'text': 'needle'}, 'submatches': [{'start': 0}]}})
            for name in ('keep', 'drop')]
        with mock.patch.object(search, '_visible', wraps=search._visible) as visible:
            for _ in range(3):
                parsed = parser.parse_matches(messages * 100)
                self.assertEqual(100, len(parsed['items']))
            self.assertEqual(2, visible.call_count)
            self.tree.work_path_set = ['src/drop']
            fresh = search.MatchParser(self.request())
            parsed = fresh.parse_matches(messages)
            self.assertEqual([str(self.root / 'drop')], [item['filename'] for item in parsed['items']])
            self.assertEqual(4, visible.call_count)

    def test_json_bytes_long_lines_and_unrepresentable_paths(self):
        request = self.request()
        event = {"type": "match", "data": {
            "path": {"text": str(self.root / "file")}, "line_number": 3,
            "lines": {"bytes": base64.b64encode(b"  needle\xff" + b"x" * 1000).decode()},
            "submatches": [{"start": 2, "end": 8}]}}
        parsed = search.parse_matches([json.dumps(event)], request)
        self.assertEqual(500, len(parsed["items"][0]["text"]))
        self.assertEqual(3, parsed["items"][0]["col"])
        event["data"]["path"] = {"bytes": base64.b64encode(b"/tmp/\xff").decode()}
        self.assertEqual({"items": [], "skipped": 1}, search.parse_matches([json.dumps(event)], request))


if __name__ == "__main__":
    unittest.main()
