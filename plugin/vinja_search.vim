if exists('g:loaded_vinja_search')
  finish
endif
let g:loaded_vinja_search = 1
let s:search = {}

function! s:Publish(state) abort
  let title = 'ProjectTree [' . a:state . '] ' . s:search.request.pattern
  let context = {'state': a:state, 'root': s:search.request.root,
        \ 'count': s:search.count, 'skipped': s:search.skipped,
        \ 'error': s:search.error}
  call setqflist([], 'a', {'id': s:search.id, 'title': title, 'context': context})
endfunction

function! s:Finish(state) abort
  py3 _vinja_tree_search_parser = None
  call timer_stop(s:search.timer)
  if has_key(s:search, 'job') && job_status(s:search.job) ==# 'run'
    call job_stop(s:search.job, 'kill')
  endif
  if get(getqflist({'id': s:search.id}), 'id', 0) == s:search.id
    call s:Publish(a:state)
  endif
  echom 'ProjectTree search ' . a:state . ': ' . s:search.count
        \ . ' matching lines; ' . s:search.skipped . ' unrepresentable paths skipped.'
        \ . (empty(s:search.error) ? '' : ' ' . s:search.error)
  let open_results = index(['completed', 'truncated'], a:state) >= 0
        \ && s:search.count > 0 && getqflist({'id': 0}).id == s:search.id
        \ && win_id2tabwin(s:search.origin_win)[0] == tabpagenr()
  let s:search = {}
  if open_results
    botright copen
  endif
endfunction

function! s:Flush() abort
  try
    if get(getqflist({'id': s:search.id}), 'id', 0) != s:search.id
      call s:Finish('cancelled')
      return
    endif
    let parsed = py3eval("_vinja_tree_search_parser.parse_matches(vim.eval('s:search.messages'))")
    let s:search.messages = []
    let items = parsed.items[: 10000 - s:search.count - 1]
    let s:search.skipped += parsed.skipped
    let s:search.count += len(items)
    if !empty(items)
      call setqflist([], 'a', {'id': s:search.id, 'items': items})
    endif
    if s:search.count >= 10000
      call s:Finish('truncated')
    else
      call s:Publish('running')
    endif
  catch
    let s:search.error = v:exception
    call s:Finish('failed')
  endtry
endfunction

function! VinjaTreeSearchStop() abort
  if !empty(s:search)
    call s:Flush()
    if !empty(s:search)
      call s:Finish('cancelled')
    endif
  endif
endfunction

function! s:Current(channel) abort
  return !empty(s:search) && a:channel == job_getchannel(s:search.job)
endfunction

function! s:Output(channel, message) abort
  if s:Current(a:channel)
    call add(s:search.messages, a:message)
    let s:search.bytes += strlen(a:message)
    " Bound batches by both record count and bytes (one record can be a long line).
    if len(s:search.messages) >= 100 || s:search.bytes >= 65536
      let s:search.bytes = 0
      call s:Flush()
    endif
  endif
endfunction

function! s:Error(channel, message) abort
  if s:Current(a:channel)
    let s:search.error = strpart(s:search.error . ' ' . a:message, 0, 2000)
  endif
endfunction

function! s:Closed(channel) abort
  if s:Current(a:channel)
    let s:search.closed = 1
  endif
endfunction

function! s:Poll(timer) abort
  if empty(s:search) || a:timer != s:search.timer
    return
  endif
  if get(getqflist({'id': s:search.id}), 'id', 0) != s:search.id
    call s:Finish('cancelled')
    return
  endif
  if !empty(s:search.messages)
    let s:search.bytes = 0
    call s:Flush()
  endif
  " close_cb guarantees output callbacks have drained, unlike process exit.
  if !empty(s:search) && s:search.closed && job_status(s:search.job) ==# 'dead'
    let code = job_info(s:search.job).exitval
    call s:Finish(code == 0 || code == 1 ? 'completed' : 'failed')
  endif
endfunction

function! VinjaTreeSearchStart(request) abort
  if !has('job') || !has('timers') || !has('channel')
    echo 'ProjectTree search requires Vim with jobs, channels and timers.'
    return
  endif
  if !executable('rg')
    echo 'ProjectTree search requires ripgrep (rg) on PATH.'
    return
  endif
  call VinjaTreeSearchStop()
  call setqflist([], ' ', {'title': 'ProjectTree search', 'items': []})
  let s:search = {'id': getqflist({'id': 0}).id, 'request': a:request,
        \ 'count': 0, 'skipped': 0, 'error': '', 'timer': -1,
        \ 'messages': [], 'bytes': 0, 'closed': 0, 'origin_win': win_getid()}
  try
    " Keep the parser in Python: vim.eval() copies Vim dictionaries on every call.
    py3 _vinja_tree_search_parser = __import__('tree_search').MatchParser(vim.eval('s:search.request'))
    let s:search.job = job_start(a:request.argv, {'in_io': 'null',
          \ 'out_mode': 'nl', 'err_mode': 'nl', 'stoponexit': 'kill',
          \ 'out_cb': function('s:Output'), 'err_cb': function('s:Error'),
          \ 'close_cb': function('s:Closed')})
    if job_status(s:search.job) ==# 'fail'
      let s:search.error = 'Could not start ripgrep.'
      call s:Finish('failed')
      return
    endif
    let s:search.timer = timer_start(20, function('s:Poll'), {'repeat': -1})
    call s:Publish('running')
    echo 'ProjectTree search started. :copen for results; :PTreeSearchStop to cancel.'
  catch
    let s:search.error = v:exception
    call s:Finish('failed')
  endtry
endfunction

command! -nargs=0 PTreeSearchStop call VinjaTreeSearchStop()
