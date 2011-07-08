gpxstat
-------

mru, 2011/07/05


Abstract
--------

The application can load GPX files and calculate basic statistics for every
Track segment, as well as showing several plots.


Program execution
-----------------

No installation is required.

After downloading the application, extract all content to some directory.
In the 'dist' directory, execute:

     java -jar gpxstat.jar


Screenshot
----------

![Screenshot 1](https://github.com/mru00/gpxstat/raw/master/doc/screenshot1.png)


Implementation Details
----------------------

Uses JAXB to generate GPX parsing code.

    sudo apt-get install xmlbeans
    scomp -out gpx.jar gpx.xsd

Uses JFreeChart to plot graphs.

Uses Swing as Application Framework.

Uses JMapViewer http://wiki.openstreetmap.org/wiki/JMapViewer to display Maps

Developed with NetBeans 6.9.