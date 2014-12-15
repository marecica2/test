// init
var now = (new Date(new Date().getTime())).toJSON();
var params = "&start="+now+"&end="+now+"&direction=upcoming&type=dashboard";
star.initItems("Upcoming", params);

var params1 = "&start="+now+"&end="+now+"&direction=previous&type=dashboard";
star.initItems("Previous", params1);



//watches
var month=new Array("cal.january","cal.february","cal.march","cal.april","cal.may","cal.june","cal.july", "cal.august", "cal.september", "cal.november", "cal.december");
var weekday=new Array("cal.sunday","cal.monday","cal.tuesday","cal.wednesday","cal.thursday","cal.friday","cal.saturday");
var d=new Date();

//function startTime() {
//    var today=new Date();
//    var h=today.getHours();
//    var m=today.getMinutes();
//    var s=today.getSeconds();
//    m = checkTime(m);
//    s = checkTime(s);
//    document.getElementById('txt1').innerHTML = h+":"+m+":<span style='width:80px'>"+s+"</span>";
//    var t = setTimeout(function(){startTime()},500);
//    $("#day").html(i18n(weekday[d.getDay()])+", "+d.getDate()+" "+i18n(month[d.getMonth()-1])+" "+d.getFullYear());
//}

function checkTime(i) {
    if (i<10) {i = "0" + i};
    return i;
}

//activities
if(starCalendar == undefined)
    var starCalendar = {};
starCalendar.activitiesLimit = 10;
starCalendar.clientId = ""; 

starEvent.loadActivities();
$(document).ready(function(){
    $(".activities-refresh").click(function(){
        starEvent.loadActivities();
    });
    
    $(".activities-more").click(function(){
        starCalendar.activitiesLimit += 10;
        starEvent.loadActivities();
    });
    //startTime();
});

