import contextlib
import importlib
import io
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import types
import unittest
from unittest import mock


REPO = Path(__file__).resolve().parents[2]


class VimSystemTests(unittest.TestCase):
    def test_commands_with_real_plugin_startup(self):
        executable = os.environ.get("VINJA_TEST_VIM") or shutil.which("mvim") or shutil.which("vim")
        if not executable:
            self.skipTest("Vim is not installed")
        with tempfile.TemporaryDirectory(dir="/tmp") as home:
            env = dict(os.environ, HOME=home, VINJA_TEST_REPO=str(REPO),
                       PYTHONDONTWRITEBYTECODE="1")
            result = subprocess.run(
                [executable, "-v", "-Nu", "NONE", "-i", "NONE", "-n", "-es", "-V1",
                 "-S", str(REPO / "vinja/test/ptree_roots.vim")],
                cwd=home, env=env, capture_output=True, text=True, timeout=30)
            if result.returncode == 77:
                self.skipTest("Vim has no usable Python 3 support")
            errors = Path(home, "errors.txt")
            detail = errors.read_text() if errors.exists() else result.stdout + result.stderr
            self.assertEqual(0, result.returncode, detail)


class IsolatedPluginTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.scope = contextlib.ExitStack()
        cls.addClassCleanup(cls.scope.close)
        home = cls.scope.enter_context(tempfile.TemporaryDirectory(dir="/tmp"))
        cls.scope.enter_context(mock.patch.dict(os.environ, HOME=home))
        cls.scope.enter_context(mock.patch.object(sys, "path", [str(REPO / "vinja/python")] + sys.path))
        cls.scope.enter_context(mock.patch("logging.basicConfig"))
        cls.vim = types.ModuleType("vim")
        cls.vim.eval = mock.Mock(return_value=str(REPO / "vinja"))
        cls.vim.command = mock.Mock()
        jde = types.ModuleType("jde")
        jde.ProjectManager = jde.EditUtil = jde.Parser = mock.Mock()
        shext = types.ModuleType("shext")
        shext.LocateCmd = mock.Mock()
        cls.scope.enter_context(mock.patch.dict(sys.modules, vim=cls.vim, jde=jde, shext=shext))
        for name in ("common", "tree", "locate"):
            sys.modules.pop(name, None)
        cls.common = importlib.import_module("common")
        cls.tree = importlib.import_module("tree")
        cls.locate = importlib.import_module("locate")

    def setUp(self):
        directory = tempfile.TemporaryDirectory(dir="/tmp")
        self.addCleanup(directory.cleanup)
        self.home = Path(directory.name)
        self.path = self.home / "ptree_roots.cfg"
        self.root = self.home / "project"
        self.root.mkdir()
        self.config = self.common.ProjectTreeRootsConfig(str(self.path))
        patcher = mock.patch.object(self.common.VinjaConf, "getDataHome", return_value=str(self.home))
        patcher.start()
        self.addCleanup(patcher.stop)
        self.tree._tree_registry.clear()
        self.vim.command.reset_mock()
        self.vim.eval.side_effect = lambda expr: "1" if expr == "tabpagenr()" else "tab-one"

    def register_tree(self, root=None, tab="tab-one"):
        tree = self.tree.ProjectTree.__new__(self.tree.ProjectTree)
        tree.root = root or self.tree.NormalDirNode("project", str(self.root), tree)
        tree.anchor_root = tree.root
        self.tree._tree_registry[tab] = tree
        return tree

    def invoke(self, method, *args):
        with contextlib.redirect_stdout(io.StringIO()) as output:
            method(*args)
        return output.getvalue()


class CommandTests(IsolatedPluginTests):
    def test_add_select_remove_round_trip_without_ui_side_effects(self):
        tree = self.register_tree()
        self.assertIn("saved", self.invoke(self.tree.ProjectTree.add_saved_root))
        manager = self.locate.ProjectTreeRootContentManager()
        self.assertEqual({"project": str(self.root)}, manager.path_dict)
        self.vim.command.assert_not_called()
        manager.open_content(manager.search_content("proj")[0])
        self.vim.command.assert_called_once_with("call ProjectTree('%s')" % self.root)
        self.vim.command.reset_mock()
        self.assertIn("configuration only", self.invoke(self.tree.ProjectTree.remove_saved_root))
        self.assertEqual([], self.locate.ProjectTreeRootContentManager().entries)
        self.assertTrue(self.root.is_dir())
        self.assertIs(tree, self.tree.get_current_tree())
        self.vim.command.assert_not_called()

    def test_displayed_root_and_tab_isolation(self):
        tree = self.register_tree()
        child = self.root / "child"
        child.mkdir()
        tree.root = self.tree.NormalDirNode("child", str(child), tree)
        other = self.register_tree(tab="tab-two")
        self.invoke(self.tree.ProjectTree.add_saved_root, "child")
        self.assertEqual("child=%s\n" % child, self.path.read_text())
        self.vim.eval.side_effect = lambda expr: "2" if expr == "tabpagenr()" else "tab-two"
        self.invoke(self.tree.ProjectTree.add_saved_root, "other")
        self.invoke(self.tree.ProjectTree.remove_saved_root)
        self.assertEqual("child=%s\n" % child, self.path.read_text())
        self.assertIs(other.root, other.anchor_root)

    def test_no_tree_reports_actionable_error_without_file_creation(self):
        for method in (self.tree.ProjectTree.add_saved_root, self.tree.ProjectTree.remove_saved_root):
            self.assertIn("run :ProjectTree first", self.invoke(method))
        self.assertFalse(self.path.exists())

    def test_virtual_roots_rejected_but_normal_workspace_children_allowed(self):
        for cls in (self.tree.WorkSpaceRootNode, self.tree.WorkSetRootNode,
                    self.tree.WorkSetNode, self.tree.ZipRootNode, self.tree.TreeNode):
            with self.subTest(node_type=cls.__name__):
                node = cls.__new__(cls)
                node.realpath = str(self.root)
                node.isDirectory = True
                tree = self.register_tree(node)
                for method in (self.tree.ProjectTree.add_saved_root, self.tree.ProjectTree.remove_saved_root):
                    self.assertIn("not a filesystem directory", self.invoke(method))
                self.assertFalse(self.path.exists())
                tree.root = self.tree.NormalDirNode("child", str(self.root), tree)
                self.assertIn("saved", self.invoke(self.tree.ProjectTree.add_saved_root))
                self.invoke(self.tree.ProjectTree.remove_saved_root)
                self.path.unlink()

    def test_project_root_supported_and_stale_directory_removable(self):
        node = self.tree.ProjectRootNode.__new__(self.tree.ProjectRootNode)
        node.realpath = str(self.root)
        self.register_tree(node)
        self.invoke(self.tree.ProjectTree.add_saved_root)
        self.root.rmdir()
        self.assertIn("not an existing directory", self.invoke(self.tree.ProjectTree.add_saved_root))
        self.assertIn("removed", self.invoke(self.tree.ProjectTree.remove_saved_root))
        self.assertEqual("", self.path.read_text())
        self.assertIn("not saved", self.invoke(self.tree.ProjectTree.remove_saved_root))

    def test_failures_report_command_name_and_never_success(self):
        self.register_tree()
        for method, command in ((self.tree.ProjectTree.add_saved_root, "PTreeRootAdd"),
                                (self.tree.ProjectTree.remove_saved_root, "PTreeRootRemove")):
            with mock.patch.object(self.config.__class__, "_read", side_effect=PermissionError("denied")):
                self.assertEqual(command + ": denied\n", self.invoke(method))

    def test_empty_selector_acceptance_is_harmless(self):
        manager = self.locate.ProjectTreeRootContentManager()
        for line in ("", " \t "):
            manager.open_content(line)
        self.vim.command.assert_not_called()


class ConfigTests(IsolatedPluginTests):
    def test_missing_file_add_and_idempotent_operations(self):
        self.assertEqual((False, []), self.config.remove(str(self.root)))
        self.assertFalse(self.path.exists())
        self.assertEqual((True, ["project"]), self.config.add(str(self.root)))
        before = self.path.read_bytes()
        with mock.patch.object(self.config, "_write") as writer:
            self.assertEqual((False, ["project"]), self.config.add(str(self.root), "another"))
            self.assertEqual((False, []), self.config.remove(str(self.home / "absent")))
            writer.assert_not_called()
        self.assertEqual(before, self.path.read_bytes())
        self.assertEqual((True, ["project"]), self.config.remove(str(self.root)))
        self.assertEqual(b"", self.path.read_bytes())
        self.assertEqual((True, ["explicit"]), self.config.add(str(self.root), "explicit"))

    def test_alias_collision_does_not_overwrite_even_if_path_already_saved(self):
        other = self.home / "other" / "project"
        other.mkdir(parents=True)
        self.config.add(str(self.root))
        with self.assertRaisesRegex(ValueError, "already points"):
            self.config.add(str(other))
        self.config.add(str(other), "other")
        before = self.path.read_bytes()
        with self.assertRaisesRegex(ValueError, "already points"):
            self.config.add(str(self.root), "other")
        self.assertEqual(before, self.path.read_bytes())

    def test_invalid_aliases_do_not_write(self):
        for name in ("", "a b", "a\tb", "a\nb", "a=b", "#comment", "\u00a0x"):
            with self.subTest(name=name), self.assertRaisesRegex(ValueError, "invalid alias"):
                self.config.add(str(self.root), name)
        self.assertFalse(self.path.exists())
        self.config.add(str(self.root))
        with self.assertRaisesRegex(ValueError, "invalid alias"):
            self.config.add(str(self.root), "bad name")

    def test_invalid_default_basename_requires_explicit_alias(self):
        spaced = self.home / "two words"
        spaced.mkdir()
        for path in (str(spaced), os.path.abspath(os.sep)):
            with self.assertRaisesRegex(ValueError, "PTreeRootAdd <name>"):
                self.config.add(path)
        self.config.add(str(spaced), "valid")

    def test_special_path_characters_round_trip(self):
        path = self.home / "space 'quote\" = \u9879\u76ee"
        path.mkdir()
        self.config.add(str(path), "\u9879\u76ee")
        manager = self.locate.ProjectTreeRootContentManager()
        self.assertEqual({"\u9879\u76ee": str(path)}, manager.path_dict)
        manager.open_content(manager.search_content("*")[0])
        self.vim.command.assert_called_once_with("call ProjectTree('%s')" % str(path).replace("'", "''"))
        self.assertTrue(self.config.remove(str(path))[0])

    def test_non_round_trippable_paths_rejected(self):
        for suffix in ("line\nbreak", "line\rbreak", "trailing "):
            path = self.home / suffix
            path.mkdir()
            for method in (self.config.add, self.config.remove):
                with self.subTest(path=str(path)), self.assertRaises(ValueError):
                    method(str(path))
        self.assertFalse(self.path.exists())

    def test_equivalent_paths_and_symlink_spelling(self):
        link = self.home / "linked"
        link.symlink_to(self.root, target_is_directory=True)
        self.config.add(str(link), "linked")
        self.assertEqual("linked=%s\n" % link, self.path.read_text())
        self.assertEqual((False, ["linked"]), self.config.add(str(self.root) + "/", "alias"))
        with mock.patch.dict(os.environ, HOME=str(self.home)):
            self.assertEqual((True, ["linked"]), self.config.remove("~/project"))
        self.path.write_text("tilde=~/project/\n")
        with mock.patch.dict(os.environ, HOME=str(self.home)):
            self.assertEqual((False, ["tilde"]), self.config.add(str(link)))

    def test_empty_saved_value_is_not_the_current_directory(self):
        self.path.write_text("empty=\n")
        self.assertEqual((False, []), self.config.remove(os.getcwd()))
        self.assertEqual((True, ["cwd"]), self.config.add(os.getcwd(), "cwd"))
        self.assertTrue(self.path.read_text().startswith("empty=\n"))

    def test_relative_saved_path_uses_current_directory(self):
        self.path.write_text("relative=%s\n" % os.path.relpath(self.root))
        self.assertEqual((False, ["relative"]), self.config.add(str(self.root)))
        self.assertEqual((True, ["relative"]), self.config.remove(str(self.root)))

    def test_removal_preserves_raw_lines_and_removes_shadowed_aliases(self):
        keep = "# comment=unchanged\r\n\r\nmalformed\n keep = /elsewhere \r\n"
        content = (keep + "same=/old\nsame=%s\nalias=%s/\n" % (self.root, self.root)
                   + "unrelated=%s\nunrelated=/different\nlast malformed" % self.root)
        self.path.write_bytes(content.encode())
        self.assertEqual((True, ["same", "alias"]), self.config.remove(str(self.root)))
        expected = keep + "unrelated=%s\nunrelated=/different\nlast malformed" % self.root
        self.assertEqual(expected.encode(), self.path.read_bytes())

    def test_add_preserves_lines_and_supplies_missing_separator(self):
        for original in (b"# comment\r\n\r\n", b"# comment\r", b"unrecognized", b""):
            with self.subTest(original=original):
                self.path.write_bytes(original)
                self.config.add(str(self.root))
                separator = b"\n" if original and not original.endswith((b"\n", b"\r")) else b""
                expected = original + separator + ("project=%s\n" % self.root).encode()
                self.assertEqual(expected, self.path.read_bytes())

    def test_reads_fresh_and_preserves_symlink_and_mode(self):
        target = self.home / "actual.cfg"
        target.write_text("# preserved\n")
        target.chmod(0o640)
        self.path.symlink_to(target.name)
        self.config.add(str(self.root))
        self.assertTrue(self.path.is_symlink())
        self.assertEqual(0o640, target.stat().st_mode & 0o777)
        target.write_text("# externally edited\nmanual=%s\n" % self.root)
        self.assertEqual((True, ["manual"]), self.config.remove(str(self.root)))
        self.assertEqual("# externally edited\n", target.read_text())
        self.assertTrue(self.path.is_symlink())

    def test_write_and_replace_failures_preserve_original_and_clean_tempfile(self):
        self.path.write_text("# original\n")
        before = self.path.read_bytes()
        for operation in ("write", "replace"):
            with self.subTest(operation=operation):
                if operation == "write":
                    temporary = self.common.tempfile.NamedTemporaryFile
                    def failing_file(*args, **kwargs):
                        handle = temporary(*args, **kwargs)
                        handle.writelines = mock.Mock(side_effect=OSError("disk full"))
                        return handle
                    patcher = mock.patch.object(self.common.tempfile, "NamedTemporaryFile", failing_file)
                else:
                    patcher = mock.patch.object(self.common.os, "replace", side_effect=PermissionError("denied"))
                with patcher, self.assertRaises(OSError):
                    self.config.add(str(self.root))
                self.assertEqual(before, self.path.read_bytes())
                self.assertEqual([], list(self.home.glob(".ptree_roots.*")))

    def test_read_errors_are_not_treated_as_empty_configuration(self):
        self.path.write_text("# original\n")
        with mock.patch("builtins.open", side_effect=PermissionError("denied")):
            with self.assertRaises(PermissionError):
                self.config.add(str(self.root))
        self.assertEqual("# original\n", self.path.read_text())
        self.path.write_bytes(b"\xff\xfe\xff")
        with self.assertRaises(UnicodeError):
            self.config.add(str(self.root))
        self.assertEqual(b"\xff\xfe\xff", self.path.read_bytes())


if __name__ == "__main__":
    unittest.main()
