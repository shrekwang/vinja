import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest import mock

from test_ptree_roots import IsolatedPluginTests, REPO


class VimStatusSystemTests(unittest.TestCase):
    def test_shutdown_saves_each_tree_once_without_per_buffer_sync(self):
        executable = os.environ.get("VINJA_TEST_VIM") or shutil.which("mvim") or shutil.which("vim")
        if not executable:
            self.skipTest("Vim is not installed")
        with tempfile.TemporaryDirectory(dir="/tmp") as home:
            env = dict(os.environ, HOME=home, VINJA_TEST_REPO=str(REPO),
                       PYTHONDONTWRITEBYTECODE="1")
            result = subprocess.run(
                [executable, "-v", "-Nu", "NONE", "-i", "NONE", "-n", "-es", "-V1",
                 "-S", str(REPO / "vinja/test/tree_status.vim")],
                cwd=home, env=env, capture_output=True, text=True, timeout=30)
            if result.returncode == 77:
                self.skipTest("Vim has no usable Python 3 support")
            errors = Path(home, "errors.txt")
            detail = errors.read_text() if errors.exists() else result.stdout + result.stderr
            self.assertEqual(0, result.returncode, detail)
            self.assertEqual("ok", Path(home, "shutdown.txt").read_text().strip())


class StatusTests(IsolatedPluginTests):
    def test_save_without_closing_never_queries_vim_or_loads_directories(self):
        tree = self.register_tree()
        tree.tree_state_path = str(self.root / ".jde_tree_state")
        tree.root.isOpen = tree.root.isLoaded = True
        child = self.tree.NormalDirNode("child", str(self.root / "child"), tree)
        child.isOpen = True
        tree.root.add_child(child)
        file_node = self.tree.NormalFileNode("a'file", str(self.root / "a'file"), False)
        file_node.isEdited = True
        tree.root.add_child(file_node)
        tree.edit_history = [file_node.realpath] + [str(self.root / str(i)) for i in range(12)]
        self.vim.eval.reset_mock()
        with mock.patch.object(child, "get_children", side_effect=AssertionError("lazy load")):
            tree.save_status(False)
        self.assertEqual([tree.root.realpath, child.realpath] + tree.edit_history[:10],
                         Path(tree.tree_state_path).read_text().splitlines())
        self.vim.eval.assert_not_called()
        self.vim.command.assert_not_called()

    def test_save_all_only_live_tabs_and_continue_after_write_error(self):
        first, second, closed = mock.Mock(), mock.Mock(), mock.Mock()
        self.tree._tree_registry.update({"one": first, "two": second, "closed": closed})
        self.vim.tabpages = [mock.Mock(number=i) for i in (1, 2, 3)]
        self.vim.eval.side_effect = lambda expr: {
            'gettabvar(1, "tab_id")': "one",
            'gettabvar(2, "tab_id")': "two",
            'gettabvar(3, "tab_id")': "",
        }[expr]
        first.save_status.side_effect = PermissionError("denied")
        output = self.invoke(self.tree.ProjectTree.save_all_status)
        self.assertIn("denied", output)
        first.save_status.assert_called_once_with(False)
        second.save_status.assert_called_once_with(False)
        closed.save_status.assert_not_called()
        self.vim.command.assert_not_called()


if __name__ == "__main__":
    unittest.main()
