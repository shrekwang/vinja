"treeFlags are syntax items that should be invisible, but give clues as to
"how things should be highlighted
syn match treeFlag #\~#
syn match treeFlag #\[RO\]#
syn match treeFlag #\[edit\]#
syn match treeFlag #\[mark\]#

"highlighting for the ~/+ symbols for the directory nodes
syn match treeClosable #\~\<#
syn match treeClosable #\~\.#
syn match treeOpenable #+\<#
syn match treeOpenable #+\.#he=e-1

"highlighting for the tree structural parts
syn match treePart #|#
syn match treePart #`#
syn match treePartFile #[|`]-#hs=s+1 contains=treePart

"quickhelp syntax elements
syn match treeHelpKey #" \{1,2\}[^ ]*:#hs=s+2,he=e-1
syn match treeHelpKey #" \{1,2\}[^ ]*,#hs=s+2,he=e-1
syn match treeHelpTitle #" .*\~#hs=s+2,he=e-1 contains=treeFlag
syn match treeToggleOn #".*(on)#hs=e-2,he=e-1 contains=treeHelpKey
syn match treeToggleOff #".*(off)#hs=e-3,he=e-1 contains=treeHelpKey
syn match treeHelpCommand #" :.\{-}\>#hs=s+3

"highlighting for readonly files
syn match treeRO #.*\[RO\]#hs=s+2 contains=treeFlag,treeBookmark,treePart,treePartFile

"highlighting for file status
syn match treeEditedFile #.*\[edit\]#hs=s+2 contains=treeFlag,treeBookmark,treePart,treePartFile
syn match treeMarkedFile #.*\[mark\]#hs=s+2 contains=treeFlag,treeBookmark,treePart,treePartFile

"highlighting for sym links
syn match treeLink #[^-| `].* -> # contains=treeBookmark,treeOpenable,treeClosable,treeDirSlash

"highlighing for directory nodes and file nodes
syn match treeDirSlash #/#
syn match treeDir #[^-| `].*/# contains=treeLink,treeDirSlash,treeOpenable,treeClosable
syn match treeExecFile  #[|`]-.*\*\($\| \)# contains=treeLink,treePart,treeRO,treePartFile,treeBookmark,treeEditedFile,treeMarkedFile
syn match treeFile  #|-.*# contains=treeLink,treePart,treeRO,treePartFile,treeBookmark,treeExecFile,treeEditedFile,treeMarkedFile
syn match treeFile  #`-.*# contains=treeLink,treePart,treeRO,treePartFile,treeBookmark,treeExecFile,treeEditedFile,treeMarkedFile
syn match treeCWD #^/.*$#

"highlighting for bookmarks
syn match treeBookmark # {.*}#hs=s+1

"highlighting for the bookmarks table
syn match treeBookmarksLeader #^>#
syn match treeBookmarksHeader #^>-\+Bookmarks-\+$# contains=treeBookmarksLeader
syn match treeBookmarkName #^>.\{-} #he=e-1 contains=treeBookmarksLeader
syn match treeBookmark #^>.*$# contains=treeBookmarksLeader,treeBookmarkName,treeBookmarksHeader

hi def link treePart Special
hi def link treePartFile Type
hi def link treeFile Normal
hi def link treeExecFile Title
hi def link treeDirSlash Identifier
hi def link treeClosable Type

hi def link treeHelp String
hi def link treeHelpKey Identifier
hi def link treeHelpCommand Identifier
hi def link treeHelpTitle Macro
hi def link treeToggleOn Question
hi def link treeToggleOff WarningMsg

hi def link treeDir Directory
hi def link treeUp Directory
hi def link treeCWD Statement
hi def link treeLink Macro
hi def link treeOpenable Title
hi def link treeFlag ignore
hi def link treeRO WarningMsg
hi def link treeBookmark Statement

hi def link treeEditedFile Label
hi def link treeMarkedFile Question


