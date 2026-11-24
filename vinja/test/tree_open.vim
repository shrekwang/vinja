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
  call mkdir(root . '/folder', 'p')
  for name in ['first', 'second', 'split', 'vertical', 'tab']
    call writefile(['content'], root . '/' . name)
  endfor
  call ProjectTree(root)
  wincmd h
  let tree_win = win_getid()
  let tree_buf = bufnr('%')
  py3 tree = get_current_tree(); renders = []
  py3 original_render = ProjectTree.render_tree
  py3 ProjectTree.render_tree = lambda self: (renders.append(self), original_render(self))[-1]

  " A fresh read and the explicit open notification must share one redraw.
  py3 tree.select_node(tree.root.get_child('first')); renders.clear()
  let tree_cursor = getcurpos()[1:2]
  let g:tree_entries = 0
  augroup OpenTest
    autocmd BufEnter * if bufnr('%') == g:tree_buf | let g:tree_entries += 1 | endif
  augroup END
  execute "normal \<CR>"
  call assert_equal(root . '/first', expand('%:p'))
  py3 assert renders == [tree], len(renders)
  call assert_equal(0, g:tree_entries)
  call assert_match('first \[edit\]', join(getbufline(tree_buf, 1, '$'), '\n'))

  " Reopening an unchanged file must not redraw or revisit the panel.
  call win_gotoid(tree_win)
  call assert_equal(tree_cursor, getcurpos()[1:2])
  py3 renders.clear()
  let g:tree_entries = 0
  execute "normal \<CR>"
  call assert_equal(root . '/first', expand('%:p'))
  py3 assert not renders, len(renders)
  call assert_equal(0, g:tree_entries)

  " Loaded buffers do not fire BufReadPost, but still update MRU history.
  execute 'edit' fnameescape(root . '/second')
  call win_gotoid(tree_win)
  py3 tree.select_node(tree.root.get_child('first')); renders.clear()
  execute "normal \<CR>"
  call assert_equal(root . '/first', expand('%:p'))
  py3 assert not renders, len(renders)
  py3 assert tree.edit_history[0].endswith('/first'), tree.edit_history

  " Preview returns to the same selected tree row without a redundant redraw.
  call win_gotoid(tree_win)
  py3 renders.clear()
  normal go
  call assert_equal(tree_win, win_getid())
  call assert_equal(tree_cursor, getcurpos()[1:2])
  py3 assert not renders, len(renders)

  " Directory toggles still render and retain the tree cursor.
  py3 tree.select_node(tree.root.get_child('folder')); renders.clear()
  execute "normal \<CR>"
  py3 assert tree.root.get_child('folder').isOpen
  py3 assert renders == [tree], len(renders)
  call assert_equal(tree_win, win_getid())

  " Split opens retain focus in the new file window.
  for [name, key] in [['split', 'i'], ['vertical', 'O']]
    py3 tree.select_node(tree.root.get_child(vim.eval('name'))); renders.clear()
    execute 'normal ' . key
    call assert_equal(root . '/' . name, expand('%:p'))
    py3 assert renders == [tree], len(renders)
    close
    call win_gotoid(tree_win)
  endfor

  " Tab opens synchronize the original tree without stealing focus.
  py3 tree.select_node(tree.root.get_child('tab')); renders.clear()
  normal t
  call assert_equal(2, tabpagenr())
  call assert_equal(root . '/tab', expand('%:p'))
  py3 assert tree.root.get_child('tab').isEdited
  py3 assert not renders, len(renders)
  tabclose

  " Unloading a buffer clears its marker; the next open renders once again.
  wincmd l
  execute 'edit' fnameescape(root . '/second')
  execute 'bunload' bufnr(root . '/first')
  py3 assert not tree.root.get_child('first').isEdited
  call win_gotoid(tree_win)
  py3 tree.select_node(tree.root.get_child('first')); renders.clear()
  execute "normal \<CR>"
  py3 assert renders == [tree], len(renders)
  call assert_equal(root . '/first', expand('%:p'))
catch
  call add(v:errors, v:exception . ' at ' . v:throwpoint)
endtry
if !empty(v:errors)
  call writefile(v:errors, $HOME . '/errors.txt')
  cquit 1
endif
qa!
