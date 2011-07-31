call NERDTreeAddKeyMap({
       \ 'key': 'yy',
       \ 'callback': 'NERDTreeYankNode',
       \ 'quickhelpText': 'yank current node to buffer' })

call NERDTreeAddKeyMap({
       \ 'key': 'ya',
       \ 'callback': 'NERDTreeYankPath',
       \ 'quickhelpText': 'yank current node path to default register' })

call NERDTreeAddKeyMap({
       \ 'key': 'dd',
       \ 'callback': 'NERDTreeCutNode',
       \ 'quickhelpText': 'cut current node to buffer' })

call NERDTreeAddKeyMap({
       \ 'key': 'p',
       \ 'callback': 'NERDTreePasteToNode',
       \ 'quickhelpText': 'paste buffer to current node' })

call NERDTreeAddKeyMap({
       \ 'key': 'DD',
       \ 'callback': 'NERDTreeRmNode',
       \ 'quickhelpText': 'remove current node recursively' })

let g:SzToolNodeBuf = ""
let g:SzToolOpType = ""
let g:SzToolParentOfRmNode = {}


function! NERDTreeYankNode()
  call NodeToBuf("yank")
endfunction

function! NERDTreeCutNode()
  call NodeToBuf("cut")
endfunction

function! NERDTreeYankPath()
    let curNode = g:NERDTreeFileNode.GetSelected()
    if curNode != {}
        echomsg 'node: ' . curNode.path.str() . " path yanked to @0. "
        let @" = curNode.path.str()
    endif
endfunction

function! NERDTreeRmNode()
    let curNode = g:NERDTreeFileNode.GetSelected()
    let parent = curNode.parent
    let curPath = curNode.path.str()
    python FileUtil.fileOrDirRm(vim.eval("curPath"))
    call parent.refresh()
    call NERDTreeRender()
endfunction

function! NERDTreePasteToNode()
    let curNode = g:NERDTreeFileNode.GetSelected()
    if curNode == {}
      return
    endif 
    let curPath = curNode.path.str()
    if g:SzToolNodeBuf != ""
      if g:SzToolOpType == "yank" 
        python FileUtil.fileOrDirCp(vim.eval("g:SzToolNodeBuf"),vim.eval("curPath"))
      else
        python FileUtil.fileOrDirMv(vim.eval("g:SzToolNodeBuf"),vim.eval("curPath"))
      endif
      echomsg 'node: ' . curNode.path.str() . " pasted. "
      let g:SzToolNodeBuf = ""
      call curNode.refresh()
      if g:SzToolParentOfRmNode != {}
        call g:SzToolParentOfRmNode.refresh()
      endif
      call NERDTreeRender()
    endif
endfunction


function! NodeToBuf(opType)
    let curNode = g:NERDTreeFileNode.GetSelected()
    if curNode != {}
        echomsg 'node: ' . curNode.path.str() . " yanked. "
        let g:SzToolNodeBuf = curNode.path.str()
        let g:SzToolOpType = a:opType
        let g:SzToolParentOfRmNode = curNode.parent
    endif
endfunction

