let g:sztodo_db_filter=""

function MyTabLine()
  let s = ''
  for i in range(tabpagenr('$'))
    " select the highlighting
    if i + 1 == tabpagenr()
      let s .= '%#TabLineSel#'
    else
      let s .= '%#TabLine#'
    endif

    " set the tab page number (for mouse clicks)
    let s .= '%' . (i + 1) . 'T'

    " the label is made by MyTabLabel()
    let s .= ' %{MyTabLabel(' . (i + 1) . ')} '
  endfor
  " after the last tab fill with TabLineFill and reset tab page nr
  let s .= '%#TabLineFill#%T'
	let &titlestring = MyTitleString()
  return s
endfunction

function MyTabLabel(n)
  " Append the tab number
  let label = a:n .': '
  let buflist = tabpagebuflist(a:n)
  let winnr = tabpagewinnr(a:n)
  let modified_part = ''
  
  for bufnum in buflist  
    if getbufvar(bufnum, "tab_name") != '' 
      return  label . getbufvar(bufnum, "tab_name")
    endif
  endfor

  if getbufvar(buflist[winnr - 1], "&modified")
    let modified_part = '+'
  endif

  if getbufvar(buflist[winnr - 1], "buf_tab_title") != '' 
    return  label . modified_part . getbufvar(buflist[winnr - 1], "buf_tab_title")
  endif

  let name = bufname(buflist[winnr - 1])
  if name == ''
    if &buftype=='quickfix'
      let name = '[Quickfix List]'
    else
      let name = '[No Name]'
    endif
  else
    let name = fnamemodify(name,":t")
  endif
  let label .= modified_part . name
  return label
endfunction

function MyTitleString()

  if exists("g:vinja_title")
    return g:vinja_title
  endif

	let v = MyTabLabel(tabpagenr()) 
	let idx = stridx(v, " ")
	let v1 = strpart(v, idx + 1)
	return v1
endfunction

function SetTabPageName(name)
  call setbufvar("%", "tab_name",a:name)
endfunction

function! GetVisualBlock() range
    let save = @"
    silent normal gvy
    let vis_cmd = @"
    let @" = save
    return vis_cmd
endfunction 

function! GetBufList()
  redir => bufoutput
  silent buffers
  redir END
  return bufoutput
endfunction

function SetVinjaBuf()
    exec "setlocal nowrap"    
    exec "setlocal buftype=nofile" 
    exec "setlocal noswapfile"
    exec "setlocal bufhidden=wipe"
    exec "setlocal nobuflisted"
    exec "setlocal nolist"
endfunction

function! SwitchToVinjaView(...)    
  let viewname = a:1
  let direct = "belowright"
  let height = winheight(0) / 2
  if len(a:000) > 1 
    let direct = a:2
  endif
  if len(a:000) > 2
    let height = a:3
  endif
  let s:cur_buf = bufnr("%")    
  let s:szdb_result_buf=bufnr("VinjaView_" . viewname)    
  if bufwinnr(s:szdb_result_buf) > 0    
    exec bufwinnr(s:szdb_result_buf) . "wincmd w"    
    "%d    
  else    
    exec 'silent! '.direct.' '.height.'split VinjaView_' . viewname    
    exec "e VinjaView_" . viewname    
    exec 'setlocal statusline=\ '.viewname
    call SetVinjaBuf()
  endif    
endfunction    

function! SwitchToVinjaViewVertical(viewname)    
  let s:cur_buf = bufnr("%")    
  let s:szdb_result_buf=bufnr("VinjaView_" . a:viewname)    
  if bufwinnr(s:szdb_result_buf) > 0    
    exec bufwinnr(s:szdb_result_buf) . "wincmd w"    
    "%d    
  else    
    exec 'silent! belowright vsplit VinjaView_' . a:viewname    
    exec "e VinjaView_" . a:viewname    
    exec 'setlocal statusline=\ '. a:viewname
    call SetVinjaBuf()
  endif    
endfunction    

function! SplitLeftPanel(splitSize,name) 
    let splitLocation="topleft "
    let splitMode="vertical "
    let splitSize=a:splitSize
    let cmd=splitLocation.splitMode.splitSize.' new '.a:name
    silent! execute cmd
    setlocal winfixwidth
    setlocal foldcolumn=0
    setlocal nobuflisted
    setlocal nospell
    setlocal cursorline
    setlocal nonumber
    call SetVinjaBuf()
endfunction

let g:vinja_home = fnamemodify(expand("<sfile>"), ':p:h:h') . '/vinja'

function RunSzPyfile(filename)
  exec "py3file ".g:vinja_home."/python/".a:filename
endfunction

function OpenChannel()
	let s:channel = ch_open('localhost:9527', {'callback': "MyHandler"})
endfunction

func MyHandler(channel, msg)
  echo "from the handler: " . a:msg
endfunc

function! Shext()
  if bufnr("VinjaView_cmd_buffer") > -1 
    echo "Shext is already running."
    return
  endif
  call RunSzPyfile("shext.py")
  file VinjaView_cmd_buffer
  exec 'setlocal statusline=\ shext_cmd_buffer\ [%r%{getcwd()}%h]'
  call SetTabPageName("Shext")
  call SetVinjaBuf()
  nnoremap <buffer><silent><cr>   :py3 shext.executeCmd(insertMode=False)<cr>
  imap <buffer><silent><cr>  <Esc>:py3 shext.executeCmd()<cr>
  ino <buffer><silent>/      <Esc>:py3 shext.tipDirName()<cr>a/
  inoremap <buffer><silent>;;  <Esc>:py3 OutputNavigator.startNavigate()<cr>
  nnoremap <buffer><silent><c-n>    :py3 OutputNavigator.next()<cr>
  nnoremap <buffer><silent><c-p>    :py3 OutputNavigator.prev()<cr>
  setlocal iskeyword+=.
  py3 Shext.runApp()
endfunction

function! SzDbextCompletion(findstart, base)
  py3 SzDbCompletion.completion(vim.eval("a:findstart"),vim.eval("a:base"))
  if a:findstart
    return g:SzCompletionIndex
  endif
    return g:SzCompletionResult
endfunction

function! FuzzyCompletion(findstart, base)
  py3 FuzzyCompletion.completion(vim.eval("a:findstart"),vim.eval("a:base"))
  if a:findstart
    return g:FuzzyCompletionIndex
  endif
    return g:FuzzyCompletionResult
endfunction

function! Dbext()
  call RunSzPyfile("dbext.py")
  set filetype=sql
  set omnifunc=SzDbextCompletion

  if exists("*SuperTabSetDefaultCompletionType")
    call SuperTabSetDefaultCompletionType("<c-x><c-o>")
  endif

  let l:bufname = bufname("%")    
  if l:bufname == "" 
    call SetVinjaBuf()
  endif    

  setlocal nobuflisted
  "setlocal ignorecase
  call SetTabPageName("Dbext")
  py3 Dbext.runApp()

  vmap <buffer><silent>,,  :py3 dbext.queryVisualSQL()<cr>
  vmap <buffer><silent>,j  :py3 dbext.queryVisualSQLToJson()<cr>
  imap <buffer><silent>,,  <C-o>:py3 dbext.executeOneStatement("line")<cr>
  nmap <buffer><silent>,,  :py3 dbext.executeOneStatement("line")<cr>

  map <buffer><silent>,gs :py3 dbext.executeOneStatement("visual")<cr>
  map <buffer><silent>,go :py3 dbext.promptDbOption()<cr>
  map <buffer><silent>,gc :py3 dbext.promptTempOption()<cr>
  map <buffer><silent>,lt :py3 QueryUtil.queryTables(True)<cr>
  map <buffer><silent>,la :py3 QueryUtil.queryTables(False)<cr>
  map <buffer><silent>,ld :py3 QueryUtil.queryDataBases()<cr>
  map <buffer><silent>,dt :py3 QueryUtil.descTable()<cr>
  map <buffer><silent>,gg :py3 dbext.exportResultToSQL()<cr>
endfunction



function CustomSub(exp,method)
  py3 MiscUtil.transform(vim.eval("a:exp"), vim.eval("a:method"))
	return g:sztransform_result
endfunction

function Transform(method) range
  let g:sztransform_result=0
  py3 MiscUtil.initIncValue()
  execute a:firstline.",".a:lastline.'s//\=CustomSub(submatch(0),a:method)/gc'
endfunction


function Pydoc(word)
  py3 pydoc(vim.eval("a:word"))
endfunction

function Javadoc()
  call RunSzPyfile("javadoc.py")
  file VinjaView_jdoc_content
  call SetTabPageName("Javadoc")
  call SetVinjaBuf()
  call SplitLeftPanel(40, 'VinjaView_jdoc_index') 
  map <silent><buffer> <cr>  :py3 jdocviewer.showJavaDoc()<cr>
  map <silent><buffer> o     :py3 jdocviewer.showJavaDoc()<cr>
  py3 Javadoc.runApp()
endfunction

function ProjectTree(...) 
  py3 ProjectTree.runApp()
  if bufname('%') =~ 'Vinja.*ProjectTree.*$'
		"call SetTabPageName("ProjectExplorer")
    nnoremap <silent><buffer> <2-leftmouse> :py3 projectTree.open_selected_node()<cr>
    map <silent><buffer> <cr>  :py3 projectTree.open_selected_node()<cr>
    map <silent><buffer> o     :py3 projectTree.open_selected_node()<cr>
    map <silent><buffer> O     :py3 projectTree.recursive_open_node()<cr>
    map <silent><buffer> t     :py3 projectTree.open_selected_node("tabnew")<cr>
    map <silent><buffer> i     :py3 projectTree.open_selected_node("leftabove split")<cr>
    map <silent><buffer> gc    :py3 projectTree.cmp_selected_node()<cr>
    map <silent><buffer> go    :py3 projectTree.preview_selected_node()<cr>
    map <silent><buffer> r     :py3 projectTree.refresh_selected_node()<cr>
    map <silent><buffer> x     :py3 projectTree.close_parent_node()<cr>
    map <silent><buffer> s     :py3 projectTree.filter_display_node()<cr>
    map <silent><buffer> z     :py3 projectTree.close_opened_file(False)<cr>
    map <silent><buffer> Z     :py3 projectTree.close_opened_file(True)<cr>
    map <silent><buffer> u     :py3 projectTree.up_one_level()<cr>
    map <silent><buffer> m     :py3 projectTree.mark_selected_node()<cr>
    map <silent><buffer> f     :py3 projectTree.recursive_search()<cr>
    map <silent><buffer> F     :py3 projectTree.recursive_search2()<cr>
    map <silent><buffer> <     :py3 projectTree.get_prev_open_node()<cr>
    map <silent><buffer> >     :py3 projectTree.get_next_open_node()<cr>
    map <silent><buffer> g<    :py3 projectTree.get_prev_marked_node()<cr>
    map <silent><buffer> g>    :py3 projectTree.get_next_marked_node()<cr>
    map <silent><buffer> e     :py3 projectTree.open_with_default()<cr>
    map <silent><buffer> E     :py3 projectTree.open_in_terminal()<cr>
    map <silent><buffer> <C-j> :py3 projectTree.goto_next_sibling()<cr>
    map <silent><buffer> <C-k> :py3 projectTree.goto_prev_sibling()<cr>

    map <silent><buffer> !     :py3 projectTree.load_java_classpath()<cr>
    map <silent><buffer> #     :py3 projectTree.toggleTreeType("workSpaceTree")<cr>
    map <silent><buffer> @     :py3 projectTree.toggleTreeType("workSetTree")<cr>
    map <silent><buffer> %     :py3 projectTree.toggleTreeType("projectTree")<cr>
    map <silent><buffer> DD    :py3 projectTree.delete_node()<cr>
    map <silent><buffer> Dm    :py3 projectTree.delete_marked_node()<cr>
    map <silent><buffer> A     :py3 projectTree.add_node()<cr>
    map <silent><buffer> I     :py3 projectTree.toggleHidden()<cr>

    map <silent><buffer> ya    :py3 projectTree.yank_node_path()<cr>
    map <silent><buffer> yr    :py3 projectTree.yank_node_rel_path()<cr>
    map <silent><buffer> yn    :py3 projectTree.yank_node_name()<cr>
    map <silent><buffer> cc    :py3 projectTree.rename_node()<cr>
    map <silent><buffer> yy    :py3 projectTree.yank_selected_node(False)<cr>
    map <silent><buffer> dd    :py3 projectTree.yank_selected_node(True)<cr>
    map <silent><buffer> ym    :py3 projectTree.yank_marked_node(False)<cr>
    map <silent><buffer> dm    :py3 projectTree.yank_marked_node(True)<cr>
    map <silent><buffer> p     :py3 projectTree.paste()<cr>
    map <silent><buffer> P     :py3 projectTree.paste_from_clipBoard()<cr>
    map <silent><buffer> YY    :py3 projectTree.copy_to_clipBoard()<cr>

    map <silent><buffer> ?     :py3 projectTree.print_help()<cr>
    map <silent><buffer> C     :py3 projectTree.change_root()<cr>
    map <silent><buffer> B     :py3 projectTree.change_back()<cr>
    map <silent><buffer> U     :py3 projectTree.change_root_upper()<cr>
    map <silent><buffer> QQ    :py3 projectTree.dispose_tree()<cr>
    "map <silent><buffer> S     :py3 projectTree.save_status(False)<cr>
    autocmd BufUnload <buffer>  py3 projectTree.save_status(False)

    vmap <silent><buffer> DD   :py3 projectTree.delete_visual_node()<cr>
    vmap <silent><buffer> m    :py3 projectTree.mark_visual_node()<cr>
    vmap <silent><buffer> yy   :py3 projectTree.yank_visual_node(False)<cr>
    vmap <silent><buffer> dd   :py3 projectTree.yank_visual_node(True)<cr>
	endif
  exec 'wincmd w'
endfunction



function LocateFile(locateType)
  call RunSzPyfile("locate.py")
  py3 fcmgr = FileContentManager(vim.eval("a:locateType"))
  py3 QuickLocater.runApp(fcmgr)
endfunction

function LocateHistory()
  call RunSzPyfile("locate.py")
  py3 hismgr = EditHistoryManager()
  py3 QuickLocater.runApp(hismgr)
endfunction

function LocateProject()
  call RunSzPyfile("locate.py")
  py3 projmgr = ProjectLocationManager()
  py3 QuickLocater.runApp(projmgr)
endfunction

function LocateMember()
  call RunSzPyfile("locate.py")
  py3 membermgr = JavaMemberContentManager()
  py3 QuickLocater.runApp(membermgr)
endfunction

function LocateClass()
  call RunSzPyfile("locate.py")
  py3 classnamemgr = JavaClassNameContentManager()
  py3 QuickLocater.runApp(classnamemgr)
endfunction

function LocateHierarchy()
  call RunSzPyfile("locate.py")
  py3 method,param = Parser.parseCurrentMethodName()
  py3 thmgr = TypeHierarchyContentManager(vim.current.buffer.name,method,param)
  py3 QuickLocater.runApp(thmgr)
endfunction


call RunSzPyfile("common.py")
call RunSzPyfile("tree.py")

set completefunc=FuzzyCompletion
set tabline=%!MyTabLine()
autocmd BufEnter * let &titlestring = MyTitleString()

function! SzJdbCompletion(findstart, base)
  py3 Jdb.completion(vim.eval("a:findstart"),vim.eval("a:base"))
  if a:findstart
    return g:SzJdbCompletionIndex
  endif
    return g:SzJdbCompletionResult
endfunction


function! SzJdeCompletion(findstart, base)
  py3 SzJdeCompletion.completion(vim.eval("a:findstart"),vim.eval("a:base"))
  if a:findstart
    return g:SzJdeCompletionIndex
  endif
    return g:SzJdeCompletionResult
endfunction

function FetchResult(...)
  py3 fetchCallBack(vim.eval("a:000"))
  redraw
endfunction

function FetchJdbResult()
  py3 jdb.fetchJdbResult()
  redraw
endfunction


function FetchAutocmdResult()
  py3 jdb.fetchAutocmdResult()
  redraw
endfunction

function HandleJdiEvent(...)
  py3 jdb.handleJdiEvent(vim.eval("a:000"))
  "redraw
endfunction

function HandleBuildResult(...)
  py3 Compiler.setQfList(vim.eval("a:000"))
endfunction


function! Jdb()
  py3 Jdb.runApp()
endfunction


function JdeDotCompletion()
  return  ".\<C-X>\<C-O>"
endfunction

function DisplayMsg(msg)
    let x=&ruler | let y=&showcmd
    set noruler noshowcmd
    redraw
    echo strpart(a:msg, 0, &columns-1)
    let &ruler=x | let &showcmd=y
endfun

function InitJavaSetting() 
  map <C-n> :cn<cr>
  map <C-p> :cp<cr>
  setlocal omnifunc=SzJdeCompletion
  "set cmdheight=2
  "au CursorHold <buffer> :py3 HighlightManager.displayMsg()
  "au CursorMoved <buffer> :py3 HighlightManager.displayMsg()
  py3 EditUtil.createSkeleton()
  if exists("*SuperTabSetDefaultCompletionType")
    call SuperTabSetDefaultCompletionType("<c-x><c-o>")
  endif
endfunction

function! JdeInit()
  call RunSzPyfile("jde.py")
  set completeopt=menuone
  autocmd BufEnter     *.java     call InitJavaSetting()
  autocmd BufWinEnter   *.java    py3 HighlightManager.highlightCurrentBuf()
  "autocmd BufReadPost   *.java    py3 HighlightManager.highlightCurrentBuf()
  autocmd BufWritePost  *.java    py3 Compiler.compileCurrentFile()
  autocmd BufWritePost  *         py3 Compiler.copyResource()

  autocmd BufRead     *.java     py3 Compiler.preloadAstInfo()

  autocmd CursorHold *.java  :py3 HighlightManager.displayMsg()
  autocmd CursorMoved *.java :py3 HighlightManager.displayMsg()

  command! -nargs=0   DumpClass        :py3 EditUtil.dumpClassInfo()
  command! -nargs=0   AutoImport       :py3 AutoImport.autoImportVar()
  command! -nargs=0   Run              :py3 Runner.runCurrentFile()
  command! -nargs=0   RunTest          :py3 Runner.runCurrentFile("runTest")
  command! -nargs=0   Overide          :py3 EditUtil.overideMethod()

  command! -nargs=0   Jdb              :call Jdb()
  command! -nargs=0   ToggleBreakPoint :py3 EditUtil.toggleBreakpoint()
  command! -nargs=1   FetchDebugOutput :call FetchDebugOutput('<args>')
  command! -nargs=*   HandleJdiEvent    :call HandleJdiEvent(<f-args>)

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>,   :py3 Runner.runCurrentFile()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><M-0>       :py3 VimUtil.closeVinjaBuffer("JdeConsole")<cr>
  autocmd BufEnter  *.java    vmap <buffer><silent><leader>gs  :py3 EditUtil.generateGseter()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>dc  :py3 EditUtil.dumpClassInfo()<cr>

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>gd  :py3 EditUtil.locateDefinition("declare")<cr>
  autocmd BufEnter  *.class   nmap <buffer><silent><leader>gd  :py3 EditUtil.locateDefinition("declare")<cr>

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>gi  :py3 EditUtil.locateDefinition("impl")<cr>
  autocmd BufEnter  *.class   nmap <buffer><silent><leader>gi  :py3 EditUtil.locateDefinition("impl")<cr>

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>gh  :py3 EditUtil.searchRef()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>ci  :py3 AutoImport.autoImportVar()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>pt  :py3 ProjectManager.projectTree()<cr>

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>go  :call LocateMember()<cr>
  autocmd BufEnter  *.class   nmap <buffer><silent><leader>go  :call LocateMember()<cr>

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>cq  :py3 Parser.copyMainClassToRegister()<cr>
  autocmd BufEnter  *.class   nmap <buffer><silent><leader>cq  :py3 Parser.copyMainClassToRegister()<cr>

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>gt  :call LocateHierarchy()<cr>
  autocmd BufEnter  *         nmap <buffer><silent><leader>gc  :call LocateClass()<cr>

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>tb  :py3 EditUtil.toggleBreakpoint()<cr>
  autocmd BufEnter  *.class   nmap <buffer><silent><leader>tb  :py3 EditUtil.toggleBreakpoint()<cr>

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>td  :py3 Jdb.runApp()<cr>
  autocmd BufEnter  *.java    imap <buffer><silent><M-9>       <c-o>:py3 EditUtil.tipMethodDefinition()<cr>
  autocmd BufEnter  *.java    imap <buffer><silent><M-8>       <c-o>:py3 EditUtil.tipMethodDefinition(True)<cr>
  autocmd BufEnter  *.java    imap <buffer><silent><M-7>       <c-o>:py3 AutoImport.autoImportVar()<cr>
  autocmd BufEnter  *.java    imap <buffer><silent><M-0>       <c-o>:py3 VimUtil.closeVinjaBuffer("JdeConsole")<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><M-9>       :py3 EditUtil.tipMethodDefinition()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><M-8>       :py3 EditUtil.tipMethodDefinition(True)<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><M-0>       :py3 VimUtil.closeVinjaBuffer("JdeConsole")<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>de  :py3 Jdb.toggleDebug()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>sc  :py3 EditUtil.showJdeConsoleOut()<cr>
  

  autocmd CompleteDone *.java  :py3 AutoImport.importAfterCompletion()
  
  autocmd BufEnter  VinjaView_Jdb  nmap <buffer><silent><leader>de    :py3 Jdb.toggleDebug()<cr>
  autocmd BufEnter  VinjaView_Jdb  nmap <buffer><silent><leader>k     :py3 jdb.qevalCmd()<cr>
  autocmd BufEnter  VinjaView_Jdb  nmap <buffer><silent><F5>     :py3 jdb.stepCmd('step_into')<cr>
  autocmd BufEnter  VinjaView_Jdb  nmap <buffer><silent><F6>     :py3 jdb.stepCmd('step_over')<cr>
  autocmd BufEnter  VinjaView_Jdb  nmap <buffer><silent><F7>     :py3 jdb.stepCmd('step_return')<cr>
  autocmd BufEnter  VinjaView_Jdb  nmap <buffer><silent><F8>     :py3 jdb.stepCmd('resume')<cr>
  autocmd BufEnter  VinjaView_Jdb  nmap <buffer><silent><c-i>    :py3 jdb.toggleQuickStep()<cr>

  autocmd BufEnter  VinjaView_Jdb  imap <buffer><silent><F5>     <c-o>:py3 jdb.stepCmd('step_into')<cr>
  autocmd BufEnter  VinjaView_Jdb  imap <buffer><silent><F6>     <c-o>:py3 jdb.stepCmd('step_over')<cr>
  autocmd BufEnter  VinjaView_Jdb  imap <buffer><silent><F7>     <c-o>:py3 jdb.stepCmd('step_return')<cr>
  autocmd BufEnter  VinjaView_Jdb  imap <buffer><silent><F8>     <c-o>:py3 jdb.stepCmd('resume')<cr>
  "autocmd BufEnter  VinjaView_Jdb  imap <buffer><silent><c-i>    <c-o>:py3 jdb.toggleQuickStep()<cr>

  autocmd BufLeave  VinjaView_JdeConsole  py3 EditUtil.saveJdeConsoleOut()
  autocmd BufEnter  VinjaView_JdeConsole  nmap <buffer><silent><leader>de  :py3 Jdb.toggleDebug()<cr>

endfunction

function! Jdext()
  call JdeInit()
  py3 ProjectManager.projectOpen()
endfunction

function! Jdesp()
  py3 MiscUtil.select_project_open()
endfunction


command! -nargs=1 -range=% Transform :<line1>,<line2>call Transform('<args>')

command! -nargs=0 StartAgent  :py3 VinjaAgent.startAgent()
command! -nargs=0 StopAgent   :py3 VinjaAgent.stopAgent()
command! -nargs=0 Shext       :call Shext()
command! -nargs=0 Jdext       :call Jdext()
"select project to open in jde.
command! -nargs=0 Jdesp       :call Jdesp()
command! -nargs=0 Dbext       :call Dbext()


command! -nargs=0 ProjectTree          :call ProjectTree()
command! -nargs=0 ProjectTreeFind      :py3 ProjectTree.locate_buf_in_tree()
command! -nargs=0 ProjectTreeDispose   :py3 ProjectTree.dispose_tree()

nmap <silent><leader>pt  :call ProjectTree()<cr>
nmap <silent><leader>pf  :py3 ProjectTree.locate_buf_in_tree()<cr>

"vinja mapping
nmap <silent><leader>zc  :py3 ScratchUtil.startScriptEdit()<cr>
vmap <silent><leader>zf  :py3 MiscUtil.simpleFormatSQL()<cr>
vmap <silent><leader>te  :py3 MiscUtil.tabulate()<cr>
vmap <silent><leader>tr  :py3 MiscUtil.arrange()<cr>
vmap <silent><leader>zg  :py3 MiscUtil.operateVisualContext()<cr>
nmap <silent><leader>rc  :py3 MiscUtil.remove_comment()<cr>
nmap <silent><leader>ya  :py3 MiscUtil.copy_buffer_path()<cr>
nmap <silent><leader>mm  :py3 VimUtil.toggleMaxWin()<cr>
nmap <silent><leader>mw  :py3 VimUtil.zoomWinWidth()<cr>
nmap <silent><leader>bc  :py3 MiscUtil.selectColumn()<cr>

nmap <silent><leader>zs  :py3 MiscUtil.startfile()<cr>
nmap <silent><leader>zv  <C-Q>

nmap <silent><leader>lc  :call LocateFile("currentDir")<cr>
nmap <silent><leader>lw  :call LocateFile("all")<cr>
nmap <silent><leader>la  :call LocateHistory()<cr>
nmap <silent><leader>lr  :call LocateProject()<cr>

function SetProjectTreeFileEditFlag(filename,flag)
  py3 ProjectTree.set_file_edit(vim.eval("a:filename"),vim.eval("a:flag"))
endfunction

autocmd BufReadCmd  jar://*	py3 ZipUtil.read_zip_cmd()
autocmd BufReadPost *  call SetProjectTreeFileEditFlag(expand("<amatch>"),"true")
autocmd BufUnload   *  call SetProjectTreeFileEditFlag(expand("<amatch>"),"false")

function RemoveFromHistory(filename)
  py3 edit_history.remove_from_history(vim.eval("a:filename"))
endfunction

autocmd BufEnter           *  py3 edit_history.record_current_buf()
autocmd VimEnter,WinEnter  *  py3 edit_history.create_win_id()
autocmd BufUnload          *  call RemoveFromHistory(expand("<amatch>"))

command! -nargs=1 Silent
\ | execute ':silent !'.<q-args>
\ | execute ':redraw!'

py3 VinjaAgent.startAgent()
