    function addScript(u){ 
        var s=document.createElement('script'); 
       s.src=u;  
       document.getElementsByTagName('*')[1].appendChild(s);
      }

     function getQueryGoogle(term, callback){
        var id="i"+Math.random().toString(36).slice(2);
        getQueryGoogle[id]=function(data){ callback(data); delete getQueryGoogle[id];};
        addScript( "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20json%20where%20url%3D%22http%3A%2F%2Fsuggestqueries.google.com%2Fcomplete%2Fsearch%3Fclient%3Dfirefox%26q%3D"+
        encodeURIComponent(term)+
       "%22%20&format=json&callback=getQueryGoogle."+id );
     }
    
    var src = [];
    function extractor(query) {
        var result = /([^,]+)$/.exec(query);
        if(result && result[1])
            return result[1].trim();
        return '';
    };
    
    $("#query-search").typeahead({
        minLength : 2, 
        source : function(value, process){
                      var q = value.replace(/ /g, '+');
                      console.log(q);
                      var items = [];
                      items.push(value);
                      getQueryGoogle(q, function(d){
                          //console.log(d);
                          //items.push(d.query.results.json.json[0]);
                          if(d.query.results.json.json[1] != null){
                              var data = d.query.results.json.json[1].json;
                              items = items.concat(data);
                          } 
                          process(items);             
                     });
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