#Python 3.6

import json
import os
import re
from urllib import request

def downloadFromURL(src_url,save_name):
	print("下载\"%s\"到\"%s\"…"%(src_url,save_name))
	url_file=request.urlopen(src_url)
	save_file=open(save_name,"wb").write(url_file.read())

jsonFile=open("anime.js",encoding="utf-8")
try:
	jsonStr=jsonFile.read()
finally:
	jsonFile.close()

jsonObject=json.loads(jsonStr[jsonStr.find("(")+1:jsonStr.rfind(")")])

if os.path.exists("covers")==False:
	os.mkdir("covers")

for animeObj in jsonObject["anime"]:
	if animeObj["cover"]=="":
		print(animeObj["title"]+" 未指定URL。")
	else:
		downloadFromURL(animeObj["cover"],"covers/"+re.sub(r'[\/:?|<>*"]',"_",animeObj["title"])+"."+animeObj["cover"].split(".").pop())
