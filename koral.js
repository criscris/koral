var Koral = {
    onloadFuncs: [],

    onload: function (callback) {
        if (document.readyState === 'complete')
        {
            callback();
        } else
        {
            Koral.onloadFuncs.push(callback);

            if (Koral.onloadFuncs.length == 1)
            {
                window.onload = function (e)
                {
                    for (var i=0; i<Koral.onloadFuncs.length; i++) Koral.onloadFuncs[i]();

                    Koral.onloadFuncs = [];
                }
            }
        }
    },

    xy: function (xmin, xmax, xby, yFunc) {
        r = [];
        for (var x = xmin; x <= xmax; x += xby)
        {
            r.push({x: x, y: yFunc(x)});
        }
        return r;
    },

    xylog10: function (xmin_log, xmax_log, xby_log, yFunc) {
        r = [];
        for (var x = xmin_log; x <= xmax_log; x += xby_log)
        {
            xl = Math.pow(10, x);
            r.push({x: xl, y: yFunc(xl)});
        }
        return r;
    },

    textWidth: function (text, font) {
        // if given, use cached canvas for better performance
        // else, create new canvas
        var canvas = Koral.textWidth.canvas || (Koral.textWidth.canvas = document.createElement("canvas"));
        var context = canvas.getContext("2d");
        context.font = font;
        var metrics = context.measureText(text);
        return metrics.width;
    },

    getJsonDataAttribute: function (element, name, defaultObj)
    {
        var d = $(element).data(name);
        if (typeof (d) == 'string') // json parsing failed
        {
            d = d.replace(/'/g, '"'); // parser needs double quotes
            d = JSON.parse(d);
        }
        return $.extend({}, defaultObj, d == null ? {} : d);
    },

    uuid: function () {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c)
        {
            var r = crypto.getRandomValues(new Uint8Array(1))[0] % 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    },

    getUrlParameter: function (name) {
        var sPageURL = decodeURIComponent(window.location.search.substring(1));
        var sURLVariables = sPageURL.split('&');

        for (var i = 0; i < sURLVariables.length; i++) {
            var sParameterName = sURLVariables[i].split('=');

            if (sParameterName[0] === name) {
                return sParameterName[1] === undefined ? true : sParameterName[1];
            }
        }
    },

    // taken from http://stackoverflow.com/a/39420250
    setUrlParameter: function (param, value) {
        var currentURL = window.location.href;
        var urlObject = currentURL.split("?");
        var newQueryString = "?";
        value = encodeURIComponent(value);
        if (urlObject.length > 1) {
            var queries = urlObject[1].split("&");
            var updatedExistingParam = false;
            for (var i = 0; i < queries.length; i++) {
                var queryItem = queries[i].split("=");
                if (queryItem.length > 1) {
                    if (queryItem[0] == param) {
                        newQueryString += queryItem[0] + "=" + value + "&";
                        updatedExistingParam = true;
                    } else {
                        newQueryString += queryItem[0] + "=" + queryItem[1] + "&";
                    }
                }
            }
            if (!updatedExistingParam) {
                newQueryString += param + "=" + value + "&";
            }
        } else {
            newQueryString += param + "=" + value + "&";
        }
        window.history.replaceState("", "", urlObject[0] + newQueryString.slice(0, -1));
    },

    loadCSV: function(url, callback)
    {
        var prefix = $("article").first().data("urlprefix"); // does not deal with the case of several articles in one html document
        if (prefix != null && prefix.length > 0) url = prefix + url;

        $.ajax({
            url: url,
        }).done(data =>
        {
            var csv = Papa.parse(data, {header: true, skipEmptyLines: true});
            callback(csv.data);
        });
    }
};

var KoralParagraph = function (dom)
{
    this.dom = dom;
    this.domStr = $(dom).clone().wrap('<div/>').parent().html();
    this.domStr = this.domStr.replace(new RegExp("&quot;", 'g'), "'");
    this.row = $("<div class='editRow'></div>");
    this.leftCol = $("<div class='editLeftCol'></div>");
    this.rightCol = $("<div class='editRightCol'></div>");
    this.leftCol.appendTo(this.row);
    this.rightCol.appendTo(this.row);

    $(this.dom).appendTo(this.leftCol);

    this.processContent = function (checkUpdate)
    {
        // rerender math
        MathJax.Hub.Queue(["Typeset", MathJax.Hub, this.leftCol.get(0)]);


        //  add url prefix
        var part = this.leftCol;
        var prefix = part.closest("article").data("urlprefix");
        if (prefix != null && prefix.length > 0)
        {
            part.find("[data-url]").each(function (i, v)
            {
                $(this).data("url", prefix + $(this).data("url"));
            });

            part.find("image[href]").each(function (i, v)
            {
                $(this).attr("href", prefix + $(this).attr("href"));
            });
        }

        // create plots
        KoralPlot.processPlots(this.leftCol.get(0));

        if (checkUpdate)
        {
            if (this.leftCol.find("figure").length > 0 || this.leftCol.find(".equation").length > 0 || this.leftCol.find("a").length > 0)
            {
                KoralInternal.updateIDs();
            }
        }
        if (this.leftCol.find(".references").length > 0)
        {
            KoralInternal.loadReferences(this.leftCol.find(".references"));
        }
        if (this.leftCol.find(".sources").length > 0)
        {
            KoralInternal.loadSources(this.leftCol.find(".sources"));
        }


        // syntax highlighting / prettify for code blocks
        this.leftCol.find("pre").each(function (index, value)
        {
            if ($(this).find("code").length > 0)
            {
                $(this).toggleClass("prettyprint", true);
                // external, needs to be async
                setTimeout(function () {
                    prettyPrint();
                }, 2);
            }
        });

        // slide
        this.leftCol.find(".slide").click(function() {
            KoralInternal.toggleFullScreen(this);
        });

        this.leftCol.find("a").each(function (index, value)
        {
            var link = $(this).attr('href');
            if (link == null)
                return;
            if (!link.startsWith("#"))
                return;
            var targetID = link.substring(1);
            // figure
            var number = KoralInternal.figureIDtoNumber[targetID];
            if (number != null)
            {
                $(this).text("Fig. " + number);
            }
            // equation
            else
            {
                number = KoralInternal.equationIDtoNumber[targetID];
                if (number != null)
                {
                    $(this).text("Eq. (" + number + ")");
                }
            }
        });
    }
};

var KoralArticle = function (article) {
    this.paragraphs = [];
    this.figureIDtoNumber = {};
    this.equationIDtoNumber = {};
    var that = this;

    KoralInternal.updateIDs();

    var articleHtml = $("<div class='editTable'></div>");
    $(article).children().each(function ()
    {
        var p = new KoralParagraph(this);
        p.row.appendTo(articleHtml);
        that.paragraphs.push(p);
    });
    $(article).children().remove();

    var outer = $("<div class='article'></div>");
    articleHtml.appendTo(outer);
    outer.appendTo($(article));

    this.update = function (paragraph, newcode)
    {
        newcode = newcode.trim();
        if (newcode.length == 0)
        {
            paragraph.row.remove();
            this.paragraphs.splice(this.paragraphs.indexOf(paragraph), 1);
            return;
        }

        var d = $(newcode); // newcode.replace(new RegExp("'", 'g'), "&quot;"); no longer an issue?

        if (d.length <= 1)
        {
            paragraph.domStr = newcode;
            paragraph.dom = d.get(0);
            paragraph.leftCol.children().remove();
            d.appendTo(paragraph.leftCol);
            paragraph.processContent(true);
        } else
        {
            var newParagraphs = [];
            for (var i = 0; i < d.length; i++)
            {
                var dom = d.get(i);
                if ($(dom).clone().wrap('<div/>').parent().html().trim().length === 0)
                    continue;

                var p = new KoralParagraph(dom);
                p.row.insertBefore(paragraph.row);
                KoralInternal.activateEditing(this, p);
                newParagraphs.push(p);
                p.processContent(true);
            }
            paragraph.row.remove();
            var index = this.paragraphs.indexOf(paragraph);
            this.paragraphs = this.paragraphs.slice(0, index).concat(newParagraphs).concat(this.paragraphs.slice(index + 1));
        }
    }

    setTimeout(() => {
        for (var i=0; i<that.paragraphs.length; i++)
        {    
            that.paragraphs[i].processContent(false);
        }
    }, 100); // give Koral.onload callbacks a chance to be called first
};

var KoralUI = {
    // entries is an array with objects that have properties 
    // 'label' (String) and 'onclick' (function), optionally 'id' (String)
    menu: function (entries) {
        var entryHeight = 32;
        $("<div class='menu'>" +
                "<div>" +
                "<svg width='31px' height='31px'>" +
                "<style>" +
                ".menuButton { cursor:pointer; } " +
                ".menuBR { fill:rgb(255,255,255); } " +
                ".menuButton:hover .menuBR { fill:rgb(230,230,230); } " +
                ".ham { fill:rgb(80,80,80); }" +
                "</style>" +
                "<g class='menuButton'>" +
                "<rect width='31' height='31' class='menuBR'></rect>" +
                "<rect x='8' y='8' width='15' height='3' class='ham'></rect>" +
                "<rect x='8' y='14' width='15' height='3' class='ham'></rect>" +
                "<rect x='8' y='20' width='15' height='3' class='ham'></rect>" +
                "</g>" +
                "</svg>" +
                "</div>" +
                "<div id='mainMenu' style='display:none'>" + 
                "<svg width='168' height='" + (entryHeight * entries.length) + "'>" +
                "<style>" +
                ".menuT { color: rgb(0,0,0); font-family: 'Arial'; font-size: 16px; } " +
                ".menuE { cursor:pointer; } " +
                ".menuR { fill:rgb(255,255,255); } " +
                ".menuE:hover .menuR { fill:rgb(230,230,230); }" +
                "</style>" +
                "<g transform='translate(8, 0)'>" +
                "<g id='menuEntries'></g>" +
                "<rect width='160' height='" + (entryHeight * entries.length) + "' style='fill:none; stroke:rgb(153, 153, 153); stroke-width:1px'></rect>" +
                "</g>" +
                "</svg>" +
                "</div>" +
                "</div>").appendTo($("body").first());
        var m = d3.select("#menuEntries");

        for (var i = 0; i < entries.length; i++)
        {
            var g = m.append("g")
                    .attr("transform", "translate(0, " + (entryHeight * i) + ")")
                    .classed("menuE", true);
            g.append("rect")
                    .attr("width", "160")
                    .attr("height", "32")
                    .classed("menuR", true);
            var text = g.append("text")
                    .attr("x", "8")
                    .attr("y", 24)
                    .classed("menuT", true)
                    .text(entries[i].label);
            if (entries[i].id)
                text.attr("id", entries[i].id);
            g.on("click", entries[i].onclick);
        }

        document.body.onclick = function (e) {
            document.getElementById('mainMenu').style.display = 'none';
        }
        document.querySelector('.menuButton').addEventListener("click", function (e) {
            document.getElementById('mainMenu').style.display = ''; 
            e.stopPropagation();
        }, false);
    },

    updateButton: function(onclick)
    {
        $("<div class='menu2'>" +
        "<div>" +
        "<svg width='31px' height='31px'>" +
        "<style>" +
        ".updateButton { cursor:pointer; } " +
        ".menuBR { fill:rgb(255,255,255); } " +
        ".updateButton:hover .menuBR { fill:rgb(230,230,230); } " +
        ".runArrow { fill:rgb(80,80,80); }" +
        "</style>" +
        "<g class='updateButton'>" +
        "<rect width='31' height='31' class='menuBR'></rect>" +
        "<path d='m 25.612314,15.5951 -8.861943,4.9902 -8.8619435,4.9902 0.1093523,-10.1698 0.1093522,-10.1697 8.752591,5.1795 z' class='runArrow'/>" +
        "</g>" +
        "</svg>" +
        "</div>" +
        "</div>").insertBefore($(".menu"));

        $(".updateButton").on("click", function() {
            onclick();
            KoralUI.removeUpdateButton();
        });
    },

    removeUpdateButton: function()
    {
        $(".menu2").remove();
    },


    drag: function (element, attachElement, lowerBound, upperBound, startCallback, moveCallback, endCallback, attachLater, boundCallback) {
        function hookEvent(element, eventName, callback)
        {
            if (typeof (element) == "string")
                element = document.getElementById(element);
            if (element == null)
                return;
            if (element.addEventListener)
                element.addEventListener(eventName, callback, false);
            else if (element.attachEvent)
                element.attachEvent("on" + eventName, callback);
        }

        function unhookEvent(element, eventName, callback)
        {
            if (typeof (element) == "string")
                element = document.getElementById(element);
            if (element == null)
                return;
            if (element.removeEventListener)
                element.removeEventListener(eventName, callback, false);
            else if (element.detachEvent)
                element.detachEvent("on" + eventName, callback);
        }

        function cancelEvent(e)
        {
            e = e ? e : window.event;
            if (e.stopPropagation)
                e.stopPropagation();
            if (e.preventDefault)
                e.preventDefault();
            e.cancelBubble = true;
            e.cancel = true;
            e.returnValue = false;
            return false;
        }

        function Position(x, y)
        {
            this.X = x;
            this.Y = y;

            this.Add = function (val)
            {
                var newPos = new Position(this.X, this.Y);
                if (val != null)
                {
                    if (!isNaN(val.X))
                        newPos.X += val.X;
                    if (!isNaN(val.Y))
                        newPos.Y += val.Y;
                }
                return newPos;
            };

            this.Subtract = function (val)
            {
                var newPos = new Position(this.X, this.Y);
                if (val != null)
                {
                    if (!isNaN(val.X))
                        newPos.X -= val.X;
                    if (!isNaN(val.Y))
                        newPos.Y -= val.Y;
                }
                return newPos;
            };

            this.Min = function (val)
            {
                var newPos = new Position(this.X, this.Y);
                if (val == null)
                    return newPos;

                if (!isNaN(val.X) && this.X > val.X)
                    newPos.X = val.X;
                if (!isNaN(val.Y) && this.Y > val.Y)
                    newPos.Y = val.Y;

                return newPos;
            };

            this.Max = function (val)
            {
                var newPos = new Position(this.X, this.Y);
                if (val == null)
                    return newPos;

                if (!isNaN(val.X) && this.X < val.X)
                    newPos.X = val.X;
                if (!isNaN(val.Y) && this.Y < val.Y)
                    newPos.Y = val.Y;

                return newPos;
            };

            this.Bound = function (lower, upper)
            {
                var newPos = this.Max(lower);
                return newPos.Min(upper);
            };

            this.Check = function ()
            {
                var newPos = new Position(this.X, this.Y);
                if (isNaN(newPos.X))
                    newPos.X = 0;
                if (isNaN(newPos.Y))
                    newPos.Y = 0;
                return newPos;
            };

            this.Apply = function (element)
            {
                if (typeof (element) == "string")
                    element = document.getElementById(element);
                if (element == null)
                    return;
                if (!isNaN(this.X))
                    element.style.left = this.X + 'px';
                if (!isNaN(this.Y))
                    element.style.top = this.Y + 'px';
            };
        }

        function absoluteCursorPostion(eventObj)
        {
            eventObj = eventObj ? eventObj : window.event;
            if (isNaN(window.scrollX))
                return new Position(eventObj.clientX + document.documentElement.scrollLeft + document.body.scrollLeft,
                        eventObj.clientY + document.documentElement.scrollTop + document.body.scrollTop);
            else
                return new Position(eventObj.clientX + window.scrollX, eventObj.clientY + window.scrollY);
        }


        if (typeof (element) == "string")
            element = document.getElementById(element);
        if (element == null)
            return;

        if (lowerBound != null && upperBound != null)
        {
            var temp = lowerBound.Min(upperBound);
            upperBound = lowerBound.Max(upperBound);
            lowerBound = temp;
        }

        var cursorStartPos = null;
        var elementStartPos = null;
        var dragging = false;
        var listening = false;
        var disposed = false;

        function dragStart(eventObj)
        {
            if (dragging || !listening || disposed)
                return;
            dragging = true;

            if (startCallback != null)
                startCallback(eventObj, element);
            cursorStartPos = absoluteCursorPostion(eventObj);
            elementStartPos = new Position(parseInt(element.style.left), parseInt(element.style.top));
            elementStartPos = elementStartPos.Check();
            hookEvent(document, "mousemove", dragGo);
            hookEvent(document, "mouseup", dragStopHook);
            return cancelEvent(eventObj);
        }

        function dragGo(eventObj)
        {
            if (!dragging || disposed)
                return;

            var newPos = absoluteCursorPostion(eventObj);
            newPos = newPos.Add(elementStartPos).Subtract(cursorStartPos);
            newPos = newPos.Bound(lowerBound, upperBound);
            newPos.Apply(element);
            if (moveCallback != null)
                moveCallback(newPos, element);
            return cancelEvent(eventObj);
        }

        function dragStopHook(eventObj)
        {
            dragStop();
            return cancelEvent(eventObj);
        }

        function dragStop()
        {
            if (!dragging || disposed)
                return;
            unhookEvent(document, "mousemove", dragGo);
            unhookEvent(document, "mouseup", dragStopHook);
            cursorStartPos = null;
            elementStartPos = null;
            if (endCallback != null)
                endCallback(element);
            dragging = false;
        }

        function DragObject()
        {
            this.Dispose = function ()
            {
                if (disposed)
                    return;
                this.StopListening(true);
                element = null;
                attachElement = null;
                lowerBound = null;
                upperBound = null;
                startCallback = null;
                moveCallback = null;
                endCallback = null;
                disposed = true;
            };

            this.StartListening = function ()
            {
                if (listening || disposed)
                    return;
                listening = true;
                hookEvent(attachElement, "mousedown", dragStart);
            };

            this.StopListening = function (stopCurrentDragging)
            {
                if (!listening || disposed)
                    return;
                unhookEvent(attachElement, "mousedown", dragStart);
                listening = false;

                if (stopCurrentDragging && dragging)
                    dragStop();
            };

            this.IsDragging = function () {
                return dragging;
            };
            this.IsListening = function () {
                return listening;
            };
            this.IsDisposed = function () {
                return disposed;
            };

            if (typeof (attachElement) == "string")
                attachElement = document.getElementById(attachElement);
            if (attachElement == null)
                attachElement = element;
            if (!attachLater)
                this.StartListening();
        }
        new DragObject();
    },

    dialog: function (title, contentNode, dialogButtonList)
    {
        var m = $("<div></div>");
        m.appendTo(document.body);

        var $backgroundLayer = $("<div></div>");
        $backgroundLayer.attr("class", "dialogBg");
        m.append($backgroundLayer);

        var cx = $backgroundLayer.width() / 2;
        var cy = $backgroundLayer.height() / 2 + $(window).scrollTop();

        var pageHeight = $(window).height();
        $backgroundLayer.css("height", pageHeight + "px");
        var $dialogDiv = $("<div style=\"position:absolute;\"/>");

        $dialogDiv.attr("class", "dialogWindow");
        m.append($dialogDiv);

        var $firstRow = $("<div style=\"width:100%\" />");
        $dialogDiv.append($firstRow);
        var $dialogHeader = $("<div style=\"float:left;\" class=\"dialogTitle\">" + title + "</div>");
        $firstRow.append($dialogHeader);
        $firstRow.append($("<div style=\"height:30px;width:100%\"><!-- _ --></div>"));

        var $dialogContent = $("<div class=\"dialogContent\"/>");
        $dialogDiv.append($dialogContent);
        $dialogContent.append(contentNode);

        var $buttons = $("<table style=\"float:right;\"/>");
        $buttons.attr("cellspacing", "0");
        $buttons.attr("cellpadding", "0");
        $dialogDiv.append($buttons);
        var $buttonsRow = $("<tr />");
        $buttons.append($buttonsRow);

        var close = function ()
        {
            $dialogDiv.remove();
            $backgroundLayer.remove();
        }

        for (var i = 0; i < dialogButtonList.length; i++)
        {
            var $button = $("<td><div class=\"dialogAction\">" + dialogButtonList[i].name + "</div></td>");
            $buttonsRow.append($button);

            (function (f)
            {
                $button.click(function ()
                {
                    if (f())
                        close();
                });
            })(dialogButtonList[i].onclickfunc);

        }

        $cancelButtonEntry = $("<td />");
        $cancelButton = $("<div class=\"dialogAction\">Cancel</div>");
        $cancelButtonEntry.append($cancelButton);
        $buttonsRow.append($cancelButtonEntry);

        $cancelButtonEntry.click(function ()
        {
            close();
        });

        $backgroundLayer.click(function ()
        {
            close();
        });

        KoralUI.drag($dialogDiv.get(0), $dialogHeader.get(0));


        var dw = $dialogDiv.width();
        var dh = $dialogDiv.height();

        var left = cx - dw / 2;
        var top = cy - dh / 2;
        $dialogDiv.css("left", left + "px");
        $dialogDiv.css("top", top + "px");
    },

    popup: function (text) {
        var m = $("<div class='infoMessage'></div>");
        var c = $("<div class='infoMessageContent'></div>");
        c.text(text);
        c.appendTo(m);
        m.appendTo(document.body);
        setTimeout(function () {
            m.remove();
        }, 2500);
    }
}

var KoralInternal = {
    koralScriptURL: null,
    originalDocument: null,
    articles: [],
    isEditMode: false,
    modifiedEditors: new Set(),
    figureIDtoNumber: {},
    equationIDtoNumber: {},
    spelling: null,
    
    koralUrl: function() {
        var scriptTags = document.getElementsByTagName('script');
        var url = null;
        for (var i = 0; i < scriptTags.length; i++)
        {
            var koralUrl = scriptTags[i].src;
            if (!koralUrl)
                continue;
            var i1 = koralUrl.indexOf("koral.js");
            if (i1 == -1)
                continue;
            KoralInternal.koralScriptURL = koralUrl;
            url = koralUrl.substring(0, i1);
            break;
        }
        if (url == null)
            throw "Invalid koral script referencing";
        return url;
    },

    importScripts: function (onLoadCallback) {
		document.writeln("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        document.writeln("<link rel='icon' href='data:;base64,iVBORw0KGgo='/>");

        var url = KoralInternal.koralUrl();

        var css = ["lib/codemirror/codemirror.css",
            "lib/codemirror/addon/display/fullscreen.css",
            "lib/code-prettify/prettify.css",
            "lib/photoswipe/photoswipe.min.css",
            "lib/photoswipe/default-skin.min.css",
            "lib/jstree/style.min.css",
            "koral.css"];
        for (var i = 0; i < css.length; i++)
        {
            document.writeln("<link rel='stylesheet' href='" + url + css[i] + "'/>");
        }

        var loadCounter = 2;
        var loadCallback = function () {
            loadCounter--;
            if (loadCounter <= 0)
                onLoadCallback();
        };

        function loadScripts(scripts, callback)
        {
            var head = document.getElementsByTagName('head')[0];
            var loadCounter_ = scripts.length;
            var loadCallback_ = function () {
                loadCounter_--;
                if (loadCounter_ <= 0)
                    callback();
            };
            for (var i = 0; i < scripts.length; i++)
            {
                var script = document.createElement('script');
                script.type = 'text/javascript';
                script.src = url + scripts[i];
                script.onreadystatechange = loadCallback_;
                script.onload = loadCallback_;
                head.appendChild(script);
            }
        }

        var scriptsFirstPass =
                ["lib/mathjax/MathJax.js?config=TeX-MML-AM_CHTML", // online at https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-MML-AM_CHTML
                    "lib/jquery/jquery-2.2.4.min.js", // online at https://ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js
                    "lib/bibtex/BibTex.js",
                    "lib/papaparse/papaparse.min.js",
                    "lib/code-prettify/prettify.js",
                    "lib/d3/d3.min.js", // online at https://d3js.org/d3.v5.min.js
                    "lib/codemirror/codemirror.js", // 5.25.2
                    "lib/photoswipe/photoswipe.min.js", // online at https://cdnjs.cloudflare.com/ajax/libs/photoswipe/4.1.1/photoswipe.min.js
                    "lib/photoswipe/photoswipe-ui-default.min.js",
                    "lib/chance/chance.min.js" // online at http://chancejs.com/chance.min.js (version 1.0.4)
                ];
        var scriptsSecondPass =
                ["lib/codemirror/mode/xml/xml.js",
                    "lib/codemirror/mode/javascript/javascript.js",
                    "lib/codemirror/mode/css/css.js",
                    "lib/codemirror/mode/htmlmixed/htmlmixed.js",
                    "lib/codemirror/mode/python/python.js",
                    "lib/codemirror/mode/r/r.js",
                    "lib/codemirror/mode/octave/octave.js",
                    "lib/codemirror/addon/display/fullscreen.js",
                    "lib/jstree/jstree.min.js", // 3.3.4
                    "lib/codemirror/spelling/spell-checker.js",
                    "lib/codemirror/spelling/typo.js"
                ];

        // html dom needs to be loaded completely && all external scripts are loaded before we continue
        window[addEventListener ? 'addEventListener' : 'attachEvent'](addEventListener ? 'load' : 'onload', loadCallback);
        loadScripts(scriptsFirstPass, function () {
            loadScripts(scriptsSecondPass, loadCallback);
        });
    },

    initDocument: function () {
        var d = document.documentElement.cloneNode(true);
        var articles = d.getElementsByTagName("article");
        for (var i = 0; i < articles.length; i++)
        {
            while (articles[i].firstChild)
            {
                articles[i].removeChild(articles[i].firstChild);
            }
        }

        var nodesToRemove = [];

        var scripts = d.getElementsByTagName("script");
        for (var i = 0; i < scripts.length; i++)
        {
            var href = scripts[i].getAttribute("src");
            if (href != null && href.indexOf("koral.js") != -1)
            {
                continue;
            }
            nodesToRemove.push(scripts[i]);
        }

        var links = d.getElementsByTagName("head")[0].getElementsByTagName("link");
        for (var i = 0; i < links.length; i++)
        {
            nodesToRemove.push(links[i]);
        }

        var metas = d.getElementsByTagName("head")[0].getElementsByTagName("meta");
        for (var i = 0; i < metas.length; i++)
        {
            if ("viewport" === metas[i].getAttribute("name")) nodesToRemove.push(metas[i]);
        }

        var styles = d.getElementsByTagName("head")[0].getElementsByTagName("style");
        for (var i = 0; i < styles.length; i++)
        {
            if (styles[i].childNodes.length > 0 && styles[i].childNodes[0].nodeValue != null)
            {
                var v = styles[i].childNodes[0].nodeValue;
                if (v.indexOf(".MathJax_") >= 0 || v.indexOf("#MathJax_") >= 0 || v.indexOf(".MJX_Assistive_MathML") >= 0)
                {
                    nodesToRemove.push(styles[i]);
                }
            }
        }

        var divs = d.getElementsByTagName("div");
        for (var i = 0; i < divs.length; i++)
        {
            if (divs[i].getAttribute("id") == "MathJax_Message")
            {
                nodesToRemove.push(divs[i]);
            }
        }

        for (var i = 0; i < nodesToRemove.length; i++)
        {
            nodesToRemove[i].parentNode.removeChild(nodesToRemove[i]);
        }

        // empty new line nodes in header after removing script nodes
        var head = d.getElementsByTagName("head")[0];
        var previousEmpty = false;
        nodesToRemove = [];
        for (var i = 0; i < head.childNodes.length; i++)
        {
            var c = head.childNodes[i];
            if (c.nodeType == 3 && c.nodeValue.trim().length == 0) // text node
            {
                if (previousEmpty)
                {
                    nodesToRemove.push(c);
                }
                previousEmpty = true;
            } else
            {
                previousEmpty = false;
            }
        }
        for (var i = 0; i < nodesToRemove.length; i++)
        {
            nodesToRemove[i].parentNode.removeChild(nodesToRemove[i]);
        }

        KoralInternal.originalDocument = d;
    },

    init: function () {
        KoralInternal.initDocument();
        MathJax.Hub.Config({
        	tex2jax: {inlineMath: [['$', '$'], ['\\(', '\\)']]},
            skipStartupTypeset: true,
        	"HTML-CSS": { linebreaks: { automatic:true }}});
        KoralInternal.initSpelling();

        $("article").each(function (index, value)
        {
            KoralInternal.articles.push(new KoralArticle(value));
        });
        if ($("article").length)
        {
            KoralInternal.addMenu();
        }
        KoralInternal.slides();
        KoralInternal.addBrowser();
        KoralInternal.addFileViewer();
        KoralInternal.addSignIn();
    },
    
    initSpelling: function() {
    	function loadTypo(affPath, dicPath) {
    		return new Promise(function(resolve, reject) {
    			var xhr_aff = new XMLHttpRequest();
    			xhr_aff.open('GET', affPath, true);
    			xhr_aff.onload = function() {
    				if (xhr_aff.readyState === 4 && xhr_aff.status === 200) {
    					//console.log('aff loaded');
    					var xhr_dic = new XMLHttpRequest();
    					xhr_dic.open('GET', dicPath, true);
    					xhr_dic.onload = function() {
    						if (xhr_dic.readyState === 4 && xhr_dic.status === 200) {
    							//console.log('dic loaded');
    							resolve(new Typo('en_US', xhr_aff.responseText, xhr_dic.responseText, { platform: 'any' }));
    						} else {
    							//console.log('failed loading dic');
    							reject();
    						}
    					};
    					//console.log('loading dic');
    					xhr_dic.send(null);
    				} else {
    					//console.log('failed loading aff');
    					reject();
    				}
    			};
    			//console.log('loading aff');
    			xhr_aff.send(null);
    		});
    	}
    	
    	// from: https://discuss.codemirror.net/t/inputstyle-contenteditable-we-may-hope-for-browser-spell-checking/608/6
    	// https://plnkr.co/edit/0y1wCHXx3k3mZaHFOpHT?p=preview
    	
    	var url = KoralInternal.koralUrl();
    	KoralInternal.spelling = loadTypo(url + "lib/codemirror/spelling/en_US.aff", url + "lib/codemirror/spelling/en_US.dic");
    },

    addBrowser: function() {
        var fileSizeLabel = size => {
            s = size;
            var ext = ["", "k", "M", "G", "T"];
            var i=0;
            for (; i<ext.length; i++)
            {
                if (s < 1000) break;
                s = s / 1000.0;
            }
            s = Math.round(s * 100) / 100;
            return s + ext[i];
        };
        var d2 = val => ('0' + val).substr(-2);
        var fileDateLabel = timestamp => {
            var date = new Date(timestamp);
            var y = "" + date.getFullYear();
            var m = d2(date.getMonth()+1);
            var d = d2(date.getDate());
            var H = d2(date.getHours());
            var M = d2(date.getMinutes());
            var S = d2(date.getSeconds());
            return y + "/" + m + "/" + d + " " + H + ":" + M + ":" + S; 
        }


        var browser = $(".browser");
        if (browser.length)
        {
            var path = window.location.pathname;
            var i1 = window.location.pathname.lastIndexOf("/");
            path = path.substring(0, i1 + 1);
            $("<h3></h3>")
            .appendTo(browser)
            .text(path);

            var p = path.substring(0, path.length - 1);
            i1 = p.lastIndexOf("/");
            if (i1 > 0)
            {
                $("<a></a>")
                .css("margin-left", "1.5rem")
                .appendTo(browser)
                .text("[...]")
                .attr("href", p.substring(0, i1 + 1));
            }

            $.getJSON($("#files").attr("src"), function(data) 
            {
                if (!data || data.length == 0) return;

                var tree = $("<div></div>").appendTo(browser);
                var ul_ = $("<ul></ul>").appendTo(tree);

                function addFileEntry(ul, entry, parentPath, level)
                {
                    var li = $("<li></li>").appendTo(ul);

                    var row = $("<div></div>")
                    .css("display", "table")
                    .appendTo(li);
                    
                    var w = level == 0 ? "240px" : "calc(240px - " + (1.5*level) + "rem)";
                    var col1 = $("<div></div>")
                    .css("display", "table-cell")
                    .css("width", w)
                    .css("min-width", w)
                    .css("max-width", w)
                    .css("white-space", "nowrap")
                    .css("overflow", "hidden")
                    .css("text-overflow", "ellipsis")
                    .appendTo(row);

                    $("<a></a>")
                    .appendTo(col1)
                    .attr("href", parentPath + (entry.files ? entry.name : entry.name + "?action=edit"))
                    .text(entry.name)
                    .attr("title", entry.name);

                    if (!entry.files)
                    {
                        $("<div></div>")
                        .css("display", "table-cell")
                        .css("width", "40px")
                        .appendTo(row)
                        .css("margin-right", "1rem")
                        .css("font-family", "monospace")
                        .css("text-align", "right")
                        .text(fileSizeLabel(entry.size));

                        $("<div></div>")
                        .css("display", "table-cell")
                        .css("width", "140px")
                        .appendTo(row)
                        .css("margin-left", "1.5rem")
                        .css("font-family", "monospace")
                        .css("text-align", "right")
                        .text(fileDateLabel(entry.lastModified));
                    }

                    if (entry.files)
                    {
                        var ulChild = $("<ul></ul>").appendTo(li);
                        for (var i=0; i<entry.files.length; i++)
                        {
                            addFileEntry(ulChild, entry.files[i], parentPath + entry.name, level + 1);
                        }
                    }
                }

                for (var i=0; i<data.length; i++)
                {
                    addFileEntry(ul_, data[i], "", 0);
                }

                tree.jstree({
                    "core": {
                        "themes":{
                            "icons":false,
                            "dots":false
                        }
                    }
                }).bind("select_node.jstree", function (e, data) {
                    if (data.event.target.href) window.location = data.event.target.href;
                });
            });
        }
    },

    addFileViewer: function() {
        var viewer = $(".viewer");
        if (viewer.length && viewer.data("url"))
        {
            var header = 
            $("<div></div>")
            .appendTo(viewer)
            .css("margin-bottom", "1rem");

            var path = window.location.pathname;
            var i1 = window.location.pathname.lastIndexOf("/");
            path = path.substring(i1 + 1);
            $("<h3></h3>")
            .css("display", "inline")
            .appendTo(header)
            .text(path);

            var sizeLimit = 1000000;
            var lineLimit = 2000;

            var url = viewer.data("url");
            var size = parseInt(viewer.data("size"));
            if (size > sizeLimit) {
                url += "?limit=" + sizeLimit;
            }
            $.ajax({
                url: url,
                dataType: "text"
            }).done(function(data) 
            {
                var text = data;
                var lines = text.split("\n");
                if (lines.length > lineLimit)
                {
                    lines = lines.slice(0, lineLimit);
                    lines.push("[..]");
                    lines.push("Content display limited to first " + lineLimit + " lines.");
                    text = lines.join("\n");
                }
                else if (size > sizeLimit)
                {
                    text += "\n[..]\nContent display limited to first " + sizeLimit + " bytes.";
                }
                var readOnly = size > sizeLimit || lines.length > lineLimit;

                $("<a></a>")
                .css("display", "inline")
                .css("margin-left", "2rem")
                .appendTo(header)
                .text(path.toUpperCase().endsWith(".HTML") ? "View" : "Download")
                .attr("href", window.location.pathname);

                var codearea = CodeMirror(viewer.get(0), {
                    value: text,
                    lineNumbers: true,
                    lineWrapping: true,
                    indentWithTabs: true,
                    indentUnit: 4,
                    readOnly:readOnly });

                if (!readOnly)
                {
                    $("<a></a>")
                    .css("display", "inline")
                    .css("margin-left", "2rem")
                    .appendTo(header)
                    .text("Save")
                    .attr("href", "#")
                    .click(function(e) 
                    {
                        e.preventDefault();
                        $.ajax({
                            type: "PUT",
                            url: url + "?action=fileCreationOrUpdate",
                            data: codearea.getValue(),
                            success: function (data) {
                                KoralUI.popup("Saved successfully.");
                            },
                            dataType: "text"
                        }).fail(function () {
                            KoralUI.popup("Saving failed.");
                        });
                    });
                }
            });
        }
    },

    addMenu: function () {
        var menuEntries = [{label: "Edit", onclick: KoralInternal.toggleEditMode, id: "editToggle"}];

        if (window.location.protocol !== "file:")
        {
            var saver = function() { KoralInternal.checkModification(KoralInternal.saveOnServer); };
            menuEntries.push({label: "Save", onclick: saver });
            
            var comitter = function() { KoralInternal.checkModification(KoralInternal.commitOnServer); };
            menuEntries.push({label: "Save and Commit", onclick: comitter});
            
            menuEntries.push({label: "History", onclick: KoralInternal.history});
        }
        
        menuEntries.push({label: "Navigate", onclick: KoralInternal.navigate});
        menuEntries.push({label: "Document statistics", onclick: KoralInternal.stats});
        menuEntries.push({label: "Download HTML", onclick: KoralInternal.downloadHTML});
        menuEntries.push({label: "Download figures", onclick: KoralInternal.downloadFigures});
        menuEntries.push({label: "Print / PDF", onclick: KoralInternal.exportAsPDF});
        //menuEntries.push({ label:"Export as LaTeX", onclick: KoralInternal.exportAsLatex });
        
        KoralUI.menu(menuEntries);
    },

    activateEditing: function (article, paragraph) {
        var hLeft = paragraph.leftCol.height();

        var codearea = CodeMirror(paragraph.rightCol.get(0), {
            value: paragraph.domStr,
            lineNumbers: true,
            lineWrapping: true,
            indentWithTabs: true,
            indentUnit: 4,
            smartIndent: false,
            mode: "htmlmixed",
            extraKeys: {
                "F11": function (cm) {
                    cm.setOption("fullScreen", !cm.getOption("fullScreen"));
                },
                "Esc": function (cm) {
                    if (cm.getOption("fullScreen"))
                        cm.setOption("fullScreen", false);
                },
                "Ctrl-Enter": function (cm) {
                    cm.article.update(cm.paragraph, cm.getValue());
                    KoralInternal.modifiedEditors.delete(cm);
                    if (KoralInternal.modifiedEditors.size == 0) KoralUI.removeUpdateButton();
                }
            }
        });
        codearea.article = article;
        codearea.paragraph = paragraph;
        $(paragraph.row).find(".editRightCol, .editLeftCol").toggleClass("stippledTop", true);
        codearea.on("change", function(cm) {
            if (KoralInternal.modifiedEditors.size == 0)
            {
                KoralUI.updateButton(function() 
                {
                    for (let cm of KoralInternal.modifiedEditors)
                    {
                        cm.article.update(cm.paragraph, cm.getValue());
                    }
                    KoralInternal.modifiedEditors = new Set();
                });      
            }
            KoralInternal.modifiedEditors.add(cm);
        });

        var hRight = paragraph.rightCol.height();
        if (hRight > hLeft) codearea.setSize(null, Math.max(hLeft, Math.min(hRight, 150)));
        
        KoralInternal.spelling.then(typo => startSpellCheck(codearea, typo));
    },

    updateIDs: function () {
        KoralInternal.figureIDtoNumber = {};
        $("figure").each(function (index, value)
        {
            var id = $(this).attr("id");
            if (id != null)
            {
                KoralInternal.figureIDtoNumber[id] = index + 1;
            }
        });

        KoralInternal.equationIDtoNumber = {};
        $(".equation").each(function (index, value)
        {
            var id = $(this).attr("id");
            if (id != null)
            {
                KoralInternal.equationIDtoNumber[id] = index + 1;
            }
        });
    },

    loadReferences: function (domPart) {
        var part = $(domPart);
        part.empty();
        var bibhtml = $("<ol></ol>").appendTo(part);

        function parseBib(data)
        {
            var bibtex = new BibTex();
            bibtex.content = data;
            bibtex.parse();
            bibIDtoBib = {};
            for (var i = 0; i < bibtex.data.length; i++)
            {
                var bib = bibtex.data[i];
                bibIDtoBib[bib.cite] = bib;
            }

            bibIDtoPosition = {};
            usedBibs = [];
            $(".editLeftCol").each(function (index, value)
            {
                $(this).find("a").each(function (index, value)
                {
                    var link = $(this).attr('href');
                    if (link == null)
                        return;
                    if (!link.startsWith("#"))
                        return;
                    var targetID = link.substring(1);
                    var linkResolved = false;

                    // bib reference
                    var bib = bibIDtoBib[targetID];
                    if (bib != null)
                    {
                        var position = bibIDtoPosition[bib.cite];
                        if (position == null)
                        {
                            position = usedBibs.length + 1;
                            usedBibs[position - 1] = bib;
                            bibIDtoPosition[bib.cite] = position;
                        }
                        $(this).text("[" + position + "]");
                        $(this).addClass("tooltip");
                    }
                });
            });

            for (var i = 0; i < usedBibs.length; i++)
            {
                var bib = usedBibs[i];
                var authors = "";
                for (var j = 0; j < bib.author.length; j++)
                {
                    var a = bib.author[j];
                    authors += a.first + " " + a.last;
                    if (j < bib.author.length - 1)
                    {
                        if (j == bib.author.length - 2)
                            authors += " and ";
                        else
                            authors += ", ";
                    }
                }
                var journal = bib.journal != null ? bib.journal + "." : "";
                var refText = "<i>" + authors + " </i>(" + bib.year + "): <b>" + bib.title + ".</b> " + journal;
                bibhtml.append("<li id='" + bib.cite + "'>" + refText + "</li>");

                // add tooltip to each link
                $("a[href='#" + bib.cite + "']").each(function (index, value)
                {
                    //$(this).attr("title", refText);
                    $("<span></span>")
                    .appendTo($(this))
                    .addClass("tooltiptext")
                    .append(refText);

                    if (bib.url)
                    {
                        $(this).attr("href", bib.url);
                        $(this).attr("target", "_blank");
                    }
                });
            }
        }

        var d = part.data("source");
        if (d != null)
        {
            parseBib(d);
        } else
        {
            var url = part.data("url");
            jQuery.get(url, parseBib);
        }
    },

    loadSources: function (domPart) {
        var part = $(domPart);
        part.empty();
        var tree = $("<div></div>").appendTo(part);
        var ul_ = $("<ul></ul>").appendTo(tree);

        function sourceFile(sourceDesc)
        {
            if (sourceDesc.func == null) return null;
            if (sourceDesc.env && sourceDesc.env.toUpperCase() === "JAVA")
            {
                var i1 = sourceDesc.func.lastIndexOf(".");
                var i2 = sourceDesc.func.indexOf(":");
                if (i1 == -1 || i2 == -1) return null;
                return { "name": sourceDesc.func.substring(i1 + 1, i2) + ".java", 
                         "url": "src/main/java/" + sourceDesc.func.substring(0, i2).split(".").join("/") + ".java" };
            }

            var i1 = sourceDesc.func.lastIndexOf("/");
            if (i1 == -1) i1 = 0; 
            var i2 = sourceDesc.func.indexOf(":");
            if (i2 == -1) return null;
            return { "name": sourceDesc.func.substring(i1 + 1, i2),
                     "url": sourceDesc.func.substring(0, i2)};
        }

        function addTo(ul, url, desc, sourceMap)
        {
            var name = url;
            var i1 = name.lastIndexOf("/");
            if (i1 != -1) name = name.substring(i1 + 1);

            var li = $("<li></li>").appendTo(ul);

            var row = $("<span></span>").appendTo(li);
            $("<a></a>")
            .appendTo(row)
            .attr("href", url + "?action=edit")
            .text(name);

            var func = sourceFile(desc);
            if (func)
            {
                $("<span> ← </span>").appendTo(row);
                $("<a></a>")
                .appendTo(row)
                .attr("href", func.url + "?action=edit")
                .text(func.name);
            }

            if (desc.args != null)
            {
                var sources = [];
                for (var s in desc.args)
                {
                    if (desc.args[s].type != null 
                        && desc.args[s].type.toUpperCase() === "SOURCE" 
                        && desc.args[s].val != null)
                    {
                        sources.push(s);
                    }
                }

                if (sources.length > 0)
                {
                    var ul = $("<ul></ul>").appendTo(li);
                    for (var i=0; i<sources.length; i++)
                    {
                        var arg = desc.args[sources[i]];
                        var url_ = arg.val;
                        var desc_ = sourceMap[url_];
                        if (desc_ == null) continue;

                        addTo(ul, url_, desc_, sourceMap);
                    }
                }
            }
        }

        function parseSrc(sourceMap)
        {
            var sourceIDtoIndex = {};
            var usedSources = [];
            $(".editLeftCol").each(function (i, v)
            {
                // find references to sources in text
                $(this).find("[data-url]").each(function (index, value)
                {
                    var url = $(this).data("url");
                    if (url == null) return;
                    if (sourceMap[url] != null)
                    {
                        var position = sourceIDtoIndex[url];
                        if (position == null)
                        {
                            position = usedSources.length;
                            usedSources[position] = url;
                            sourceIDtoIndex[url] = position;
                        }
                    }
                });
            });

            // resolved sources
            for (var i=0; i<usedSources.length; i++)
            {
                var url = usedSources[i];
                var desc = sourceMap[url];
                addTo(ul_, url, desc, sourceMap);
            }

            tree.jstree({
                "core": {
                    "themes":{
                        "icons":false,
                        "dots":false
                    }
                }
            }).bind("select_node.jstree", function (e, data) {
                if (data.event.target.href && 
                    !data.event.target.href.endsWith("#")) // jstree internal link 
                {
                    window.open(data.event.target.href);
                }
            });
        }

        var d = part.data("source");
        if (d != null)
        {
            parseSrc(d);
        } else
        {
            var url = part.data("url");
            jQuery.get(url, parseSrc);
        }
    },

    toggleFullScreen: function(activatedSlide) {
        var pswpElement = document.querySelectorAll('.pswp')[0];

        var w = 640;
        var h = 360;
        var s1 = screen.width / w;
        var s2 = screen.height / h;
        var scale = Math.min(s1, s2);

        var slides =  $(".slide").not(".fullscreen").toArray();
        var items = [];
        var slideIndex = -1;
        for (var i=0; i<slides.length; i++)
        {
            if (slides[i] === activatedSlide) slideIndex = i;

            var container = $("<div></div>") 
            container.css("transform-origin", "0% 0%");
            container.css("transform", "scale(" + scale + ")");

            var slide = slides[i].cloneNode(true);
            $(slide).css("background-color", "rgb(255,255,255)");
            $(slide).css("margin", "0");
            $(slide).addClass("fullscreen");
            container.append(slide);

            var item = {};
            item["html"] = container.wrap('<div/>').parent().html();
            item["w"] = w*scale;
            item["h"] = h*scale;
            items[i] = item;
        }

        var options = {
            history: false,
            focus: false,
            shareEl: false,
            loop: false,
            showAnimationDuration: 0,
            hideAnimationDuration: 0,
            timeToIdle: 1000
        };
    
        var gallery = new PhotoSwipe(pswpElement, PhotoSwipeUI_Default, items, options);
        gallery.init();
        if (slideIndex > 0) gallery.goTo(slideIndex);
        $(".pswp__button--fs").click();

        var slideChange = () => {
            $.ajax({
                type: "GET",
                url: window.location.pathname + "?action=setSlide&slide=" + gallery.getCurrentIndex(),
                success: function (data) {},
            });
        };
        slideChange();
        gallery.listen('afterChange', slideChange);
       
        /*
        var container = document.getElementById("fullscreenContainer");

        if (!document.fullscreenElement && !document.mozFullScreenElement &&
            !document.webkitFullscreenElement && !document.msFullscreenElement) 
        {
            if (container.requestFullscreen) container.requestFullscreen();
            else if (container.msRequestFullscreen) container.msRequestFullscreen();
            else if (container.mozRequestFullScreen) container.mozRequestFullScreen();
            else if (container.webkitRequestFullscreen) container.webkitRequestFullscreen(Element.ALLOW_KEYBOARD_INPUT);

            $(activatedSlide).addClass("fullscreen");
            var copy = $(activatedSlide).clone();
            copy.appendTo(container);
            copy.click(function() {
                KoralInternal.toggleFullScreen(this);
            });

            // instead of using :-webkit-full-screen
            var w = 640;
            var h = 360;
            var s1 = screen.width / w;
            var s2 = screen.height / h;
            var s = Math.min(s1, s2);
            $(container).css("transform", "scale(" + s + ")");
        } 
        else 
        {
            if (document.exitFullscreen) document.exitFullscreen();
            else if (document.msExitFullscreen) document.msExitFullscreen();
            else if (document.mozCancelFullScreen) document.mozCancelFullScreen();
            else if (document.webkitExitFullscreen) document.webkitExitFullscreen();

            $(activatedSlide).removeClass("fullscreen");
        }*/
    },

    currentSlideIndex: 0,
    slideListener: function()
    {
        window.setTimeout(function() {
            $.ajax({
                type: "GET",
                url: window.location.pathname + "?action=getSlide&slide=" + KoralInternal.currentSlideIndex,
                success: function (data) {
                    KoralInternal.currentSlideIndex = parseInt(data);
                    var slides = $(".slide").toArray();
                    if (slides.length > 0)
                    {
                        var slide = slides[Math.max(0, Math.min(slides.length - 1, KoralInternal.currentSlideIndex))];
                        slide.scrollIntoView(true);
                    }
                    KoralInternal.slideListener();
                },
                dataType: "text"
            }).fail(function () {
                KoralInternal.slideListener();
            });
        }, 500);
    },

    slides: function()
    {
        $("<div class='pswp' tabindex='-1' role='dialog' aria-hidden='true'>" +
            "<div class='pswp__bg'></div>" +
            "<div class='pswp__scroll-wrap'>" +
                "<div class='pswp__container'>" +
                    "<div class='pswp__item'></div>" +
                    "<div class='pswp__item'></div>" +
                    "<div class='pswp__item'></div>" +
                "</div>" +
                "<div class='pswp__ui pswp__ui--hidden'>" +
                    "<div class='pswp__top-bar'>" +
                        "<div class='pswp__counter'></div>" +
                        "<button class='pswp__button pswp__button--close' title='Close (Esc)'></button>" +
                        "<button class='pswp__button pswp__button--fs' title='Toggle fullscreen'></button>" +
                        "<button class='pswp__button pswp__button--zoom' title='Zoom in/out'></button>" +
                        "<div class='pswp__preloader'>" +
                            "<div class='pswp__preloader__icn'>" +
                              "<div class='pswp__preloader__cut'>" +
                                "<div class='pswp__preloader__donut'></div>" +
                              "</div>" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                    "<div class='pswp__share-modal pswp__share-modal--hidden pswp__single-tap'>" +
                        "<div class='pswp__share-tooltip'></div>" +
                    "</div>" +
                    "<button class='pswp__button pswp__button--arrow--left' title='Previous (arrow left)'></button>" +
                    "<button class='pswp__button pswp__button--arrow--right' title='Next (arrow right)'></button>" +
                    "<div class='pswp__caption'>" +
                        "<div class='pswp__caption__center'></div>" +
                    "</div>" +
                "</div>" +
            "</div>" +
        "</div>").appendTo($("body").first());
        if (Koral.getUrlParameter("slideListener") != null) KoralInternal.slideListener();

        /*
        $("body").append($("<div style='margin:0;padding:0' id='fullscreenContainer'></div>"));

        function exitHandler()
        {
            var fullscreenElement = document.fullscreenElement || document.mozFullScreenElement || document.webkitFullscreenElement;
            if (fullscreenElement == null)
            {
                $("#fullscreenContainer").empty();
            }

        }

        document.addEventListener('webkitfullscreenchange', exitHandler, false);
        document.addEventListener('mozfullscreenchange', exitHandler, false);
        document.addEventListener('fullscreenchange', exitHandler, false);
        document.addEventListener('MSFullscreenChange', exitHandler, false);

        $(document).keydown(function(event) 
        {
            if (event.keyCode == 37 || event.keyCode == 39)
            {
                var slides = $(".slide").toArray();
                for (var i=0; i<slides.length; i++)
                {
                    if ($(slides[i]).hasClass("fullscreen"))
                    {
                        if (event.keyCode == 37 && i > 0) // left arrow
                        {
                            var newslide = slides[i-1];
                            $(slides[i]).removeClass("fullscreen");
                            $(newslide).addClass("fullscreen");
                            var container = $("#fullscreenContainer")
                            var copy = $(newslide).clone();
                            container.empty();
                            copy.appendTo(container);
                            copy.click(function() {
                                KoralInternal.toggleFullScreen(this);
                            });
                        }
                        else if (event.keyCode == 39 && i < slides.length - 1) // right arrow
                        {
                            var newslide = slides[i+1];

                            $(slides[i]).removeClass("fullscreen");
                            $(newslide).addClass("fullscreen");
                            var container = $("#fullscreenContainer")
                            var copy = $(newslide).clone();
                            container.empty();
                            copy.appendTo(container);
                            copy.click(function() {
                                KoralInternal.toggleFullScreen(this);
                            });
                        }
                        break;
                    }
                }
            }
        });
        */
    },

    toggleEditMode: function ()
    {
        KoralInternal.isEditMode = !KoralInternal.isEditMode;

        if (KoralInternal.isEditMode)
        {
            for (var i = 0; i < KoralInternal.articles.length; i++)
            {
                var a = KoralInternal.articles[i];
                for (var j = 0; j < KoralInternal.articles[i].paragraphs.length; j++)
                {
                    var p = a.paragraphs[j];
                    KoralInternal.activateEditing(a, p);
                }
            }
            d3.select("#editToggle").text("Read")
        } else
        {
            for (var i = 0; i < KoralInternal.articles.length; i++)
            {
                for (var j = 0; j < KoralInternal.articles[i].paragraphs.length; j++)
                {
                    var p = KoralInternal.articles[i].paragraphs[j];
                    p.rightCol.children().remove();
                }
            }
            d3.selectAll(".editRightCol, .editLeftCol").classed("stippledTop", false);
            d3.select("#editToggle").text("Edit");
        }
    },
    
    /*
    signIn: function ()
    {
        var p = $("<div></div>").addClass("form", true);
        p.append($("<label for='username'>Username</label>"));
        var userField = $("<input autocapitalize='off' autocorrect='off' autofocus='autofocus' tabindex='1' id='username' style='width:100%;' type='text'/>");
        p.append(userField);
        
        p.append($("<label for='password'>Password</label>"));
        var pwdField = $("<input tabindex='2' id='password' style='width:100%;' type='password'/>");
        p.append(pwdField);

        var buttons = [{name: "Sign in", onclickfunc: function ()
        {
            $.ajax({
                type: "POST",
                url: "/?action=signIn",
                data: { username: $("#username").val(), password: $("#password").val() },
                success: function (data) 
                {
                	KoralUI.popup("Signed in.");
                },
                dataType: "json"
            }).fail(function () {
                KoralUI.popup("Sign in failed.");
            });
            return true;
        }}];
        KoralUI.dialog("Sign in", p.get(0), buttons);
    },
    */

    signIn: function(username, password, onsuccess)
    {
        try
        {
            $.ajax({
                type: "POST",
                url: "/?action=signIn",
                data: JSON.stringify({ "username": username,"password": password }),
                contentType : "application/json"
            }).done(function() {
                KoralUI.popup("Signed in successfully.");
                if (onsuccess != null) onsuccess();
            }).fail(function() {
                KoralUI.popup("Not signed in.");
            });
        }
        catch (e) {}
    },
    
    signUp: function ()
    {
        var p = $("<div></div>").addClass("form", true);
        p.append($("<label for='username'>Username</label>"));
        var userField = $("<input autocapitalize='off' autocorrect='off' autofocus='autofocus' tabindex='1' id='username' style='width:100%;' type='text'/>");
        p.append(userField);
        
        p.append($("<label for='password'>Password</label>"));
        var pwdField = $("<input tabindex='2' id='password' style='width:100%;' type='password'/>");
        p.append(pwdField);

        var buttons = [{name: "Sign up", onclickfunc: function ()
        {
            $.ajax({
                type: "POST",
                url: "/?action=signUp",
                data: { username: $("#username").val(), password: $("#password").val(), signUpToken: Koral.getUrlParameter("signUpToken") },
                success: function (data) 
                {
                	KoralUI.popup("Signed up.");
                },
                dataType: "json"
            }).fail(function () {
                KoralUI.popup("Sign up failed.");
            });
            return true;
        }}];
        KoralUI.dialog("Sign up", p.get(0), buttons);
    },
    
    addSignIn: function()
    {
        if ($(".koralSignUp").length)
        {
        	KoralInternal.signUp();
        }
    },
    
    addProjectList: function() {
        $.getJSON($("#projects").attr("src"), function(data) 
        {
            if (!data || data.length == 0) return;

            var p = $(".koralProjects");
            p.empty();
            p.css("margin", "2rem");
            p.append($("<h3></h3>").text("Projects"));
            
            if (data.authoredProjects)
            {
            	for (var i=0; i<data.authoredProjects.length; i++)
            	{
            		var name = data.authoredProjects[i];
            		var link = $("<p></p>");
            		link.append($("<a></a>").attr("href", name + "/").text(name));
            		p.append(link);
            	}
            }
        });
    },

    history: function ()
    {
        window.open(window.location.pathname + "?action=history", "_blank");
    },

    checkModification: function(onOkCallback)
    {
        if (KoralInternal.modifiedEditors.size == 0)
        {
            onOkCallback();
            return;
        }

        var p = $("<div>Changes in one or more text editors are not executed yet.<br>Only updated content will be saved.<br>To update content, click on the 'run' button before saving.</div>");
        var buttons = [{name: "Continue saving", onclickfunc: function() {
            onOkCallback(); 
            return true;
        } }];
        KoralUI.dialog("Warning", p.get(0), buttons);
    },

    saveOnServer: function ()
    {
        KoralInternal.getDocumentLocal(function (str) {
            $.ajax({
                type: "PUT",
                url: window.location.pathname + "?action=fileCreationOrUpdate",
                data: str,
                success: function (data) {
                    KoralUI.popup("Saved successfully.");
                },
                dataType: "text"
            }).fail(function () {
                KoralUI.popup("Saving failed.");
            });
        });
    },

    commitOnServer: function ()
    {
        var author = Koral.getUrlParameter("author");
        if (author == null)
            author = "";

        var p = $("<div></div>").addClass("form", true);
        //p.append($("<label for='authorField'>Author: </label>"));
        //var authorField = $("<input id='authorField' style='width:100%;' type='text'/>").val(author);
        //p.append(authorField);
        p.append($("<label for='messageField'>Commit message: </label>"));
        var messageField = $("<textarea id='messageField' rows='4' cols='40'/>");
        p.append(messageField);

        var buttons = [{name: "Save", onclickfunc: function ()
                {
                    //author = authorField.val();
                    //author = author.trim();
                    //if (author.length > 0)
                    //    Koral.setUrlParameter("author", author);
                    var message = messageField.val();

                    KoralInternal.getDocumentLocal(function (str) {
                        $.ajax({
                            type: "PUT",
                            url: window.location.pathname + "?action=fileCreationOrUpdate",
                            data: str,
                            success: function (data) 
                            {
                                KoralInternal.getDocumentLocal(function (str) {
                                    $.ajax({
                                        type: "POST",
                                        url: window.location.pathname + "?action=commit&message=" + encodeURIComponent(message),
                                        success: function (data) {
                                            KoralUI.popup("Saved and committed.");
                                        },
                                        dataType: "text"
                                    }).fail(function () {
                                        KoralUI.popup("Committing failed.");
                                    });
                                });
                            },
                            dataType: "text"
                        }).fail(function () {
                            KoralUI.popup("Saving failed.");
                        });
                    });
                    return true;
                }}];
        KoralUI.dialog("Save and commit", p.get(0), buttons);
    },
    
    navigate: function()
    {
    	var p = $("<div class='navigator'></div>");
        $(".editLeftCol h2, .editLeftCol h3, .editLeftCol h4").each(function (index, value)
        {
        	$("<div></div>")
        	.text($(this).text())
        	.addClass(this.nodeName)
        	.appendTo(p);
        });	
    	KoralUI.dialog("Navigate", p.get(0), []);
    },
     
    wordCount: function()
    {
    	var countWords = function(s)
    	{
    		s = s.replace(/(^\s*)|(\s*$)/gi,"");//exclude  start and end white-space
    		s = s.replace(/[ ]{2,}/gi," ");//2 or more space to 1
    		s = s.replace(/\n /,"\n"); // exclude newline with a start spacing
    		return s.split(' ').length; 
    	}
    	
    	var counts = [0, 0, 0, 0]; // wordcount, wc - fig, charcount, cc - fig
        for (var i=0; i<KoralInternal.articles.length; i++)
        {
            var a = KoralInternal.articles[i];
            for (var j = 0; j < KoralInternal.articles[i].paragraphs.length; j++)
            {
                var p = a.paragraphs[j];
                if (p.leftCol.find(".skipWordCount").length > 0)
                {
                	continue;
                }
                
                var text = p.leftCol.text();
                counts[2] += text.length;
                counts[0] += countWords(text);
                
                p.leftCol.find("figcaption").each(function (index, value)
                {
                	var text = $(this).text();
                	counts[3] -= text.length;
                	counts[1] -= countWords(text);
                });
            }
        }
        
        counts[1] += counts[0];
        counts[3] += counts[2];
    	return counts;
    },
    
    stats: function()
    {
    	var counts = KoralInternal.wordCount();
    	
    	var p = $("<div class='navigator'></div>");
    	
    	$("<div></div>")
    	.text(counts[0] + " words (" + counts[1] + " without figure captions).")
    	.addClass(this.nodeName)
    	.appendTo(p);
    	
    	$("<div></div>")
    	.text(counts[2] + " characters (" + counts[3] + " without figure captions).")
    	.addClass(this.nodeName)
    	.appendTo(p);
        
        
    	KoralUI.dialog("Document statistics", p.get(0), []);
    },

    getDocumentLocal: function (callback)
    {
        var d = KoralInternal.originalDocument.cloneNode(true);
        var articles = d.getElementsByTagName("article");
        for (var i = 0; i < articles.length; i++)
        {
            for (var j = 0; j < KoralInternal.articles[i].paragraphs.length; j++)
            {
                var p = KoralInternal.articles[i].paragraphs[j];

                articles[i].appendChild(document.createTextNode("\n"));
                articles[i].appendChild($(p.domStr).get(0));
            }
            articles[i].appendChild(document.createTextNode("\n"));
        }
        callback(new XMLSerializer().serializeToString(document.doctype) + "\n" + d.outerHTML);
    },

    getDocumentRemote: function (callback)
    {
        $.ajax({
            url: document.location.href,
            async: true,
            dataType: "text",
            success: function (data)
            {
                var parts = [];

                var oldIndex = 0;
                var index = -1;
                var article = 0;
                while ((index = data.indexOf("<article", index + 1)) != -1)
                {
                    index = data.indexOf(">", index + 1);
                    parts.push(data.substring(oldIndex, index + 1));

                    for (var j = 0; j < KoralInternal.articles[article].paragraphs.length; j++)
                    {
                        var p = KoralInternal.articles[article].paragraphs[j];
                        parts.push("\n");
                        parts.push(p.domStr);
                    }
                    parts.push("\n");

                    article++;

                    index = data.indexOf("</article>", index + 1);
                    oldIndex = index;
                }
                parts.push(data.substring(oldIndex));
                var str = parts.join("");
                callback(str);
            }
        });
    },

    downloadHTML: function () {
        KoralInternal.getDocumentLocal(function (str) {
            var relURL = KoralInternal.koralScriptURL.replace(/^(?:\/\/|[^\/]+)*\//, "");
            var reg = new RegExp("src=\".*?" + relURL + "\"");
            str = str.replace(reg, "src=\"" + KoralInternal.koralScriptURL + "\"");

            var file = new Blob([str], {type: "text/html"});
            var url = URL.createObjectURL(file);
            var a = document.createElement("a");
            a.href = url;
            a.download = window.location.pathname.split("/").pop();
            document.body.appendChild(a);
            a.click();
            setTimeout(function () {
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
            }, 0);
        });
    },
    
    downloadFigures: function (figIDs) {
    	
    	// may not include everything because of CORS
    	var cssDeclaration_ = () =>
    	{
    		var css = "";
    		for (var i=0; i<document.styleSheets.length; i++)
    		{
				var rules = document.styleSheets[i].rules || document.styleSheets[i].cssRules;
				if (rules != null)
					for (var j=0; j<rules.length; j++)
					{
						css += rules[j].cssText + "\n";
					}
    		}
    		return css;
    	};
    	

    	var convertToPngAndDownload_wImages = (elem, downloadName, minSize, cssDeclaration) => 
    	{
        	var images = elem.querySelectorAll('image');
        	var total = images.length;
        	if (images.length == 0) convertToPngAndDownload(elem, downloadName, minSize, cssDeclaration);
        	var encoded = 0;
    		
        	// convert images within the svg to embedded data; otherwise they are not shown in the exported png 
        	var toDataURL = (image) => 
        	{
        	    var img = new Image();
        	    // CORS workaround, this won't work in IE<11
        	    // If you are sure you don't need it, remove the next line and the double onerror handler
        	    // First try with crossorigin set, it should fire an error if not needed
        	    img.crossOrigin = 'anonymous';

        	    img.onload = function() {
        	      // we should now be able to draw it without tainting the canvas
        	      var canvas = document.createElement('canvas');
        	      canvas.width = this.width;
        	      canvas.height = this.height;
        	      // draw the loaded image
        	      canvas.getContext('2d').drawImage(this, 0, 0);
        	      // set our <image>'s href attribute to the dataURL of our canvas
        	      image.setAttribute('href', canvas.toDataURL());
        	      
        	      // that was the last one
        	      if (++encoded === total) convertToPngAndDownload(elem, downloadName, minSize, cssDeclaration);
        	    };

        	    // No CORS set in the response		
        	    img.onerror = function() {
        	      // save the src
        	      var oldSrc = this.src;
        	      // there is an other problem
        	      this.onerror = function() {
        	        console.warn('failed to load an image at : ', this.src);
        	        if (--total === encoded && encoded > 0) convertToPngAndDownload(elem, downloadName, minSize, cssDeclaration);
        	      };
        	      // remove the crossorigin attribute
        	      this.removeAttribute('crossorigin');
        	      // retry
        	      this.src = '';
        	      this.src = oldSrc;
        	    };
        	    // load our external image into our img
        	    var href = image.getAttribute('href');
        	    // really weird bug that appeared since this answer was first posted
        	    // we need to force a no-cached request for the crossOrigin be applied
        	    img.src = href + (href.indexOf('?') > -1 ? + '&1': '?1');
        	};
        	
        	for (var i=0; i<images.length; i++) 
        	{
        	    toDataURL(images[i]);
        	}
    	};
    	
    	var convertToPngAndDownload = (elem, downloadName, minSize, cssDeclaration) => 
    	{
            var svg = $(elem);
    		var w = svg.attr("width");
    		var h = svg.attr("height");
    		if (w < minSize || h < minSize)
    		{
    			s = Math.max(minSize / w, minSize / h);
    			w = w*s;
    			h = h*s;
    			svg.attr("width", w);
    			svg.attr("height", h);
    		}
    		svg.css("background-color", "rgb(255,255,255)");
            //svg.find(".figureLetter").remove();
    		
    		$("<style></style>").text(cssDeclaration).appendTo(svg);
    		
    		var svgText = new XMLSerializer().serializeToString(svg.get(0));
    		var svgBlob = new Blob([svgText], {type:"image/svg+xml;charset=utf-8"});
    		
    		var canvas = $("<canvas></canvas>")
			.attr("width", w)
			.attr("height", h)
			.css("display", "none");
    		canvas.insertAfter(document.body);
    		var ctx = canvas.get(0).getContext("2d");
    		
    		var domURL = self.URL || self.webkitURL || self;
    		var url = domURL.createObjectURL(svgBlob);
    		var img = new Image();
    		img.onload =  function () {
    			ctx.drawImage(this, 0, 0);     
    			domURL.revokeObjectURL(url);
    			
    			var a = $("<a></a>")
				.attr("href", canvas.get(0).toDataURL())
				.attr("download", downloadName)
			
				a.insertAfter(document.body);
    			a.get(0).click();
    			canvas.remove();
    			a.remove();
    		};
    		img.src = url;
    	};
    	
//    	$("figure").each(function( index, element ) {
//    		$(element).data("index", index);
//    		$(element).data("count", 0);
//    	});
    	
		$.ajax({
			url: KoralInternal.koralUrl() + "koral.css",
			dataType: "text",
			success: function(cssDeclaration)
			{
		    	if (figIDs == null)
                {
                    $("svg").each( function( index, element )
                    {
                        var svg = $(element);
                        var fig = svg.closest("figure");
                        if (fig.length == 0) return;
                        
                        var id = svg.attr("id");
                        if (id == null) return;
                        
                        var name = id + ".png";
                        
                        var svgCopy = $(element).clone().get(0);
                        convertToPngAndDownload_wImages(svgCopy, name, 1080, cssDeclaration);
                    });
                }
                else
                {
                    if (!Array.isArray(figIDs))
                    {
                        figIDs = [figIDs];
                    }

                    for (var id of figIDs)
                    {
                        var svg = $("#" + id);
                        var name = id + ".png";
                        var svgCopy = svg.clone().get(0);
                        convertToPngAndDownload_wImages(svgCopy, name, 1080, cssDeclaration);
                    }
                }
			}
		});
    },

    exportAsPDF: function () {
        setTimeout(function () {
            window.print();
        }, 0);
    },

    exportAsLatex: function () {

    }
};


var KoralPlot = {
	svgScale: 1.0,	
		
    tableDefaults: {
        cols: undefined, // which columns to display; array of 0-based column indices
        toFixed: undefined // whether numbers shall be rounded; number of digits to be rounded
    },

    plotDefaults: {
        plotWidth: 240, // size of actual plot area
        plotHeight: 240,

        // examples:
        //xLabel: "x",
        //yLabel: "y",
        //xTransform: { type: "linear", min: 0, max: 1}   // { type:"log", min: 1e-7 max: 1}  //d3.scaleLinear().domain([0, 1]),
        //yTransform: d3.scaleLinear().domain([0, 1]),
        //xTicks:      [0,   0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1],
        //xTickLabels: ["0", "",  "",  "",  "", ".5", "",  "",  "",  "", "1"],
        //yTicks:      [0,   0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1],
        //yTickLabels: ["0", "",  "",  "",  "", ".5", "",  "",  "",  "", "1"],
        
        xTickLabelAngle:0,

        plotBorder: true,
        left: 56,
        right: 24,
        top: 24,
        bottom: 56,
        tickHalfLength: 4,
        tickHalfLengthSmall: 2,
        plotMargin: 12,

        legend: undefined // { "vert":0, "horiz":1, "label": undefined} // label optional
    },

    plotDrawDefaults: {
        rows: undefined, // row selector
        xData: 0,
        yData: 1,
        zData: 2,
        x2Data: undefined,
        y2Data: undefined,
        z2Data: undefined,
        type: "points", // line, errorBars, area, errorBarsAbs
        size: 2.0,
        color: "rgb(0, 0, 255)",
        errorBarThickness: 1.0,
        errorCrossBarWidth: 5.0,
        label: undefined, // { vert:0, horiz:0 }
        shape: "circle", // for points. choices: circle, rect
        realSize: undefined, // [1,1] width and height in data space
        colorMapper: undefined, // [0, 100], ["rgb(0,0,255)", "rgb(255,0,0)"]
        colorLegend: undefined, // { "label":"mylabel", "vert":0, "horiz":1 }
    },

    logTicks: function (min, max) {
        var superscripts = [
            "\u2070", // 0
            "\u00B9", // 1
            "\u00B2", // 2  ...
            "\u00B3",
            "\u2074",
            "\u2075",
            "\u2076",
            "\u2077",
            "\u2078",
            "\u2079"]; // 9
        var superscriptMinus = "\u207B";
        function p(val)
        {
            var pow10 = Math.floor(Math.log10(val));
            return [val / Math.pow(10, pow10), pow10];
        }

        var minp = p(min);
        minp[0] = Math.ceil(minp[0]);
        var maxp = p(max);
        maxp[0] = Math.floor(maxp[0]);

        var currentScale = minp[0];
        var current10 = minp[1];
        ticks = [];
        tickLabels = [];
        while (currentScale <= maxp[0] || current10 < maxp[1])
        {
            var t = currentScale * Math.pow(10, current10);
            ticks.push(t);

            var l = "";
            if (currentScale == 1) // main tick
            {
                if (current10 == 0) l = "1";
                else if (current10 == 1) l = "10";
                else if (current10 == -1) l = "0.1";
                else if (current10 == -2) l = "0.01";
                else
                {
                    var ea = Math.abs(current10);
                    l = "10" + (current10 < 0 ? superscriptMinus : "") +
                        (ea >= 100 ? superscripts[(ea / 100) % 10] : "") +
                        (ea >= 10 ? superscripts[(ea / 10) % 10] : "") +
                        superscripts[ea % 10];
                }
            }
            tickLabels.push(l);

            currentScale++;
            if (currentScale == 10)
            {
                currentScale = 1;
                current10++;
            }
        }

        return {ticks: ticks, tickLabels: tickLabels};
    },

    processPlots: function (domPart)
    {
        var part = $(domPart);

        if (part.find("figure").length == 0 && part.find("table").length == 0)
            return;

        // step 1: load all csv data
        var urls = {};
        part.find("[data-url]").each(function (index, value)
        {
            var url = $(this).data("url");
            urls[url] = true;
        });
        part.find("[data-generator]").each(function (index, value)
        {
            var code = $(this).data("generator");
            var data = eval(code);

            var url = $(this).data("url");
            if (url == null)
            {
                url = Koral.uuid();
                $(this).attr("data-url", url);
            }
            urls[url] = data;
        });
        part.find("[data-source]").each(function (index, value)
        {
            var dataSerialized = $(this).data("source");
            var lines = dataSerialized.match(/[^\r\n]+/g);
            var linesTrimmed = []
            for (var i = 0; i < lines.length; i++)
            {
                var l = lines[i].trim();
                if (l.length > 0)
                    linesTrimmed.push(l);
            }
            var data = Papa.parse(linesTrimmed.join("\n"), {header: true, skipEmptyLines: true}).data;

            var url = $(this).data("url");
            if (url == null)
            {
                url = Koral.uuid();
                $(this).attr("data-url", url);
            }
            urls[url] = data;
        });

        var ajaxes = [];
        for (var url in urls)
        {
            var url_ = url;
            if (urls[url_] == true) // no local data source
                ajaxes.push(jQuery.ajax({
                    url: url_,
                    success: function (data)
                    {
                        var csv = Papa.parse(data, {header: true, skipEmptyLines: true});
                        urls[this.url] = csv.data;
                    },
                    async: true
                }));
        }

        // step 3: layout tables + draw plots
        $.when.apply($, ajaxes).done(function () // // it's ok for ajaxes to be empty array
        {
            part.find("table").each(function (index, value)
            {
                var url = $(this).data("url");
                if (url == null)
                    return;
                var data = urls[url];

                var colNames = Object.getOwnPropertyNames(data[0]);

                var conf = Koral.getJsonDataAttribute(this, "config", KoralPlot.tableDefaults);
                if (conf.cols == null)
                {
                    conf.cols = [];
                    for (var i = 0; i < colNames.length; i++)
                        conf.cols.push(i);
                }

                var tbody = $(this).find("tbody");
                if (tbody.length == 0)
                {
                    tbody = $(this).append("<tbody></tbody>");
                }

                if (tbody.find("th").length == 0) // no user-defined table header
                {
                    var row = $("<tr></tr>");
                    for (var i = 0; i < conf.cols.length; i++)
                    {
                        row.append($("<th></th>").text(colNames[conf.cols[i]]));
                    }
                    tbody.append(row);
                }

                var f = conf.toFixed != null;
                for (var i = 0; i < data.length; i++)
                {
                    var row = $("<tr></tr>");
                    var e = data[i];
                    for (var j = 0; j < conf.cols.length; j++)
                    {
                        var x = e[colNames[conf.cols[j]]];
                        var isNum = !isNaN(x);

                        if (f && isNum)
                        {
                            x = Number(x).toFixed(conf.toFixed);
                        }

                        var col = $("<td></td>").text(x);
                        if (isNum)
                            col.toggleClass("number", true);
                        row.append(col);
                    }
                    tbody.append(row);
                }
            });
            
            var initPlot = function (index, value)
            {
                var conf = Koral.getJsonDataAttribute(this, "config", KoralPlot.plotDefaults);
                var svg = d3.select($(this).get(0));
                var isInset = $(this).prop("tagName").toLowerCase() !== "svg";
                var gsWithUrl = $(this).find("g[data-url]").filter((i, e) =>  isInset || $(e).parents(".inset").length == 0);
                var gsWithConf = $(this).find("g[data-config]").filter((i, e) =>  isInset || (!$(e).hasClass("inset") && $(e).parents(".inset").length == 0));

                var minx = 0;
                var maxx = 0;
                var miny = 0;
                var maxy = 0;
                gsWithUrl.each(function (i, v)
                {
                    var url = $(this).data("url");

                    var drawConf = Koral.getJsonDataAttribute(this, "config", KoralPlot.plotDrawDefaults);

                    var data = urls[url];
                    var colNames = Object.getOwnPropertyNames(data[0]);

                    if (conf.xLabel == null)
                    {
                        conf.xLabel = colNames[drawConf.xData];
                    }
                    if (conf.yLabel == null)
                    {
                        conf.yLabel = colNames[drawConf.yData];
                    }

                    for (var i = 0; i < data.length; i++)
                    {
                        var e = data[i];
                        var x = e[colNames[drawConf.xData]];
                        minx = Math.min(x, minx);
                        maxx = Math.max(x, maxx);

                        var y = e[colNames[drawConf.yData]];

                        miny = Math.min(y, miny);
                        maxy = Math.max(y, maxy);
                    }
                });
                if (conf.xTransform == null)
                    conf.xTransform = {type: "linear", min: minx, max: maxx};
                if (conf.xTransform.type == "linear")
                {
                    if (conf.xTicks == null)
                        conf.xTicks = [conf.xTransform.min, conf.xTransform.max];
                    if (conf.xTickLabels == null)
                    {
                        conf.xTickLabels = [];
                        for (var i = 0; i < conf.xTicks.length; i++)
                            conf.xTickLabels[i] = "" + conf.xTicks[i];
                    }
                    conf.xTransform = d3.scaleLinear().domain([conf.xTransform.min, conf.xTransform.max]);
                } else if (conf.xTransform.type == "log")
                {
                    if (conf.xTransform.min == 0) conf.xTransform.min = conf.xTransform.max >= 1 ? 1e-5 : conf.xTransform.max/1000.0; // prevent log10(0)
                    if (!(conf.xTicks != null && conf.xTickLabels != null))
                    {
                        var t = KoralPlot.logTicks(conf.xTransform.min, conf.xTransform.max);
                        conf.xTicks = t.ticks;
                        conf.xTickLabels = t.tickLabels;
                    }
                    conf.xTransform = d3.scaleLog().domain([conf.xTransform.min, conf.xTransform.max]);
                }

                if (conf.yTransform == null)
                    conf.yTransform = {type: "linear", min: miny, max: maxy};
                if (conf.yTransform.type == "linear")
                {
                    if (conf.yTicks == null)
                        conf.yTicks = [conf.yTransform.min, conf.yTransform.max];
                    if (conf.yTickLabels == null)
                    {
                        conf.yTickLabels = [];
                        for (var i = 0; i < conf.yTicks.length; i++)
                            conf.yTickLabels[i] = "" + conf.yTicks[i];
                    }
                    conf.yTransform = d3.scaleLinear().domain([conf.yTransform.min, conf.yTransform.max]);
                } else if (conf.yTransform.type == "log")
                {
                    if (conf.yTransform.min == 0) conf.yTransform.min = conf.yTransform.max >= 1 ? 1e-5 : conf.yTransform.max/1000.0; // prevent log10(0)
                    if (!(conf.yTicks != null && conf.yTickLabels != null))
                    {
                        var t = KoralPlot.logTicks(conf.yTransform.min, conf.yTransform.max);
                        conf.yTicks = t.ticks;
                        conf.yTickLabels = t.tickLabels;	
                    }                
                    conf.yTransform = d3.scaleLog().domain([conf.yTransform.min, conf.yTransform.max]);
                }
                
                if (!isInset)
                {
                    var width = conf.left + conf.plotWidth + conf.right;
                    var height = conf.top + conf.plotHeight + conf.bottom;
                	svg.attr("width", width * KoralPlot.svgScale).attr("height", height * KoralPlot.svgScale).attr("viewBox", (-conf.left) + " " + (-conf.top) + " " + width + " " + height);
                }


                // debug
                //svg.append("rect").attr("style", "fill:none;stroke:#000000;stroke-width:1px;").attr("stroke-dasharray", "5,5").attr("x", 0).attr("y", 0).attr("width", conf.plotWidth).attr("height", conf.plotHeight);
                //svg.append("rect").attr("style", "fill:none;stroke:#000000;stroke-width:1px;").attr("stroke-dasharray", "5,5").attr("x", -conf.left).attr("y", -conf.top).attr("width", width).attr("height", height);
                //svg.append("rect").attr("style", "fill:none;stroke:#000000;stroke-width:1px;").attr("stroke-dasharray", "5,5").attr("x", -conf.left + 12).attr("y", -conf.top + 12).attr("width", width - 24).attr("height", height - 24);


                if (conf.plotBorder) svg.append("rect").classed("plotStroke", true).attr("x", -conf.plotMargin).attr("y", -conf.plotMargin)
                        .attr("width", (conf.plotWidth + 2 * conf.plotMargin)).attr("height", (conf.plotHeight + 2 * conf.plotMargin));
                if (conf.xLabel) svg.append("text").text(conf.xLabel).classed("plotText axis", true).attr("x", conf.plotWidth / 2.0).attr("y", conf.plotHeight + 49).attr("text-anchor", "middle"); // 52
                if (conf.yLabel) svg.append("text").text(conf.yLabel).classed("plotText axis", true).attr("text-anchor", "middle").attr("x", 0).attr("y", 0)
                        .attr("transform", "translate(" + (-38) + "," + (conf.plotHeight / 2) + ") rotate(-90)"); // -41


                if (conf.plotBorder)
                {
                    var xaxis = svg.append("g").attr("transform", "translate(0 " + (conf.plotHeight + conf.plotMargin) + ")");
                  
                    for (var i = 0; i < conf.xTicks.length; i++)
                    {
                        var tick = conf.xTicks[i];
                        var label = conf.xTickLabels[i];

                        var norm = conf.xTransform(tick);
                        var screen = norm * conf.plotWidth;
                        
                        var tl = label != null && label.length > 0 ? conf.tickHalfLength : conf.tickHalfLengthSmall;
                        xaxis.append("line").classed("plotStroke", true).attr("x1", screen).attr("x2", screen).attr("y1", -tl).attr("y2", tl);

                        if (label != null && label.length > 0)
                        { 
                        	if (conf.xTickLabelAngle == 0)
                        	{
                        		xaxis.append("text").text(label).classed("plotText tick", true).attr("text-anchor", "middle").attr("x", screen).attr("y", 18);
                        	}
                        	else
                        	{
                        		var x = screen;
                        		var y = 12;
                        		xaxis.append("text").text(label).classed("plotText tick", true)
                        		.attr("dominant-baseline", "central")
                        		.attr("text-anchor", "end")
                        		.attr("x", x).attr("y", y)
                        		.attr("transform", "rotate(-" + conf.xTickLabelAngle + "," + x + "," + y + ")");
                        	}
                        	
                            
                        }
                    }
                    var yaxis = svg.append("g").attr("transform", "translate(" + (-conf.plotMargin) + "," + conf.plotHeight + ") rotate(-90)");
                    for (var i = 0; i < conf.yTicks.length; i++)
                    {
                        var tick = conf.yTicks[i];
                        var label = conf.yTickLabels[i];

                        var norm = conf.yTransform(tick);
                        var screen = norm * conf.plotHeight;

                        var tl = label != null && label.length > 0 ? conf.tickHalfLength : conf.tickHalfLengthSmall;
                        yaxis.append("line").classed("plotStroke", true).attr("x1", screen).attr("x2", screen).attr("y1", -tl).attr("y2", tl);

                        if (label != null && label.length > 0)
                        {
                            yaxis.append("text").text(label).classed("plotText tick", true).attr("text-anchor", "middle").attr("x", screen).attr("y", -8);
                        }
                    }                    
                }
                
                // plots                
                gsWithUrl.each(function (i, v)
                {
                    var url = $(this).data("url");

                    var drawConf = Koral.getJsonDataAttribute(this, "config", KoralPlot.plotDrawDefaults);

                    var data = urls[url];
                    
                    var g = d3.select($(domPart).get(0));
                    g.append("title").text(url);

                    KoralPlot.draw(this, conf, drawConf, data);
                });

                // legends
                gsWithConf.each(function (i, v)
                {
                    var drawConf = Koral.getJsonDataAttribute(this, "config", KoralPlot.plotDrawDefaults);
                    if (drawConf.colorLegend && drawConf.colorMapper)
                    {
                        var g = svg.append("g");
                        var bb = g.append("rect");
                        var gl = g.append("g");

                        var lineHeight = 18;
                        gl.append("text").text(drawConf.colorLegend.label).classed("plotText legendText", true).attr("x", 5).attr("y", 16);

                        var cmid = "cm_" + Koral.uuid();
                        var defs = svg.insert("defs", ":first-child");
                        var lg = defs.append("linearGradient").attr("id", cmid).attr("y1", "100%").attr("x2", "0%").attr("y2", "0%");
                        var inp = drawConf.colorMapper.in;
                        var maxIn = inp[inp.length - 1];
                        var minIn = inp[0];
                        for (var i = 0; i < inp.length; i++)
                        {
                            var o = (inp[i] - minIn) / (maxIn - minIn) * 100;
                            lg.append("stop").attr("stop-color", drawConf.colorMapper.out[i]).attr("offset", o + "%");
                        }
                        gl.append("rect").attr("x", 5).attr("y", 30).attr("width", 10).attr("height", 2 * lineHeight)
                                .attr("fill", "url(#" + cmid + ")");

                        gl.append("text").text("" + maxIn).classed("plotText legendText", true).attr("x", 20).attr("y", 16 + lineHeight);
                        gl.append("text").text("" + minIn).classed("plotText legendText", true).attr("x", 20).attr("y", 16 + 3 * lineHeight);

                        var b = gl.node().getBBox();
                        var w = b.width + b.x + 5;
                        var h = b.height + b.y + 5;
                        bb.attr("x", 0).attr("y", 0).attr("width", w).attr("height", h).classed("colorLegendBox", true);

                        var x = (conf.plotWidth - w) * drawConf.colorLegend.horiz;
                        var y = (conf.plotHeight - h) * drawConf.colorLegend.vert;
                        g.attr("transform", "translate(" + x + "," + y + ")");
                    }
                });

                if (conf.legend)
                {
                    var g = svg.append("g");
                    var bb = g.append("rect");
                    var gl = g.append("g");

                    var lineHeight = 18;
                    var legendIndex = 0;
                    if (conf.legend.label != null)
                    {
                        gl.append("text").text(conf.legend.label).classed("plotText legendText", true).attr("x", 5).attr("y", lineHeight * legendIndex + 16);
                        legendIndex++;
                    }

                    gsWithConf.each(function (i, v)
                    {
                        var drawConf = Koral.getJsonDataAttribute(this, "config", KoralPlot.plotDrawDefaults);


                        if (drawConf.label != null && (drawConf.type === "points" || drawConf.type === "line" || drawConf.type === "cross"))
                        {
                            gl.append("text").text(drawConf.label).classed("plotText legendText", true).attr("x", 24).attr("y", lineHeight * legendIndex + 16);

                            var y = lineHeight * legendIndex + 11;
                            if (drawConf.type === "points")
                            {
                                gl.append("circle").attr("fill", drawConf.color).attr("r", drawConf.size).attr("cx", 12).attr("cy", y);
                            } 
                            else if (drawConf.type === "line")
                            {
                                gl.append("path").attr("d", "M5 " + y + " L19 " + y).attr("style", "fill:none; stroke:" + drawConf.color + "; stroke-width:" + drawConf.size + "px");
                            }
                            else if (drawConf.type === "cross")
                            {
                                gl.append("path").attr("d", "M9 " + y + " L15 " + y).attr("style", "fill:none; stroke:" + drawConf.color + "; stroke-width:" + drawConf.size + "px");
                                gl.append("path").attr("d", "M12 " + (y-3) + " L12 " + (y+3)).attr("style", "fill:none; stroke:" + drawConf.color + "; stroke-width:" + drawConf.size + "px");
                            }

                            legendIndex++;
                        }
                    });

                    if (legendIndex > 0)
                    {
                        var b = gl.node().getBBox();
                        var w = b.width + b.x + 5;
                        var h = b.height + b.y + 5;
                        bb.attr("x", 0).attr("y", 0).attr("width", w).attr("height", h).classed("legendBox", true);

                        var x = (conf.plotWidth - w) * conf.legend.horiz;
                        var y = (conf.plotHeight - h) * conf.legend.vert;
                        g.attr("transform", "translate(" + x + "," + y + ")");
                    }
                }
            };

            part.find("svg.plot").each(initPlot);
            part.find("svg.plot").each((index, el) => {
            	$(el).find("g.inset").each(initPlot);
            });
            
            // step 4: arrange figures
            part.find("figure").each(function (index, value)
            {
                var children = $(this).children("svg, img, object");
                if (children.length > 1)
                {
                    var index = 0;
                    children.each(function (i, v)
                    {
                        $(this).wrap("<div style='float:left'></div>");

                        if ($(this).prop("tagName") === "svg")
                        {
                            var svg = d3.select($(this).get(0));
                            var letter = String.fromCharCode("A".charCodeAt(0) + index);
                            
                            var xOffset = 8;
                            var yOffset = 24;
                            var viewBox = this.getAttribute("viewBox");
                            if (viewBox != null)
                            {
                            	var v = viewBox.split(" ");
                            	if (v.length == 4)
                            	{
                                    xOffset += parseInt(v[0]);
                            		yOffset += parseInt(v[1]);
                            	}           	
                            }

                            svg.append("text").text(letter).classed("figureLetter", true).attr("x", xOffset).attr("y", yOffset);
                        } else
                        {
                            var letter = String.fromCharCode("a".charCodeAt(0) + index);
                            $(this).parent().append("<p style='margin-left: 3rem'>(" + letter + ")</p>");
                        }
                        index++;
                    });
                    $(this).children("figcaption").css("clear", "left");
                }
            });
        });
    },
    
    filterRows: function(drawConf, data)
    {
    	if (drawConf.rows != null)
        {
    		var colNames = Object.getOwnPropertyNames(data[0]);
            var d = [];
            var col = colNames[drawConf.rows.column];
            for (var i = 0; i < data.length; i++)
            {
                var e = data[i];
                if (e[col] === drawConf.rows.equals)
                {
                    d.push(e);
                }
            }
            data = d;
        }
    	return data;
    },
    
    draw: function (domPart, conf, drawConf, data)
    {
        var g = d3.select(domPart);
        if (drawConf.color) g.attr("fill", drawConf.color);
        
        data = KoralPlot.filterRows(drawConf, data);
    	
    	var drawFunc = null;
    	switch (drawConf.type)
    	{
    	case "points": drawFunc = KoralPlot.drawPoints; break;
    	case "line": drawFunc = KoralPlot.drawLine; break;
    	case "errorBars": drawFunc = KoralPlot.drawErrorBars; break;
    	case "errorBarsAbs": drawFunc = KoralPlot.drawErrorBarsAbs; break;
    	case "area": drawFunc = KoralPlot.drawArea; break;
    	}
    	
    	if (drawFunc && data.length > 0) drawFunc(domPart, conf, drawConf, data);
    },
    
    drawPoints: function(domPart, conf, drawConf, data)
    {
    	var g = d3.select(domPart);
    	var colNames = Object.getOwnPropertyNames(data[0]);
        var cinterp = drawConf.colorMapper != null ? d3.scaleLinear().domain(drawConf.colorMapper.in).interpolate(d3.interpolateRgb).range(drawConf.colorMapper.out) : null;

        for (var i = 0; i < data.length; i++)
        {
            var e = data[i];
            var x = Number(e[colNames[drawConf.xData]]);
            var y = Number(e[colNames[drawConf.yData]]);
            var z = Number(e[colNames[drawConf.zData]]);
            if (isNaN(x) || isNaN(y))
                continue;
            var normx = conf.xTransform(x);
            var normy = conf.yTransform(y);
            if (normx < 0 || normx > 1 || normy < 0 || normy > 1 || isNaN(normx) || isNaN(normx))
                continue;

            var t = null;
            if (drawConf.x2Data != null && drawConf.y2Data != null)
            {
                var x0 = x;
                var x1 = Number(e[colNames[drawConf.x2Data]]);
                var y0 = y;
                var y1 = Number(e[colNames[drawConf.y2Data]]);
                var normx0 = conf.xTransform(x0);
                var normx1 = conf.xTransform(x1);
                var normy0 = conf.yTransform(y0);
                var normy1 = conf.yTransform(y1);
                var screenx0 = normx0 * conf.plotWidth;
                var screenx1 = normx1 * conf.plotWidth;
                var screeny0 = (1.0 - normy0) * conf.plotHeight;
                var screeny1 = (1.0 - normy1) * conf.plotHeight;

                if (drawConf.shape === "rect")
                {
                    t = g.append("rect").attr("width", Math.abs(screenx1 - screenx0)).attr("height",
                            Math.abs(screeny0 - screeny1)).attr("x", Math.min(screenx0, screenx1)).attr("y", Math.min(screeny0, screeny1));
                }
            } else if (drawConf.realSize == null)
            {
                var screenx = normx * conf.plotWidth;
                var screeny = (1.0 - normy) * conf.plotHeight;
                if (isNaN(screenx) || isNaN(screeny))
                    continue;

                if (drawConf.shape === "circle")
                {
                    t = g.append("circle").attr("r", drawConf.size).attr("cx", screenx).attr("cy", screeny);

                } else if (drawConf.shape === "rect")
                {
                    t = g.append("rect").attr("width", drawConf.size).attr("height", drawConf.size).attr("x", screenx - drawConf.size / 2.0).attr("y", screeny - drawConf.size / 2.0);
                }
            } else
            {
                var x0 = x - drawConf.realSize[0] / 2.0;
                var x1 = x + drawConf.realSize[0] / 2.0;
                var y0 = y - drawConf.realSize[1] / 2.0;
                var y1 = y + drawConf.realSize[1] / 2.0;
                var normx0 = conf.xTransform(x0);
                var normx1 = conf.xTransform(x1);
                var normy0 = conf.yTransform(y0);
                var normy1 = conf.yTransform(y1);
                var screenx0 = normx0 * conf.plotWidth;
                var screenx1 = normx1 * conf.plotWidth;
                var screeny0 = (1.0 - normy0) * conf.plotHeight;
                var screeny1 = (1.0 - normy1) * conf.plotHeight;

                if (drawConf.shape === "rect")
                {
                    t = g.append("rect").attr("width", screenx1 - screenx0).attr("height", screeny0 - screeny1).attr("x", screenx0).attr("y", screeny1);
                }
            }

            if (t != null && cinterp != null)
                t.attr("fill", cinterp(z));
        }
    },
    
    drawLine: function(domPart, conf, drawConf, data)
    {
    	var g = d3.select(domPart);
    	var colNames = Object.getOwnPropertyNames(data[0]);
        var startIndex = 0;
        while (startIndex < data.length)
        {
            var sb = [];
            var index = 0;
            for (; startIndex < data.length; startIndex++)
            {
                var e = data[startIndex];
                var x = e[colNames[drawConf.xData]];
                var y = e[colNames[drawConf.yData]];
                var normx = conf.xTransform(x);
                var normy = conf.yTransform(y);
                if (!isFinite(x) || !isFinite(y) || !isFinite(normx) || !isFinite(normy) || normx < 0 || normx > 1 || normy < 0 || normy > 1)
                {
                    startIndex++;
                    break; // start new line
                }
                var screenx = normx * conf.plotWidth;
                var screeny = (1.0 - normy) * conf.plotHeight;

                sb.push(index == 0 ? "M" : " L");
                sb.push(screenx + " " + screeny);
                index++;
            }
            g.append("path").attr("d", sb.join("")).attr("style", "fill:none; stroke:" + drawConf.color + "; stroke-width:" + drawConf.size + "px");
        }
    },
    
    drawErrorBars: function(domPart, conf, drawConf, data)
    {
    	var g = d3.select(domPart);
    	var colNames = Object.getOwnPropertyNames(data[0]);
        var css = "fill:none; stroke:" + drawConf.color + "; stroke-width:" + drawConf.errorBarThickness + "px";
        for (var i = 0; i < data.length; i++)
        {
            var e = data[i];
            var x = Number(e[colNames[drawConf.xData]]);
            var y = Number(e[colNames[drawConf.yData]]);
            var z = Number(e[colNames[drawConf.zData]]);
            if (isNaN(z))
                continue;

            var normx = conf.xTransform(x);

            var normy = conf.yTransform(y);
            var normyL = conf.yTransform(y - z);
            var normyU = conf.yTransform(y + z);

            if (normx < 0 || normx > 1 || normy < 0 || normy > 1)
                continue;
            var screenx = normx * conf.plotWidth;

            var screenyL = (1.0 - normyL) * conf.plotHeight;
            var screenyU = (1.0 - normyU) * conf.plotHeight;

            g.append("path").attr("d", "M" + screenx + " " + screenyL + " L" + screenx + " " + screenyU).attr("style", css);

            if (drawConf.errorCrossBarWidth > 0)
            {
                var w = drawConf.errorCrossBarWidth / 2.0;
                g.append("path").attr("d", "M" + (screenx - w) + " " + screenyL + " L" + (screenx + w) + " " + screenyL).attr("style", css);
                g.append("path").attr("d", "M" + (screenx - w) + " " + screenyU + " L" + (screenx + w) + " " + screenyU).attr("style", css);
            }
        }
    },
    
    drawErrorBarsAbs: function(domPart, conf, drawConf, data)
    {
    	var g = d3.select(domPart);
    	var colNames = Object.getOwnPropertyNames(data[0]);
        var css = "fill:none; stroke:" + drawConf.color + "; stroke-width:" + drawConf.errorBarThickness + "px";
        for (var i = 0; i < data.length; i++)
        {
            var e = data[i];
            var x = Number(e[colNames[drawConf.xData]]);
            var y = Number(e[colNames[drawConf.yData]]);
            var z1 = Number(e[colNames[drawConf.zData]]);
            var z2 = Number(e[colNames[drawConf.z2Data]]);
            if (isNaN(z1) || isNaN(z2))
                continue;

            var normx = conf.xTransform(x);

            var normy = conf.yTransform(y);
            var normyL = conf.yTransform(Math.min(z1, z2));
            var normyU = conf.yTransform(Math.max(z1, z2));

            if (normx < 0 || normx > 1 || normy < 0 || normy > 1)
                continue;
            var screenx = normx * conf.plotWidth;

            var screenyL = (1.0 - normyL) * conf.plotHeight;
            var screenyU = (1.0 - normyU) * conf.plotHeight;

            g.append("path").attr("d", "M" + screenx + " " + screenyL + " L" + screenx + " " + screenyU).attr("style", css);

            if (drawConf.errorCrossBarWidth > 0)
            {
                var w = drawConf.errorCrossBarWidth / 2.0;
                g.append("path").attr("d", "M" + (screenx - w) + " " + screenyL + " L" + (screenx + w) + " " + screenyL).attr("style", css);
                g.append("path").attr("d", "M" + (screenx - w) + " " + screenyU + " L" + (screenx + w) + " " + screenyU).attr("style", css);
            }
        }
    },
    
    drawArea: function(domPart, conf, drawConf, data)
    {
    	var g = d3.select(domPart);
    	var colNames = Object.getOwnPropertyNames(data[0]);
        var sb = [];
        var index = 0;
        for (var i = 0; i < data.length * 2; i++)
        {
            var j = i >= data.length ? data.length - 1 - (i - data.length) : i;

            var e = data[j];
            var x = Number(e[colNames[drawConf.xData]]);
            var y = i >= data.length ? Number(e[colNames[drawConf.zData]]) : Number(e[colNames[drawConf.yData]]);
            if (!isFinite(x) || !isFinite(y))
                continue;
            var normx = conf.xTransform(x);
            var normy = conf.yTransform(y);
            var screenx = normx * conf.plotWidth;
            var screeny = (1.0 - normy) * conf.plotHeight;

            sb.push(index == 0 ? "M" : " L");
            sb.push(screenx + " " + screeny);

            index++;
        }
        sb.push(" Z");
        g.append("path").attr("d", sb.join("")).attr("style", "fill:" + drawConf.color + "; stroke:none");
    }
};

KoralInternal.importScripts(KoralInternal.init);