set nocompatible
set encoding=utf-8
if !empty($VINJA_TEST_PYTHON_DLL)
  let &pythonthreedll = $VINJA_TEST_PYTHON_DLL
endif
if !has('python3')
  cquit 77
endif
try
  execute 'source' fnameescape($VINJA_TEST_REPO . '/plugin/vinja.vim')
  call mkdir($HOME . '/project/child', 'p')
  call mkdir($HOME . '/other', 'p')
  call ProjectTree($HOME . '/project')
  PTreeRootAdd demo
  call assert_equal(['demo=' . $HOME . '/project'], readfile($HOME . '/.vinja/ptree_roots.cfg'))
  PTreeRootAdd duplicate
  call assert_equal(1, len(readfile($HOME . '/.vinja/ptree_roots.cfg')))
  call assert_equal(2, exists(':PTreeRootAdd'))
  call assert_equal(2, exists(':PTreeRootRemove'))
  call assert_true(index(getcompletion('PTreeRoot', 'command'), 'PTreeRootAdd') >= 0)
  call assert_true(index(getcompletion('PTreeRoot', 'command'), 'PTreeRootRemove') >= 0)
  try
    PTreeRootRemove unexpected
    call assert_report('PTreeRootRemove accepted an argument')
  catch /E488:/
  endtry
  PTreeRootAdd invalid alias
  call assert_equal(1, len(readfile($HOME . '/.vinja/ptree_roots.cfg')))

  py3 current_tree = get_current_tree()
  py3 current_tree.get_selected_node = lambda: current_tree.anchor_root.get_child('child')
  py3 current_tree.change_root()
  PTreeRootAdd
  call assert_equal('child=' . $HOME . '/project/child', readfile($HOME . '/.vinja/ptree_roots.cfg')[-1])
  PTreeRootRemove
  py3 current_tree.change_root_upper()
  PTreeRootAdd
  call assert_equal(1, len(readfile($HOME . '/.vinja/ptree_roots.cfg')))
  py3 current_tree.change_root()
  py3 current_tree.change_back()
  py3 del current_tree.get_selected_node
  py3 assert current_tree.root is current_tree.anchor_root

  call ProjectTree($HOME . '/project')
  enew!
  PTreeRootRemove
  call assert_equal([], readfile($HOME . '/.vinja/ptree_roots.cfg'))
  PTreeRootAdd o'quote
  call assert_equal("o'quote=" . $HOME . '/project', readfile($HOME . '/.vinja/ptree_roots.cfg')[0])
  PTreeRootRemove
  PTreeRootAdd

  tabnew
  PTreeRootAdd absent
  call assert_equal(1, len(readfile($HOME . '/.vinja/ptree_roots.cfg')))
  call ProjectTree($HOME . '/other')
  PTreeRootAdd project
  call assert_equal(1, len(readfile($HOME . '/.vinja/ptree_roots.cfg')))
  PTreeRootAdd other
  PTreeRootRemove
  tabprevious
  call assert_equal(['project=' . $HOME . '/project'], readfile($HOME . '/.vinja/ptree_roots.cfg'))

  ProjectTreeSelect
  py3 assert quickLocater.content_manager.entries == ['project']
  py3 quickLocater.open_content()
  py3 assert get_current_tree() is current_tree
  PTreeRootRemove
  ProjectTreeSelect
  py3 assert quickLocater.content_manager.entries == []
  py3 quickLocater.open_content()
  call assert_equal([], readfile($HOME . '/.vinja/ptree_roots.cfg'))
  call assert_true(isdirectory($HOME . '/project'))
catch
  call add(v:errors, v:exception . ' at ' . v:throwpoint)
endtry
if !empty(v:errors)
  call writefile(v:errors, $HOME . '/errors.txt')
  cquit 1
endif
qa!
