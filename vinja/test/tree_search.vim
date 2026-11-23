set nocompatible
set encoding=utf-8
if !empty($VINJA_TEST_PYTHON_DLL)
  let &pythonthreedll = $VINJA_TEST_PYTHON_DLL
endif
if !has('python3') || !has('job') || !has('timers')
  cquit 77
endif

function! WaitSearch(id) abort
  let started = reltime()
  while get(getqflist({'id': a:id, 'context': 1}).context, 'state', '') ==# 'running'
    if reltimefloat(reltime(started)) > 8
      call assert_report('Search did not finish')
      PTreeSearchStop
      break
    endif
    sleep 10m
  endwhile
  return getqflist({'id': a:id, 'context': 1, 'items': 1})
endfunction

function! StartSearch(pattern) abort
  let g:test_pattern = a:pattern
  let request = py3eval("__import__('tree_search').make_request(search_node, search_tree, vim.eval('g:test_pattern'))")
  call VinjaTreeSearchStart(request)
  return getqflist({'id': 0}).id
endfunction

try
  execute 'source' fnameescape($VINJA_TEST_REPO . '/plugin/vinja.vim')
  call mkdir($HOME . '/project', 'p')
  call writefile(['  needle a.b', 'needle axb'], $HOME . "/project/a 'quote: file")
  py3 search_tree = ProjectTree(os.path.join(os.environ['HOME'], 'project'))
  py3 search_node = search_tree.root
  call assert_equal(2, exists(':PTreeSearchStop'))

  " Actual entry point, including safe input handling and the existing mappings.
  call SetupProjectTreeMappings()
  call assert_match('PTreeSearchStop', maparg('<C-c>', 'n'))
  py3 search_tree.get_selected_node = lambda: search_node
  call feedkeys("a.b\<CR>", 't')
  py3 search_tree.recursive_search()
  let id = getqflist({'id': 0}).id
  let results = WaitSearch(id)
  call assert_equal('completed', results.context.state)
  call assert_equal(1, len(results.items))
  call assert_equal(10, results.items[0].col)
  call assert_equal('  needle a.b', results.items[0].text)
  call assert_equal('quickfix', &buftype)
  cclose

  let @/ = '\<needle\>'
  call feedkeys("\<CR>", 't')
  py3 search_tree.recursive_search2()
  let results = WaitSearch(getqflist({'id': 0}).id)
  call assert_equal(2, len(results.items))
  cclose

  let id = StartSearch('/a.b/')
  let original_window = win_getid()
  " Completion must neither steal focus nor append to an unrelated list.
  tabnew
  let other_window = win_getid()
  call setqflist([], ' ', {'title': 'unrelated', 'items': [{'text': 'untouched'}]})
  let other_id = getqflist({'id': 0}).id
  let results = WaitSearch(id)
  call assert_equal(2, len(results.items))
  call assert_equal(other_window, win_getid())
  call assert_equal(other_id, getqflist({'id': 0}).id)
  call assert_equal('untouched', getqflist()[0].text)
  tabclose
  call assert_equal(original_window, win_getid())

  " Even with the search list current, do not open results in another tab.
  let id = StartSearch('needle')
  tabnew
  let other_window = win_getid()
  let results = WaitSearch(id)
  call assert_equal(2, len(results.items))
  call assert_equal(other_window, win_getid())
  call assert_equal(0, getqflist({'winid': 0}).winid)
  tabclose

  let results = WaitSearch(StartSearch('no matches'))
  call assert_equal('completed', results.context.state)
  call assert_equal([], results.items)
  call assert_equal(0, getqflist({'winid': 0}).winid)
  let results = WaitSearch(StartSearch('/[/'))
  call assert_equal('failed', results.context.state)
  call assert_match('regex', results.context.error)
  call assert_equal(0, getqflist({'winid': 0}).winid)

  " Keep a producer alive to deterministically test cancellation and late output.
  let request = py3eval("__import__('tree_search').make_request(search_node, search_tree, 'needle')")
  let request.argv = ['sh', '-c', 'while :; do :; done']
  call VinjaTreeSearchStart(request)
  let stopped_id = getqflist({'id': 0}).id
  let before = reltime()
  PTreeSearchStop
  call assert_true(reltimefloat(reltime(before)) < 1.0)
  call assert_equal('cancelled', getqflist({'id': stopped_id, 'context': 1}).context.state)
  call assert_equal(0, getqflist({'winid': 0}).winid)
  let results = WaitSearch(StartSearch('needle'))
  call assert_equal('completed', results.context.state)
  call assert_equal(2, len(results.items))
  sleep 50m
  call assert_equal('cancelled', getqflist({'id': stopped_id, 'context': 1}).context.state)

  call VinjaTreeSearchStart(request)
  let replaced_id = getqflist({'id': 0}).id
  let results = WaitSearch(StartSearch('a.b'))
  call assert_equal('cancelled', getqflist({'id': replaced_id, 'context': 1}).context.state)
  call assert_equal(1, len(results.items))

  " A freed quickfix list must not be recreated by later callbacks.
  call VinjaTreeSearchStart(request)
  let producer = filter(job_info(), {_, job -> job_info(job).cmd ==# g:request.argv && job_status(job) ==# 'run'})[0]
  call setqflist([], 'f')
  sleep 100m
  call assert_equal('dead', job_status(producer))
  call assert_equal([], getqflist())

  call writefile(['needle ' . repeat('x', 600)], $HOME . '/project/long')
  let results = WaitSearch(StartSearch('needle x'))
  call assert_equal(500, strlen(results.items[0].text))
  call delete($HOME . '/project/long')

  cclose
  " Stay below the per-file size cap while exceeding the result cap.
  call writefile(repeat(['needle'], 12000), $HOME . '/project/many')
  let results = WaitSearch(StartSearch('needle'))
  call assert_equal('truncated', results.context.state)
  call assert_equal(10000, len(results.items))
  call assert_equal('quickfix', &buftype)
  call delete($HOME . '/project/many')

  " Unsupported nodes never expand or enumerate children.
  py3 search_tree.get_selected_node = lambda: TreeNode('virtual', '/unused', True)
  let before_id = getqflist({'id': 0}).id
  py3 search_tree.recursive_search()
  call assert_equal(before_id, getqflist({'id': 0}).id)
catch
  call add(v:errors, v:exception . ' at ' . v:throwpoint)
endtry
silent! PTreeSearchStop
if !empty(v:errors)
  call writefile(v:errors, $HOME . '/errors.txt')
  cquit 1
endif
qa!
