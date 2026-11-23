set nocompatible
set encoding=utf-8
set hidden
if !empty($VINJA_TEST_PYTHON_DLL)
  let &pythonthreedll = $VINJA_TEST_PYTHON_DLL
endif
if !has('python3')
  cquit 77
endif

function! CheckShutdown() abort
  try
    py3 assert len(status_saves) == 3, status_saves
    py3 assert len(set(status_saves)) == 3, status_saves
    py3 assert not status_syncs, status_syncs
    py3 assert not status_renders, status_renders
    py3 assert all(tree.edit_history == histories[id(tree)] for tree in live_trees)
    py3 assert all(os.path.exists(tree.tree_state_path) for tree in live_trees)
    py3 assert all(tree.edit_history[0] in open(tree.tree_state_path).read().splitlines() for tree in live_trees)
  catch
    call add(v:errors, v:exception . ' at ' . v:throwpoint)
  endtry
  if !empty(v:errors)
    call writefile(v:errors, $HOME . '/errors.txt')
    cquit 1
  endif
  call writefile(['ok'], $HOME . '/shutdown.txt')
endfunction

try
  execute 'source' fnameescape($VINJA_TEST_REPO . '/plugin/vinja.vim')
  py3 status_saves = []
  py3 original_save = ProjectTree.save_status
  py3 ProjectTree.save_status = lambda self, closeFile=True: (status_saves.append(self.project_root), original_save(self, closeFile))[-1]
  for index in range(1, 3)
    if index > 1
      tabnew
    endif
    let root = $HOME . '/project' . index
    call mkdir(root . '/child', 'p')
    call writefile(['content'], root . '/child/file')
    call ProjectTree(root)
    execute 'edit' fnameescape(root . '/child/file')
    py3 get_current_tree().anchor_root.get_child('child').isOpen = True
  endfor

  " A refused quit must not disable ordinary unload handling.
  call setline(1, 'modified')
  try
    qall
    call assert_report('Quit with modifications unexpectedly succeeded')
  catch /E37:/
  endtry
  call assert_equal(v:null, v:exiting)
  execute 'bunload!' bufnr('%')
  py3 assert not get_current_tree().edit_history
  execute 'edit' fnameescape(root . '/child/file')

  " Repeated setup must not register multiple saves.
  py3 vim.command("call SwitchToVinjaView('ProjectTree_%s')" % _get_tab_id_global())
  call SetupProjectTreeMappings()
  call SetupProjectTreeMappings()
  let panel = bufnr('%')
  py3 status_saves.clear()
  " Unload a panel from a different tab: save its owner, not the current tab.
  tabfirst
  execute 'bwipeout!' panel
  py3 assert status_saves == [os.path.join(os.environ['HOME'], 'project3')], status_saves

  " The hidden tree's in-memory history must still be saved on exit.
  tablast
  call writefile(['another'], root . '/child/another')
  execute 'edit' fnameescape(root . '/child/another')
  " Include many loaded buffers, not just the visible file windows.
  call mkdir($HOME . '/buffers', 'p')
  for index in range(1, 60)
    let path = $HOME . '/buffers/file' . index
    call writefile(['content'], path)
    call bufload(bufadd(path))
  endfor
  py3 live_trees = list(get_all_trees().values())
  py3 histories = {id(tree): list(tree.edit_history) for tree in live_trees}
  py3 status_saves.clear()
  py3 status_syncs, status_renders = [], []
  py3 original_sync = ProjectTree.set_file_edit
  py3 ProjectTree.set_file_edit = staticmethod(lambda path, flag: (status_syncs.append(path), original_sync(path, flag))[-1])
  py3 original_render = ProjectTree.render_tree
  py3 ProjectTree.render_tree = lambda self: (status_renders.append(self.project_root), original_render(self))[-1]
  autocmd VimLeave * call CheckShutdown()
catch
  call add(v:errors, v:exception . ' at ' . v:throwpoint)
endtry
if !empty(v:errors)
  call writefile(v:errors, $HOME . '/errors.txt')
  cquit 1
endif
qa!
