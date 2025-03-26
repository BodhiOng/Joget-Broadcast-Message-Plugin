<html>
    <head>
        <script type="text/javascript" src="${request.contextPath}/wro/common.js"></script>
        <script type="text/javascript" src="${request.contextPath}/js/jquery/jquery-3.5.1.min.js"></script>
        <script type="text/javascript" src="${request.contextPath}/js/json/util.js"></script>
        <script>
            $(function(){
                alert("${savedUrl!}");
                
                var callback = {
                    success: function(){
                        window.location = '${plugin.properties.redirect!}';
                    }
                };

                AssignmentManager.login("${request.contextPath}", "admin", "admin", callback);
            });
        </script>
    </head>
    <body>
        Please wait...
    </body>
</html>
