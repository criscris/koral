#! python3
import sys
import time
import io
import base64
import threading
import http.server
import urllib
import json
import pandas as pd

print("python compute server.")


toMime = { 
	"json": "application/json;charset=utf-8",
	"jsonl": "application/json;charset=utf-8",
	"csv": "text/csv;charset=utf-8",
	"txt": "text/plain;charset=utf-8",
	"bin": "application/octet-stream" 
}
def dataFormatToMime(dataFormat):
	toMime.get(dataFormat, default="text/plain;charset=utf-8")

server = None
cache = {}

def shutDown():
	time.sleep(2)
	print("shutDown server...")
	if server is not None: 
		server.shutdown()

def inputFromStr(text, format):
    dataObject = None
    if format == "csv":
        dataObject = read_csv(io.StringIO(text))
    elif format == "json":
        dataObject = json.loads(text)
    elif format == "jsonl":
        dataObject = []
        for x in text.split("\n"):
            dataObject.append(json.loads(x))
    elif format == "txt":
        dataObject = text.split("\n")
    elif format == "bin":
        dataObject = base64.b64decode(text)
    
    return dataObject   

def outputToStr(dataObject, format):
    text = ""
    if format == "csv":
        text = object.to_csv(index=False)  # StringIO.getvalue()
    elif format == "json":
        # pandas dataframe
        if callable(getattr(dataObject, "to_json", None)):
            text = object.to_json()
        else:
            # any object
            text = json.dumps(dataObject)
    elif format == "jsonl":
        # pandas dataframe
        if callable(getattr(dataObject, "to_json", None)):
            text = object.to_json(lines=True)
        else:
            # list or generator
            lines = []
            for x in dataObject:
                lines.append(json.dumps(x))
            text = "\n".join(lines)
    elif format == "txt":
        lines = []
        for x in dataObject:
            lines.append(str(x))
        text = "\n".join(lines)
    elif format == "bin":
    	text = base64.b64encode(dataObject)
    
    return text

def koralCompute(uri, ds, cache):
	args = {}
	for name in ds["params"].keys():
		p = ds["params"][name]
		value = None
		if "uri" in p.keys() and p["uri"] in cache.keys():
			value = cache[p["uri"]][0]
		elif "val" in p.keys():
			if p["type"] == "json":
				value = p["val"]
			else:
				value = inputFromStr(p["val"], p["type"])
		elif "url" in p.keys():
			valueStr = urllib.urlopen(p["url"]).read()
			value = inputFromStr(valueStr, p["type"])

		if value is not None:
			args[name] = value
	
	return globals()[ds["func"]](**args)

def addCode(codeText):
	exec(codeText)

class ComputeServer(http.server.BaseHTTPRequestHandler):
	def getUri(self):
		req = urllib.parse.urlparse(self.path)
		return req.path[1:]

	def getAction(self):
		req = urllib.parse.urlparse(self.path)
		q = urllib.parse.parse_qs(req.query)
		action = ""
		if "action" in q.keys():
			l = q["action"]
			if (len(l) == 1):
				action = l[0]
		return action	

	def do_GET(self):
		uri = self.getUri()
		action = self.getAction()
		print("GET: " + self.path + " " + action)

		if action == "shutDown":
			threading.Thread(target=shutDown).start()
			self.send_response(200)
			return

		if len(uri) < 1:
			self.send_response(400)
			return

		if uri in cache.keys():
			d = cache[uri]
			s = outputToStr(d[0], d[1])
			self.send_response(200)
			self.send_header('Content-type', dataFormatToMime(d[1]))
			self.end_headers()
			self.wfile.write(s.encode("utf-8"))
			return

		self.send_response(404)

	def do_POST(self):
		uri = self.getUri()
		action = self.getAction()

		if action == "compute":
			ds = json.loads(self.rfile.read().decode())

			if len(uri) < 1:
				self.send_response(400)
				return

			result = koralCompute(uri, ds, cache)
			self.send_response(200)

			if not ds.get("nostore", default=false):
				self.send_header('Content-type', dataFormatToMime(ds.type))
				self.end_headers()
				self.wfile.write(outputToStr(result, ds.type).encode("utf-8"))
			return

		if action == "addCode":
			print("addCode")
			addCode(self.rfile.read().decode())
			self.send_response(200)
			return

		self.send_response(400)

if len(sys.argv) != 2:
	sys.exit("USAGE: port")

port = int(sys.argv[1])
server = http.server.HTTPServer(("", port), ComputeServer)

#try:
#    server.serve_forever()
#except KeyboardInterrupt:
#    pass
#server.server_close()
#print(time.asctime(), "Server stopped.")

threading.Thread(target=server.serve_forever).start()
print(time.asctime(), "Server started.")