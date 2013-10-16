syn match SzjdeCurrentFrame "current frame"
syn match SzjdeCurrentThread "current thread"
"syn match SzjdeVarName "^\s*[_$a-zA-Z0-9][^:]*\s*\(:\)\@="

highlight link SzjdeCurrentFrame Title
highlight link SzjdeCurrentThread Title
"highlight link SzjdeVarName Statement

syn match treeClosable #\~\<#
syn match treeClosable #\~\[#he=e-1
syn match treeClosable #\~\.#
syn match treeOpenable #+\<#
syn match treeOpenable #+\[#he=e-1
syn match treeOpenable #+\.#he=e-1

"highlighting for the tree structural parts
syn match treePart #|#
syn match treePart #`#
syn match treePartFile #[|`]-#hs=s+1 contains=treePart

hi def link treePart Special
hi def link treePartFile Type
hi def link treeFile Normal

hi def link treeOpenable Title
hi def link treeClosable Type

