set terminal png size 1000,1000
set output ARG1.'.png'
set datafile separator ','
plot ARG1.'-'.ARG2.'.log' with lines, ARG1.'-'.ARG3.'.log' with lines
