<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <title>GetchaNotes</title>
    <link rel="stylesheet" href="style.css">
    <script src="script.js"></script>
  </head>
  <body>
    <div id="content">
      <h1>GetchaNotes</h1>
      <p>Download your notes here: <a href= <%= request.getAttribute("url") %>>
          <%= request.getAttribute("fileName") %> </a></p>
    </div>
  </body>
</html>
