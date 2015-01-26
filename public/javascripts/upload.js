//fileupload for event edit
$(document).ready(function(){
    $("#upload-button").click(function(){
        $("#upload").click();
    });
    $("#upload").change(function(){
        var params = "";
        if(starCalendar.event != undefined){
            params = "item="+starCalendar.event;
        } else {
            params = "temp="+starCalendar.temp;
        }
        params += "&avatar=true";
        
        star.utils.uploadFiles('/fileupload?'+star.token+'&'+params, this.files, function(json){
            var resp = JSON.parse(json);
            console.log(resp);
            $(".avatar-container").addClass("eg-wrapper");
            $(".eg-preview").show();
            $("#crop-button").show();
            $("#upload-button").hide();
            $("#image").attr("style", "");
            $("#image").attr("src", "../public/uploads/"+resp.url);
            $("#imageUrl").val("public/uploads/"+resp.url+"");
            $("#imageId").val(resp.uuid);

            $(".avatar-container > img").cropper({
                aspectRatio : starCalendar.aspectRatio,
                preview: ".avatar-preview",
                done : function(data) {
                    $("#x1").val(Math.round(data.x));
                    $("#y1").val(Math.round(data.y));
                    $("#x2").val(Math.round(data.x + data.width));
                    $("#y2").val(Math.round(data.y + data.height));
                    $("#imageIdCrop").val($("#imageId").val());
                }
            });
        });
    });
    $("#crop-button").click(function(){
        $("#cropForm").submit();
    });
    
});