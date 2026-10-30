set pagination off
set confirm off
set debuginfod enabled off
set breakpoint pending on
set print thread-events off
target remote 127.0.0.1:31359
source research/dkii-lava/trace_lava.py
continue
detach
quit
