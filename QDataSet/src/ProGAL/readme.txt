The Java files under here were extracted from the open-source library ProGAL, see
http://www.diku.dk/~rfonseca/ProGAL/ and the GitHub project 
https://github.com/DIKU-Steiner/ProGAL.  The following commands were used to
extract the subset of functions needed for 3-D and 2-D tesselations:

unix>  cd ~/eg/java/ProGAL/src
unix>  javac -cp ../lib/j3dcore.jar:../lib/j3dutils.jar:../lib/vecmath.jar:. ProGAL/geom2d/delaunay/DTWithBigPoints.java 
unix>  javac -cp ../lib/j3dcore.jar:../lib/j3dutils.jar:../lib/vecmath.jar:. ProGAL/geom3d/tessellation/BowyerWatson/RegularTessellation.java
unix>  find . -name '*.class'

This list of class names is converted to .java names, and the files moved over to here.

unix> find . -name '*.class' | sed 's/class/java/g' | grep -v '\$' >  filelist.txt
unix> for i in `cat filelist.txt`; do  mkdir -p /home/jbf/temp/autoplot/QDataSet/src/${i%/*}; done
unix> for i in `cat filelist.txt`; do cp $i /home/jbf/temp/autoplot/QDataSet/src/$i; done

= remove all the java3d stuff=
All main methods are removed.
All toScene methods are removed.