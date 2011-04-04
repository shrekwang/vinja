highlight def SuspendLine  ctermbg=Green ctermfg=Black  guibg=#A4E57E guifg=Black
highlight def BreakPoint   ctermbg=Blue     ctermfg=Black  guibg=#9999FF    guifg=Black

highlight link SzjdeError SpellBad
highlight link SzjdeWarning SpellLocal

sign define SzjdeBreakPoint text=O texthl=BreakPoint
sign define SzjdeError text=>> texthl=ErrorMsg
sign define SzjdeWarning text=>> texthl=TODO
sign define SuspendLine linehl=SuspendLine
sign define SuspendLineBP text=O texthl=BreakPoint linehl=SuspendLine
sign define SzjdeFR text=R9 texthl=BreakPoint
