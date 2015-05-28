
// 
//
// Canvas drawing
//
//
var canvas = {};
canvas.type = "pen";
canvas.canvas = null;
canvas.ctx; 
canvas.flag = false, 
canvas.prevX = 0; 
canvas.currX = 0; 
canvas.prevY = 0; 
canvas.currY = 0, 
canvas.dot_flag = false;
canvas.x = "rgba(255, 255, 255, 0.95)"; 
canvas.y = 2;
canvas.history = [];

canvas.init = function() {
    canvas.canvas = $(".canvas-paper")[0];
    canvas.ctx = canvas.canvas.getContext("2d");
    canvas.w = canvas.canvas.width;
    canvas.h = canvas.canvas.height;

    canvas.canvas.addEventListener("mousemove", function (e) {
        canvas.findxy('move', e);
    }, false);
    canvas.canvas.addEventListener("mousedown", function (e) {
        canvas.findxy('down', e);
    }, false);
    canvas.canvas.addEventListener("mouseup", function (e) {
        canvas.findxy('up', e);
        if(canvas.history.length >= 11)
            canvas.history.shift();
        canvas.history.push(canvas.canvas.toDataURL());
    }, false);
    canvas.canvas.addEventListener("mouseout", function (e) {
        canvas.findxy('out', e);
    }, false);

}

canvas.mode = function(style){
    if(style == 'pen'){
        canvas.type = "pen";
        $("#mode-eraser").removeClass("active");
    } else {
        canvas.type = "eraser";
        $("#mode-pen").removeClass("active");
        $("#mode-eraser").addClass("active");
    }
}

canvas.undo = function() {
    if(canvas.history.length > 0){
        var undo =  canvas.history.pop();
        var image = new Image;
        image.src = undo;
        canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
        canvas.ctx.drawImage(image,0, 0);
        
        var data = {};
        data.image = canvas.canvas.toDataURL();
        star.socket_message_broadcast("canvas-undo", data);
    }
}

canvas.undoMessage = function(data){
    var image = new Image;
        image.src = data.image;
        canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
        canvas.ctx.drawImage(image,0, 0);
}

canvas.draw = function() {
    canvas.ctx.beginPath();
    
    if(canvas.type == "pen"){
        canvas.ctx.globalCompositeOperation="source-over";
        canvas.ctx.moveTo(canvas.prevX, canvas.prevY);
        canvas.ctx.lineTo(canvas.currX, canvas.currY);
        canvas.ctx.lineCap="round";
        canvas.ctx.strokeStyle = canvas.x;
        canvas.ctx.lineWidth = canvas.y;
        canvas.ctx.stroke();
        canvas.ctx.closePath();
    } else {
        canvas.ctx.globalCompositeOperation="destination-out";
        canvas.ctx.arc(canvas.currX,canvas.currY,30,0,Math.PI*2,false);
        canvas.ctx.fill();
    }
    
    var data = {};
    data.type = canvas.type;
    data.prevX = canvas.prevX;
    data.prevY = canvas.prevY;
    data.currX = canvas.currX;
    data.currY = canvas.currY;
    star.socket_message_broadcast("canvas-draw", data);
}

canvas.drawMessage = function(data){
    if(data != undefined){
        canvas.ctx.beginPath();
        if(data.type == "pen"){
            canvas.ctx.globalCompositeOperation="source-over";
            canvas.ctx.moveTo(data.prevX, data.prevY);
            canvas.ctx.lineTo(data.currX, data.currY);
            canvas.ctx.lineCap="round";
            canvas.ctx.strokeStyle = canvas.x;
            canvas.ctx.lineWidth = canvas.y;
            canvas.ctx.stroke();
            canvas.ctx.closePath();
        } else {
            canvas.ctx.globalCompositeOperation="destination-out";
            canvas.ctx.arc(data.currX,data.currY,30,0,Math.PI*2,false);
            canvas.ctx.fill();            
        }
    }
}

canvas.resize = function(){
    $(canvas.canvas).attr({ width: canvas.w, height: canvas.h });
    var image = new Image;
    if(canvas.history != undefined && canvas.history.length > 0){
        image.src = canvas.history[canvas.history.length-1];
        canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
        canvas.ctx.drawImage(image,0, 0);
    }
    var data = {};
    data.width = canvas.w;
    data.height = canvas.h;
    data.image = canvas.canvas.toDataURL();
    star.socket_message_broadcast("canvas-resize", data);
}

canvas.resizeMessage = function(data){
    $(".canvas-container").show();
    
    canvas.w = data.width;
    canvas.h = data.height;
    $(canvas.canvas).height(data.height);
    $(canvas.canvas).width(data.width);
    $(canvas.canvas).attr({ width: data.width, height: data.height });
    $(".ui-wrapper").height(data.height);
    $(".ui-wrapper").width(data.width);
    
    //redraw image
    var image = new Image;
    image.src = data.image;
    canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
    canvas.ctx.drawImage(image,0, 0);
    
    // activate button
    star.drawing = true;
    $("#canvas-open").removeClass("btn-dark");
    $("#canvas-open").addClass("btn-success");
    $(".canvas-container").show();
}

canvas.erase = function() {
    canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
    var data = {};
    star.socket_message_broadcast("canvas-erase", data);
}

canvas.eraseMessage = function(){
    canvas.ctx.clearRect(0, 0, canvas.w, canvas.h);
}

canvas.save = function() {
    // draw patter on background
    var img = new Image();
    img.src = '../images/canvas_pattern_0.jpg';
    canvas.ctx.globalCompositeOperation="destination-over";
    var ptrn = canvas.ctx.createPattern(img,'repeat');
    canvas.ctx.fillStyle = ptrn;
    canvas.ctx.fillRect(0,0,canvas.w,canvas.h);

    var dataURL = canvas.canvas.toDataURL("image/png");

    // upload the image
    var blob = canvas.dataURItoBlob(dataURL);
    var name = "canvas-"+starUtils.s4()+".png";
    if($("#canvas-name").val().length > 0)
        name = $("#canvas-name").val()+".png";
    blob.name = name;
    blob.lastModifiedDate = new Date();
    blob.lastModified = blob.lastModifiedDate.getTime();
    var files = [];
    files.push(blob)
    var params = "";
    var temp = star.utils.uuid();
    params = "temp="+temp;
    star.utils.uploadFiles('/fileupload?'+star.token+'&'+params, files, function(json){
        var response = JSON.parse(json);
        
        // save comment
        var data = {};
        data.uuid = star.room;
        data.type = "file";
        data.objectType = "event";
        data.comment = "canvas.jpg";
        data.tempId = temp;
        
        canvas.erase();

        starServices.addComment(data, function(){
            var d = {};
            star.socket_message_broadcast("files-reload", d);
            star.loadComments(star.params, star.dashboard);
        });        
    });
}

canvas.filesReload = function(){
    star.loadComments(star.params, star.dashboard);
    canvas.commentsOpen();
}

canvas.dataURItoBlob = function(dataURI) {
    dataURI = dataURI.split(',');
    var type = dataURI[0].split(':')[1].split(';')[0],
        byteString = atob(dataURI[1]),
        byteStringLength = byteString.length,
        arrayBuffer = new ArrayBuffer(byteStringLength),
        intArray = new Uint8Array(arrayBuffer);
    for (var i = 0; i < byteStringLength; i++) {
        intArray[i] = byteString.charCodeAt(i);
    }
    return new Blob([intArray], {
        type: type
    });
}

canvas.findxy = function(res, e) {
    if(e.type == "touchmove"){
        canvas.prevX = canvas.currX;
        canvas.prevY = canvas.currY;
        canvas.currX = e.changedTouches[0].clientX - $(canvas.canvas).offset().left;
        canvas.currY = e.changedTouches[0].clientY - $(canvas.canvas).offset().top;
        if(isNaN(canvas.prevX)){
            canvas.prevX = canvas.currX;
            canvas.prevY = canvas.currY;
        }
    } else {
        canvas.prevX = canvas.currX;
        canvas.prevY = canvas.currY;
        canvas.currX = e.clientX - $(canvas.canvas).offset().left;
        canvas.currY = e.clientY - $(canvas.canvas).offset().top;
    }

    if (res == 'down') {
        canvas.flag = true;
        canvas.dot_flag = true;
        if (canvas.dot_flag) {
            canvas.ctx.beginPath();
            canvas.ctx.moveTo(canvas.currX-1, canvas.currY);
            canvas.ctx.lineTo(canvas.currX, canvas.currY);
            canvas.ctx.lineCap="round";
            canvas.ctx.strokeStyle = canvas.x;
            canvas.ctx.lineWidth = canvas.y;
            canvas.ctx.stroke();
            canvas.ctx.closePath();
            
            var data = {};
            data.type = canvas.type;
            data.prevX = canvas.currX-1;
            data.prevY = canvas.currY;
            data.currX = canvas.currX;
            data.currY = canvas.currY;
            star.socket_message_broadcast("canvas-draw", data);
        }
    }
    if (res == 'up' || res == "out") {
        canvas.flag = false;
    }
    if (res == 'move') {
        if (canvas.flag) {
            canvas.draw();
        }
    }
}

canvas.share = function(elm){
    var data = {};
    data.width = canvas.w;
    data.height = canvas.h;
    data.image = canvas.canvas.toDataURL();
    star.socket_message_broadcast("canvas-resize", data);
    $(elm).addClass("btn-primary");
}

canvas.close = function(){
    star.drawing = false;
    $(".canvas-container").hide();
    $("#canvas-open").addClass("btn-dark");
    $("#canvas-open").removeClass("btn-success");    
}

canvas.commentsClose = function(){
    star.commentsDialog = false;
    $(".comments-container").hide();
    $("#comments-open").addClass("btn-dark");
    $("#comments-open").removeClass("btn-success");    
}

canvas.commentsOpen = function(){
    star.commentsDialog = true;
    $(".comments-container").show();
    $("#comments-open").removeClass("btn-dark");
    $("#comments-open").addClass("btn-success");    
}

canvas.touchHandler = function(event) {
    var touch = event.changedTouches[0];
    
    var simulatedEvent = document.createEvent("MouseEvent");
        simulatedEvent.initMouseEvent({
        touchstart: "mousedown",
        touchmove: "mousemove",
        touchend: "mouseup"
    }[event.type], true, true, window, 1,
        touch.screenX, touch.screenY,
        touch.clientX, touch.clientY, false,
        false, false, false, 0, null);

    touch.target.dispatchEvent(simulatedEvent);
}

$(document).ready(function(){
    socket.on('socket_message', function(data) {
        if(data.event == "canvas-draw"){
            canvas.drawMessage(data.data);
        }     
        if(data.event == "canvas-undo"){
            canvas.undoMessage(data.data);
        }     
        if(data.event == "canvas-resize"){
            canvas.resizeMessage(data.data);
        }     
        if(data.event == "canvas-erase"){
            canvas.eraseMessage();
        }     
        if(data.event == "canvas-save"){
        }     
        if(data.event == "files-reload"){
            canvas.filesReload();
        }     
     });
    
    document.addEventListener("touchstart", canvas.touchHandler, true);
    document.addEventListener("touchmove", canvas.touchHandler, true);
    document.addEventListener("touchend", canvas.touchHandler, true);
    document.addEventListener("touchcancel", canvas.touchHandler, true);        
    
    $("#canvas-open").click(function(e){
        $(".draggable").css("z-index", "1000");
        $(".canvas-container").css("z-index", "1001");
        
        if(star.drawing == false){
            star.drawing = true;
            $("#canvas-open").removeClass("btn-dark");
            $("#canvas-open").addClass("btn-success");
            $(".canvas-container").show();
        } else {
            star.drawing = false;
            $(".canvas-container").hide();
            $("#canvas-open").addClass("btn-dark");
            $("#canvas-open").removeClass("btn-success");             
        }
    });

    $("#comments-open").click(function(e){
        $(".draggable").css("z-index", "1000");
        $(".comments-container").css("z-index", "1001");
        
        if(star.commentsDialog == false){
            star.commentsDialog = true;
            $("#comments-open").removeClass("btn-dark");
            $("#comments-open").addClass("btn-success");
            $(".comments-container").show();
        } else {
            star.commentsDialog = false;
            $(".comments-container").hide();
            $("#comments-open").addClass("btn-dark");
            $("#comments-open").removeClass("btn-success");             
        }
    });
    
    $(".comments-container").css('position','fixed');
    $(".comments-container").css("left", "30px");
    $(".comments-container").css("top", "30px");
    $(".comments-resize-2").resizable({
        minHeight: 300,
        minWidth: 340
    });
    
    // dragging
    $(".draggable").draggable({handle:".drag-slider"});
    $(".draggable").mousedown(function(){
        $(".draggable").css("z-index", "1000");
        $(this).css("z-index", "1001");
    });
    
    $(".canvas-container").css('position','fixed');
    $(".canvas-container").css("left", "10px");
    $(".canvas-container").css("top", "10px");
    $(".canvas-container").show();
    $(".canvas-paper").resizable({
        minHeight: 340,
        minWidth: 460,
        stop: function(event, ui) {
              canvas.w = ui.size.width;
              canvas.h = ui.size.height;
              canvas.resize();
        }
    });

    canvas.init(); 
    $(".canvas-container").hide();
});

