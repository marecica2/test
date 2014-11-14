filepicker.setKey("AvWSXuNKTTKH2hr4j9Lrwz");
$("#chat-upload").click(function(){
    filepicker.pick(function(InkBlob){
        var data = {};
        data.event = star.room;
        data.comment = "[url]"+InkBlob.url+"[/url][name]"+InkBlob.filename+"[/name]";
        roomServices.saveFeed(data, function(succ){
            console.log(succ);
            send_chat(succ.comment, $("#chat-name").val());
        });        
    });
});
