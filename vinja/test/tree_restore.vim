set nocompatible
set encoding=utf-8
set hidden
if !empty($VINJA_TEST_PYTHON_DLL)
  let &pythonthreedll = $VINJA_TEST_PYTHON_DLL
endif
if !has('python3')
  cquit 77
endif
try
  execute 'source' fnameescape($VINJA_TEST_REPO . '/plugin/vinja.vim')
  let root = resolve($HOME) . '/project'
  call mkdir(root . '/saved', 'p')
  call mkdir(root . '/current', 'p')
  let files = []
  for index in range(1, 10)
    let path = root . '/saved/file' . index
    call writefile(['content'], path)
    call add(files, path)
  endfor
  call writefile(['current'], root . '/current/file')
  call writefile([root, root . '/saved'] + files, root . '/.jde_tree_state')
  execute 'edit' fnameescape(root . '/current/file')
  py3 renders, restores = [], []
  py3 original_render, original_restore = ProjectTree.render_tree, ProjectTree.restore_status
  py3 ProjectTree.render_tree = lambda self: (renders.append(self), original_render(self))[-1]
  py3 ProjectTree.restore_status = lambda self, node_type='dir': (restores.append((self, node_type)), original_restore(self, node_type))[-1]
  call ProjectTree(root)
  wincmd h
  py3 tree = get_current_tree()
  py3 assert renders == [tree], len(renders)
  py3 assert [kind for obj, kind in restores] == ['dir', 'file']
  py3 assert len(tree.edit_history) == 10, tree.edit_history
  py3 assert all(tree.find_node(path).isEdited for path in tree.edit_history)
  py3 assert sum('[edit]' in line for line in vim.current.buffer) >= 10, list(vim.current.buffer)
  py3 assert tree.get_selected_node().realpath.endswith('/current/file')
  py3 assert tree.root.get_child('saved').isOpen

  " Hiding then reopening must not replay disk state or closed file buffers.
  call ProjectTree(root)
  execute 'bunload' bufnr(files[0])
  py3 tree.root.get_child('saved').isOpen = False
  call writefile([root, root . '/saved'] + files, root . '/.jde_tree_state')
  execute 'edit' fnameescape(root . '/current/file')
  py3 renders.clear(); restores.clear()
  call ProjectTree(root)
  py3 assert get_current_tree() is tree
  py3 assert not restores, restores
  py3 assert renders == [tree], len(renders)
  py3 assert not tree.root.get_child('saved').isOpen
  call assert_equal(0, bufloaded(files[0]))
  wincmd l
  call assert_equal(root . '/current/file', expand('%:p'))

  " Ordinary reads still redraw and synchronize after restoration.
  py3 renders.clear()
  execute 'edit' fnameescape(files[0])
  py3 assert renders == [tree], len(renders)
  py3 assert tree.find_node(vim.current.buffer.name).isEdited
catch
  call add(v:errors, v:exception . ' at ' . v:throwpoint)
endtry
if !empty(v:errors)
  call writefile(v:errors, $HOME . '/errors.txt')
  cquit 1
endif
qa!
