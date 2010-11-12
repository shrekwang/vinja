<html>
  <body>
    
    <!-- list -->
    <#assign colors = ["red", "green", "blue"]>
    ${colors?seq_index_of("blue")}
    ${colors?seq_index_of("red")}
    ${colors?seq_index_of("purple")}  

    <!-- string -->
    ${'abc'?substring(0)}
    ${'abc'?substring(0,2)}

    ${'abc'?ends_with('a')}

  </body>

</html>

