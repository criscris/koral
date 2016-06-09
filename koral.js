function loadScripts() 
{
	document.writeln("<script async src='https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-MML-AM_CHTML'></script>");
	document.writeln("<script async src='https://ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js'></script>");
	document.writeln("<script async src='https://koral-xyz.github.io/lib/BibTex.min.js'></script>");
	document.writeln("<script async src='https://koral-xyz.github.io/lib/papaparse.min.js'></script>");
	document.writeln("<script async src='https://d3js.org/d3.v3.min.js'></script>");
}

function init() 
{
    MathJax.Hub.Config({tex2jax: {inlineMath: [['$','$'], ['\\(','\\)']]}});
    numberLinks();
    loadReferences();
    loadPlots();
};

function numberLinks()
{
	figureIDtoNumber = {};
	$("figure").each(function (index, value) 
	{ 
		var id = $(this).attr("id");
		if (id != null)
		{
			figureIDtoNumber[id] = index + 1;
		}
	});

	equationIDtoNumber = {};
	$(".equation").each(function (index, value) 
	{ 
		var id = $(this).attr("id");
		if (id != null)
		{
			equationIDtoNumber[id] = index + 1;
		}
	});

	$("a").each(function (index, value) 
	{ 
		var link = $(this).attr('href');
		if (link == null) return;
		if (!link.startsWith("#")) return;
		var targetID = link.substring(1);
		// figure
		var number = figureIDtoNumber[targetID];
		if (number != null)
		{
			$(this).text("Fig. " + number);
		}
		// equation
		else
		{
			number = equationIDtoNumber[targetID];
			if (number != null)
			{
				$(this).text("Eq. (" + number + ")");
			}
		}
	});
};

function loadReferences()
{
	$("div.references").each(function (index, value) 
	{ 
		var bibhtml = $("<ol></ol>").appendTo($(this));

		function parseBib(data)
		{
			var bibtex = new BibTex();
		   	bibtex.content = data;  
		   	bibtex.parse();
		   	bibIDtoBib = {};
		   	for (var i=0; i<bibtex.data.length; i++)
		   	{
		   		var bib = bibtex.data[i];
		   		bibIDtoBib[bib.cite] = bib;
		   	}

		   	bibIDtoPosition = {};
		   	usedBibs = [];
		   	$("a").each(function (index, value) 
			{ 
				var link = $(this).attr('href');
				if (link == null) return;
				if (!link.startsWith("#")) return;
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
				}
			});

			for (var i=0; i<usedBibs.length; i++)
		   	{
		   		var bib = usedBibs[i];
		   		var authors = "";
		   		for (var j=0; j<bib.author.length; j++)
		   		{
		   			var a = bib.author[j];
		   			authors += a.first + " " + a.last;
		   			if (j < bib.author.length - 1)
		   			{
		   				if (j == bib.author.length - 2) authors += " and ";
		   				else authors += ", ";
		   			}
		   		}
		   		var journal = bib.journal != null ? bib.journal + "." : "";
		   		bibhtml.append("<li id='" + bib.cite + "'>"+ authors + " (" + bib.year + "): " + bib.title + ". " + journal + "</li>");
		   	}
		}

		var d = $(this).data("source");
		if (d != null)
		{
			parseBib(d);
		}
		else
		{
			var url = $(this).data("url");
			jQuery.get(url, parseBib);
		}
	});
}

function loadPlots()
{
	function logTicks(min, max)
	{
		var superscripts = [
			"\u2070",  // 0
			"\u00B9",  // 1
			"\u00B2",  // 2  ...
			"\u00B3",
			"\u2074",
			"\u2075",
			"\u2076",
			"\u2077",
			"\u2078",
			"\u2079" ]; // 9
		var superscriptMinus = "\u207B";
		function p(val)
		{
			var pow10 = Math.floor(Math.log10(val));
			return [val/Math.pow(10, pow10), pow10]; 
		}

		var minp = p(min);
		var maxp = p(max);

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
				var ea = Math.abs(current10);
				l = "10" + (current10 < 0 ? superscriptMinus : "") + 
						(ea >= 100 ? superscripts[(ea/100) % 10] : "") +
						(ea >= 10 ? superscripts[(ea/10) % 10] : "") + 
						superscripts[ea % 10];
			}
			tickLabels.push(l);
			
			currentScale++;
			if (currentScale == 10)
			{
				currentScale = 1;
				current10++;
			}
		}

		return { ticks: ticks, tickLabels: tickLabels};
	}

	function uuid()
	{
		return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) 
		{
		    var r = crypto.getRandomValues(new Uint8Array(1))[0]%16|0, v = c == 'x' ? r : (r&0x3|0x8);
		    return v.toString(16);
		});
	}

	var plotDefaults = { 
		plotWidth: 240, // size of actual plot area
		plotHeight: 240,

		// examples:
		//xLabel: "x",
		//yLabel: "y",
		//xTransform: { type: "linear", min: 0, max: 1}   // { type:"log", min: 1e-7 max: 1}  //d3.scale.linear().domain([0, 1]),
		//yTransform: d3.scale.linear().domain([0, 1]),
		//xTicks:      [0,   0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1],
		//xTickLabels: ["0", "",  "",  "",  "", ".5", "",  "",  "",  "", "1"],
		//yTicks:      [0,   0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1],
		//yTickLabels: ["0", "",  "",  "",  "", ".5", "",  "",  "",  "", "1"],

		plotBorder: true,
		left: 56,
		right: 24,
		top: 24,
		bottom: 56,
		tickHalfLength: 4,
		tickHalfLengthSmall: 2,
		plotMargin: 12,

		legend: true
	};

	var plotDrawDefaults = {
		xData: 0,
		yData: 1,
		zData: 2,
		type: "points", // line, errorBars
		size: 2.0,
		color: "rgb(0, 0, 255)",
		errorBarThickness: 1.0,
		errorCrossBarWidth: 5.0,
		label: undefined // for legend
	}

	// step 0: arrange figures
	$("figure").each(function (index, value) 
	{
		var children = $(this).children("svg, img, object");
		if (children.length > 1)
		{
			var firstLetter = "a".charCodeAt(0);
			var index = 0;
			children.each(function (i, v) 
			{
				$(this).wrap(index == 0 ? "<div style='float:left'></div>" : "<div></div>");
				$(this).parent().append("<p style='margin-left: 3rem'>(" + String.fromCharCode(firstLetter + index) + ")</p>");
				index++;
			});
		}
	});

	// step 1: load all csv data
	var urls = {};
	$("svg > [data-url]").each(function (index, value) 
	{
		var url = $(this).data("url");
		urls[url] = true;
	});
	$("svg > [data-source]").each(function (index, value) 
	{
		var dataSerialized = $(this).data("source");
		var lines = dataSerialized.match(/[^\r\n]+/g);
		var	linesTrimmed = []
		for (var i=0; i<lines.length; i++) 
		{
			var l = lines[i].trim();
			if (l.length > 0) linesTrimmed.push(l);
		}
		var data = Papa.parse(linesTrimmed.join("\n"), { header: true, skipEmptyLines: true }).data;

		var url = $(this).data("url");
		if (url == null)
		{
			url = uuid();
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
		        success: function(data) 
						{
							var csv = Papa.parse(data, { header: true, skipEmptyLines: true });
							urls[this.url] = csv.data;
						},
		        async: true
		}));
	}

	// step 2: draw plots
	$.when.apply($, ajaxes).done(function() // // it's ok for ajaxes to be empty array
	{
		$("svg.plot").each(function (index, value)
		{
			var options = $(this).data("config");
			var conf = $.extend({}, plotDefaults, options == null ? {} : options);

			var minx = 0;
			var maxx = 0;
			var miny = 0;
			var maxy = 0;
			$(this).find("[data-url]").each(function (i, v) 
			{
	 			var url = $(this).data("url");
				var drawOptions = $(this).data("config");
				var drawConf = $.extend({}, plotDrawDefaults, options == null ? {} : options);
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

				for (var i=0; i<data.length; i++)
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
			if (conf.xTransform == null) conf.xTransform = { type: "linear", min: minx, max: maxx };
			if (conf.xTransform.type == "linear")
			{
				if (conf.xTicks == null) conf.xTicks = [conf.xTransform.min, conf.xTransform.max];
				if (conf.xTickLabels == null)
				{
					conf.xTickLabels = [];
					for (var i=0; i<conf.xTicks.length; i++) conf.xTickLabels[i] = "" + conf.xTicks[i];
				}
				conf.xTransform = d3.scale.linear().domain([conf.xTransform.min, conf.xTransform.max]);
			}
			else if (conf.xTransform.type == "log")
			{
				var t = logTicks(conf.xTransform.min, conf.xTransform.max);
				conf.xTicks = t.ticks;
				conf.xTickLabels = t.tickLabels;

				conf.xTransform = d3.scale.log().domain([conf.xTransform.min, conf.xTransform.max]);
			}

			if (conf.yTransform == null) conf.yTransform = { type: "linear", min: miny, max: maxy };
			if (conf.yTransform.type == "linear")
			{
				if (conf.yTicks == null) conf.yTicks = [conf.yTransform.min, conf.yTransform.max];
				if (conf.yTickLabels == null)
				{
					conf.yTickLabels = [];
					for (var i=0; i<conf.yTicks.length; i++) conf.yTickLabels[i] = "" + conf.yTicks[i];
				}
				conf.yTransform = d3.scale.linear().domain([conf.yTransform.min, conf.yTransform.max]);
			}
			else if (conf.yTransform.type == "log") 
			{
				var t = logTicks(conf.yTransform.min, conf.yTransform.max);
				conf.yTicks = t.ticks;
				conf.yTickLabels = t.tickLabels;
				conf.yTransform = d3.scale.log().domain([conf.yTransform.min, conf.yTransform.max]);
			}


			var width = conf.left + conf.plotWidth + conf.right;
			var height = conf.top + conf.plotHeight + conf.bottom;

			var svg = d3.select($(this).get(0));
			svg.attr("width", width).attr("height", height).attr("viewBox", (-conf.left) + " " + (-conf.top) + " " + width + " " + height);

			// debug
			//svg.append("rect").attr("style", "fill:none;stroke:#000000;stroke-width:1px;").attr("stroke-dasharray", "5,5").attr("x", -conf.left).attr("y", -conf.top).attr("width", width).attr("height", height);
			//svg.append("rect").attr("style", "fill:none;stroke:#000000;stroke-width:1px;").attr("stroke-dasharray", "5,5").attr("x", -conf.left + 12).attr("y", -conf.top + 12).attr("width", width - 24).attr("height", height - 24);


			svg.append("rect").classed("plotStroke", true).attr("x", -conf.plotMargin).attr("y", -conf.plotMargin)
				.attr("width", (conf.plotWidth + 2*conf.plotMargin)).attr("height", (conf.plotHeight + 2*conf.plotMargin));
			svg.append("text").text(conf.xLabel).classed("plotText axis", true).attr("x", conf.plotWidth/2.0).attr("y", conf.plotHeight + 52).attr("text-anchor", "middle");	
			svg.append("text").text(conf.yLabel).classed("plotText axis", true).attr("text-anchor", "middle").attr("x", 0).attr("y", 0)
				.attr("transform", "translate(" + (-41) + "," + (conf.plotHeight/2) + ") rotate(-90)");


			var xaxis = svg.append("g").attr("transform", "translate(0 " + (conf.plotHeight + conf.plotMargin) + ")");
			for (var i=0; i<conf.xTicks.length; i++)
			{
				var tick = conf.xTicks[i];
				var label = conf.xTickLabels[i];

				var norm = conf.xTransform(tick);
				var screen = norm * conf.plotWidth;

				var tl = label != null && label.length > 0 ? conf.tickHalfLength : conf.tickHalfLengthSmall;
				xaxis.append("line").classed("plotStroke", true).attr("x1", screen).attr("x2", screen).attr("y1", -tl).attr("y2", tl);

				if (label != null && label.length > 0)
				{
					xaxis.append("text").text(label).classed("plotText tick", true).attr("text-anchor", "middle").attr("x", screen).attr("y", 18);
				}
			}
			var yaxis = svg.append("g").attr("transform", "translate(" + (-conf.plotMargin) + "," + conf.plotHeight + ") rotate(-90)");
			for (var i=0; i<conf.yTicks.length; i++)
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

			$(this).find("g[data-url]").each(function (i, v) 
			{
	 			var url = $(this).data("url");
				var drawOptions = $(this).data("config");
				var drawConf = $.extend({}, plotDrawDefaults, drawOptions == null ? {} : drawOptions);
				var data = urls[url];

				var colNames = Object.getOwnPropertyNames(data[0]);


				
				var g = d3.select($(this).get(0));
				g.attr("fill", drawConf.color);

				if (drawConf.type === "points")
				{
					for (var i=0; i<data.length; i++)
					{
						var e = data[i];
						var x = e[colNames[drawConf.xData]];
						var y = e[colNames[drawConf.yData]];
						var normx = conf.xTransform(x);
						var normy = conf.yTransform(y);
						if (normx < 0 || normx > 1 || normy < 0 || normy > 1) continue;

						var screenx = normx * conf.plotWidth;
						var screeny = (1.0 - normy) * conf.plotHeight;

						g.append("circle").attr("r", drawConf.size).attr("cx", screenx).attr("cy", screeny);
					}
				}
				else if (drawConf.type === "line")
				{
					var startIndex = 0;
					while (startIndex < data.length)
					{
						var sb = [];
						var index = 0;
						for (; startIndex<data.length; startIndex++)
						{
							var e = data[startIndex];
							var x = e[colNames[drawConf.xData]];
							var y = e[colNames[drawConf.yData]];
							var normx = conf.xTransform(x);
							var normy = conf.yTransform(y);
							if (!isFinite(x) || !isFinite(y) || normx < 0 || normx > 1 || normy < 0 || normy > 1)
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
				}
				else if (drawConf.type == "errorBars")
				{
					var css = "fill:none; stroke:" + drawConf.color + "; stroke-width:" + drawConf.errorBarThickness + "px";
					for (var i=0; i<data.length; i++)
					{
						var e = data[i];
						var x = Number(e[colNames[drawConf.xData]]);
						var y = Number(e[colNames[drawConf.yData]]);
						var z = Number(e[colNames[drawConf.zData]]);

						var normx = conf.xTransform(x);

						var normy = conf.yTransform(y);
						var normyL = conf.yTransform(y - z);
						var normyU = conf.yTransform(y + z);

						if (normx < 0 || normx > 1 || normy < 0 || normy > 1) continue;
						var screenx = normx * conf.plotWidth;
						
						var screenyL = (1.0 - normyL) * conf.plotHeight;
						var screenyU = (1.0 - normyU) * conf.plotHeight;

						g.append("path").attr("d", "M" + screenx + " " + screenyL + " L" + screenx + " " + screenyU).attr("style", css);
					
						if (drawConf.errorCrossBarWidth > 0)
						{
							var w = drawConf.errorCrossBarWidth/2.0;
							g.append("path").attr("d", "M" + (screenx-w) + " " + screenyL + " L" + (screenx+w) + " " + screenyL).attr("style",  css);
							g.append("path").attr("d", "M" + (screenx-w) + " " + screenyU + " L" + (screenx+w) + " " + screenyU).attr("style",  css);	
						}
					}
				}
			});

			if (conf.legend)
			{
				var g = svg.append("g");
				var bb = g.append("rect");
				var gl = g.append("g"); 

				var legendIndex = 0;
				$(this).find("g[data-url]").each(function (i, v) 
				{
		 			var url = $(this).data("url");
					var drawOptions = $(this).data("config");
					var drawConf = $.extend({}, plotDrawDefaults, drawOptions == null ? {} : drawOptions);

					if (drawConf.label != null && (drawConf.type === "points" || drawConf.type === "line"))
					{
						gl.append("text").text(drawConf.label).classed("plotText legendText", true).attr("x", 32).attr("y", 24*legendIndex + 19);

						var y = 24*legendIndex + 13;
						if (drawConf.type === "points")
						{
							gl.append("circle").attr("fill", drawConf.color).attr("r", drawConf.size).attr("cx", 15).attr("cy", y);
						}
						else if (drawConf.type === "line")
						{
							gl.append("path").attr("d", "M5 " + y + " L25 " + y).attr("style", "fill:none; stroke:" + drawConf.color + "; stroke-width:" + drawConf.size + "px");
						}

						legendIndex++;
					}
				});

				if (legendIndex > 0)
				{
					var b = gl.node().getBBox();
					bb.attr("x", (b.x-5)).attr("y", (b.y - 5)).attr("width", (b.width + 10)).attr("height", (b.height + 10)).classed("legendBox", true);

					g.attr("transform", "translate(" + (conf.plotWidth - (b.width + 10)) + ",0)");
				}
			}
		});
    });
}

loadScripts();
window[addEventListener ? 'addEventListener' : 'attachEvent'](addEventListener ? 'load' : 'onload', init)