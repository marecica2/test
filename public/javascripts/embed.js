var star = {};
star.embedInit = function(){
    star.container = $("#widgr-embed-container");
    star.container.load(function() {
        star.baseUrl = star.container.contents().find("#data-bs").val(); 
        star.server_host = star.container.contents().find("#data-ws").val();
        star.chatRoom = star.container.contents().find("#data-room").val(); 
        star.ownerName = star.container.contents().find("#data-usr").val();
        star.ownerAvatar = star.container.contents().find("#data-usr-ava").val();
        star.user = "User"+ Math.floor(Math.random()*900) ;
        star.avatar = 'public/images/avatar';
        star.isOwner = false;
        
        if(star.container.contents().find("#data-lg-user") != undefined){
            star.user = star.container.contents().find("#data-lg-user").val();
            star.avatar = star.container.contents().find("#data-lg-user-ava").val();
            star.lg = star.container.contents().find("#data-lg-user-lg").val();
        }
    
        $('head').append('<link href="//fonts.googleapis.com/css?family=Open+Sans:400italic,700italic,400,700,300&amp;subset=latin,latin-ext" rel="stylesheet" type="text/css">');
        $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'public/fonts/font-awesome/css/font-awesome.css">');
        $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'public/stylesheets/embed.css">');
        $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'public/css/skins/purple.css">');


        $.getScript(star.server_host+"/socket.io/socket.io.js", function() {
            $.getScript(star.baseUrl+"public/javascripts/utils.js", function(){
                $.getScript(star.baseUrl+"public/javascripts/room.js", function(){
                    // add chat box
                    $('body').append(star.chatContent(star));
    
                    $.getScript(star.baseUrl+"public/style-switcher/style-switcher.js", function(){
                        
                        // chat open event handler
                        var elm = star.container.contents().find("#widgr-open-chat");
                        elm.click(function(){
                            $(".style-switcher-container .trigger").click();
                        });
                        
                        // resize iframe
                        star.container.height(star.container.contents().height());
                        $(window).resize(function() {
                            if(this.resizeTO) clearTimeout(this.resizeTO);
                            this.resizeTO = setTimeout(function() {
                                star.container.height(star.container.contents().find("#embed-item").height());
                            }, 200);
                        });                    
                    });    
                });    
            });    
        });
    });
};

star.loadScript = function(path, clbck){
    var script = document.createElement("SCRIPT");
    script.src = path;
    script.type = 'text/javascript';
    document.getElementsByTagName("head")[0].appendChild(script);
    var checkReady = function(callback) {
        if (window.jQuery) {
            callback(jQuery);
        }
        else {
            window.setTimeout(function() { checkReady(callback); }, 100);
        }
    };
    checkReady(function($) {
        clbck();
    });    
}
star.loadScript("https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js", star.embedInit);


star.chatContent = function(star){
    var html = '';
    html += '<div class="style-switcher shadow closed shadow-small style-switcher-container" style="display:none; font-family:\'Open Sans\', sans-serif;">';
    html += '<div class="header" style="background: #954db3">';
    html += '<a class="trigger btn-default" href="#chat"><i class="fa fa-comments-o"></i></a>';
    html += '<span style="position:relative;top:10px;left:10px; color:white; font-size:17px;"><img class="img-circle" style="height:16px" src="'+star.ownerAvatar+'_32x32"> '+star.ownerName+'</span>';
    html += '</div>';
    html += '<div style="background: #fafafa;padding:5px;font-size:0.9em; color:black" class="style-switcher-container-2">';
    html += '<div id="content2" class="" style="height:385px; overflow-y: auto; overflow-x:hidden; text-align:left; color:black"></div>';
    html += '<div class="input-group">';
    html += '<input id="chat-text2" class="form-control" placeholder="Your message">';
    html += '<input id="chat-name2" type="hidden" value="${name}">'; 
    html += '<span class="input-group-btn">';
    html += '<button id="chat-send2" class="btn btn-default btn-short" style="margin:0px;height:40px"><i class="fa fa-share"></i></button>';
    html += '</span>';
    html += '</div>';
    html += '<div style="clear:both;text-align:center;padding:5px;"><small style="line-height:75px;"><i>Powered by </i> <img style="vertical-align: middle;height: 30px" src="/public/images/logo_purple.png"> @2015</small></div>';
    html += '</div>';
    html += '</div>';
    return html;
};
