var star = {};
star.eventMethod = window.addEventListener ? "addEventListener" : "attachEvent";
star.messageEvent = star.eventMethod == "attachEvent" ? "onmessage" : "message";
eventer = window[star.eventMethod];

// Listen to message from child window
eventer(star.messageEvent,function(e) {
    var key = e.message ? "message" : "data";
    var params = e.data;
    params = JSON.parse(params)

    // init params
    if(params.type == "init"){
        star.baseUrl = params["data-bs"];
        star.server_host = params["data-ws"]; 
        star.chatRoom = params["data-room"]; 
        star.ownerName = params["data-usr"]; 
        star.ownerAvatar = params["data-usr-ava"]; 
        star.user = "Guest"+ Math.floor(Math.random()*900) ;
        star.avatar = 'public/images/avatar';
        star.isOwner = false;
        
        if(params["data-lg-user"] != undefined){
            star.userUuid = params["data-lg-user-ui"]; 
            star.user = params["data-lg-user"]; 
            star.userAvatar = params["data-lg-user-ava"]; 
            star.lg = params["data-lg-user-lg"];
        } else {
            star.userAvatar = "public/images/avatar";
            star.userUuid = (new Date()).getTime()+"";
        }    
    }
    
    if(params.type == "chat-open"){
        star.chatOpen();
    }

    if(params.type == "resize"){
        star.resize(params);
    }
    
},false);


star.embedInit = function(){
    star.container = $("#widgr-embedded-container");
    star.container.load(function() {

        star.container[0].contentWindow.postMessage("widgr-frame-ready", '*');
        // Create IE + others compatible event handler

        // reload for refresh
        if($(".style-switcher-container")[0] != undefined && star.container.contents()[0].location.href.indexOf("embed/listing") != -1){
            console.log("reload");
            window.location.reload();
        }

        console.log($.fn.jquery);
        
        $('head').append('<link href="https://fonts.googleapis.com/css?family=Open+Sans:400italic,700italic,400,700,300&amp;subset=latin,latin-ext" rel="stylesheet" type="text/css">');
        $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/fonts/embedded.css">');
        $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/stylesheets/embed.css">');
        $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/css/skins/purple.css">');


        $.getScript(star.server_host+"/socket.io/socket.io.js", function() {
            $.getScript(star.baseUrl+"/public/javascripts/utils.js", function(){
                $.getScript(star.baseUrl+"/public/javascripts/room.js", function(){
                    // add chat box if not exist
                    if($(".style-switcher-container")[0] == undefined)
                        $('body').append(star.chatContent(star));

                    $.getScript(star.baseUrl+"/public/style-switcher/style-switcher.js", function(){
                    });    
                });    
            });    
        });
    });
};

star.chatOpen = function(){
    $(".style-switcher-container .trigger").click();
};

star.resize = function(data){
    star.container.height(data.height);
    $(window).resize(function() {
        if(this.resizeTO) 
            clearTimeout(this.resizeTO);
        this.resizeTO = setTimeout(function(){
            star.container.height(data.height);
        }, 200);
    });    
}

star.chatContent = function(star){
    var html = '';
    html += '<div class="style-switcher shadow closed shadow-small style-switcher-container" style="display:none; font-family:\'Open Sans\', sans-serif;">';
    html += '<div class="header" style="background: #954db3">';
    html += '<a class="trigger btn-default" href="#chat" style="height:25px"><i class="fa fa-comments-o" style="position:relative;top:-2px"></i></a>';
    html += '<span style="position:relative;top:10px;left:10px; color:white; font-size:17px;"><img class="img-circle" style="height:16px" src="'+star.baseUrl+"/"+star.ownerAvatar+'_32x32"> '+star.ownerName+'</span>';
    html += '</div>';
    html += '<div style="padding:5px;" class="style-switcher-container-2">';
    html += '<div id="content2" class="" style="height:385px; margin-bottom:5px; overflow-y: auto; overflow-x:hidden; text-align:left; color:black"></div>';
    html += '<div class="">';
    html += '<input id="chat-text2" class="form-control" style="width:78%;height:30px" placeholder="Your message">';
    html += '<button id="chat-send2" class="btn btn-default btn-short" style="width:14%;height:44px;margin-top:6px;"><i class="fa fa-share fa-flip-horizontal"></i></button>';
    html += '</div>';
    
    html += '<div style="text-align:center;padding:10px;font-size:12px">';
    html += '<a target="_blank" class="black-link" href="'+star.baseUrl+'"><img style="vertical-align: middle;height: 30px" src="'+star.baseUrl+'/public/images/logo_purple.png"></a>';
    html += '</div>';
    html += '</div>';
    return html;
};

star.i18n = function(code) {
    var locale = navigator.language;
    var message = i18nMessages && i18nMessages[code] || code;
    // Encode %% to handle it later
    message = message.replace(/%%/g, "\0%\0");
    if (arguments.length > 1) {
        // Explicit ordered parameters
        for (var i=1; i<arguments.length; i++) {
            var r = new RegExp("%" + i + "\\$\\w", "g");
            message = message.replace(r, arguments[i]);
        }
        // Standard ordered parameters
        for (var i=1; i<arguments.length; i++) {
            message = message.replace(/%\w/, arguments[i]);
        }
    }
    // Decode encoded %% to single %
    message = message.replace("\0%\0", "%");
    // Imbricated messages
    var imbricated = message.match(/&\{.*?\}/g);
    if (imbricated) {
        for (var i=0; i<imbricated.length; i++) {
            var imbricated_code = imbricated[i].substring(2, imbricated[i].length-1).replace(/^\s*(.*?)\s*$/, "$1");
            message = message.replace(imbricated[i], i18nMessages[imbricated_code] || "");
        }
    }
    return message;
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