(function(d, s, id) {
    var js, fjs = d.getElementsByTagName(s)[0];
    if (d.getElementById(id)) return;
    js = d.createElement(s); js.id = id;
    js.src = "https://connect.facebook.net/"+navigator.language+"/sdk.js#xfbml=1&appId=731388346951866&version=v2.0";
    fjs.parentNode.insertBefore(js, fjs);
  }(document, 'script', 'facebook-jssdk'));

window.fbAsyncInit = function() {
    if($(".page-wrapper")[0] != undefined){
        $(document).ready(function(){
            FB.Canvas.setSize({ width: 640, height: 700});
        });
    }
    
    FB.Event.subscribe('auth.authResponseChange', function(response) {
        if (response.status === 'connected') {
           star.fbClbck(response);
        } else if (response.status === 'not_authorized') {
        } else {
        }
    });
};
  
$(document).ajaxStop(function () {
    if(typeof FB !== "undefined"){
        if($(".page-wrapper")[0] != undefined){
            FB.Canvas.setSize({ width: 640, height: 700});
        }
    }
});      
