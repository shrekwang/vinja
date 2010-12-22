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

function SetSzToolBuf()
    exec "setlocal nowrap"    
    exec "setlocal buftype=nofile" 
    exec "setlocal noswapfile"
    exec "setlocal bufhidden=wipe"
    exec "setlocal nobuflisted"
endfunction

function! SwitchToSzToolView(viewname)    
  let s:cur_buf = bufnr("%")    
  let s:szdb_result_buf=bufnr("SzToolView_" . a:viewname)    
  if bufwinnr(s:szdb_result_buf) > 0    
    exec bufwinnr(s:szdb_result_buf) . "wincmd w"    
    "%d    
  else    
    exec 'silent! belowright split SzToolView_' . a:viewname    
    exec "e SzToolView_" . a:viewname    
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
  setlocal ignorecase
  call SetTabPageName("Dbext")
  python Dbext.runApp()
  map <buffer><silent>,,  :python dbext.queryVisualSQL()<cr>
  map <buffer><silent>,go :python dbext.promptDbOption()<cr>
  map <buffer><silent>,gc :python dbext.promptTempOption()<cr>
  map <buffer><silent>,lt :python QueryUtil.queryTables()<cr>
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
  python transform(vim.eval("a:exp"), vim.eval("a:method"))
	return g:sztransform_result
endfunction

function Transform(method)
  %s//\=CustomSub(submatch(0),a:method)/gc
endfunction

function SearchDict(word)
  call RunSzPyfile("pystardict.py")
  python searchDict(vim.eval("a:word"))
  python playWordSound(vim.eval("a:word"))
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

function PlayDict(word)
  python playWordSound(vim.eval("a:word"))
endfunction

function Ledit(name)
  call RunSzPyfile("shext.py")
  python Shext.ledit(vim.eval("a:name"))
endfunction

function WatchExample(name)
  python watchExample(vim.eval("a:name"))
endfunction

function LocateFile()
  call RunSzPyfile("locate.py")
  python fcmgr = FileContentManager()
  python QuickLocater.runApp(fcmgr)
endfunction

function LocateMember()
  call RunSzPyfile("locate.py")
  python membermgr = JavaMemberContentManager()
  python QuickLocater.runApp(membermgr)
endfunction

function StartMailAgent()
  python startMailAgent()
endfunction


call RunSzPyfile("common.py")
call RunSzPyfile("notext.py")

set completefunc=FuzzyCompletion
set tabline=%!MyTabLine()

function! SzJdeCompletion(findstart, base)
  python SzJdeCompletion.completion(vim.eval("a:findstart"),vim.eval("a:base"))
  if a:findstart
    return g:SzJdeCompletionIndex
  endif
    return g:SzJdeCompletionResult
endfunction

function FetchResult(guid)
  python Runner.fetchResult(vim.eval("a:guid"))
endfunction

function RunAntBuild(...)
  if a:0 > 0
    python Runner.runAntBuild(vim.eval("a:1"))
  else
    python Runner.runAntBuild()
  endif
endfunction 

function JdeHotSwapEnable(port)
  python Talker.hotswapEnabled("true",vim.eval("a:port"))
endfunction 

function JdeHotSwapDisable()
  python Talker.hotswapEnabled("false")
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

function! Jdext()
  call RunSzPyfile("jde.py")
  python startAgent()
  set completeopt=menuone
  autocmd BufEnter     *.java      setlocal omnifunc=SzJdeCompletion
  autocmd BufNewFile   *.java      python EditUtil.createSkeleton()
  autocmd BufEnter     *.java      au CursorHold <buffer> :python Compiler.displayMsg()
  autocmd BufEnter     *.java      au CursorMoved <buffer> :python Compiler.displayMsg()
  if exists("*SuperTabSetDefaultCompletionType")
    autocmd BufEnter *.java        call SuperTabSetDefaultCompletionType("<c-x><c-o>")
  endif
  autocmd BufWritePost  *.java    python Compiler.compileCurrentFile()
  autocmd BufWritePost  *         python Compiler.copyResource()
  map <C-n> :cn<cr>
  map <C-p> :cp<cr>

  command! -nargs=0   DumpClass    :python EditUtil.dumpClassInfo()
  command! -nargs=0   AutoImport   :python AutoImport.autoImportVar()
  command! -nargs=0   Run          :python Runner.runCurrentFile()
  command! -nargs=0   Overide      :python EditUtil.overideMethod()
  command! -nargs=0   ProjectInit  :python ProjectManager.projectInit()
  command! -nargs=0   ProjectClean :python ProjectManager.projectClean()
  command! -nargs=?   Ant          :call RunAntBuild('<args>')
  command! -nargs=1   FetchResult  :call FetchResult('<args>')
  command! -nargs=0   StopHotswap  :call JdeHotSwapDisable()
  command! -nargs=1   StartHotswap  :call JdeHotSwapEnable('<args>')

  autocmd BufEnter     *.java      nmap <silent><leader>,   :python Runner.runCurrentFile()<cr>
  autocmd BufEnter     *.java      vmap <silent><leader>gg  :python EditUtil.generateGseter()<cr>
  autocmd BufEnter     *.java      nmap <silent><leader>dc  :python EditUtil.dumpClassInfo()<cr>
  autocmd BufEnter     *.java      nmap <silent><leader>gd  :python EditUtil.locateDefinition()<cr>
  autocmd BufEnter     *.java      nmap <silent><leader>ai  :python AutoImport.autoImportVar()<cr>
  
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

command! -nargs=1 Transform    :call Transform('<args>')

command! -nargs=0 StartAgent  :python startAgent()
command! -nargs=0 Shext       :call Shext()
command! -nargs=0 Jdext       :call Jdext()
command! -nargs=0 Dbext       :call Dbext()
command! -nargs=0 Notext      :call Notext()
command! -nargs=0 SaveNote            :python Notext.saveBufContent()
command! -nargs=0 MakeNoteTemplate    :python Notext.makeTemplate()

"sztools mapping
nmap <silent><leader>zc  :python startScriptEdit()<cr>
nmap <silent><leader>zd  :call SearchDict('<C-R><C-W>')<CR>
vmap <silent><leader>zf  :python simpleFormatSQL()<cr>
vmap <silent><leader>zm  :python markVisual()<cr>

nmap <silent><leader>zs  :python startfile()<cr>
nmap <silent><leader>zv  <C-Q>
nmap <silent><leader>zw  :w<cr>

nmap <silent><leader>ff  :python openInFirefox()<cr>
nmap <silent><leader>lw  :call LocateFile()<cr>
nmap <silent><leader>zo  :call LocateMember()<cr>
