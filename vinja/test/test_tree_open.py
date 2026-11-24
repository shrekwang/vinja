import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest import mock

from test_ptree_roots import IsolatedPluginTests, REPO


class VimOpenSystemTests(unittest.TestCase):
    def test_file_open_redraws_and_focus(self):
        executable = os.environ.get('VINJA_TEST_VIM') or shutil.which('mvim') or shutil.which('vim')
        if not executable:
            self.skipTest('Vim is not installed')
        with tempfile.TemporaryDirectory(dir='/tmp') as home:
            result = subprocess.run(
                [executable, '-v', '-Nu', 'NONE', '-i', 'NONE', '-n', '-es', '-V1',
                 '-S', str(REPO / 'vinja/test/tree_open.vim')],
                cwd=home, env=dict(os.environ, HOME=home, VINJA_TEST_REPO=str(REPO),
                                  PYTHONDONTWRITEBYTECODE='1'),
                capture_output=True, text=True, timeout=30)
            if result.returncode == 77:
                self.skipTest('Vim has no usable Python 3 support')
            errors = Path(home, 'errors.txt')
            self.assertEqual(0, result.returncode,
                             errors.read_text() if errors.exists() else result.stdout + result.stderr)


class OpenTests(IsolatedPluginTests):
    def setUp(self):
        super().setUp()
        self.path = self.root / 'file'
        self.path.touch()
        with mock.patch.object(self.tree.ProjectRootNode, '_load_class_path'), \
                mock.patch.object(self.tree.ProjectRootNode, '_build_virtual_noes'):
            self.project = self.tree.ProjectTree(str(self.root))
        self.tree._tree_registry['tab-one'] = self.project
        self.node = self.project.root.get_child('file')
        self.vim.current = mock.Mock()
        self.vim.current.window.cursor = (2, 0)
        self.vim.current.buffer.name = str(self.root / 'other')

    def test_open_renders_once_with_or_without_read_autocommand(self):
        for read_event in (True, False):
            with self.subTest(read_event=read_event):
                self.node.isEdited = False

                def command(cmd):
                    if read_event and cmd.startswith('silent edit'):
                        self.tree.ProjectTree.set_file_edit(str(self.path), 'true')

                with mock.patch.object(self.project, 'get_selected_node', return_value=self.node), \
                        mock.patch.object(self.project, 'render_tree') as render, \
                        mock.patch.object(self.tree.VimUtil, 'isVinjaBufferVisible', return_value=True), \
                        mock.patch.object(self.vim, 'command', side_effect=command):
                    self.project.open_selected_node()
                render.assert_called_once_with()
                self.assertTrue(self.node.isEdited)
                self.assertEqual([str(self.path.resolve())], self.project.edit_history)

    def test_unchanged_flag_updates_history_without_ui_work(self):
        self.node.isEdited = True
        self.project.edit_history = ['other', str(self.path.resolve())]
        with mock.patch.object(self.project, 'render_tree') as render, \
                mock.patch.object(self.tree.VimUtil, 'isVinjaBufferVisible') as visible:
            self.tree.ProjectTree.set_file_edit(str(self.path), 'true')
            self.assertEqual([str(self.path.resolve()), 'other'], self.project.edit_history)
            render.assert_not_called()
            visible.assert_not_called()
            self.vim.command.assert_not_called()

    def test_failed_edit_does_not_set_flag_or_render(self):
        def command(cmd):
            if cmd.startswith('silent edit'):
                raise RuntimeError('unsaved changes')

        with mock.patch.object(self.project, 'get_selected_node', return_value=self.node), \
                mock.patch.object(self.project, 'render_tree') as render, \
                mock.patch.object(self.vim, 'command', side_effect=command):
            with self.assertRaisesRegex(RuntimeError, 'unsaved changes'):
                self.project.open_selected_node()
        self.assertFalse(self.node.isEdited)
        self.assertEqual([], self.project.edit_history)
        render.assert_not_called()
