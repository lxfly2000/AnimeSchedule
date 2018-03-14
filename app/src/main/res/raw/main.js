//获取URL数据,onReceive(data,status,extra)
function getUrlData(url,onReceive,extra){
	var xhr=new XMLHttpRequest();
	xhr.open("GET",url,true);
	xhr.onreadystatechange=function(){
		if(xhr.readyState==4){
			onReceive(xhr.responseText,xhr.status,extra);
		}
	};
	xhr.send(null);
}

//获取URL参数
//http://www.cnblogs.com/jiekk/archive/2011/06/28/2092444.html
function getQueryString(name) {
	var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
	var r = location.search.substr(1).match(reg);
	if (r != null)
		return unescape(r[2]);
	return null;
}

//Callback
function setJsonData(anime_json){
	if(clearList())
		writeList(anime_json,"降序");
}

function appendLocalScript(src){
	var sobj=document.createElement("script");
	sobj.src=src;
	document.body.appendChild(sobj);
}

//初始化，下载JSON数据
(function initPage(){
	var src=getQueryString("src");
	if(src==null)
		src="anime.js";
	getTemplate();
	if(location.href.substring(0,4).toLowerCase()=="file"){
		appendLocalScript(src);
	}else{
		getUrlData(src,function(d,s){
			if(s==200){
				setJsonData(JSON.parse(d));
			}else{
				appendLocalScript(src);
			}
		});
	}
	document.getElementsByClassName("ButtonCloseBox")[0].addEventListener("click",function(e){
		document.getElementsByClassName("FloatingBoxRoot")[0].remove();
	});
}());

var listTemplate;

function getTemplate(){
	listTemplate=document.getElementsByClassName("ItemAnimeBase")[0];
}

//清除列表项
function clearList(){
	document.getElementsByClassName("AnimeList")[0].innerHTML="";
	if(listTemplate.innerHTML==""){
		alert("当前浏览器无法显示此网页。");
		return false;
	}
	return true;
}

//日期计算，输入日期参数为Date类型，addDays为整型，无返回值
function dateAdd(originalDate,addInterval,addUnit){
	switch(addUnit.toLowerCase()){
	case "year":
		originalDate.setYear(originalDate.getYear()+addInterval);
		break;
	case "month":
		originalDate.setMonth(originalDate.getMonth()+addInterval);
		break;
	case "day":
		originalDate.setDate(originalDate.getDate()+addInterval);
		break;
	}
}

//日期比较
function dateCompare(dateA,opComp,dateB){
	var ndateA=dateA.getFullYear()*10000+(dateA.getMonth()+1)*100+dateA.getDate();
	var ndateB=dateB.getFullYear()*10000+(dateB.getMonth()+1)*100+dateB.getDate();
	switch(opComp){
	case "<":
		return ndateA<ndateB;
	case ">":
		return ndateA>ndateB;
	case "==":
		return ndateA==ndateB;
	case "<=":return !dateCompare(dateA,">",dateB);
	case ">=":return !dateCompare(dateA,"<",dateB);
	}
	return false;
}

//将日期转换成"yyyy-M-d"格式
function dateToString(dateObject){
	return dateObject.getFullYear().toString()+"-"+(dateObject.getMonth()+1)+"-"+dateObject.getDate();
}

function setCoverCallback(responseData,responseStatus,extra){
	var resIsLocal=true;
	switch(extra.next_url[0].split(":")[0]){
	case "http":case "https":case "data":
		resIsLocal=false;
		break;
	}
	if(responseStatus==200||resIsLocal){
		extra.cover_dom.src=extra.next_url[0];
		console.log("设置图片："+extra.next_url[0]);
	}else{
		var logString="获取 "+extra.next_url[0]+" 失败，";
		extra.next_url.splice(0,1);
		if(extra.next_url.length>0){
			logString+="尝试下一链接："+extra.next_url[0];
			console.log(logString);
			getUrlData(extra.next_url[0],setCoverCallback,{
				cover_dom:extra.cover_dom,
				next_url:extra.next_url
			});
		}else{
			logString+="已无可用链接。";
			console.log(logString);
		}
	}
}

//生成列表项，sortOrder可以为""（不排序），"升序"（上方日期较早），"降序"（上方日期较晚）
//排序依据为最新一集的更新日期
function writeList(jsonData,sortOrder){
	sortOrder=sortOrder?sortOrder:"";
	var listObjectsList=[];
	var tagAnimeList=document.getElementsByClassName("AnimeList")[0];
	for (var i = 0; i < jsonData["anime"].length; i++) {
		var listObject=listTemplate.cloneNode(true);
		var animeObject=jsonData["anime"][i];
		getUrlData(animeObject["cover"],setCoverCallback,{
			cover_dom:listObject.getElementsByClassName("ItemAnimeCover")[0],
			next_url:[
				animeObject["cover"],
				"covers/"+animeObject["title"].replace(/[/\\":\|<>\?\*]/g,"_")+"."+animeObject["cover"].split(".").pop()
			],
		});
		listObject.getElementsByClassName("ItemAnimeTitle")[0].innerHTML="";
		var linkedTitleObject=document.createElement("a");
		linkedTitleObject.textContent=animeObject["title"];
		linkedTitleObject.setAttribute("href",animeObject["watch_url"]);
		listObject.getElementsByClassName("ItemAnimeTitle")[0].appendChild(linkedTitleObject);
		var rankString="";
		for (var j=0;j<5;j++){
			rankString+=j<animeObject["rank"]?"★":"☆";
		}
		listObject.getElementsByClassName("ItemAnimeRank")[0].innerHTML=rankString;
		var descriptionSplittedLines=animeObject["description"].split("\n");
		var descriptionObject=listObject.getElementsByClassName("ItemAnimeDescription")[0];
		descriptionObject.textContent="";
		for(var j=0;j<descriptionSplittedLines.length;j++){
			if(j>0)
				descriptionObject.appendChild(document.createElement("br"));
			descriptionObject.appendChild(document.createTextNode(descriptionSplittedLines[j]));
		}
		if(animeObject["category"].length==0){
			listObject.getElementsByClassName("ItemAnimeCategory")[0].appendChild(document.createTextNode("未分类"));
		}else{
			for (var j=0;j<animeObject["category"].length;j++){
				var tagCategoryObject=document.createElement("div");
				tagCategoryObject.className="ItemAnimeCategoryItem";
				tagCategoryObject.textContent=animeObject["category"][j];
				listObject.getElementsByClassName("ItemAnimeCategory")[0].appendChild(tagCategoryObject);
			}
		}
		animeObject["last_update_date"]=animeObject["start_date"];
		var lastUpdateDate=new Date(animeObject["last_update_date"]);
		var nowDate=new Date();
		animeObject["last_update_episode"]=0-animeObject["absense_count"];//注意：此处的集数是从0数起的
		while(true){
			dateAdd(lastUpdateDate,animeObject["update_period"],animeObject["update_period_unit"]);
			if(dateCompare(lastUpdateDate,">",nowDate))
				break;
			if(animeObject["episode_count"]!=-1&&animeObject["last_update_episode"]+1>=animeObject["episode_count"])
				break;
			animeObject["last_update_date"]=dateToString(lastUpdateDate);
			animeObject["last_update_episode"]++;
		}
		animeObject.lastUpdateDateObject=lastUpdateDate=new Date(animeObject["last_update_date"]);
		var maxEpisodes=animeObject["episode_count"];
		if(maxEpisodes==-1)
			maxEpisodes=animeObject["last_update_episode"]+1;
		var weekStr=["日","一","二","三","四","五","六"];
		with(listObject.getElementsByClassName("ItemAnimeSchedule")[0]){
			if(animeObject["last_update_episode"]+1==animeObject["episode_count"]){
				textContent="已完结，最后更新于"+animeObject["last_update_date"]+"，";
				if(animeObject["absense_count"]>0)
					textContent+="推迟"+animeObject["absense_count"]+"次，";
				textContent+="共"+animeObject["episode_count"]+"话。";
			}else{
				textContent="放送中，";
				switch(animeObject["update_period_unit"]){
				case "day":
					if(animeObject["update_period"]==7)
						textContent+="每周"+weekStr[lastUpdateDate.getDay()]+"更新，"
					else
						textContent+="每"+animeObject["update_period"]+"日期更新，";
					break;
				case "month":
					textContent+="每月"+lastUpdateDate.getDate()+"日更新，";
					break;
				}
				textContent+=animeObject["last_update_date"]+"更新第"+(animeObject["last_update_episode"]+1)+"话";
				if(animeObject["absense_count"]>0)
					textContent+="，推迟"+animeObject["absense_count"]+"次";
				if(animeObject["episode_count"]==-1)
					textContent+="。";
				else
					textContent+="，共"+animeObject["episode_count"]+"话。";
			}
		}
		for (var j=0;j<maxEpisodes;j++){
			var tagEpisodeObject=document.createElement("div");
			tagEpisodeObject.textContent=(j+1).toString();
			if(animeObject["watched_episode"][j]==true){
				if(jsonData["last_watch_index"]==i&&jsonData["last_watch_episode"]==j+1)
					tagEpisodeObject.className="ItemAnimeEpisodeTagLastWatched";
				else
					tagEpisodeObject.className="ItemAnimeEpisodeTagWatched";
			}else if(j<=animeObject["last_update_episode"]){
				tagEpisodeObject.className="ItemAnimeEpisodeTagNotWatched";
				animeObject.haveNotWatchedEpi=true;
			}else{
				tagEpisodeObject.className="ItemAnimeEpisodeTagNotReleased";
			}
			listObject.getElementsByClassName("ItemAnimeEpisodesLine")[0].appendChild(tagEpisodeObject);
		}
		listObject.addEventListener("click",function(e){
			if(e.srcElement.tagName.toLowerCase()!="a"){
				open(this.getElementsByTagName("a")[0].href);
			}
		});
		listObject.setAttribute("mouseentercolor",animeObject["color"]);
		listObject.addEventListener("mouseenter",function(e){
			this.style.background="linear-gradient(180deg,"+e.srcElement.getAttribute("mouseentercolor")+",rgba(0,0,0,0))";
		});
		listObject.addEventListener("mouseleave",function(e){
			this.style.background="initial";
		});
		if(animeObject["abandoned"]==true){
			listObject.style.color="gray";
		}
		listObjectsList.push(listObject);
	}
	var sortTableByUpdateDate=[];
	for(var i=0;i<listObjectsList.length;i++){
		sortTableByUpdateDate[i]=i;
	}
	if(sortOrder!=""){
		var sortOperSymbol={"升序":"<","降序":">"};
		sortTableByUpdateDate.sort(function(a,b){
			return dateCompare(jsonData["anime"][a].lastUpdateDateObject,sortOperSymbol[sortOrder],jsonData["anime"][b].lastUpdateDateObject)?-1:1;
		});
	}
	var processingTotal=listObjectsList.length;
	for(var i=0;i<processingTotal;){
		if(jsonData["anime"][sortTableByUpdateDate[i]]["abandoned"]==true){
			sortTableByUpdateDate.push(sortTableByUpdateDate[i]);
			sortTableByUpdateDate.splice(i,1);
			processingTotal--;
		}else{
			i++;
		}
	}
	for(var i=0;i<listObjectsList.length;i++){
		tagAnimeList.appendChild(listObjectsList[sortTableByUpdateDate[i]]);
	}
	var floatingBoxObject=document.getElementsByClassName("FloatingBox")[0];
	var paragObject=document.createElement("p");
	if(listObjectsList.length==0)
		paragObject.textContent="无数据。";
	else
		paragObject.textContent="上次观看："+jsonData["last_watch_date"]+" "+jsonData["anime"][jsonData["last_watch_index"]]["title"]+" 第"+jsonData["last_watch_episode"]+"话";
	floatingBoxObject.appendChild(paragObject);
	paragObject=document.createElement("p");
	paragObject.appendChild(document.createTextNode("更新信息："));
	if(sortOrder=="降序")
		sortTableByUpdateDate.reverse();
	var behindCount=0;
	for(var i=0;i<listObjectsList.length;i++){
		var animeObject=jsonData["anime"][sortTableByUpdateDate[i]];
		if(animeObject["abandoned"]==false&&animeObject.haveNotWatchedEpi==true){
			behindCount++;
			paragObject.appendChild(document.createElement("br"));
			paragObject.appendChild(document.createTextNode(animeObject["title"]+" "+animeObject["last_update_date"]+" 已更新至"+(animeObject["last_update_episode"]+1)+"话"));
		}
	}
	if(behindCount==0){
		paragObject.appendChild(document.createElement("br"));
		paragObject.appendChild(document.createTextNode("你已跟上所有番剧的更新进度。"));
	}
	floatingBoxObject.appendChild(paragObject);
}
