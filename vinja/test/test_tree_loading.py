from unittest import mock

from test_ptree_roots import IsolatedPluginTests


class LoadingTests(IsolatedPluginTests):
    def make_tree(self):
        with mock.patch.object(self.tree.ProjectRootNode, '_load_class_path'), \
                mock.patch.object(self.tree.ProjectRootNode, '_build_virtual_noes'):
            return self.tree.ProjectTree(str(self.root))

    def test_nested_projects_defer_loading_and_sdk_discovery_until_expanded(self):
        nested = self.root / 'nested'
        nested.mkdir()
        (nested / 'go.mod').touch()
        (nested / 'file').touch()
        with mock.patch.object(self.tree.ProjectRootNode, '_load_class_path') as classpath, \
                mock.patch.object(self.tree.ProjectRootNode, '_build_virtual_noes') as sdk:
            tree = self.tree.ProjectTree(str(self.root))
            child = tree.root.get_child('nested')
            self.assertIsInstance(child, self.tree.ProjectRootNode)
            self.assertFalse(child.isLoaded)
            self.assertEqual(1, sdk.call_count)
            self.assertEqual(1, classpath.call_count)
            self.assertIsNotNone(child.get_child('file'))
            self.assertTrue(child.isLoaded)
            self.assertEqual(2, sdk.call_count)
            self.assertEqual(2, classpath.call_count)
            child.get_children()
            self.assertEqual(2, sdk.call_count)

    def test_ancestor_symlinks_stop_but_external_directory_links_still_load(self):
        (self.root / 'go.mod').touch()
        (self.root / 'loop').symlink_to(self.root, target_is_directory=True)
        external = self.home / 'external'
        external.mkdir()
        (external / 'file').touch()
        (self.root / 'external').symlink_to(external, target_is_directory=True)
        with mock.patch.object(self.tree.ProjectRootNode, '_load_class_path'), \
                mock.patch.object(self.tree.ProjectRootNode, '_build_virtual_noes'):
            tree = self.tree.ProjectTree(str(self.root))
            loop = tree.root.get_child('loop')
            with mock.patch.object(self.tree.os, 'listdir', side_effect=AssertionError('cycle traversal')):
                self.assertEqual([], loop.get_children())
                loop.refresh()
                self.assertEqual([], loop.get_children())
            self.assertIsNotNone(tree.root.get_child('external').get_child('file'))

    def test_plain_directory_cycle_is_blocked_on_expansion(self):
        child = self.root / 'child'
        child.mkdir()
        (child / 'back').symlink_to(self.root, target_is_directory=True)
        tree = self.make_tree()
        back = tree.root.get_child('child').get_child('back')
        self.assertEqual([], back.get_children())

    def test_virtual_parent_without_path_is_not_an_ancestor_directory(self):
        (self.root / 'file').touch()
        tree = self.make_tree()
        parent = self.tree.TreeNode('workset', '', True)
        parent.add_child(tree.root)
        with mock.patch.object(self.tree.os, 'getcwd', return_value=str(self.root)), \
                mock.patch.object(tree.root, '_build_virtual_noes'):
            tree.root.refresh()
        self.assertIsNotNone(tree.root.get_child('file'))

    def test_refresh_reuses_nodes_with_linear_name_comparisons(self):
        tree = self.make_tree()
        names = ['file-%04d' % i for i in range(1000)]
        class Name(str):
            comparisons = 0
            __hash__ = str.__hash__

            def __eq__(self, other):
                Name.comparisons += 1
                return super().__eq__(other)

        nodes = [self.tree.NormalFileNode(Name(name), str(self.root / name), False) for name in names]
        nodes[0].isEdited = True
        tree.root._children = nodes[:]
        with mock.patch.object(self.tree.os, 'listdir', return_value=names), \
                mock.patch.object(tree.root, '_build_virtual_noes'):
            tree.root.refresh()
        self.assertEqual(len(nodes), len(tree.root._children))
        self.assertTrue(all(old is new for old, new in zip(nodes, tree.root._children)))
        self.assertTrue(tree.root._children[0].isEdited)
        self.assertLessEqual(Name.comparisons, 2 * len(nodes))

    def test_refresh_replaces_file_when_it_becomes_directory(self):
        path = self.root / 'entry'
        path.touch()
        tree = self.make_tree()
        original = tree.root.get_child('entry')
        path.unlink()
        path.mkdir()
        with mock.patch.object(tree.root, '_build_virtual_noes'):
            tree.root.refresh()
        replacement = tree.root.get_child('entry')
        self.assertIsNot(original, replacement)
        self.assertTrue(replacement.isDirectory)
