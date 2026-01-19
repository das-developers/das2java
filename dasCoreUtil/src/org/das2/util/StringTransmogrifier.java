# See also https://github.com/jbfaden/Utilities/blob/main/src/com/cottagesystems/JGrep.java

from java.util.regex import Pattern
from org.das2.util import StringTransmogrifier

# java -jar autoplot.jar --headless --script=https://github.com/autoplot/dev/blob/master/demos/2021/20210108/transmogrify.jy  'pj(\d+)_(.+)_(n|s).(dat)'  '$1-$2-$3'
# uptime | java -jar autoplot.jar --headless --script=https://github.com/autoplot/dev/blob/master/demos/2021/20210108/transmogrify.jy  '.+up (\d+) days.+(\d+) user.+load average: ([\d\.]+), ([\d\.]+), ([\d\.]+)'  '$1 $2 $3 $4 $5'

setScriptLabel('Transmography')
setScriptDescription("""Transmogriphy command takes a regular expression and a
format specification and converts each line of the input (on stdin) by matching
it and then formatting it. <p>java -jar autoplot.jar --script=transmogrify.jy mod_(\d+).txt '$1'
Note Autoplot/Das2 now has a StringTransmogriphy class which can be used.
See https://github.com/das-developers/das2java/blob/main/dasCoreUtil/src/org/das2/util/StringTransmogrifier.java
""")
inp= getParam( 'arg_1', 'pj(\d+)_(.+)_(n|s).(dat)', 'The regular expression' )
outp= getParam( 'arg_2', '$1 $2 $3', 'print these to stdout' )
ins= getParam( 'arg_3', '', 'line to run through, or empty string (the default) for stdin', { 'examples':['pj1_5_n.dat',''] } )

import sys

st= StringTransmogrifier( inp, outp )
if ins!='':        
    print st.transmogrify( ins )
else:
    for line in sys.stdin:
        print st.transmogrify(line.strip())

