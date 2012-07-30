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

function SetSzToolBuf()
    exec "setlocal nowrap"    
    exec "setlocal buftype=nofile" 
    exec "setlocal noswapfile"
    exec "setlocal bufhidden=wipe"
    exec "setlocal nobuflisted"
endfunction

function! SwitchToSzToolView(...)    
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
  let s:szdb_result_buf=bufnr("SzToolView_" . viewname)    
  if bufwinnr(s:szdb_result_buf) > 0    
    exec bufwinnr(s:szdb_result_buf) . "wincmd w"    
    "%d    
  else    
    exec 'silent! '.direct.' '.height.'split SzToolView_' . viewname    
    exec "e SzToolView_" . viewname    
    exec 'setlocal statusline=\ '.viewname
    call SetSzToolBuf()
  endif    
endfunction    

function! SwitchToSzToolViewVertical(viewname)    
  let s:cur_buf = bufnr("%")    
  let s:szdb_result_buf=bufnr("SzToolView_" . a:viewname)    
  if bufwinnr(s:szdb_result_buf) > 0    
    exec bufwinnr(s:szdb_result_buf) . "wincmd w"    
    "%d    
  else    
    exec 'silent! belowright vsplit SzToolView_' . a:viewname    
    exec "e SzToolView_" . a:viewname    
    exec 'setlocal statusline=\ '. a:viewname
    call SetSzToolBuf()
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
    call SetSzToolBuf()
endfunction


function RunSzPyfile(filename)
  exec "pyfile ".g:sztool_home."/python/".a:filename
endfunction

function! Shext()
  if bufnr("SzToolView_cmd_buffer") > -1 
    echo "Shext is already running."
    return
  endif
  call RunSzPyfile("shext.py")
  file SzToolView_cmd_buffer
  exec 'setlocal statusline=\ shext_cmd_buffer\ [%r%{getcwd()}%h]'
  call SetTabPageName("Shext")
  call SetSzToolBuf()
  nnoremap <buffer><silent><cr>   :python shext.executeCmd(insertMode=False)<cr>
  imap <buffer><silent><cr>  <Esc>:python shext.executeCmd()<cr>
  ino <buffer><silent>/      <Esc>:python shext.tipDirName()<cr>a/
  inoremap <buffer><silent>;;  <Esc>:python OutputNavigator.startNavigate()<cr>
  nnoremap <buffer><silent><c-n>    :python OutputNavigator.next()<cr>
  nnoremap <buffer><silent><c-p>    :python OutputNavigator.prev()<cr>
  setlocal iskeyword+=.
  python Shext.runApp()
endfunction

function! SzDbextCompletion(findstart, base)
  python SzDbCompletion.completion(vim.eval("a:findstart"),vim.eval("a:base"))
  if a:findstart
    return g:SzCompletionIndex
  endif
    return g:SzCompletionResult
endfunction

function! FuzzyCompletion(findstart, base)
  python FuzzyCompletion.completion(vim.eval("a:findstart"),vim.eval("a:base"))
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
    call SetSzToolBuf()
  endif    

  setlocal nobuflisted
  "setlocal ignorecase
  call SetTabPageName("Dbext")
  python Dbext.runApp()

  vmap <buffer><silent>,,  :python dbext.queryVisualSQL()<cr>
  imap <buffer><silent>,,  <C-o>:python dbext.executeOneStatement("line")<cr>
  nmap <buffer><silent>,,  :python dbext.executeOneStatement("line")<cr>

  map <buffer><silent>,gs :python dbext.executeOneStatement("visual")<cr>
  map <buffer><silent>,go :python dbext.promptDbOption()<cr>
  map <buffer><silent>,gc :python dbext.promptTempOption()<cr>
  map <buffer><silent>,lt :python QueryUtil.queryTables(True)<cr>
  map <buffer><silent>,la :python QueryUtil.queryTables(False)<cr>
  map <buffer><silent>,ld :python QueryUtil.queryDataBases()<cr>
  map <buffer><silent>,dt :python QueryUtil.descTable()<cr>
  map <buffer><silent>,gg :python QueryUtil.generateSQL()<cr>
endfunction

function! SzDtdCompletion(findstart,base)
  python SzDtdCompletion(vim.eval("a:findstart"),vim.eval("a:base"))
  if a:findstart
    return g:SzCompletionIndex
  endif
    return g:SzCompletionResult
endfunction

function! LoadDtd()
  call RunSzPyfile("dtd.py")
  set omnifunc=SzDtdCompletion
  python parseDtdDecl()
endfunction

function! CodeGen()
  call RunSzPyfile("dbext.py")
  call RunSzPyfile("codegen.py")
  map <buffer><silent>,, :python generateCode()<cr>
endfunction

function! Recite()
  if bufnr("SzToolView_recite") > -1 
    echo "Recite is already running."
    return
  endif
  file SzToolView_recite
  call SetSzToolBuf()
  call RunSzPyfile("engext.py")
  map <silent><buffer> o  :python recite.wordDetail()<cr>
  map <silent><buffer> n  :python recite.listWords(20)<cr>
  map <silent><buffer> t  :python recite.trainning()<cr>
  python Recite.runApp()
endfunction

function! ClassicReader()
  if bufnr("SzToolView_book_content") > -1 
    echo "ClassicReader is already running."
    return
  endif
  file SzToolView_book_content
  call SetTabPageName("ClassicReader")
  call SetSzToolBuf()
  call SplitLeftPanel(43, 'SzToolView_book_index') 
  call RunSzPyfile("engext.py")
  python ClassicReader.runApp()
  map <silent><buffer> <cr>  :python classicReader.updateContentView()<cr>
  map <silent><buffer> o     :python classicReader.updateContentView()<cr>
endfunction

function! Notext()
  if bufnr("SzToolView_note_list") > -1 
    echo "Notext is already running."
    return
  endif
  file SzToolView_note_list
  call SetTabPageName("Notext")
  call SetSzToolBuf()
  call SplitLeftPanel(20, 'SzToolView_tag_list') 
  map <silent><buffer> <cr>  :python notext.listCurrentTagItems()<cr>
  map <silent><buffer> o     :python notext.listCurrentTagItems()<cr>
  command! -nargs=0 ExitNote   :python notext.exit()
  python Notext.runApp()
endfunction

function! Tagext()
  call RunSzPyfile("tagext.py")
  python TagExt.runApp()
  python tagext.edit_tag()
endfunction

function! TagList()
  call RunSzPyfile("tagext.py")
  python TagExt.runApp()
  python tagext.list_buf()
endfunction


function! NoteBufferSetting()  
  map <silent><buffer> o  :python notext.queryDetail()<cr>
  map <silent><buffer> i  :python notext.makeTemplate()<cr>
  command! -nargs=0 RemoveItem   :python notext.removeNoteItem()
  command! -nargs=0 ExitNote   :python notext.exit()
endfunction  


function! NoteItemSyntax()  
  
  syn keyword sztodoKeyword tag title id status create_date   
  syn keyword sztodoStatus unstarted done doing postpone  
  
  syn match tags "^tags:.*"   
  syn match title "^title:.*"  
  syn match id  "^id:.*"  
  syn match status  "^status:.*"  
  
  hi def link sztodoKeyword Keyword  
  hi def link sztodoStatus Identifier  
  hi def link tags String  
  hi def link id String  
  hi def link title String  
  hi def link status String  
  
endfunction  


function! SzSudoku()  

  call RunSzPyfile("gamext.py")
  call SetSzToolBuf()
  call SetTabPageName("Sudoku")
  python Sudoku.runApp()
  command! -nargs=0 CheckSudoku  :python sudoku.checkBufferMap()
  command! -nargs=0 Hint         :python sudoku.hint()
endfunction

function! SzMineSweeper()  
  call RunSzPyfile("gamext.py")
  call SetTabPageName("MineSweeper")
  python mf=MineField()
  python content=mf.getFormatedMap()
  python output(content)
  map <buffer><silent>d    :python mf.digField()<cr>
  map <buffer><silent>m    :python mf.markField()<cr>
endfunction

function CustomSub(exp,method)
  python MiscUtil.transform(vim.eval("a:exp"), vim.eval("a:method"))
	return g:sztransform_result
endfunction

function Transform(method) range
  let g:sztransform_result=0
  python MiscUtil.initIncValue()
  execute a:firstline.",".a:lastline.'s//\=CustomSub(submatch(0),a:method)/gc'
endfunction

function SearchDict(word)
  call RunSzPyfile("pystardict.py")
  python DictUtil.searchDict(vim.eval("a:word"))
  python DictUtil.playWordSound(vim.eval("a:word"))
endfunction


function Pydoc(word)
  python pydoc(vim.eval("a:word"))
endfunction

function Javadoc()
  call RunSzPyfile("javadoc.py")
  file SzToolView_jdoc_content
  call SetTabPageName("Javadoc")
  call SetSzToolBuf()
  call SplitLeftPanel(40, 'SzToolView_jdoc_index') 
  map <silent><buffer> <cr>  :python jdocviewer.showJavaDoc()<cr>
  map <silent><buffer> o     :python jdocviewer.showJavaDoc()<cr>
  command! -nargs=0 ExitNote   :python notext.exit()
  python Javadoc.runApp()
endfunction

function ProjectTree(...) 
  python ProjectTree.runApp()
  if bufname('%') =~ 'SzTool.*ProjectTree.*$'
    nnoremap <silent><buffer> <2-leftmouse> :python projectTree.open_selected_node()<cr>
    map <silent><buffer> <cr>  :python projectTree.open_selected_node()<cr>
    map <silent><buffer> o     :python projectTree.open_selected_node()<cr>
    map <silent><buffer> O     :python projectTree.recursive_open_node()<cr>
    map <silent><buffer> t     :python projectTree.open_selected_node("tabnew")<cr>
    map <silent><buffer> i     :python projectTree.open_selected_node("split")<cr>
    map <silent><buffer> go    :python projectTree.preview_selected_node()<cr>
    map <silent><buffer> r     :python projectTree.refresh_selected_node()<cr>
    map <silent><buffer> x     :python projectTree.close_parent_node()<cr>
    map <silent><buffer> s     :python projectTree.filter_display_node()<cr>
    map <silent><buffer> z     :python projectTree.close_opened_file(False)<cr>
    map <silent><buffer> Z     :python projectTree.close_opened_file(True)<cr>
    map <silent><buffer> u     :python projectTree.up_one_level()<cr>
    map <silent><buffer> m     :python projectTree.mark_selected_node()<cr>
    map <silent><buffer> f     :python projectTree.recursive_search()<cr>
    map <silent><buffer> F     :python projectTree.recursive_search2()<cr>
    map <silent><buffer> <     :python projectTree.get_prev_open_node()<cr>
    map <silent><buffer> >     :python projectTree.get_next_open_node()<cr>

    map <silent><buffer> @     :python projectTree.gotoBookmark()<cr>
    map <silent><buffer> DD    :python projectTree.delete_node()<cr>
    map <silent><buffer> Dm    :python projectTree.delete_marked_node()<cr>
    map <silent><buffer> A     :python projectTree.add_node()<cr>

    map <silent><buffer> ya    :python projectTree.yank_node_path()<cr>
    map <silent><buffer> cc    :python projectTree.rename_node()<cr>
    map <silent><buffer> yy    :python projectTree.yank_selected_node(False)<cr>
    map <silent><buffer> dd    :python projectTree.yank_selected_node(True)<cr>
    map <silent><buffer> ym    :python projectTree.yank_marked_node(False)<cr>
    map <silent><buffer> dm    :python projectTree.yank_marked_node(True)<cr>
    map <silent><buffer> p     :python projectTree.paste()<cr>
    map <silent><buffer> P     :python projectTree.paste_from_clipBoard()<cr>
    map <silent><buffer> YY    :python projectTree.copy_to_clipBoard()<cr>

    map <silent><buffer> ?     :python projectTree.print_help()<cr>
    map <silent><buffer> C     :python projectTree.change_root()<cr>
    map <silent><buffer> B     :python projectTree.change_back()<cr>
    map <silent><buffer> U     :python projectTree.change_root_upper()<cr>
    map <silent><buffer> QQ    :python projectTree.dispose_tree()<cr>
	endif
endfunction

function PlayDict(word)
  python playWordSound(vim.eval("a:word"))
endfunction

function Ledit(name)
  call RunSzPyfile("shext.py")
  python Shext.ledit(vim.eval("a:name"))
endfunction

function WatchExample(name)
  python MiscUtil.watchExample(vim.eval("a:name"))
endfunction

function LocateFile()
  call RunSzPyfile("locate.py")
  python fcmgr = FileContentManager()
  python QuickLocater.runApp(fcmgr)
endfunction

function LocateHistory()
  call RunSzPyfile("locate.py")
  python hismgr = EditHistoryManager()
  python QuickLocater.runApp(hismgr)
endfunction

function LocateMember()
  call RunSzPyfile("locate.py")
  python membermgr = JavaMemberContentManager()
  python QuickLocater.runApp(membermgr)
endfunction

function LocateClass()
  call RunSzPyfile("locate.py")
  python classnamemgr = JavaClassNameContentManager()
  python QuickLocater.runApp(classnamemgr)
endfunction

function LocateHierarchy()
  call RunSzPyfile("locate.py")
  python method,param = Parser.parseCurrentMethodName()
  python thmgr = TypeHierarchyContentManager(vim.current.buffer.name,method,param)
  python QuickLocater.runApp(thmgr)
endfunction

function StartMailAgent()
  python startMailAgent()
endfunction


call RunSzPyfile("common.py")
call RunSzPyfile("notext.py")
call RunSzPyfile("tree.py")

set completefunc=FuzzyCompletion
set tabline=%!MyTabLine()

function! SzJdeCompletion(findstart, base)
  python SzJdeCompletion.completion(vim.eval("a:findstart"),vim.eval("a:base"))
  if a:findstart
    return g:SzJdeCompletionIndex
  endif
    return g:SzJdeCompletionResult
endfunction

function FetchResult(...)
  python fetchCallBack(vim.eval("a:000"))
  redraw
endfunction

function FetchJdbResult()
  python jdb.fetchJdbResult()
  redraw
endfunction

function HandleJdiEvent(...)
  python jdb.handleJdiEvent(vim.eval("a:000"))
  "redraw
endfunction


function RunAntBuild(...)
  if a:0 > 0
    python Runner.runAntBuild(vim.eval("a:1"))
  else
    python Runner.runAntBuild()
  endif
endfunction 

function! Jdb()
  python Jdb.runApp()
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
  "au CursorHold <buffer> :python HighlightManager.displayMsg()
  "au CursorMoved <buffer> :python HighlightManager.displayMsg()
  python EditUtil.createSkeleton()
  if exists("*SuperTabSetDefaultCompletionType")
    call SuperTabSetDefaultCompletionType("<c-x><c-o>")
  endif
endfunction

function! Jdext()
  call RunSzPyfile("jde.py")
  set completeopt=menuone
  autocmd BufEnter     *.java     call InitJavaSetting()
  autocmd BufWinEnter   *.java    python HighlightManager.highlightCurrentBuf()
  "autocmd BufReadPost   *.java    python HighlightManager.highlightCurrentBuf()
  autocmd BufWritePost  *.java    python Compiler.compileCurrentFile()
  autocmd BufWritePost  *         python Compiler.copyResource()

  autocmd CursorHold *.java  :python HighlightManager.displayMsg()
  autocmd CursorMoved *.java :python HighlightManager.displayMsg()

  command! -nargs=0   BuildProject     :python Compiler.compileCurrentFile(buildProject=True)
  command! -nargs=0   DumpClass        :python EditUtil.dumpClassInfo()
  command! -nargs=0   AutoImport       :python AutoImport.autoImportVar()
  command! -nargs=0   Run              :python Runner.runCurrentFile()
  command! -nargs=0   RunTest          :python Runner.runCurrentFile("runTest")
  command! -nargs=0   Overide          :python EditUtil.overideMethod()
  command! -nargs=0   ProjectInit      :python ProjectManager.projectInit()
  command! -nargs=0   ProjectClean     :python ProjectManager.projectClean()
  command! -nargs=0   LoadJarMeta      :python ProjectManager.loadJarMeta()
  command! -nargs=?   Ant              :call RunAntBuild('<args>')

  command! -nargs=0   Jdb              :call Jdb()
  command! -nargs=0   ToggleBreakPoint :python EditUtil.toggleBreakpoint()
  command! -nargs=1   FetchDebugOutput :call FetchDebugOutput('<args>')
  command! -nargs=*   HandleJdiEvent    :call HandleJdiEvent(<f-args>)

  autocmd BufEnter  *.java    nmap <buffer><silent><leader>,   :python Runner.runCurrentFile()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><M-0>       :python VimUtil.closeSzToolBuffer("JdeConsole")<cr>
  autocmd BufEnter  *.java    vmap <buffer><silent><leader>gs  :python EditUtil.generateGseter()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>dc  :python EditUtil.dumpClassInfo()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>gd  :python EditUtil.locateDefinition("declare")<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>gi  :python EditUtil.locateDefinition("impl")<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>gh  :python EditUtil.searchRef()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>ai  :python AutoImport.autoImportVar()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>pt  :python ProjectManager.projectTree()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>go  :call LocateMember()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>gt  :call LocateHierarchy()<cr>
  autocmd BufEnter  *         nmap <buffer><silent><leader>gc  :call LocateClass()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><leader>tb  :python EditUtil.toggleBreakpoint()<cr>
  autocmd BufEnter  *.java    imap <buffer><silent><M-9>       <c-o>:python EditUtil.tipMethodParameter()<cr>
  autocmd BufEnter  *.java    imap <buffer><silent><M-0>       <c-o>:python VimUtil.closeSzToolBuffer("JdeConsole")<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><M-9>       :python EditUtil.tipMethodParameter()<cr>
  autocmd BufEnter  *.java    nmap <buffer><silent><M-0>       :python VimUtil.closeSzToolBuffer("JdeConsole")<cr>
  
  autocmd BufEnter  SzToolView_Jdb  nmap <buffer><silent><F5>     :python jdb.stepCmd('step_into')<cr>
  autocmd BufEnter  SzToolView_Jdb  nmap <buffer><silent><F6>     :python jdb.stepCmd('step_over')<cr>
  autocmd BufEnter  SzToolView_Jdb  nmap <buffer><silent><F7>     :python jdb.stepCmd('step_return')<cr>
  autocmd BufEnter  SzToolView_Jdb  nmap <buffer><silent><F8>     :python jdb.stepCmd('resume')<cr>
  autocmd BufEnter  SzToolView_Jdb  nmap <buffer><silent><c-[><c-[> :python jdb.toggleQuickStep()<cr>

  autocmd BufEnter  SzToolView_Jdb  imap <buffer><silent><F5>     <c-o>:python jdb.stepCmd('step_into')<cr>
  autocmd BufEnter  SzToolView_Jdb  imap <buffer><silent><F6>     <c-o>:python jdb.stepCmd('step_over')<cr>
  autocmd BufEnter  SzToolView_Jdb  imap <buffer><silent><F7>     <c-o>:python jdb.stepCmd('step_return')<cr>
  autocmd BufEnter  SzToolView_Jdb  imap <buffer><silent><F8>     <c-o>:python jdb.stepCmd('resume')<cr>
  "load project java info in background
  python ProjectManager.projectOpen()
endfunction

function! Jdesp()
  python MiscUtil.select_project_open()
  call Jdext()
  call ProjectTree()
endfunction


command! -nargs=1 Example       :call WatchExample('<args>')
command! -nargs=1 Dict          :call SearchDict('<args>')
command! -nargs=0 Recite        :call Recite()
command! -nargs=0 ClassicReader :call ClassicReader()
"command! -nargs=0 StartMailAgent :call StartMailAgent()

command! -nargs=0 CodeGen      :call CodeGen()
command! -nargs=0 LoadDtd      :call LoadDtd()

command! -nargs=0 SzSudoku    :call SzSudoku()
command! -nargs=0 SzMineSweeper  :call SzMineSweeper()

command! -nargs=1 -range=% Transform :<line1>,<line2>call Transform('<args>')

command! -nargs=0 StartAgent  :python SztoolAgent.startAgent()
command! -nargs=0 StopAgent   :python SztoolAgent.stopAgent()
command! -nargs=0 Shext       :call Shext()
command! -nargs=0 Jdext       :call Jdext()
"select project to open in jde.
command! -nargs=0 Jdesp       :call Jdesp()
command! -nargs=0 Dbext       :call Dbext()
command! -nargs=0 Notext      :call Notext()
command! -nargs=0 Tagext      :call Tagext()
command! -nargs=0 TagList      :call TagList()
command! -nargs=0 SaveNote            :python Notext.saveBufContent()
command! -nargs=0 MakeNoteTemplate    :python Notext.makeTemplate()
"command! -nargs=+ FetchResult      :call FetchResult(<f-args>)


command! -nargs=0 ProjectTree          :call ProjectTree()
command! -nargs=0 ProjectTreeFind      :python ProjectTree.locate_buf_in_tree()
command! -nargs=0 ProjectTreeDispose   :python ProjectTree.dispose_tree()

nmap <silent><leader>pt  :call ProjectTree()<cr>
nmap <silent><leader>pf  :python ProjectTree.locate_buf_in_tree()<cr>

"sztools mapping
nmap <silent><leader>zc  :python ScratchUtil.startScriptEdit()<cr>
nmap <silent><leader>zd  :call SearchDict('<C-R><C-W>')<CR>
vmap <silent><leader>zf  :python MiscUtil.simpleFormatSQL()<cr>
vmap <silent><leader>zm  :python MiscUtil.markVisual()<cr>
vmap <silent><leader>te  :python MiscUtil.tabulate()<cr>
nmap <silent><leader>rc  :python MiscUtil.remove_comment()<cr>
nmap <silent><leader>ya  :python MiscUtil.copy_buffer_path()<cr>

nmap <silent><leader>zs  :python MiscUtil.startfile()<cr>
nmap <silent><leader>zv  <C-Q>
nmap <silent><leader>zw  :w<cr>

nmap <silent><leader>zt  :call Tagext()<cr>
nmap <silent><leader>zl  :call TagList()<cr>
nmap <silent><leader>lw  :call LocateFile()<cr>
nmap <silent><leader>la  :call LocateHistory()<cr>

function SetProjectTreeFileEditFlag(filename,flag)
  python ProjectTree.set_file_edit(vim.eval("a:filename"),vim.eval("a:flag"))
endfunction

autocmd BufReadCmd  jar://*	python ZipUtil.read_zip_cmd()
autocmd BufReadPost *  call SetProjectTreeFileEditFlag(expand("<amatch>"),"true")
autocmd BufUnload   *  call SetProjectTreeFileEditFlag(expand("<amatch>"),"false")

function RemoveFromHistory(filename)
  python edit_history.remove_from_history(vim.eval("a:filename"))
endfunction

autocmd BufEnter           *  python edit_history.record_current_buf()
autocmd VimEnter,WinEnter  *  python edit_history.create_win_id()
autocmd BufUnload          *  call RemoveFromHistory(expand("<amatch>"))

command! -nargs=1 Silent
\ | execute ':silent !'.<q-args>
\ | execute ':redraw!'

python SztoolAgent.startAgent()
