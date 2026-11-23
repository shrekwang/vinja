from pathlib import Path
import runpy
import sys
import types
import unittest
from unittest import mock


REPO = Path(__file__).resolve().parents[2]


class TabLocateTests(unittest.TestCase):
    def setUp(self):
        self.selector_open = False
        self.vim = types.ModuleType("vim")
        self.vim.eval = mock.Mock(side_effect=self.eval_vim)
        self.vim.command = mock.Mock(side_effect=self.command_vim)
        common = types.ModuleType("common")
        common.VimUtil = types.SimpleNamespace(hasGuiRunning=lambda: False)
        self.output = common.output = mock.Mock()
        shext = types.ModuleType("shext")
        shext.LocateCmd = mock.Mock()
        with mock.patch.dict(sys.modules, vim=self.vim, common=common, shext=shext):
            self.module = runpy.run_path(
                str(REPO / "vinja/python/locate.py"),
                init_globals={"EditUtil": mock.Mock(), "Parser": mock.Mock()},
            )

    def eval_vim(self, expression):
        if expression == "tabpagenr('$')":
            return "2"
        if expression == "MyTabLabel(1)":
            name = "Tab Locate" if self.selector_open else "catkin.vim"
            return "vim-catkin-plugin: " + name
        if expression == "MyTabLabel(2)":
            return "vinja: bclose.vim"
        return "1"

    def command_vim(self, command):
        if command.startswith("silent! keepalt botright 1split "):
            self.selector_open = True

    def open_selector(self):
        manager = self.module["TabContentManager"]()
        self.module["QuickLocater"].runApp(manager)
        self.assertTrue(self.selector_open)
        return manager

    def test_selector_displays_original_current_tab_label(self):
        self.open_selector()
        self.output.assert_called_once_with([
            "* 1: vim-catkin-plugin: catkin.vim",
            "  2: vinja: bclose.vim",
        ])

    def test_filter_uses_original_buffer_name(self):
        manager = self.open_selector()
        self.assertEqual(["* 1: vim-catkin-plugin: catkin.vim"],
                         manager.search_content("CATKIN.VIM*"))
        self.assertEqual([], manager.search_content("Tab Locate*"))
        self.assertEqual(["  2: vinja: bclose.vim"],
                         manager.search_content("bclose*"))


if __name__ == "__main__":
    unittest.main()
