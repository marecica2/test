var star = {};
star.loaded = false;
star.visible = true;
star.eventMethod = window.addEventListener ? "addEventListener" : "attachEvent";
star.messageEvent = star.eventMethod == "attachEvent" ? "onmessage" : "message";
eventer = window[star.eventMethod];

eventer(star.messageEvent,function(e) {
    var key = e.message ? "message" : "data";
    var params = e.data;
    params = JSON.parse(params)

    // init params
    if(params.type == "init"){
        star.baseUrl = params["data-bs"];
        star.server_host = params["data-ws"]; 
        star.chatRoom = params["data-room"]; 

        star.listingTitle = params["data-listingTitle"]; 
        star.listingUuid = params["data-listingUuid"]; 
        star.listing = star.listingUuid;
        star.listingImage = params["data-listingImage"]; 
        star.listingCharging = params["data-listingCharging"]; 
        star.listingPrice = params["data-listingPrice"]; 
        star.listingDuration = params["data-listingDuration"]; 
        star.listingCurrency = params["data-listingCurrency"]; 
        star.listingFirstFree = params["data-listingFirstFree"];
        star.listingStars = params["data-listingStars"];
        star.listingReviews = params["data-listingReviews"];
        
        star.ownerName = params["data-owner"]; 
        star.ownerAvatar = params["data-owner-avatar"]; 
        star.ownerUuid = params["data-owner-uuid"]; 
        star.ownerCompany = params["data-owner-company"]; 
        star.isOwner = false;
        star.logged = false;
        star.userAvatar = "public/images/avatar";
        star.userName = "Guest"+ Math.floor(Math.random()*900);
        if(params["data-lg-user"] != undefined){
            star.logged = true;
            star.userUuid = params["data-lg-user-ui"]; 
            star.userName = params["data-lg-user"]; 
            star.userAvatar = params["data-lg-user-ava"]; 
            star.userLogin = params["data-lg-user-lg"];
        }
        star.embedInit();  
    }
    if(params.type == "chat-open"){
        star.chatOpen();
    }
    if(params.type == "resize"){
        star.resize(params);
    }
    if(params.type == "reload"){
        document.location.reload();
    }
},false);


star.embedInit = function(){
    star.container = $(".widgr-embed-container");
    if(star.loaded)
        return;
    
    star.container[0].contentWindow.postMessage("widgr-frame-ready", '*');
    $('head').append('<link href="https://fonts.googleapis.com/css?family=Open+Sans:400italic,700italic,400,700,300&amp;subset=latin,latin-ext" rel="stylesheet" type="text/css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/fonts/embedded.css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/stylesheets/embed.css">');
    $('head').append('<link rel="stylesheet" type="text/css" href="'+star.baseUrl+'/public/stylesheets/embed-red.css">');

    var s = star.server_host+"/socket.io/socket.io.js";
    $.getScript(s, function() {
        $.getScript(star.baseUrl+"/public/javascripts/utils.js", function(){
            star.loaded = true;
            
            // add chat box if not exist
            if($(".style-switcher-container")[0] == undefined){
                $('body').append(star.utils.chatContent(star));
                
                var widgr_int2 = setInterval(function(){
                        $("#widgr-container").show();
                        clearInterval(widgr_int2);
                    }, 1000);                    
            }
  
            // read user name from cookie
            var name = star.utils.getCookie("widgr-name");
            star.userUuid = star.utils.uuid();
            if(!star.logged && name != ""){
                star.userName = name;
                star.userUuid = star.utils.getCookie("widgr-user-uuid");
                $(".widgr-custom-name").val(star.userName);
                $(".widgr-custom-name").hide();
                $(".widgr-chat-input").show();
                $(".widgr-startchat-btn").hide();
            }
            

            $.getScript(star.baseUrl+"/public/javascripts/room.js", function(){
                star.utils.chatEvents();
            });
        
        });    
    });
}

star.chatOpen = function(){
    $(".style-switcher-trigger").click();
};

star.resize = function(data){
    if(data != undefined){
        $(data.selector).height(data.height);
    }
}


i18n = function(code) {
    var locale = navigator.language;
    locale = locale.substring(0, 2);
    var message = star.i18nMessages && star.i18nMessages[locale][code] || code;
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

star.i18nMessages = {};
star.i18nMessages.en = {
        "submit":"Submit", 
        "available":"Available", 
        "your-email":"Your email", 
        "subject":"Subject", 
        "message":"Write your message", 
        "your-name":"Enter your name", 
        "chat-with":"Chat with",
        "click-to-open-chat":"Open chat",
        "join-vide-conference":"Join video call",
        "start-chat":"Start chat",
        "not-logged":"You are not logged in. To use some features you would need an account. Just <a href='#' class='widgr-iframe-btn'>sign in or register.</a> Learn more <a href='https://wid.gr' target='_blank'>about Widgr.</a>",
        "not-available-now":"is not online now, you can leave him message and he will reply you later.",
        "sign-in":"Sign in or register. Fastest with Facebook",
        "learn-more":"or learn <a href='' target='_blank'>more about Widgr.</a>",
        "we-are-online":"is available"
};
star.i18nMessages.de = {
        "submit":"Submit", 
        "available":"Available", 
        "your-email":"Your email", 
        "subject":"Subject", 
        "message":"Write your message",         
        "message":"Schreiben Sie eine Nachricht", 
        "your-name":"Eingeben Sie Ihre Name", 
        "chat-with":"Chat mit",
        "click-to-open-chat":"Chat offnen",
        "join-vide-conference":"Join Videoanruf",
        "start-chat":"Start chat",
        "not-logged":"You are not logged in. To use some features you would need an account. Just <a href='#' class='widgr-iframe-btn'>sign in or register. </a> Learn more <a href='https://wid.gr' target='_blank'>about Widgr.</a>",
        "not-available-now":"is not online now, you can leave him message and he will reply you later.",
        "sign-in":"Sign in or register. Fastest with Facebook",
        "we-are-online":"ist online"
};

star.loadScript = function(path){
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
}
star.loadScript("https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js");