# das2java

This is the Java Data Analysis library used by [Autoplot](http://autoplot.org/), 
[MIDL](http://sd-www.jhuapl.edu/MIDL/) and small 
[das2 apps](https://das2.org/demo-apps.html).  It is based on Java/Swing, where
the DasCanvas is embedded within the GUI and by adding DasPlot objects and
data Renderers.  Mouse events are converted
into more abstract events with science coordinates, allowing rich interactive graphics tools to be built.
This repository contains all
history back to 2003 at the start of the [das2 project](https://das2.org/das2overview2020piker.mp4).

These libraries were originally created in support of the 
[Cassini](https://www.jpl.nasa.gov/missions/cassini-huygens) mission
though over the years they have become useful in many other contexts.
Since 2008 many updates have been provided by the Autoplot project, including
a more flexible data model defined by the 
[QDataSet](http://autoplot.org/QDataSet) interface.

This code contained here has traditionally been called dasCore, but since
that is the name of one of the sub-project directories within the repository,
and to distinguish it from core das2 support in other languages, it is
referred to as das2java here.


Das2java provides client-side data operations such as:

 * slicing
 * zooming
 * export original or reduced resolution and
 * publication-quality printing
 
as well as parsers for reading [das2 streams](
https://github.com/das-developers/das2docs/tree/master/das2.2.2-ICD) and 
[QStreams](http://autoplot.org/qstream).

Typically these libraries are used in 
[Netbeans](https://netbeans.apache.org/) 
projects, so ant files are provided for building the jars.


