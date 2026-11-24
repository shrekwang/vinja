import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest import mock

from test_ptree_roots import IsolatedPluginTests, REPO


class VimRestoreSystemTests(unittest.TestCase):
    def test_initial_restore_renders_once_and_reopening_preserves_live_state(self):
        executable = os.environ.get('VINJA_TEST_VIM') or shutil.which('mvim') or shutil.which('vim')
        if not executable:
            self.skipTest('Vim is not installed')
        with tempfile.TemporaryDirectory(dir='/tmp') as home:
            result = subprocess.run(
                [executable, '-v', '-Nu', 'NONE', '-i', 'NONE', '-n', '-es', '-V1',
                 '-S', str(REPO / 'vinja/test/tree_restore.vim')],
                cwd=home, env=dict(os.environ, HOME=home, VINJA_TEST_REPO=str(REPO),
                                  PYTHONDONTWRITEBYTECODE='1'),
                capture_output=True, text=True, timeout=30)
            if result.returncode == 77:
                self.skipTest('Vim has no usable Python 3 support')
            errors = Path(home, 'errors.txt')
            self.assertEqual(0, result.returncode,
                             errors.read_text() if errors.exists() else result.stdout + result.stderr)


class RestoreTests(IsolatedPluginTests):
    def test_tree_type_switch_renders_after_restoring_files(self):
        path = self.root / 'file'
        path.touch()
        with mock.patch.object(self.tree.ProjectRootNode, '_load_class_path'), \
                mock.patch.object(self.tree.ProjectRootNode, '_build_virtual_noes'):
            tree = self.tree.ProjectTree(str(self.root))
        Path(tree.tree_state_path).write_text(str(path) + '\n')
        old_tree = mock.Mock()
        self.tree._tree_registry['tab-one'] = old_tree
        self.vim.current = mock.Mock()
        self.vim.current.buffer = mock.MagicMock()
        events = []

        def command(cmd):
            if cmd.startswith('silent edit'):
                events.append('edit')
                self.tree.ProjectTree.set_file_edit(str(path), 'true')

        with mock.patch.object(self.tree.ProjectTree, 'create_project_tree', return_value=tree), \
                mock.patch.object(tree, 'render_tree', side_effect=lambda: events.append('render')), \
                mock.patch.object(self.vim.command, 'side_effect', command):
            self.tree.ProjectTree.toggleTreeType('projectTree')
        old_tree.save_status.assert_called_once_with()
        self.assertIs(tree, self.tree.get_current_tree())
        self.assertEqual(['edit', 'render'], events)
        self.assertEqual([str(path.resolve())], tree.edit_history)
        self.assertTrue(tree.find_node(str(path)).isEdited)

    def test_failed_file_restore_resumes_normal_edit_rendering(self):
        with mock.patch.object(self.tree.ProjectRootNode, '_load_class_path'), \
                mock.patch.object(self.tree.ProjectRootNode, '_build_virtual_noes'):
            tree = self.tree.ProjectTree(str(self.root))
        self.tree._tree_registry['one'] = tree
        self.vim.current = mock.Mock()
        self.vim.current.window.cursor = (1, 0)
        path = self.root / 'file'
        path.touch()
        Path(tree.tree_state_path).write_text(str(path) + '\n')
        node = self.tree.NormalFileNode('file', str(path), False)
        tree.root.add_child(node)

        def fail_edit(command):
            if command.startswith('silent edit'):
                self.tree.ProjectTree.set_file_edit(str(path), 'true')
                raise RuntimeError('restore failed')

        with mock.patch.object(tree, 'render_tree') as render, \
                mock.patch.object(self.tree.VimUtil, 'isVinjaBufferVisible', return_value=True):
            self.vim.command.side_effect = fail_edit
            with self.assertRaisesRegex(RuntimeError, 'restore failed'):
                tree.restore_status('file')
            self.assertTrue(node.isEdited)
            self.assertEqual([str(path.resolve())], tree.edit_history)
            render.assert_not_called()
            self.vim.command.side_effect = None
            self.tree.ProjectTree.set_file_edit(str(path), 'false')
            render.assert_called_once_with()
            self.assertFalse(node.isEdited)
            self.assertEqual([], tree.edit_history)
