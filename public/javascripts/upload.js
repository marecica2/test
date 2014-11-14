//fileupload for event edit
$(document).ready(function(){
    $("#upload-button").click(function(){
        $("#upload").click();
    });
    $("#upload").change(function(){
        // on edit page set ads uuid 
        var params = "";
        
        var params = "";
        if(starInit.event != undefined){
            params = "?item="+starInit.event;
        } else {
            params = "?temp="+starInit.temp;
        }
        params += "&avatar=true";
        
        star.utils.uploadFiles('/fileupload'+params, this.files, function(json){
            var resp = JSON.parse(json);
            console.log(resp);
            $("#crop-button").show();
            $("#upload-button").hide();
            $("#image").attr("style", "");
            $("#image").attr("src", "../public/uploads/"+resp.url);
            $("#imageUrl").val("public/uploads/"+resp.url+"");
            $("#imageId").val(resp.uuid);
            jQuery(function($) {
                $('#image').Jcrop({aspectRatio:starInit.aspectRatio, setSelect: [ 0, 0, 100, 100 ], bgColor: 'black',  onChange: showCoords});
            });
        });
    });
    $("#crop-button").click(function(){
        $("#cropForm").submit();
    });
    
    function showCoords(c){
       // variables can be accessed here as
       //c.x, c.y, c.x2, c.y2, c.w, c.h
       $("#x1").val(c.x);
       $("#x2").val(c.x2);
       $("#y1").val(c.y);
       $("#y2").val(c.y2);
       $("#imageIdCrop").val($("#imageId").val());
    };
});