    function addScript(u){ 
        var s=document.createElement('script'); 
       s.src=u;  
       document.getElementsByTagName('*')[1].appendChild(s);
      }
    
    
    $("#query-search").typeahead({
        minLength : 2, 
        source : function(value, process){
          var items = [];
          items.push(value);    
          var q = value.replace(/ /g, '+');
          $.getJSON("https://suggestqueries.google.com/complete/search?callback=?",
                  {
                    //"hl":"de", // Language
                    //"ds":"yt", // Restrict lookup to youtube
                    "jsonp":"suggestCallBack", // jsonp callback function name
                    "q":q, // query term
                    "client":"chrome" // force youtube style response, i.e. jsonp
                  }
              );
              suggestCallBack = function (data) {
                  data = data[1];
                  for(var i = 0; i < data.length; i++)
                      items.push(data[i]);
                  process(items);             
              };    
        },
        updater : function(item){
            //console.log("up " + item);
            return item;
        }, 
        matcher : function(item){
            //console.log("ma " + item);
            return true;
        }
    });