require("httpuv")
require("jsonlite")
require("curl")
require("purrr")

serverState = list()
serverState$isRunning = TRUE

toMime = list()
toMime$json = "application/json;charset=utf-8"
toMime$jsonl = "application/json;charset=utf-8"
toMime$csv = "text/csv;charset=utf-8"
toMime$txt = "text/plain;charset=utf-8"
toMime$bin = "application/octet-stream"
dataFormatToMime = function(dataFormat)
{
  mime = toMime[dataFormat][[1]]
  if (is.null(mime)) mime = ""
  mime
}

# data caching
cache = list()
cache$map = list()
cache$exists = function(uri)
{
  !is.null(cache$map[[uri]])
}
cache$add = function(uri, format, value)
{
  cache$map[[uri]] <<- list(format=format, value=value)
}
cache$get = function(uri)
{
  cache$map[[uri]]
}

# data conversion
inputFromText = function(text, format)
{
  x = NULL
  
  if (format == "csv")
  {
    x = read.csv(textConnection(text), header=T)
  }
  else if (format == "json")
  {
    x = fromJSON(text)
  }
  else if (format == "jsonl")
  {
    x = stream_in(textConnection(text))
  }
  else if (format == "txt")
  {
    x = readLines(textConnection(text))
  }
  else if (format == "bin")
  {
    x = base64_dec(text)
  }
  x
}
inputFromUrl = function(url, format)
{
  x = NULL
  
  if (format == "csv")
  {
    x = read.csv(url, header=T)
  }
  else if (format == "json")
  {
    x = fromJSON(url)
  }
  else if (format == "jsonl")
  {
    x = stream_in(url(url))
  }
  else if (format == "txt")
  {
    x = readLines(url(url))
  }
  else if (format == "bin")
  {
    x = readBin(url, "raw", n=20000000)
  }
  x
}
output = function(dataObject, format)
{
  x = ""
  if (format == "csv")
  {
    con = textConnection("tc1", open="w")
    write.csv(dataObject, con, row.names=F)
    x = paste(textConnectionValue(con), collapse="\n")
    close(con)
  }
  else if (format == "json")
  {
    x = toJSON(dataObject, auto_unbox=T) # auto_unbox: don't put simple data types into json array
  }
  else if (format == "jsonl")
  {
    con = textConnection("tc1", open="w")
    stream_out(dataObject, con)
    x = paste(textConnectionValue(con), collapse="\n")
    close(con)
  }
  else if (format == "txt")
  {
    con = textConnection("tc1", open="w")
    writeLines(dataObject, con)
    x = paste(textConnectionValue(con), collapse="\n")
    close(con)
  }
  else if (format == "bin")
  {
    x = base64_enc(dataObject)
  }
  x
}

koralCompute = function(uri, ds, cache)
{
  get(ds$func) # throws an error when function not found
  
  # load input
  params = list()
  for (name in names(ds$params))
  {
    p = ds$params[[name]]
    value = NULL
    if (!is.null(p$uri) && cache$exists(p$uri))
    {
      value = cache$get(p$uri)$value
    }
    else if (!is.null(p$val))
    {
      if (p$type == "json")
      {
        value = p$val # already converted to json object representation
      }
      else
      {
        valueStr = toJSON(p$val) # back to string representation
        value = inputFromText(valueStr, p$type);
      }
    }
    else if (!is.null(p$url))
    {
      value = inputFromUrl(p$url, p$type)
    }
    if (!is.null(value))
    {
      params[name] = value
    }
  }
  
  # execute
  result = invoke(ds$func, params)
  
  if (!is.null(result)) cache$add(uri, ds$type, result)
  result
}

addCode = function(expressionText)
{
  eval(expr=parse(text=expressionText), envir=globalenv())
}

http_badRequest = list(status = 400L)
http_NotFound = list(status = 404L)
http_okEmpty = list(status = 200L, headers = list('Content-Type' = 'text/html'), body = "")

parseQuery = function(query) 
{
  if (is.null(query) || nchar(query) == 0) return(list())
  if (substring(query, 1, 1) == '?') query = substring(query, 2)
  if (nchar(query) == 0) return (list())
  
  pairs = strsplit(strsplit(query, "&")[[1]], "=")
  params = list()
  for (i in 1:length(pairs))
  {
    kv = pairs[[i]]
    if (length(kv) == 1) next
    k = kv[1]
    v = kv[2]
    if (is.null(k) || nchar(k) == 0 || is.null(v) || nchar(v) == 0) next
    params[URLdecode(k)] = URLdecode(v)
  }
  params
}

app = list(
  call = function(req) {
    verb = req$REQUEST_METHOD
    uri = substring(req$PATH_INFO, 2)
    params = parseQuery(req$QUERY_STRING)
    
    if (verb == "GET")
    {
      if (params$action == "shutDown")
      {
        serverState$isRunning <<- FALSE
        return (http_okEmpty)
      }
      
      if (cache$exists(uri))
      {
        d = cache$get(uri)
        s = output(d$value, d$format)
        return (list(status = 200L, 
                headers=list("Content-Type"= dataFormatToMime(d$format)), 
                body = s))
      }
      
      return (http_NotFound)
    }
    else if (verb == "POST" && params$action == "compute" && nchar(uri) > 0)
    {
      ds = fromJSON(req$rook.input$read_lines())
      result = koralCompute(uri, ds, cache)
      resultStr = ""
      if (is.null(ds$nostore) || !ds$nostore) resultStr = output(result, ds$type)
      
      return (list(status = 200L, 
                   headers=list("Content-Type"= dataFormatToMime(ds$type)), 
                   body = resultStr))
    }
    else if (verb == "POST" && params$action == "addCode") # for unit tests
    {
      code = req$rook.input$read_lines()
      addCode(code)
      return (http_okEmpty)
    }

    http_badRequest
  }
)

print(.libPaths())
args = commandArgs(trailingOnly=T)
if (length(args) != 1)
{
  print("Specify port.")
} else
{
  port = as.numeric(args)
  interruptIntervalMs = ifelse(interactive(), 100, 1000)
  
  server <- startServer(host="127.0.0.1", port=port, app=app)
  serverState$isRunning = TRUE
  while (serverState$isRunning) 
  {
    service(interruptIntervalMs)
    Sys.sleep(0.001)
  }
  stopServer(server)
  print("Server stopped.")
}



